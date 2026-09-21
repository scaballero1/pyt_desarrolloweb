package entities;

import java.time.LocalDateTime;

/**
 * Clase entidad que representa un registro de la tabla 'Pedidos' en la base de datos.
 * 
 * Forma parte de la Capa 1 (Entities) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Mapeo de campos SQL → Java:
 *   - id (INT AUTO_INCREMENT)          → int id
 *   - tipo_pedido (VARCHAR 20)         → String tipoPedido
 *   - mesa_id (INT NULL)               → Integer mesaId  (nullable)
 *   - mesero_id (INT NOT NULL)         → int meseroId
 *   - estado (VARCHAR 30)              → String estado
 *   - fecha_creacion (DATETIME)        → LocalDateTime fechaCreacion
 *   - fecha_ultima_actualizacion (DATETIME) → LocalDateTime fechaUltimaActualizacion
 * 
 * @author PoliRestaurante
 */
public class Pedido {

    // ==================== ATRIBUTOS ====================

    /** Identificador único del pedido (clave primaria, auto-incremental). */
    private int id;

    /** Tipo de pedido: puede ser 'EN_MESA', 'PARA_LLEVAR', 'DOMICILIO', etc. */
    private String tipoPedido;

    /**
     * Identificador de la mesa asociada al pedido.
     * Se usa Integer en lugar de int para permitir valores nulos,
     * ya que no todos los pedidos están asociados a una mesa
     * (ej: pedidos para llevar o a domicilio).
     */
    private Integer mesaId;

    /** Identificador del mesero responsable del pedido. */
    private int meseroId;

    /** Estado actual del pedido (ej: 'PENDIENTE', 'EN_PREPARACION', 'ENTREGADO', 'CANCELADO'). */
    private String estado;

    /** Fecha y hora en que se creó el pedido en el sistema. */
    private LocalDateTime fechaCreacion;

    /** Fecha y hora de la última modificación realizada al pedido. */
    private LocalDateTime fechaUltimaActualizacion;

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor vacío.
     * Requerido para la instanciación por reflexión y para el mapeo
     * desde ResultSet en la capa DAO.
     */
    public Pedido() {
    }

    /**
     * Constructor con todos los parámetros.
     * Útil para crear instancias completas al leer registros de la base de datos.
     *
     * @param id                        Identificador único del pedido.
     * @param tipoPedido                Tipo de pedido (ej: 'EN_MESA', 'PARA_LLEVAR').
     * @param mesaId                    ID de la mesa (puede ser null).
     * @param meseroId                  ID del mesero responsable.
     * @param estado                    Estado actual del pedido.
     * @param fechaCreacion             Fecha y hora de creación.
     * @param fechaUltimaActualizacion  Fecha y hora de la última actualización.
     */
    public Pedido(int id, String tipoPedido, Integer mesaId, int meseroId,
                  String estado, LocalDateTime fechaCreacion,
                  LocalDateTime fechaUltimaActualizacion) {
        this.id = id;
        this.tipoPedido = tipoPedido;
        this.mesaId = mesaId;
        this.meseroId = meseroId;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    /**
     * Constructor sin ID ni fechas.
     * Útil para crear nuevos pedidos antes de insertarlos en la base de datos,
     * donde el ID es auto-generado y las fechas se asignan automáticamente.
     *
     * @param tipoPedido Tipo de pedido.
     * @param mesaId     ID de la mesa (puede ser null).
     * @param meseroId   ID del mesero responsable.
     * @param estado     Estado inicial del pedido.
     */
    public Pedido(String tipoPedido, Integer mesaId, int meseroId, String estado) {
        this.tipoPedido = tipoPedido;
        this.mesaId = mesaId;
        this.meseroId = meseroId;
        this.estado = estado;
    }

    // ==================== MÉTODOS GET Y SET ====================

    /**
     * Obtiene el identificador único del pedido.
     * @return ID del pedido.
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del pedido.
     * @param id ID del pedido.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el tipo de pedido.
     * @return Tipo de pedido (ej: 'EN_MESA', 'PARA_LLEVAR', 'DOMICILIO').
     */
    public String getTipoPedido() {
        return tipoPedido;
    }

    /**
     * Establece el tipo de pedido.
     * @param tipoPedido Tipo de pedido.
     */
    public void setTipoPedido(String tipoPedido) {
        this.tipoPedido = tipoPedido;
    }

    /**
     * Obtiene el ID de la mesa asociada al pedido.
     * @return ID de la mesa, o null si el pedido no está asociado a una mesa.
     */
    public Integer getMesaId() {
        return mesaId;
    }

    /**
     * Establece el ID de la mesa asociada al pedido.
     * @param mesaId ID de la mesa (puede ser null).
     */
    public void setMesaId(Integer mesaId) {
        this.mesaId = mesaId;
    }

    /**
     * Obtiene el ID del mesero responsable del pedido.
     * @return ID del mesero.
     */
    public int getMeseroId() {
        return meseroId;
    }

    /**
     * Establece el ID del mesero responsable del pedido.
     * @param meseroId ID del mesero.
     */
    public void setMeseroId(int meseroId) {
        this.meseroId = meseroId;
    }

    /**
     * Obtiene el estado actual del pedido.
     * @return Estado del pedido (ej: 'PENDIENTE', 'EN_PREPARACION', 'ENTREGADO', 'CANCELADO').
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado actual del pedido.
     * @param estado Nuevo estado del pedido.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene la fecha y hora de creación del pedido.
     * @return Fecha de creación.
     */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Establece la fecha y hora de creación del pedido.
     * @param fechaCreacion Fecha de creación.
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Obtiene la fecha y hora de la última actualización del pedido.
     * @return Fecha de última actualización.
     */
    public LocalDateTime getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    /**
     * Establece la fecha y hora de la última actualización del pedido.
     * @param fechaUltimaActualizacion Fecha de última actualización.
     */
    public void setFechaUltimaActualizacion(LocalDateTime fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    // ==================== MÉTODO toString ====================

    /**
     * Representación en texto del pedido para depuración y logging.
     * @return Cadena con todos los campos del pedido.
     */
    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", tipoPedido='" + tipoPedido + '\'' +
                ", mesaId=" + mesaId +
                ", meseroId=" + meseroId +
                ", estado='" + estado + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaUltimaActualizacion=" + fechaUltimaActualizacion +
                '}';
    }
}
