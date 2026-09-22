package entities;

import java.math.BigDecimal;

/**
 * Clase entidad que representa un registro de la tabla 'Productos' en la base de datos.
 * 
 * Forma parte de la Capa 1 (Entities) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Entidad base de HU-06 (Gestión del Menú y Productos - CRUD).
 * 
 * Mapeo de campos SQL → Java:
 *   - id (INT AUTO_INCREMENT)          → int id
 *   - categoria_id (INT NOT NULL)      → int categoriaId
 *   - nombre (VARCHAR 100)             → String nombre
 *   - descripcion (TEXT)               → String descripcion  (puede ser null)
 *   - precio (DECIMAL 10,2)            → BigDecimal precio
 *   - disponible (BOOLEAN)             → boolean disponible
 * 
 * Nota: el precio se maneja como {@link BigDecimal} (y no como double)
 * para evitar errores de redondeo en valores monetarios.
 * 
 * @author PoliRestaurante
 */
public class Producto {

    // ==================== ATRIBUTOS ====================

    /** Identificador único del producto (clave primaria, auto-incremental). */
    private int id;

    /** Identificador de la categoría a la que pertenece el producto (FK → Categorias.id). */
    private int categoriaId;

    /** Nombre del plato o producto (ej: 'Bandeja Paisa'). */
    private String nombre;

    /** Descripción opcional del producto. Puede ser null. */
    private String descripcion;

    /** Precio de venta del producto. */
    private BigDecimal precio;

    /** Indica si el producto está disponible para ser pedido. */
    private boolean disponible;

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor vacío.
     * Requerido para el mapeo desde ResultSet en la capa DAO.
     */
    public Producto() {
    }

    /**
     * Constructor con todos los parámetros.
     * Útil para crear instancias completas al leer registros de la base de datos.
     *
     * @param id          Identificador único del producto.
     * @param categoriaId ID de la categoría.
     * @param nombre      Nombre del producto.
     * @param descripcion Descripción (puede ser null).
     * @param precio      Precio de venta.
     * @param disponible  Disponibilidad del producto.
     */
    public Producto(int id, int categoriaId, String nombre, String descripcion,
                    BigDecimal precio, boolean disponible) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = disponible;
    }

    /**
     * Constructor sin ID.
     * Útil para crear nuevos productos antes de insertarlos en la base de datos,
     * donde el ID es auto-generado. Un producto nuevo nace disponible.
     *
     * @param categoriaId ID de la categoría.
     * @param nombre      Nombre del producto.
     * @param descripcion Descripción (puede ser null).
     * @param precio      Precio de venta.
     */
    public Producto(int categoriaId, String nombre, String descripcion, BigDecimal precio) {
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = true;
    }

    // ==================== MÉTODOS GET Y SET ====================

    /**
     * Obtiene el identificador único del producto.
     * @return ID del producto.
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del producto.
     * @param id ID del producto.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el ID de la categoría del producto.
     * @return ID de la categoría.
     */
    public int getCategoriaId() {
        return categoriaId;
    }

    /**
     * Establece el ID de la categoría del producto.
     * @param categoriaId ID de la categoría.
     */
    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    /**
     * Obtiene el nombre del producto.
     * @return Nombre del producto.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del producto.
     * @param nombre Nombre del producto.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene la descripción del producto.
     * @return Descripción, o null si no tiene.
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Establece la descripción del producto.
     * @param descripcion Descripción (puede ser null).
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Obtiene el precio de venta del producto.
     * @return Precio del producto.
     */
    public BigDecimal getPrecio() {
        return precio;
    }

    /**
     * Establece el precio de venta del producto.
     * @param precio Precio del producto.
     */
    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    /**
     * Indica si el producto está disponible.
     * @return {@code true} si está disponible, {@code false} en caso contrario.
     */
    public boolean isDisponible() {
        return disponible;
    }

    /**
     * Establece la disponibilidad del producto.
     * @param disponible Nueva disponibilidad.
     */
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    // ==================== MÉTODO toString ====================

    /**
     * Representación en texto del producto para depuración y logging.
     * @return Cadena con todos los campos del producto.
     */
    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", categoriaId=" + categoriaId +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", precio=" + precio +
                ", disponible=" + disponible +
                '}';
    }
}
