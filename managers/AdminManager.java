package managers;

import dao.ProductoDAO;
import entities.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Gestor de lógica de negocio para las operaciones del Administrador.
 * 
 * Forma parte de la Capa 3 (Managers / Lógica de Negocio) de la arquitectura
 * de 4 capas del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Orquestar las operaciones entre la capa UI (Capa 4) y la capa DAO (Capa 2).
 *   - Validar TODAS las reglas de negocio antes de tocar la base de datos.
 *   - Generar registros de auditoría en consola para las operaciones críticas.
 * 
 * Flujo de la arquitectura:
 *   AdminView (Capa 4) → AdminManager (Capa 3) → ProductoDAO (Capa 2) → BD
 * 
 * Historias de Usuario cubiertas:
 *   - HU-06: Gestión del Menú y Productos (CRUD) — cubierta de forma completa:
 *            → Crear: registrar platos nuevos en el menú.
 *            → Editar: modificar nombre/descripción/categoría de un plato,
 *              y cambiar su precio (con auditoría de precio anterior → nuevo).
 *            → Eliminar: dar de baja un plato (con protección si ya tiene
 *              pedidos asociados en el historial).
 *            → Disponibilidad: marcar un plato como disponible / no disponible,
 *              de modo que el mesero no pueda seleccionarlo en pedidos nuevos
 *              mientras esté marcado como no disponible (criterio 2 de HU-06).
 *            → Consultar el menú completo y las categorías disponibles.
 * 
 * Reglas de negocio:
 *   - El nombre del plato es obligatorio, de máximo 100 caracteres y único.
 *   - El precio debe ser mayor a 0, con máximo 2 decimales y no superar 99.999.999,99
 *     (límite de la columna DECIMAL(10,2)).
 *   - La categoría debe existir.
 *   - No se puede eliminar un plato que ya tiene pedidos asociados en el
 *     historial; en ese caso se recomienda desactivarlo (disponible = false)
 *     en lugar de eliminarlo, preservando la trazabilidad de ventas.
 * 
 * @author PoliRestaurante
 */
public class AdminManager {

    // ==================== CONSTANTES ====================

    /** Longitud máxima del nombre (columna VARCHAR(100)). */
    private static final int MAX_LONGITUD_NOMBRE = 100;

    /** Precio máximo permitido por la columna DECIMAL(10,2). */
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    /** Formato estándar para las fechas en los registros de auditoría. */
    private static final DateTimeFormatter FORMATO_AUDITORIA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== ATRIBUTOS ====================

