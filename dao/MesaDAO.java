package dao;
 
import entities.Mesa;
import db.DBConnection;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Clase de Acceso a Datos (DAO) para la entidad {@link Mesa}.
 * 
 * Forma parte de la Capa 2 (DAO) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Encapsular toda la lógica de acceso a la base de datos para la tabla 'Mesas'.
 *   - Ejecutar operaciones CRUD mediante JDBC puro con PreparedStatement.
 *   - Manejar excepciones SQL de forma controlada.
 * 
 * Método clave para la integración con Pedidos:
 *   - actualizarEstado(id, nuevoEstado) → UPDATE estado ('LIBRE' / 'OCUPADA')
 *     Este es el método que MeseroManager invoca para ocupar/liberar mesas
 *     de forma automática al registrar o entregar un pedido.
 * 
 * @author Carlos - PoliRestaurante
 */
public class MesaDAO {
 
    // ==================== ATRIBUTOS ====================
 
    /** Conexión activa a la base de datos MySQL, obtenida desde DBConnection. */
    private Connection conexion;
 
    // ==================== CONSTRUCTOR ====================
 
    /**
     * Constructor que inicializa la conexión a la base de datos
     * utilizando la clase utilitaria DBConnection del proyecto.
     */
    public MesaDAO() {
        try {
            this.conexion = DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("[MesaDAO] Error al obtener la conexión a la base de datos: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }
 
    // ==================== MÉTODOS CRUD ====================
 
    /**
     * Actualiza el estado de una mesa existente en la tabla 'Mesas'.
     * 
     * Este es el método central para la integración con el módulo de Pedidos:
     *   - MeseroManager.registrarNuevoPedido() lo invoca con "OCUPADA".
     *   - MeseroManager.entregarPedido() lo invoca con "LIBRE".
     * 
     * @param id          Identificador único de la mesa a actualizar.
     * @param nuevoEstado Nuevo estado a asignar (ej: 'LIBRE', 'OCUPADA').
     * @return {@code true} si la mesa fue actualizada exitosamente,
     *         {@code false} si el ID no existe o si ocurrió un error.
     */
    public boolean actualizarEstado(int id, String nuevoEstado) {
        String sql = "UPDATE Mesas SET estado = ? WHERE id = ?";
 
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
 
            // Parámetro 1: nuevo estado
            ps.setString(1, nuevoEstado);
 
            // Parámetro 2: ID de la mesa a actualizar (cláusula WHERE)
            ps.setInt(2, id);
 
            // Ejecutar y verificar que exactamente 1 fila fue afectada
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
 
        } catch (SQLException e) {
            System.err.println("[MesaDAO] Error al actualizar el estado de la mesa con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
 
    /**
     * Obtiene una mesa por su identificador único.
     * 
     * @param id Identificador único de la mesa.
     * @return Objeto {@link Mesa} si existe, o {@code null} si no se encontró
     *         o si ocurrió un error.
     */
    public Mesa obtenerPorId(int id) {
        String sql = "SELECT id, numero, capacidad, estado FROM Mesas WHERE id = ?";
 
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
 
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                } else {
                    System.err.println("[MesaDAO] No existe mesa con ID " + id);
                    return null;
                }
            }
 
        } catch (SQLException e) {
            System.err.println("[MesaDAO] Error al obtener la mesa con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
 
    /**
     * Obtiene la lista completa de mesas registradas en el sistema,
     * ordenadas por número de mesa.
     * 
     * @return Lista de objetos {@link Mesa}. Retorna una lista vacía
     *         si no hay mesas o si ocurre un error.
     */
    public List<Mesa> listarTodas() {
        List<Mesa> mesas = new ArrayList<>();
 
        String sql = "SELECT id, numero, capacidad, estado FROM Mesas ORDER BY numero ASC";
 
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
 
            while (rs.next()) {
                mesas.add(mapearResultSet(rs));
            }
 
        } catch (SQLException e) {
            System.err.println("[MesaDAO] Error al listar las mesas: " + e.getMessage());
            e.printStackTrace();
        }
 
        return mesas;
    }
 
    /**
     * Obtiene la lista de mesas que se encuentran actualmente LIBRES.
     * Útil para la vista del mesero al momento de asignar una mesa a un
     * nuevo pedido.
     * 
     * @return Lista de objetos {@link Mesa} con estado 'LIBRE'.
     *         Retorna una lista vacía si no hay mesas libres o si ocurre un error.
     */
    public List<Mesa> listarLibres() {
        List<Mesa> mesasLibres = new ArrayList<>();
 
        String sql = "SELECT id, numero, capacidad, estado FROM Mesas "
                   + "WHERE estado = 'LIBRE' ORDER BY numero ASC";
 
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
 
            while (rs.next()) {
                mesasLibres.add(mapearResultSet(rs));
            }
 
        } catch (SQLException e) {
            System.err.println("[MesaDAO] Error al listar las mesas libres: " + e.getMessage());
            e.printStackTrace();
        }
 
        return mesasLibres;
    }
 
    // ==================== MÉTODOS AUXILIARES ====================
 
    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Mesa}.
     * 
     * @param rs ResultSet posicionado en la fila actual a mapear.
     * @return Objeto {@link Mesa} con todos los campos mapeados.
     * @throws SQLException Si ocurre un error al leer los datos del ResultSet.
     */
    private Mesa mapearResultSet(ResultSet rs) throws SQLException {
        Mesa mesa = new Mesa();
 
        mesa.setId(rs.getInt("id"));
        mesa.setNumero(rs.getInt("numero"));
        mesa.setCapacidad(rs.getInt("capacidad"));
        mesa.setEstado(rs.getString("estado"));
 
        return mesa;
    }
}
