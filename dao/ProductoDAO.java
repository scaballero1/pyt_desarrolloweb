package dao;

import entities.Producto;
import db.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase de Acceso a Datos (DAO) para la entidad {@link Producto}.
 * 
 * Forma parte de la Capa 2 (DAO) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Encapsular toda la lógica de acceso a la base de datos para la tabla 'Productos'.
 *   - Ejecutar operaciones CRUD mediante JDBC puro con PreparedStatement.
 *   - Manejar excepciones SQL de forma controlada.
 * 
 * Soporta HU-06 (Gestión del Menú y Productos - CRUD) de forma completa:
 * alta, edición (datos y precio), eliminación y cambio de disponibilidad.
 * 
 * Métodos implementados:
 *   - crear(Producto)                    → INSERT INTO Productos              (HU-06: crear)
 *   - actualizarDatos(...)               → UPDATE nombre/descripcion/categoría (HU-06: editar)
 *   - actualizarPrecio(id, precio)       → UPDATE precio                       (HU-06: editar)
 *   - actualizarDisponibilidad(id, bool) → UPDATE disponible                   (HU-06: disponibilidad)
 *   - eliminar(id)                       → DELETE FROM Productos               (HU-06: eliminar)
 *   - buscarPorId(id)                    → SELECT por clave primaria           (HU-06: apoyo)
 *   - existePorNombre(nombre)            → SELECT para evitar platos duplicados (HU-06: apoyo)
 *   - existePorNombre(nombre, idExcluir) → ídem, excluyendo el propio registro (HU-06: apoyo edición)
 *   - listarTodos()                      → SELECT de todo el menú              (HU-06: apoyo)
 *   - listarDisponibles()                → SELECT solo platos disponibles      (HU-06 criterio 2 / futura HU-01)
 *   - listarCategorias()                 → SELECT de Categorias (id → nombre)  (HU-06: apoyo)
 * 
 * @author PoliRestaurante
 */