    /** DAO para operaciones CRUD sobre la tabla Productos. */
    private ProductoDAO productoDAO;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor que inicializa las dependencias de la capa DAO.
     */
    public AdminManager() {
        this.productoDAO = new ProductoDAO();
    }

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * HU-06 (Registrar): Registra un plato nuevo en el menú.
     * 
     * Cubre el criterio de aceptación 1 de HU-06: permite registrar los campos
     * clave del producto (nombre, descripción, precio, categoría). La
     * disponibilidad se asigna por defecto en {@code true} (visible para el
     * mesero desde el momento de su creación).
     * 
     * Flujo de operación:
     *   1. Valida nombre, precio y categoría (ver reglas en la documentación de la clase).
     *   2. Verifica que no exista ya un plato con el mismo nombre.
     *   3. Delega la inserción al ProductoDAO. El plato nace disponible.
     *   4. Registra en consola un log de auditoría.
     * 
     * @param categoriaId ID de la categoría del plato (debe existir).
     * @param nombre      Nombre del plato (obligatorio, único).
     * @param descripcion Descripción del plato (opcional; vacío o null → sin descripción).
     * @param precio      Precio de venta (mayor a 0).
     * @return El {@link Producto} creado (con su ID ya asignado),
     *         o {@code null} si alguna validación falló o hubo un error.
     */
    public Producto registrarPlato(int categoriaId, String nombre, String descripcion, BigDecimal precio) {
        try {
            // --- Validación del nombre ---
            if (nombre == null || nombre.trim().isEmpty()) {
                System.err.println("[AdminManager] ✗ El nombre del plato es obligatorio.");
                return null;
            }
            nombre = nombre.trim();

            if (nombre.length() > MAX_LONGITUD_NOMBRE) {
                System.err.println("[AdminManager] ✗ El nombre no puede superar los "
                        + MAX_LONGITUD_NOMBRE + " caracteres.");
                return null;
            }

            // --- Validación del precio ---
            if (!esPrecioValido(precio)) {
                return null;
            }

            // --- Validación de la categoría ---
            if (!productoDAO.listarCategorias().containsKey(categoriaId)) {
                System.err.println("[AdminManager] ✗ La categoría con ID " + categoriaId
                        + " no existe.");
                return null;
            }

            // --- Validación de nombre único ---
            if (productoDAO.existePorNombre(nombre)) {
                System.err.println("[AdminManager] ✗ Ya existe un plato llamado '" + nombre + "'.");
                return null;
            }

            // --- Normalizar descripción opcional ---
            String descripcionFinal = (descripcion == null || descripcion.trim().isEmpty())
                    ? null
                    : descripcion.trim();

            // --- Inserción ---
            Producto nuevo = new Producto(categoriaId, nombre, descripcionFinal, precio);

            if (productoDAO.crear(nuevo)) {
                System.out.println("[AdminManager] ✓ Plato registrado. ID: " + nuevo.getId()
                        + " | Nombre: " + nuevo.getNombre()
                        + " | Precio: $" + nuevo.getPrecio().toPlainString()
                        + " | Fecha: " + LocalDateTime.now().format(FORMATO_AUDITORIA));
                return nuevo;
            }

            System.err.println("[AdminManager] ✗ No se pudo registrar el plato. "
                    + "Revise los detalles en consola.");
            return null;

        } catch (Exception e) {
            System.err.println("[AdminManager] ✗ Excepción inesperada al registrar plato: "
                    + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * HU-06 (Editar): Cambia el precio de un plato existente y registra la auditoría.
     * 
     * Cubre parcialmente el criterio de aceptación 1 de HU-06 (edición de un
     * campo clave del producto: el precio). Editar nombre, descripción,
     * categoría o disponibilidad no está cubierto por este método.
     * 
     * Flujo de operación:
     *   1. Valida el nuevo precio.
     *   2. Verifica que el plato exista (y guarda su precio anterior).
     *   3. Delega la actualización al ProductoDAO.
     *   4. Imprime un registro de auditoría con precio anterior, precio nuevo
     *      y fecha/hora de la operación.
     * 
     * @param productoId  ID del plato a modificar.
     * @param nuevoPrecio Nuevo precio (mayor a 0).
     * @return {@code true} si el precio fue actualizado,
     *         {@code false} si el precio es inválido, el plato no existe o hubo un error.
     */
    public boolean cambiarPrecio(int productoId, BigDecimal nuevoPrecio) {
        try {
            // --- Validación del precio ---
            if (!esPrecioValido(nuevoPrecio)) {
                return false;
            }

            // --- Verificar que el plato exista ---
            Producto actual = productoDAO.buscarPorId(productoId);
            if (actual == null) {
                System.err.println("[AdminManager] ✗ No existe un plato con ID " + productoId + ".");
                return false;
            }

            BigDecimal precioAnterior = actual.getPrecio();

            // --- Actualización ---
            if (productoDAO.actualizarPrecio(productoId, nuevoPrecio)) {
                System.out.println("╔══════════════════════════════════════════════════════╗");
                System.out.println("║         REGISTRO DE AUDITORÍA — CAMBIO DE PRECIO     ║");
                System.out.println("╠══════════════════════════════════════════════════════╣");
                System.out.println("║  Plato         : [" + productoId + "] " + actual.getNombre());
                System.out.println("║  Precio previo : $" + precioAnterior.toPlainString());
                System.out.println("║  Precio nuevo  : $" + nuevoPrecio.toPlainString());
                System.out.println("║  Fecha/Hora    : " + LocalDateTime.now().format(FORMATO_AUDITORIA));
                System.out.println("╚══════════════════════════════════════════════════════╝");
                return true;
            }

            System.err.println("[AdminManager] ✗ No se pudo actualizar el precio del plato ID "
                    + productoId + ".");
            return false;

        } catch (Exception e) {
            System.err.println("[AdminManager] ✗ Excepción inesperada al cambiar precio del plato ID "
                    + productoId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (Editar): Actualiza el nombre, la descripción y la categoría de
     * un plato existente. El precio se maneja por separado con
     * {@link #cambiarPrecio} y la disponibilidad con
     * {@link #cambiarDisponibilidad}, para que cada operación quede
     * auditada de forma independiente.
     * 
     * Flujo de operación:
     *   1. Verifica que el plato exista.
     *   2. Valida nombre y categoría (mismas reglas que al registrar), permitiendo
     *      que el plato conserve su propio nombre sin marcarlo como duplicado.
     *   3. Delega la actualización al ProductoDAO.
     *   4. Registra en consola un log de auditoría con los datos anteriores y los nuevos.
     * 
     * @param productoId  ID del plato a modificar.
     * @param categoriaId Nueva categoría del plato (debe existir).
     * @param nombre      Nuevo nombre del plato (obligatorio, único).
     * @param descripcion Nueva descripción (opcional; vacío o null → sin descripción).
     * @return {@code true} si el plato fue actualizado exitosamente,
     *         {@code false} si alguna validación falló, el plato no existe o hubo un error.
     */
    public boolean editarDatosPlato(int productoId, int categoriaId, String nombre, String descripcion) {
        try {
            // --- Verificar que el plato exista ---
            Producto actual = productoDAO.buscarPorId(productoId);
            if (actual == null) {
                System.err.println("[AdminManager] ✗ No existe un plato con ID " + productoId + ".");
                return false;
            }

            // --- Validación del nombre ---
            if (nombre == null || nombre.trim().isEmpty()) {
                System.err.println("[AdminManager] ✗ El nombre del plato es obligatorio.");
                return false;
            }
            nombre = nombre.trim();

            if (nombre.length() > MAX_LONGITUD_NOMBRE) {
                System.err.println("[AdminManager] ✗ El nombre no puede superar los "
                        + MAX_LONGITUD_NOMBRE + " caracteres.");
                return false;
            }

            // --- Validación de la categoría ---
            if (!productoDAO.listarCategorias().containsKey(categoriaId)) {
                System.err.println("[AdminManager] ✗ La categoría con ID " + categoriaId
                        + " no existe.");
                return false;
            }

            // --- Validación de nombre único (excluyendo el propio plato) ---
            if (productoDAO.existePorNombre(nombre, productoId)) {
                System.err.println("[AdminManager] ✗ Ya existe otro plato llamado '" + nombre + "'.");
                return false;
            }

            // --- Normalizar descripción opcional ---
            String descripcionFinal = (descripcion == null || descripcion.trim().isEmpty())
                    ? null
                    : descripcion.trim();

            // --- Actualización ---
            if (productoDAO.actualizarDatos(productoId, categoriaId, nombre, descripcionFinal)) {
                System.out.println("╔══════════════════════════════════════════════════════╗");
                System.out.println("║        REGISTRO DE AUDITORÍA — EDICIÓN DE PLATO      ║");
                System.out.println("╠══════════════════════════════════════════════════════╣");
                System.out.println("║  Plato ID       : " + productoId);
                System.out.println("║  Nombre previo  : " + actual.getNombre());
                System.out.println("║  Nombre nuevo   : " + nombre);
                System.out.println("║  Categoría prev.: " + actual.getCategoriaId());
                System.out.println("║  Categoría nueva: " + categoriaId);
                System.out.println("║  Fecha/Hora     : " + LocalDateTime.now().format(FORMATO_AUDITORIA));
                System.out.println("╚══════════════════════════════════════════════════════╝");
                return true;
            }

            System.err.println("[AdminManager] ✗ No se pudo actualizar el plato ID "
                    + productoId + ".");
            return false;

        } catch (Exception e) {
            System.err.println("[AdminManager] ✗ Excepción inesperada al editar plato ID "
                    + productoId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (Disponibilidad): Marca un plato como disponible o no disponible.
     * 
     * Cubre el criterio de aceptación 2 de HU-06: cuando un plato se marca
     * como no disponible, deja de aparecer en {@link dao.ProductoDAO#listarDisponibles()},
     * que es el listado que debe consultar el módulo del Mesero (HU-01) al
     * armar un pedido nuevo. De este modo el mesero no puede seleccionarlo.
     * 
     * @param productoId  ID del plato a modificar.
     * @param disponible  Nueva disponibilidad ({@code true} = disponible para el mesero).
     * @return {@code true} si el plato fue actualizado exitosamente,
     *         {@code false} si el plato no existe o hubo un error.
     */
    public boolean cambiarDisponibilidad(int productoId, boolean disponible) {
        try {
            // --- Verificar que el plato exista ---
            Producto actual = productoDAO.buscarPorId(productoId);
            if (actual == null) {
                System.err.println("[AdminManager] ✗ No existe un plato con ID " + productoId + ".");
                return false;
            }

            if (actual.isDisponible() == disponible) {
                System.out.println("[AdminManager] ℹ El plato '" + actual.getNombre()
                        + "' ya estaba marcado como "
                        + (disponible ? "DISPONIBLE" : "NO DISPONIBLE") + ".");
                return true;
            }

            // --- Actualización ---
            if (productoDAO.actualizarDisponibilidad(productoId, disponible)) {
                System.out.println("[AdminManager] ✓ Plato '" + actual.getNombre() + "' (ID " + productoId
                        + ") marcado como " + (disponible ? "DISPONIBLE" : "NO DISPONIBLE")
                        + " | Fecha: " + LocalDateTime.now().format(FORMATO_AUDITORIA));
                return true;
            }

            System.err.println("[AdminManager] ✗ No se pudo cambiar la disponibilidad del plato ID "
                    + productoId + ".");
            return false;

        } catch (Exception e) {
            System.err.println("[AdminManager] ✗ Excepción inesperada al cambiar disponibilidad del plato ID "
                    + productoId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (Eliminar): Elimina un plato del menú.
     * 
     * Si el plato ya tiene pedidos asociados en el historial, el ProductoDAO
     * rechaza el borrado (para no perder la trazabilidad de ventas) y este
     * método lo informa al administrador, sugiriendo desactivarlo en su
     * lugar con {@link #cambiarDisponibilidad}.
     * 
     * @param productoId ID del plato a eliminar.
     * @return {@code true} si el plato fue eliminado exitosamente,
     *         {@code false} si no existe, tiene pedidos asociados, o hubo un error.
     */
    public boolean eliminarPlato(int productoId) {
        try {
            // --- Verificar que el plato exista ---
            Producto actual = productoDAO.buscarPorId(productoId);
            if (actual == null) {
                System.err.println("[AdminManager] ✗ No existe un plato con ID " + productoId + ".");
                return false;
            }

            if (productoDAO.eliminar(productoId)) {
                System.out.println("[AdminManager] ✓ Plato '" + actual.getNombre() + "' (ID " + productoId
                        + ") eliminado del menú | Fecha: "
                        + LocalDateTime.now().format(FORMATO_AUDITORIA));
                return true;
            }

            // El DAO ya imprimió el motivo específico (no existe / tiene pedidos asociados)
            return false;

        } catch (Exception e) {
            System.err.println("[AdminManager] ✗ Excepción inesperada al eliminar plato ID "
                    + productoId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-06 (Consultar): Obtiene el menú completo para mostrarlo en la interfaz.
     * 
     * @return Lista de {@link Producto}; vacía si no hay platos registrados.
     */
    public List<Producto> listarMenu() {
        return productoDAO.listarTodos();
    }

    /**
     * HU-06 (apoyo): Obtiene las categorías disponibles (id → nombre) para que el
     * administrador pueda elegir una al registrar un plato.
     * 
     * @return Mapa de categorías; vacío si no hay ninguna registrada.
     */
    public Map<Integer, String> listarCategorias() {
        return productoDAO.listarCategorias();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Valida un precio según las reglas de negocio:
     * no nulo, mayor a 0, máximo 2 decimales y dentro del rango de DECIMAL(10,2).
     * Si no es válido, imprime el motivo por consola.
     * 
     * @param precio Precio a validar.
     * @return {@code true} si el precio es válido.
     */
    private boolean esPrecioValido(BigDecimal precio) {
        if (precio == null) {
            System.err.println("[AdminManager] ✗ El precio es obligatorio.");
            return false;
        }
        if (precio.signum() <= 0) {
            System.err.println("[AdminManager] ✗ El precio debe ser mayor a 0.");
            return false;
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            System.err.println("[AdminManager] ✗ El precio admite máximo 2 decimales.");
            return false;
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            System.err.println("[AdminManager] ✗ El precio supera el máximo permitido ($"
                    + PRECIO_MAXIMO.toPlainString() + ").");
            return false;
        }
        return true;
    }
}
