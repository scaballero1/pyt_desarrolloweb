package views;

import entities.Pedido;
import managers.MeseroManager;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Vista de consola interactiva para el módulo del Mesero.
 * 
 * Forma parte de la Capa 4 (UI / Vista) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Presentar un menú interactivo al usuario vía consola.
 *   - Capturar los datos de entrada del usuario mediante Scanner.
 *   - Delegar TODA la lógica de negocio al MeseroManager (Capa 3).
 *   - Mostrar los resultados de cada operación al usuario.
 * 
 * REGLA DE ORO DE ARQUITECTURA:
 *   Esta clase NUNCA instancia ni llama directamente a PedidoDAO, MesaDAO
 *   ni a ninguna clase de la Capa 2 o la base de datos. Su único punto
 *   de contacto con el backend es la instancia de {@link MeseroManager}.
 * 
 * Flujo completo de la arquitectura:
 *   MeseroView (Capa 4) → MeseroManager (Capa 3) → DAOs (Capa 2) → BD
 * 
 * Historias de Usuario cubiertas:
 *   - [1] HU-01: Crear nuevo pedido en mesa.
 *   - [2] HU-03: Entregar pedido y liberar mesa.
 *   - [3] HU-07: Cancelar pedido con motivo de auditoría.
 * 
 * @author PoliRestaurante
 */
public class MeseroView {

    // ==================== ATRIBUTOS ====================

    /**
     * Instancia del Manager de la Capa 3.
     * Es el ÚNICO punto de contacto de esta vista con la lógica de negocio.
     */
    private MeseroManager meseroManager;

    /** Scanner para la lectura de datos desde la entrada estándar (teclado). */
    private Scanner scanner;

    /** Controla el ciclo principal del menú. */
    private boolean ejecutando;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor que inicializa el Manager y el Scanner.
     * No recibe parámetros; las dependencias se crean internamente.
     */
    public MeseroView() {
        this.meseroManager = new MeseroManager();
        this.scanner = new Scanner(System.in);
        this.ejecutando = true;
    }

    // ==================== PUNTO DE ENTRADA ====================

    /**
     * Método principal de la aplicación.
     * Instancia la vista e inicia el menú interactivo de consola.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        MeseroView vista = new MeseroView();
        vista.iniciarMenu();
    }

    // ==================== MENÚ PRINCIPAL ====================

    /**
     * Inicia el ciclo principal del menú interactivo.
     * 
     * Muestra las opciones disponibles, lee la selección del usuario
     * y delega la ejecución al método correspondiente. El ciclo se
     * repite hasta que el usuario seleccione la opción de salir.
     * 
     * Manejo de errores:
     *   - Si el usuario ingresa una letra en vez de un número, se captura
     *     la excepción {@link InputMismatchException} y se muestra un
     *     mensaje amigable sin que el programa se caiga.
     */
    public void iniciarMenu() {
        mostrarBienvenida();

        while (ejecutando) {
            mostrarOpciones();

            try {
                int opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar el salto de línea residual del buffer

                switch (opcion) {
                    case 1:
                        crearPedido();
                        break;
                    case 2:
                        entregarPedido();
                        break;
                    case 3:
                        cancelarPedido();
                        break;
                    case 4:
                        salir();
                        break;
                    default:
                        System.out.println("\n  ⚠ Opción no válida. Ingrese un número del 1 al 4.\n");
                        break;
                }

            } catch (InputMismatchException e) {
                System.out.println("\n  ⚠ Error de entrada: debe ingresar un número entero.");
                System.out.println("    Intente nuevamente.\n");
                scanner.nextLine(); // Limpiar la entrada inválida del buffer
            }
        }
    }

    // ==================== OPCIONES DEL MENÚ ====================