public class ProductoDAO {

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
    public ProductoDAO() {
        try {
            this.conexion = DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al obtener la conexión a la base de datos: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== MÉTODOS CRUD ====================

    /**
     * Inserta un nuevo producto en la tabla 'Productos'.
     * 
     * Si la inserción es exitosa, el ID auto-generado por MySQL se asigna
     * al objeto recibido (mediante {@code setId}), de modo que las capas
     * superiores puedan mostrarlo al usuario.
     * 
     * El campo {@code descripcion} es opcional: si es null se envía un NULL
     * explícito a la base de datos.
     * 
     * @param producto Objeto {@link Producto} con los datos del nuevo producto.
     *                 Requeridos: categoriaId, nombre, precio.
     *                 El campo id es IGNORADO (lo genera la base de datos).
     * @return {@code true} si el registro fue insertado exitosamente,
     *         {@code false} si ocurrió un error (ej: categoría inexistente).
     */
    public boolean crear(Producto producto) {
        String sql = "INSERT INTO Productos (categoria_id, nombre, descripcion, precio, disponible) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, producto.getCategoriaId());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion()); // null → NULL en la BD
            ps.setBigDecimal(4, producto.getPrecio());
            ps.setBoolean(5, producto.isDisponible());

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas > 0) {
                // Recuperar el ID generado y dejarlo en el objeto
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        producto.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al crear el producto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza únicamente el precio de un producto existente.
     * 
     * @param id          Identificador único del producto a actualizar.
     * @param nuevoPrecio Nuevo precio a asignar.
     * @return {@code true} si el producto fue actualizado exitosamente,
     *         {@code false} si el ID no existe o si ocurrió un error.
     */
    public boolean actualizarPrecio(int id, BigDecimal nuevoPrecio) {
        String sql = "UPDATE Productos SET precio = ? WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setBigDecimal(1, nuevoPrecio);
            ps.setInt(2, id);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al actualizar el precio del producto con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (editar): Actualiza el nombre, la descripción y la categoría
     * de un producto existente. El precio y la disponibilidad NO se tocan
     * aquí; para eso están {@link #actualizarPrecio} y
     * {@link #actualizarDisponibilidad}, que se pueden usar de forma
     * independiente sin reescribir el resto del registro.
     * 
     * @param id          Identificador único del producto a actualizar.
     * @param categoriaId Nueva categoría del producto.
     * @param nombre      Nuevo nombre del producto.
     * @param descripcion Nueva descripción (puede ser null).
     * @return {@code true} si el producto fue actualizado exitosamente,
     *         {@code false} si el ID no existe o si ocurrió un error.
     */
    public boolean actualizarDatos(int id, int categoriaId, String nombre, String descripcion) {
        String sql = "UPDATE Productos SET categoria_id = ?, nombre = ?, descripcion = ? WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, categoriaId);
            ps.setString(2, nombre);
            ps.setString(3, descripcion); // null → NULL en la BD
            ps.setInt(4, id);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al actualizar los datos del producto con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (disponibilidad): Cambia la disponibilidad de un producto.
     * 
     * Un producto marcado como no disponible ({@code disponible = false})
     * no debe poder ser seleccionado por el mesero al crear nuevos pedidos
     * (criterio de aceptación 2 de HU-06). Ese filtro se aplica consultando
     * el menú a través de {@link #listarDisponibles()}.
     * 
     * @param id          Identificador único del producto a actualizar.
     * @param disponible  Nueva disponibilidad ({@code true} = disponible).
     * @return {@code true} si el producto fue actualizado exitosamente,
     *         {@code false} si el ID no existe o si ocurrió un error.
     */
    public boolean actualizarDisponibilidad(int id, boolean disponible) {
        String sql = "UPDATE Productos SET disponible = ? WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setBoolean(1, disponible);
            ps.setInt(2, id);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al actualizar la disponibilidad del producto con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (eliminar): Elimina físicamente un producto de la tabla 'Productos'.
     * 
     * Si el producto ya tiene pedidos asociados (por ejemplo, una fila en
     * 'Detalle_Pedidos' que lo referencia mediante FK), la base de datos
     * rechaza el borrado para no perder el historial de ventas; en ese caso
     * este método captura la violación de integridad referencial y retorna
     * {@code false} en vez de propagar la excepción, dejando un mensaje
     * claro en consola para que el administrador use la desactivación
     * (disponible = false) en lugar de la eliminación.
     * 
     * @param id Identificador único del producto a eliminar.
     * @return {@code true} si el producto fue eliminado exitosamente,
     *         {@code false} si el ID no existe, si tiene pedidos asociados,
     *         o si ocurrió otro error.
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Productos WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("[ProductoDAO] No se puede eliminar el producto con ID " + id
                    + ": tiene pedidos asociados en el historial. "
                    + "Use 'cambiar disponibilidad' para retirarlo del menú sin perder el historial.");
            return false;

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al eliminar el producto con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca un producto por su identificador único.
     * 
     * @param id Identificador del producto.
     * @return El {@link Producto} encontrado, o {@code null} si no existe
     *         o si ocurrió un error.
     */
    public Producto buscarPorId(int id) {
        String sql = "SELECT id, categoria_id, nombre, descripcion, precio, disponible "
                   + "FROM Productos WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al buscar el producto con ID "
                    + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Verifica si ya existe un producto con el nombre indicado.
     * 
     * La comparación depende de la colación de la tabla; en MySQL por defecto
     * no distingue mayúsculas de minúsculas ('Sopa' = 'sopa').
     * 
     * @param nombre Nombre a verificar.
     * @return {@code true} si ya existe un producto con ese nombre.
     *         Si ocurre un error retorna {@code true} por seguridad
     *         (evita insertar duplicados a ciegas).
     */
    public boolean existePorNombre(String nombre) {
        String sql = "SELECT 1 FROM Productos WHERE nombre = ? LIMIT 1";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al verificar el nombre '"
                    + nombre + "': " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Verifica si ya existe OTRO producto (distinto de {@code idExcluir})
     * con el nombre indicado. Se usa al editar un plato, para permitir que
     * conserve su propio nombre sin que se marque como duplicado.
     * 
     * @param nombre    Nombre a verificar.
     * @param idExcluir ID del producto que se está editando (se excluye de la búsqueda).
     * @return {@code true} si otro producto ya tiene ese nombre.
     *         Si ocurre un error retorna {@code true} por seguridad.
     */
    public boolean existePorNombre(String nombre, int idExcluir) {
        String sql = "SELECT 1 FROM Productos WHERE nombre = ? AND id <> ? LIMIT 1";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al verificar el nombre '"
                    + nombre + "': " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Obtiene todos los productos del menú, ordenados por categoría y nombre.
     * 
     * @return Lista de objetos {@link Producto}.
     *         Retorna una lista vacía si no hay productos o si ocurre un error.
     */
    public List<Producto> listarTodos() {
        List<Producto> productos = new ArrayList<>();

        String sql = "SELECT id, categoria_id, nombre, descripcion, precio, disponible "
                   + "FROM Productos "
                   + "ORDER BY categoria_id ASC, nombre ASC";

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productos.add(mapearResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al listar los productos: " + e.getMessage());
            e.printStackTrace();
        }

        return productos;
    }

    /**
     * HU-06 (criterio 2): Obtiene únicamente los productos marcados como
     * disponibles. Este es el listado que debe consultar el módulo del
     * Mesero (HU-01) al armar un pedido nuevo, de modo que un producto
     * marcado como "No disponible" quede automáticamente fuera de las
     * opciones que el mesero puede seleccionar.
     * 
     * @return Lista de objetos {@link Producto} con disponible = true.
     *         Retorna una lista vacía si no hay productos disponibles o si ocurre un error.
     */
    public List<Producto> listarDisponibles() {
        List<Producto> productos = new ArrayList<>();

        String sql = "SELECT id, categoria_id, nombre, descripcion, precio, disponible "
                   + "FROM Productos "
                   + "WHERE disponible = TRUE "
                   + "ORDER BY categoria_id ASC, nombre ASC";

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productos.add(mapearResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al listar los productos disponibles: "
                    + e.getMessage());
            e.printStackTrace();
        }

        return productos;
    }

    /**
     * Obtiene las categorías registradas como un mapa id → nombre.
     * 
     * Se expone desde este DAO (y no desde uno propio de Categorías) porque
     * el único fin es validar y mostrar la categoría al registrar un plato.
     * Usa {@link LinkedHashMap} para conservar el orden por ID.
     * 
     * @return Mapa con las categorías existentes.
     *         Retorna un mapa vacío si no hay categorías o si ocurre un error.
     */
    public Map<Integer, String> listarCategorias() {
        Map<Integer, String> categorias = new LinkedHashMap<>();

        String sql = "SELECT id, nombre FROM Categorias ORDER BY id ASC";

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categorias.put(rs.getInt("id"), rs.getString("nombre"));
            }

        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error al listar las categorías: " + e.getMessage());
            e.printStackTrace();
        }

        return categorias;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Producto}.
     * 
     * Centraliza la conversión de datos desde el ResultSet hacia la entidad,
     * evitando duplicar código en los métodos de consulta.
     * 
     * @param rs ResultSet posicionado en la fila actual a mapear.
     * @return Objeto {@link Producto} con todos los campos mapeados.
     * @throws SQLException Si ocurre un error al leer los datos del ResultSet.
     */
    private Producto mapearResultSet(ResultSet rs) throws SQLException {
        Producto producto = new Producto();

        producto.setId(rs.getInt("id"));
        producto.setCategoriaId(rs.getInt("categoria_id"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion")); // null si es NULL en la BD
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setDisponible(rs.getBoolean("disponible"));

        return producto;
    }
}
