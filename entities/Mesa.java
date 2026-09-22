package entities;
 
/**
 * Clase entidad que representa un registro de la tabla 'Mesas' en la base de datos.
 * 
 * Forma parte de la Capa 1 (Entities) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Mapeo de campos SQL → Java:
 *   - id (INT AUTO_INCREMENT)   → int id
 *   - numero (INT NOT NULL)     → int numero
 *   - capacidad (INT NOT NULL)  → int capacidad
 *   - estado (VARCHAR 20)       → String estado  (ej: 'LIBRE', 'OCUPADA')
 * 
 * @author Carlos - PoliRestaurante
 */
public class Mesa {
 
    // ==================== ATRIBUTOS ====================
 
    /** Identificador único de la mesa (clave primaria, auto-incremental). */
    private int id;
 
    /** Número visible de la mesa (el que ve el cliente/mesero, ej: Mesa 5). */
    private int numero;
 
    /** Capacidad máxima de comensales de la mesa. */
    private int capacidad;
 
    /** Estado actual de la mesa (ej: 'LIBRE', 'OCUPADA'). */
    private String estado;
 
    // ==================== CONSTRUCTORES ====================
 
    /**
     * Constructor vacío.
     * Requerido para la instanciación por reflexión y para el mapeo
     * desde ResultSet en la capa DAO.
     */
    public Mesa() {
    }
 
    /**
     * Constructor con todos los parámetros.
     * Útil para crear instancias completas al leer registros de la base de datos.
     *
     * @param id        Identificador único de la mesa.
     * @param numero    Número visible de la mesa.
     * @param capacidad Capacidad máxima de comensales.
     * @param estado    Estado actual de la mesa.
     */
    public Mesa(int id, int numero, int capacidad, String estado) {
        this.id = id;
        this.numero = numero;
        this.capacidad = capacidad;
        this.estado = estado;
    }
 
    /**
     * Constructor sin ID.
     * Útil para crear nuevas mesas antes de insertarlas en la base de datos,
     * donde el ID es auto-generado.
     *
     * @param numero    Número visible de la mesa.
     * @param capacidad Capacidad máxima de comensales.
     * @param estado    Estado inicial de la mesa (normalmente 'LIBRE').
     */
    public Mesa(int numero, int capacidad, String estado) {
        this.numero = numero;
        this.capacidad = capacidad;
        this.estado = estado;
    }
 
    // ==================== MÉTODOS GET Y SET ====================
 
    public int getId() {
        return id;
    }
 
    public void setId(int id) {
        this.id = id;
    }
 
    public int getNumero() {
        return numero;
    }
 
    public void setNumero(int numero) {
        this.numero = numero;
    }
 
    public int getCapacidad() {
        return capacidad;
    }
 
    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }
 
    public String getEstado() {
        return estado;
    }
 
    public void setEstado(String estado) {
        this.estado = estado;
    }
 
    // ==================== MÉTODO toString ====================
 
    @Override
    public String toString() {
        return "Mesa{" +
                "id=" + id +
                ", numero=" + numero +
                ", capacidad=" + capacidad +
                ", estado='" + estado + '\'' +
                '}';
    }
}
 
