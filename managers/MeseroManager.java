package managers;

import dao.PedidoDAO;
// import dao.MesaDAO; // TODO: Descomentar cuando Carlos entregue el DAO de Mesas
import entities.Pedido;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestor de lógica de negocio para las operaciones del Mesero.
 * 
 * Forma parte de la Capa 3 (Managers / Lógica de Negocio) de la arquitectura
 * de 4 capas del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Orquestar las operaciones entre la capa UI (Capa 4) y la capa DAO (Capa 2).
 *   - Coordinar transacciones que involucran múltiples DAOs (Pedidos + Mesas).
 *   - Aplicar reglas de negocio que trascienden una sola entidad.
 *   - Generar registros de auditoría en consola para las operaciones críticas.
 * 
 * Flujo de la arquitectura:
 *   UI (Capa 4) → MeseroManager (Capa 3) → PedidoDAO / MesaDAO (Capa 2) → BD
 * 
 * Historias de Usuario cubiertas:
 *   - HU-01: Registrar nuevo pedido y ocupar la mesa asociada.
 *   - HU-03: Entregar pedido y liberar la mesa asociada.
 *   - HU-07: Anular (cancelar) un pedido con registro del motivo de auditoría.
 * 
 * @author PoliRestaurante
 */
public class MeseroManager {

    // ==================== ATRIBUTOS ====================

    /** DAO para operaciones CRUD sobre la tabla Pedidos. */
    private PedidoDAO pedidoDAO;

    /**
     * DAO para operaciones CRUD sobre la tabla Mesas.
     * TODO: Pendiente de integración — Carlos está desarrollando MesaDAO.
     * Descomentar cuando esté disponible.
     */
    // private MesaDAO mesaDAO;