    /**
     * HU-01: Solicita los datos al usuario y registra un nuevo pedido en mesa.
     * 
     * Datos solicitados:
     *   - ID del mesero (obligatorio).
     *   - ID de la mesa (opcional: 0 = pedido sin mesa / para llevar).
     * 
     * Construye un objeto {@link Pedido} con tipo 'EN_MESA' (o 'PARA_LLEVAR'
     * si no se asigna mesa) y lo envía al Manager para su registro.
     */
    private void crearPedido() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║        CREAR NUEVO PEDIDO  (HU-01)          ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        try {
            // Solicitar ID del mesero
            System.out.print("  Ingrese el ID del mesero: ");
            int meseroId = scanner.nextInt();
            scanner.nextLine();

            // Solicitar ID de la mesa
            System.out.print("  Ingrese el ID de la mesa (0 = sin mesa / para llevar): ");
            int mesaInput = scanner.nextInt();
            scanner.nextLine();

            // Determinar tipo de pedido y valor de mesa_id según la entrada
            String tipoPedido;
            Integer mesaId;

            if (mesaInput > 0) {
                tipoPedido = "EN_MESA";
                mesaId = mesaInput;
            } else {
                tipoPedido = "PARA_LLEVAR";
                mesaId = null; // Sin mesa asociada → se enviará NULL a la BD
            }

            // Construir el objeto Pedido (el estado lo fuerza el DAO a 'PENDIENTE')
            Pedido nuevoPedido = new Pedido(tipoPedido, mesaId, meseroId, "PENDIENTE");

            // Delegar al Manager
            System.out.println("\n  Procesando...");
            boolean resultado = meseroManager.registrarNuevoPedido(nuevoPedido);

            if (resultado) {
                System.out.println("  ──────────────────────────────────────────");
                System.out.println("  ✓ Pedido creado exitosamente.");
                System.out.println("    Tipo    : " + tipoPedido);
                System.out.println("    Mesero  : " + meseroId);
                System.out.println("    Mesa    : " + (mesaId != null ? mesaId : "N/A (sin mesa)"));
                System.out.println("    Estado  : PENDIENTE");
                System.out.println("  ──────────────────────────────────────────\n");
            } else {
                System.out.println("  ✗ No se pudo crear el pedido. Revise la consola para detalles.\n");
            }

        } catch (InputMismatchException e) {
            System.out.println("\n  ⚠ Error: los IDs deben ser números enteros. Operación cancelada.\n");
            scanner.nextLine();
        }
    }

    /**
     * HU-03: Solicita los datos al usuario y marca un pedido como ENTREGADO.
     * 
     * Datos solicitados:
     *   - ID del pedido a entregar.
     *   - ID de la mesa a liberar.
     * 
     * Delega al Manager, que actualiza el pedido y libera la mesa.
     */
    private void entregarPedido() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║         ENTREGAR PEDIDO  (HU-03)            ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        try {
            // Solicitar ID del pedido
            System.out.print("  Ingrese el ID del pedido a entregar: ");
            int pedidoId = scanner.nextInt();
            scanner.nextLine();

            // Solicitar ID de la mesa
            System.out.print("  Ingrese el ID de la mesa a liberar: ");
            int mesaId = scanner.nextInt();
            scanner.nextLine();

            // Delegar al Manager
            System.out.println("\n  Procesando...");
            boolean resultado = meseroManager.entregarPedido(pedidoId, mesaId);

            if (resultado) {
                System.out.println("  ──────────────────────────────────────────");
                System.out.println("  ✓ Pedido ID " + pedidoId + " entregado exitosamente.");
                System.out.println("    Mesa ID " + mesaId + " liberada.");
                System.out.println("  ──────────────────────────────────────────\n");
            } else {
                System.out.println("  ✗ No se pudo entregar el pedido. Revise la consola para detalles.\n");
            }

        } catch (InputMismatchException e) {
            System.out.println("\n  ⚠ Error: los IDs deben ser números enteros. Operación cancelada.\n");
            scanner.nextLine();
        }
    }

    /**
     * HU-07: Solicita los datos al usuario y cancela (anula) un pedido.
     * 
     * Datos solicitados:
     *   - ID del pedido a cancelar.
     *   - Motivo de la cancelación (texto libre para auditoría).
     * 
     * Delega al Manager, que ejecuta la cancelación lógica (Soft Delete)
     * y registra el motivo en consola como log de auditoría.
     */
    private void cancelarPedido() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║        CANCELAR PEDIDO  (HU-07)             ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        try {
            // Solicitar ID del pedido
            System.out.print("  Ingrese el ID del pedido a cancelar: ");
            int pedidoId = scanner.nextInt();
            scanner.nextLine();

            // Solicitar motivo de cancelación
            System.out.print("  Ingrese el motivo de la cancelación: ");
            String motivo = scanner.nextLine().trim();

            // Validar que el motivo no esté vacío
            if (motivo.isEmpty()) {
                System.out.println("\n  ⚠ El motivo de cancelación es obligatorio. Operación cancelada.\n");
                return;
            }

            // Delegar al Manager
            System.out.println("\n  Procesando...");
            boolean resultado = meseroManager.anularPedido(pedidoId, motivo);

            if (resultado) {
                System.out.println("  ──────────────────────────────────────────");
                System.out.println("  ✓ Pedido ID " + pedidoId + " cancelado exitosamente.");
                System.out.println("  ──────────────────────────────────────────\n");
            } else {
                System.out.println("  ✗ No se pudo cancelar el pedido. Revise la consola para detalles.\n");
            }

        } catch (InputMismatchException e) {
            System.out.println("\n  ⚠ Error: el ID debe ser un número entero. Operación cancelada.\n");
            scanner.nextLine();
        }
    }

    // ==================== MÉTODOS AUXILIARES DE INTERFAZ ====================

    /**
     * Muestra el encabezado de bienvenida al iniciar la aplicación.
     */
    private void mostrarBienvenida() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                          ║");
        System.out.println("║          🍽  P O L I R E S T A U R A N T E  🍽           ║");
        System.out.println("║              Sistema de Gestión de Pedidos                ║");
        System.out.println("║                  Módulo del Mesero                        ║");
        System.out.println("║                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Muestra las opciones del menú principal.
     */
    private void mostrarOpciones() {
        System.out.println("═══════════════════ MENÚ PRINCIPAL ═══════════════════");
        System.out.println("  [1]  Crear nuevo pedido en mesa       (HU-01)");
        System.out.println("  [2]  Entregar pedido                  (HU-03)");
        System.out.println("  [3]  Cancelar pedido                  (HU-07)");
        System.out.println("  [4]  Salir");
        System.out.println("═════════════════════════════════════════════════════");
        System.out.print("  Seleccione una opción: ");
    }

    /**
     * Finaliza el ciclo del menú y cierra los recursos.
     */
    private void salir() {
        ejecutando = false;
        scanner.close();
        System.out.println();
        System.out.println("  ──────────────────────────────────────────────────");
        System.out.println("  Sesión finalizada. ¡Gracias por usar PoliRestaurante!");
        System.out.println("  ──────────────────────────────────────────────────");
        System.out.println();
    }
}
