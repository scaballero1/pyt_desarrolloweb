package dao;

import entities.Pedido;
import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de Acceso a Datos (DAO) para la entidad {@link Pedido}.
 * 
 * Forma parte de la Capa 2 (DAO) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Encapsular toda la lógica de acceso a la base de datos para la tabla 'Pedidos'.
 *   - Ejecutar operaciones CRUD mediante JDBC puro con PreparedStatement.
 *   - Manejar excepciones SQL de forma controlada.
 * 
 * Métodos implementados:
 *   - crear(Pedido)           → INSERT INTO Pedidos
 *   - listarActivos()         → SELECT pedidos no cancelados ni entregados
 *   - actualizarEstado(id, e) → UPDATE estado + fecha_ultima_actualizacion
 *   - cancelarPedido(id)      → UPDATE (Soft Delete) → estado = 'CANCELADO'
 * 
 * @author PoliRestaurante
 */
public class PedidoDAO {

    // ==================== ATRIBUTOS ====================

    /** Conexión activa a la base de datos MySQL, obtenida desde DBConnection. */
    private Connection conexion;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor que inicializa la conexión a la base de datos
     * utilizando la clase utilitaria DBConnection del proyecto.
     * 
     * Si la conexión falla, se captura la excepción y se muestra
     * un mensaje de error en consola para facilitar la depuración.
     */
    public PedidoDAO() {
        try {
            this.conexion = DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al obtener la conexión a la base de datos: " 
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== MÉTODOS CRUD ====================

    /**
     * Inserta un nuevo pedido en la tabla 'Pedidos'.
     * 
     * Las fechas {@code fecha_creacion} y {@code fecha_ultima_actualizacion}
     * se asignan automáticamente con la fecha y hora actual del sistema,
     * independientemente de los valores que tenga el objeto Pedido recibido.
     * 
     * El campo {@code mesa_id} se maneja como nullable: si el valor es null,
     * se envía un NULL explícito a la base de datos usando {@link Types#INTEGER}.
     * 
     * @param pedido Objeto {@link Pedido} con los datos del nuevo pedido.
     *               Los campos requeridos son: tipoPedido y meseroId.
     *               El campo mesaId es opcional (puede ser null).
     *               El campo estado es IGNORADO: el DAO fuerza 'PENDIENTE' (regla HU-01).
     * @return {@code true} si el registro fue insertado exitosamente,
     *         {@code false} si ocurrió un error durante la inserción.
     */
    public boolean crear(Pedido pedido) {
        String sql = "INSERT INTO Pedidos (tipo_pedido, mesa_id, mesero_id, estado, "
                   + "fecha_creacion, fecha_ultima_actualizacion) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {

            // Fecha actual del sistema para ambos campos de auditoría
            LocalDateTime ahora = LocalDateTime.now();

            // Parámetro 1: tipo_pedido (VARCHAR, NOT NULL)
            ps.setString(1, pedido.getTipoPedido());

            // Parámetro 2: mesa_id (INT, NULL permitido)
            if (pedido.getMesaId() != null) {
                ps.setInt(2, pedido.getMesaId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            // Parámetro 3: mesero_id (INT, NOT NULL)
            ps.setInt(3, pedido.getMeseroId());

            // Parámetro 4: estado (VARCHAR, NOT NULL)
            // REGLA DE NEGOCIO HU-01: todo pedido nuevo DEBE nacer como 'PENDIENTE'.
            // Se ignora el valor del objeto para garantizar integridad desde el DAO.
            ps.setString(4, "PENDIENTE");

            // Parámetro 5: fecha_creacion (DATETIME) → fecha actual del sistema
            ps.setTimestamp(5, Timestamp.valueOf(ahora));

            // Parámetro 6: fecha_ultima_actualizacion (DATETIME) → fecha actual del sistema
            ps.setTimestamp(6, Timestamp.valueOf(ahora));

            // Ejecutar la inserción y verificar que al menos 1 fila fue afectada
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al crear el pedido: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene la lista de todos los pedidos activos de la tabla 'Pedidos'.
     * 
     * Un pedido se considera "activo" si su estado NO es 'CANCELADO' ni 'ENTREGADO'.
     * Esto permite a las capas superiores (Manager y UI) trabajar únicamente
     * con los pedidos que requieren atención operativa.
     * 
     * Los resultados se ordenan por fecha de creación de forma ascendente (más antiguos primero),
     * siguiendo la lógica FIFO (primero en entrar, primero en atender).
     * 
     * @return Lista de objetos {@link Pedido} con estado activo.
     *         Retorna una lista vacía si no hay pedidos activos o si ocurre un error.
     */
    public List<Pedido> listarActivos() {
        List<Pedido> pedidosActivos = new ArrayList<>();

        String sql = "SELECT id, tipo_pedido, mesa_id, mesero_id, estado, "
                   + "fecha_creacion, fecha_ultima_actualizacion "
                   + "FROM Pedidos "
                   + "WHERE estado NOT IN ('CANCELADO', 'ENTREGADO') "
                   + "ORDER BY fecha_creacion ASC";

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pedido pedido = mapearResultSet(rs);
                pedidosActivos.add(pedido);
            }

        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al listar pedidos activos: " + e.getMessage());
            e.printStackTrace();
        }

        return pedidosActivos;
    }

    /**
     * Actualiza el estado de un pedido existente en la tabla 'Pedidos'.
     * 
     * Además de cambiar el campo {@code estado}, este método actualiza
     * automáticamente el campo {@code fecha_ultima_actualizacion} con la
     * fecha y hora actual del sistema, garantizando la trazabilidad
     * de los cambios para fines de auditoría.
     * 
     * @param id          Identificador único del pedido a actualizar.
     * @param nuevoEstado Nuevo estado a asignar (ej: 'EN_PREPARACION', 'LISTO', 'ENTREGADO').
     * @return {@code true} si el pedido fue actualizado exitosamente,
     *         {@code false} si el ID no existe o si ocurrió un error.
     */
    public boolean actualizarEstado(int id, String nuevoEstado) {
        String sql = "UPDATE Pedidos SET estado = ?, fecha_ultima_actualizacion = ? WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {

            // Parámetro 1: nuevo estado
            ps.setString(1, nuevoEstado);

            // Parámetro 2: fecha de actualización → fecha actual del sistema
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            // Parámetro 3: ID del pedido a actualizar (cláusula WHERE)
            ps.setInt(3, id);

            // Ejecutar y verificar que exactamente 1 fila fue afectada
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al actualizar el estado del pedido con ID " 
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Realiza una cancelación lógica (Soft Delete) de un pedido.
     * 
     * En lugar de eliminar físicamente el registro de la base de datos,
     * cambia el estado del pedido a 'CANCELADO'. Esta estrategia cumple
     * con las reglas de auditoría del proyecto, permitiendo conservar
     * el historial completo de todos los pedidos.
     * 
     * Validaciones de integridad (HU-07):
     *   - No se puede cancelar un pedido con estado 'ENTREGADO'.
     *   - No se puede cancelar un pedido ya 'CANCELADO' (idempotencia).
     *   - Si el ID no existe, retorna {@code false}.
     * 
     * @param id Identificador único del pedido a cancelar.
     * @return {@code true} si el pedido fue cancelado exitosamente,
     *         {@code false} si el ID no existe, el estado no lo permite,
     *         o si ocurrió un error.
     */
    public boolean cancelarPedido(int id) {
        // REGLA HU-07: Consultar el estado actual antes de cancelar
        String sqlConsulta = "SELECT estado FROM Pedidos WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sqlConsulta)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estadoActual = rs.getString("estado");

                    // Validación: no se puede cancelar un pedido ya ENTREGADO
                    if ("ENTREGADO".equals(estadoActual)) {
                        System.err.println("[PedidoDAO] No se puede cancelar el pedido ID "
                                + id + ": ya fue ENTREGADO.");
                        return false;
                    }

                    // Validación: no se puede cancelar un pedido ya CANCELADO (idempotencia)
                    if ("CANCELADO".equals(estadoActual)) {
                        System.err.println("[PedidoDAO] El pedido ID "
                                + id + " ya está CANCELADO.");
                        return false;
                    }

                    // Estado válido para cancelación → delegar al método actualizarEstado
                    return actualizarEstado(id, "CANCELADO");

                } else {
                    System.err.println("[PedidoDAO] No existe pedido con ID " + id);
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al cancelar el pedido con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Pedido}.
     * 
     * Este método auxiliar centraliza la lógica de conversión de datos
     * desde el ResultSet hacia la entidad, evitando la duplicación de
     * código en los métodos de consulta.
     * 
     * Manejo especial del campo {@code mesa_id}:
     *   - Si el valor en la BD es NULL, {@link ResultSet#getInt(String)} retorna 0.
     *   - Se usa {@link ResultSet#wasNull()} para detectar el NULL real
     *     y asignar {@code null} al atributo {@code mesaId} del objeto Pedido.
     * 
     * @param rs ResultSet posicionado en la fila actual a mapear.
     * @return Objeto {@link Pedido} con todos los campos mapeados.
     * @throws SQLException Si ocurre un error al leer los datos del ResultSet.
     */
    private Pedido mapearResultSet(ResultSet rs) throws SQLException {
        Pedido pedido = new Pedido();

        pedido.setId(rs.getInt("id"));
        pedido.setTipoPedido(rs.getString("tipo_pedido"));

        // Manejo especial para mesa_id (campo nullable)
        int mesaId = rs.getInt("mesa_id");
        if (rs.wasNull()) {
            pedido.setMesaId(null);
        } else {
            pedido.setMesaId(mesaId);
        }

        pedido.setMeseroId(rs.getInt("mesero_id"));
        pedido.setEstado(rs.getString("estado"));

        // Conversión de Timestamp SQL → LocalDateTime de Java
        Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
        if (tsCreacion != null) {
            pedido.setFechaCreacion(tsCreacion.toLocalDateTime());
        }

        Timestamp tsActualizacion = rs.getTimestamp("fecha_ultima_actualizacion");
        if (tsActualizacion != null) {
            pedido.setFechaUltimaActualizacion(tsActualizacion.toLocalDateTime());
        }

        return pedido;
    }
}