    /** Formato estándar para las fechas en los registros de auditoría. */
    private static final DateTimeFormatter FORMATO_AUDITORIA = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor que inicializa las dependencias de la capa DAO.
     * 
     * Cada Manager debe instanciar (o recibir) los DAOs que necesita
     * para coordinar las operaciones de negocio. En este caso:
     *   - PedidoDAO: para gestionar los pedidos.
     *   - MesaDAO: para gestionar el estado de las mesas (pendiente de Carlos).
     */
    public MeseroManager() {
        this.pedidoDAO = new PedidoDAO();
        // this.mesaDAO = new MesaDAO(); // TODO: Descomentar cuando Carlos entregue MesaDAO
    }

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * HU-01: Registra un nuevo pedido en el sistema y, si corresponde,
     * marca la mesa asociada como OCUPADA.
     * 
     * Flujo de operación:
     *   1. Delega la inserción del pedido al PedidoDAO (que fuerza estado 'PENDIENTE').
     *   2. Si la inserción es exitosa y el pedido tiene una mesa asociada (mesa_id no nulo),
     *      coordina con MesaDAO para cambiar el estado de la mesa a 'OCUPADA'.
     *   3. Registra en consola un log de auditoría con el resultado de la operación.
     * 
     * Nota: El estado inicial 'PENDIENTE' es forzado por el DAO (regla HU-01),
     * por lo que este Manager no necesita validar ni asignar el estado.
     * 
     * @param pedido Objeto {@link Pedido} con los datos del nuevo pedido.
     *               Campos requeridos: tipoPedido, meseroId.
     *               Campo opcional: mesaId (null para pedidos a domicilio o para llevar).
     * @return {@code true} si el pedido fue creado exitosamente,
     *         {@code false} si ocurrió un error en la creación.
     */
    public boolean registrarNuevoPedido(Pedido pedido) {
        try {
            // Paso 1: Crear el pedido en la base de datos
            boolean pedidoCreado = pedidoDAO.crear(pedido);

            if (pedidoCreado) {
                System.out.println("[MeseroManager] ✓ Pedido registrado exitosamente. "
                        + "Tipo: " + pedido.getTipoPedido()
                        + " | Mesero ID: " + pedido.getMeseroId()
                        + " | Fecha: " + LocalDateTime.now().format(FORMATO_AUDITORIA));

                // Paso 2: Si el pedido tiene mesa asociada, marcarla como OCUPADA
                if (pedido.getMesaId() != null) {
                    // TODO [DEPENDENCIA - Carlos]: Descomentar cuando MesaDAO esté disponible.
                    // mesaDAO.actualizarEstado(pedido.getMesaId(), "OCUPADA");
                    System.out.println("[MeseroManager] → Mesa ID " + pedido.getMesaId()
                            + " marcada como OCUPADA. (Pendiente integración MesaDAO)");
                }

                return true;

            } else {
                System.err.println("[MeseroManager] ✗ Error al registrar el pedido. "
                        + "Verifique los datos e intente nuevamente.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("[MeseroManager] ✗ Excepción inesperada al registrar pedido: "
                    + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-03: Marca un pedido como ENTREGADO y libera la mesa asociada.
     * 
     * Flujo de operación:
     *   1. Actualiza el estado del pedido a 'ENTREGADO' mediante PedidoDAO.
     *   2. Si la actualización es exitosa, coordina con MesaDAO para liberar
     *      la mesa cambiando su estado a 'LIBRE'.
     *   3. Registra en consola un log de auditoría con el resultado.
     * 
     * Esta operación es una transacción lógica de dos pasos:
     * Pedido → ENTREGADO + Mesa → LIBRE. Ambos deben ejecutarse para
     * mantener la consistencia del sistema.
     * 
     * @param pedidoId Identificador único del pedido a entregar.
     * @param mesaId   Identificador de la mesa a liberar. Puede obtenerse
     *                 del objeto Pedido antes de llamar a este método.
     * @return {@code true} si el pedido fue marcado como entregado exitosamente,
     *         {@code false} si el ID no existe, el estado no lo permite,
     *         o si ocurrió un error.
     */
    public boolean entregarPedido(int pedidoId, int mesaId) {
        try {
            // Paso 1: Actualizar el estado del pedido a ENTREGADO
            boolean pedidoEntregado = pedidoDAO.actualizarEstado(pedidoId, "ENTREGADO");

            if (pedidoEntregado) {
                System.out.println("[MeseroManager] ✓ Pedido ID " + pedidoId
                        + " marcado como ENTREGADO."
                        + " | Fecha: " + LocalDateTime.now().format(FORMATO_AUDITORIA));

                // Paso 2: Liberar la mesa asociada
                // TODO [DEPENDENCIA - Carlos]: Descomentar cuando MesaDAO esté disponible.
                // mesaDAO.actualizarEstado(mesaId, "LIBRE");
                System.out.println("[MeseroManager] → Mesa ID " + mesaId
                        + " liberada (estado: LIBRE). (Pendiente integración MesaDAO)");

                return true;

            } else {
                System.err.println("[MeseroManager] ✗ No se pudo entregar el pedido ID "
                        + pedidoId + ". Verifique que exista y su estado sea válido.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("[MeseroManager] ✗ Excepción inesperada al entregar pedido ID "
                    + pedidoId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HU-07: Anula (cancela) un pedido y registra el motivo para auditoría.
     * 
     * Flujo de operación:
     *   1. Delega la cancelación lógica (Soft Delete) al PedidoDAO.
     *      El DAO internamente valida que el pedido NO esté 'ENTREGADO'
     *      ni ya 'CANCELADO' antes de proceder (regla HU-07).
     *   2. Si la cancelación es exitosa, imprime en consola un registro
     *      de auditoría que incluye: ID del pedido, motivo de la cancelación,
     *      y fecha/hora exacta de la operación.
     * 
     * El motivo de cancelación es un campo libre que el mesero ingresa
     * desde la interfaz (ej: "Cliente se retiró", "Error en el pedido").
     * 
     * @param pedidoId Identificador único del pedido a cancelar.
     * @param motivo   Razón de la cancelación proporcionada por el mesero.
     *                 Este valor se registra en consola como log de auditoría.
     * @return {@code true} si el pedido fue cancelado exitosamente,
     *         {@code false} si el pedido no existe, ya fue entregado/cancelado,
     *         o si ocurrió un error.
     */
    public boolean anularPedido(int pedidoId, String motivo) {
        try {
            // Paso 1: Ejecutar la cancelación lógica en el DAO
            // El DAO ya valida internamente los estados no cancelables (HU-07)
            boolean pedidoCancelado = pedidoDAO.cancelarPedido(pedidoId);

            if (pedidoCancelado) {
                // Paso 2: Registro de auditoría en consola
                System.out.println("╔══════════════════════════════════════════════════════╗");
                System.out.println("║           REGISTRO DE AUDITORÍA — CANCELACIÓN        ║");
                System.out.println("╠══════════════════════════════════════════════════════╣");
                System.out.println("║  Pedido ID  : " + pedidoId);
                System.out.println("║  Motivo     : " + motivo);
                System.out.println("║  Fecha/Hora : " + LocalDateTime.now().format(FORMATO_AUDITORIA));
                System.out.println("╚══════════════════════════════════════════════════════╝");

                return true;

            } else {
                System.err.println("[MeseroManager] ✗ No se pudo cancelar el pedido ID "
                        + pedidoId + ". Puede que ya esté entregado, cancelado,"
                        + " o que el ID no exista.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("[MeseroManager] ✗ Excepción inesperada al anular pedido ID "
                    + pedidoId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
