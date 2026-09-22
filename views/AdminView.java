package views;

import entities.Producto;
import managers.AdminManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Vista de consola interactiva para el módulo del Administrador.
 * 
 * Forma parte de la Capa 4 (UI / Vista) de la arquitectura de 4 capas
 * del proyecto PoliRestaurante.
 * 
 * Responsabilidades:
 *   - Presentar un menú interactivo al administrador vía consola.
 *   - Capturar los datos de entrada mediante Scanner.
 *   - Delegar TODA la lógica de negocio al AdminManager (Capa 3).
 *   - Mostrar los resultados de cada operación al usuario.
 * 
 * REGLA DE ORO DE ARQUITECTURA:
 *   Esta clase NUNCA instancia ni llama directamente a ProductoDAO ni a
 *   ninguna clase de la Capa 2 o la base de datos. Su único punto de
 *   contacto con el backend es la instancia de {@link AdminManager}.
 * 
 * Flujo completo de la arquitectura:
 *   AdminView (Capa 4) → AdminManager (Capa 3) → ProductoDAO (Capa 2) → BD
 * 
 * Historias de Usuario cubiertas:
 *   - [1] HU-06: Registrar plato nuevo en el menú (crear).
 *   - [2] HU-06: Editar datos de un plato (nombre/descripción/categoría).
 *   - [3] HU-06: Cambiar precio de un plato.
 *   - [4] HU-06: Cambiar disponibilidad de un plato (criterio de aceptación 2).
 *   - [5] HU-06: Eliminar un plato del menú.
 *   - [6] HU-06 (apoyo): Ver menú completo.
 *   - [7] Salir.
 * 
 * @author PoliRestaurante
 */
public class AdminView {

    // ==================== ATRIBUTOS ====================

    /**
     * Instancia del Manager de la Capa 3.
     * Es el ÚNICO punto de contacto de esta vista con la lógica de negocio.
     */
    private AdminManager adminManager;

    /** Scanner para la lectura de datos desde la entrada estándar (teclado). */
    private Scanner scanner;

    /** Controla el ciclo principal del menú. */
    private boolean ejecutando;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor que inicializa el Manager y el Scanner.
     * No recibe parámetros; las dependencias se crean internamente.
     */
    public AdminView() {
        this.adminManager = new AdminManager();
        this.scanner = new Scanner(System.in);
        this.ejecutando = true;
    }

    // ==================== PUNTO DE ENTRADA ====================

    /**
     * Método principal de la aplicación del administrador.
     * Instancia la vista e inicia el menú interactivo de consola.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        AdminView vista = new AdminView();
        vista.iniciarMenu();
    }

    // ==================== MENÚ PRINCIPAL ====================

    /**
     * Inicia el ciclo principal del menú interactivo.
     * 
     * Muestra las opciones, lee la selección y delega la ejecución al método
     * correspondiente. El ciclo se repite hasta que se elija salir.
     * 
     * Manejo de errores: la opción se lee como texto y se convierte a entero;
     * si el usuario escribe algo no numérico se muestra un mensaje amigable
     * sin que el programa se caiga.
     */
    public void iniciarMenu() {
        mostrarBienvenida();

        while (ejecutando) {
            mostrarOpciones();

            // Si la entrada se cerró (Ctrl+D / fin de stdin) se sale limpiamente
            if (!scanner.hasNextLine()) {
                salir();
                break;
            }

            String entrada = scanner.nextLine().trim();

            try {
                int opcion = Integer.parseInt(entrada);

                switch (opcion) {
                    case 1:
                        registrarPlato();
                        break;
                    case 2:
                        editarPlato();
                        break;
                    case 3:
                        cambiarPrecio();
                        break;
                    case 4:
                        cambiarDisponibilidad();
                        break;
                    case 5:
                        eliminarPlato();
                        break;
                    case 6:
                        verMenu();
                        break;
                    case 7:
                        salir();
                        break;
                    default:
                        System.out.println("\n  ⚠ Opción no válida. Ingrese un número del 1 al 7.\n");
                        break;
                }

            } catch (NumberFormatException e) {
                System.out.println("\n  ⚠ Error de entrada: debe ingresar un número entero.");
                System.out.println("    Intente nuevamente.\n");
            }
        }
    }

    // ==================== OPCIONES DEL MENÚ ====================

    /**
     * HU-06: Solicita los datos al administrador y registra un plato nuevo.
     * 
     * Datos solicitados:
     *   - Categoría (se muestra la lista de categorías disponibles).
     *   - Nombre del plato (obligatorio).
     *   - Descripción (opcional: Enter para omitir).
     *   - Precio (mayor a 0).
     * 
     * Las validaciones de negocio las hace el Manager; la vista solo
     * verifica que los números tengan formato válido.
     */
    private void registrarPlato() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║      REGISTRAR PLATO NUEVO  (HU-06)         ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // Mostrar categorías disponibles
        Map<Integer, String> categorias = adminManager.listarCategorias();
        if (categorias.isEmpty()) {
            System.out.println("  ⚠ No hay categorías registradas. "
                    + "Cree al menos una categoría en la base de datos primero.\n");
            return;
        }

        System.out.println("  Categorías disponibles:");
        for (Map.Entry<Integer, String> cat : categorias.entrySet()) {
            System.out.println("    [" + cat.getKey() + "] " + cat.getValue());
        }

        try {
            int categoriaId = leerEntero("\n  ID de la categoría: ");
            String nombre = leerTexto("  Nombre del plato: ");
            String descripcion = leerTexto("  Descripción (Enter para omitir): ");
            BigDecimal precio = leerPrecio("  Precio: $");

            System.out.println("\n  Procesando...");
            Producto creado = adminManager.registrarPlato(categoriaId, nombre, descripcion, precio);

            if (creado != null) {
                System.out.println("  ──────────────────────────────────────────");
                System.out.println("  ✓ Plato registrado exitosamente.");
                System.out.println("    ID        : " + creado.getId());
                System.out.println("    Nombre    : " + creado.getNombre());
                System.out.println("    Categoría : " + categorias.get(creado.getCategoriaId()));
                System.out.println("    Precio    : $" + creado.getPrecio().toPlainString());
                System.out.println("  ──────────────────────────────────────────\n");
            } else {
                System.out.println("  ✗ No se pudo registrar el plato. Revise la consola para detalles.\n");
            }

        } catch (NumberFormatException e) {
            System.out.println("\n  ⚠ Error: " + e.getMessage() + " Operación cancelada.\n");
        }
    }

    /**
     * HU-06: Solicita los datos al administrador y edita el nombre, la
     * descripción y la categoría de un plato existente.
     * 
     * Muestra primero el menú completo para que el administrador identifique
     * el ID del plato a editar. Cada campo se puede dejar igual presionando
     * Enter, para no tener que reescribir todo cuando solo se quiere cambiar
     * un dato.
     */
    private void editarPlato() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║        EDITAR DATOS DE PLATO  (HU-06)       ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        List<Producto> menu = adminManager.listarMenu();
        if (menu.isEmpty()) {
            System.out.println("  ⚠ No hay platos registrados todavía.\n");
            return;
        }
        imprimirTabla(menu);

        Map<Integer, String> categorias = adminManager.listarCategorias();

        try {
            int productoId = leerEntero("\n  ID del plato a editar: ");

            Producto actual = menu.stream()
                    .filter(p -> p.getId() == productoId)
                    .findFirst()
                    .orElse(null);

            if (actual == null) {
                System.out.println("  ✗ No existe un plato con ID " + productoId + ".\n");
                return;
            }

            System.out.println("\n  Deje vacío (Enter) para conservar el valor actual.");
            System.out.println("  Categorías disponibles:");
            for (Map.Entry<Integer, String> cat : categorias.entrySet()) {
                System.out.println("    [" + cat.getKey() + "] " + cat.getValue());
            }

            String entradaCategoria = leerTexto("\n  Categoría actual [" + actual.getCategoriaId()
                    + " - " + categorias.getOrDefault(actual.getCategoriaId(), "?") + "]: ");
            int categoriaId = entradaCategoria.isEmpty()
                    ? actual.getCategoriaId()
                    : Integer.parseInt(entradaCategoria);

            String entradaNombre = leerTexto("  Nombre actual [" + actual.getNombre() + "]: ");
            String nombre = entradaNombre.isEmpty() ? actual.getNombre() : entradaNombre;

            String descripcionActual = actual.getDescripcion() == null ? "" : actual.getDescripcion();
            String entradaDescripcion = leerTexto("  Descripción actual [" + descripcionActual + "]: ");
            String descripcion = entradaDescripcion.isEmpty() ? actual.getDescripcion() : entradaDescripcion;

            System.out.println("\n  Procesando...");
            boolean resultado = adminManager.editarDatosPlato(productoId, categoriaId, nombre, descripcion);

            if (resultado) {
                System.out.println("  ✓ Plato ID " + productoId + " actualizado exitosamente.\n");
            } else {
                System.out.println("  ✗ No se pudo editar el plato. Revise la consola para detalles.\n");
            }

        } catch (NumberFormatException e) {
            System.out.println("\n  ⚠ Error: la categoría debe ser un número entero. Operación cancelada.\n");
        }
    }

    /**
     * HU-06: Solicita los datos al administrador y cambia el precio de un plato.
     * 
     * Datos solicitados:
     *   - ID del plato (se muestra antes el menú para poder consultarlo).
     *   - Nuevo precio.
     * 
     * Delega al Manager, que valida y registra la auditoría del cambio.
     */
    private void cambiarPrecio() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║     CAMBIAR PRECIO DE PLATO  (HU-06)        ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // Mostrar el menú para que el admin vea los IDs y precios actuales
        List<Producto> menu = adminManager.listarMenu();
        if (menu.isEmpty()) {
            System.out.println("  ⚠ No hay platos registrados todavía.\n");
            return;
        }
        imprimirTabla(menu);

        try {
            int productoId = leerEntero("\n  ID del plato a modificar: ");
            BigDecimal nuevoPrecio = leerPrecio("  Nuevo precio: $");

            System.out.println("\n  Procesando...");
            boolean resultado = adminManager.cambiarPrecio(productoId, nuevoPrecio);

            if (resultado) {
                System.out.println("  ✓ Precio del plato ID " + productoId
                        + " actualizado a $" + nuevoPrecio.toPlainString() + ".\n");
            } else {
                System.out.println("  ✗ No se pudo cambiar el precio. Revise la consola para detalles.\n");
            }

        } catch (NumberFormatException e) {
            System.out.println("\n  ⚠ Error: " + e.getMessage() + " Operación cancelada.\n");
        }
    }

    /**
     * HU-06: Solicita los datos al administrador y cambia la disponibilidad
     * de un plato (criterio de aceptación 2 de HU-06).
     * 
     * Un plato marcado como "No disponible" deja de poder ser seleccionado
     * por el mesero al crear nuevos pedidos.
     */
    private void cambiarDisponibilidad() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║    CAMBIAR DISPONIBILIDAD DE PLATO  (HU-06) ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        List<Producto> menu = adminManager.listarMenu();
        if (menu.isEmpty()) {
            System.out.println("  ⚠ No hay platos registrados todavía.\n");
            return;
        }
        imprimirTabla(menu);

        try {
            int productoId = leerEntero("\n  ID del plato a modificar: ");
            String entrada = leerTexto("  ¿Disponible? (S = Sí / N = No): ").trim().toUpperCase();

            if (!entrada.equals("S") && !entrada.equals("N")) {
                System.out.println("\n  ⚠ Respuesta no válida. Escriba S o N. Operación cancelada.\n");
                return;
            }

            boolean disponible = entrada.equals("S");

            System.out.println("\n  Procesando...");
            boolean resultado = adminManager.cambiarDisponibilidad(productoId, disponible);

            if (resultado) {
                System.out.println("  ✓ Disponibilidad del plato ID " + productoId + " actualizada a "
                        + (disponible ? "DISPONIBLE" : "NO DISPONIBLE") + ".\n");
            } else {
                System.out.println("  ✗ No se pudo cambiar la disponibilidad. Revise la consola para detalles.\n");
            }

        } catch (NumberFormatException e) {
            System.out.println("\n  ⚠ Error: " + e.getMessage() + " Operación cancelada.\n");
        }
    }

    /**
     * HU-06: Solicita el ID de un plato y lo elimina del menú.
     * 
     * Si el plato ya tiene pedidos asociados en el historial, el Manager
     * rechaza la eliminación y sugiere usar "Cambiar disponibilidad" en su
     * lugar para retirarlo del menú sin perder la trazabilidad de ventas.
     */
    private void eliminarPlato() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║          ELIMINAR PLATO  (HU-06)            ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        List<Producto> menu = adminManager.listarMenu();
        if (menu.isEmpty()) {
            System.out.println("  ⚠ No hay platos registrados todavía.\n");
            return;
        }
        imprimirTabla(menu);

        try {
            int productoId = leerEntero("\n  ID del plato a eliminar: ");
            String confirmacion = leerTexto("  ¿Confirma la eliminación? (S/N): ").trim().toUpperCase();

            if (!confirmacion.equals("S")) {
                System.out.println("\n  Operación cancelada por el usuario.\n");
                return;
            }

            System.out.println("\n  Procesando...");
            boolean resultado = adminManager.eliminarPlato(productoId);

            if (resultado) {
                System.out.println("  ✓ Plato ID " + productoId + " eliminado del menú.\n");
            } else {
                System.out.println("  ✗ No se pudo eliminar el plato. Revise la consola para detalles.\n");
            }

        } catch (NumberFormatException e) {
            System.out.println("\n  ⚠ Error: " + e.getMessage() + " Operación cancelada.\n");
        }
    }

    /**
     * HU-06 (apoyo): Muestra el menú completo del restaurante en formato de tabla.
     */
    private void verMenu() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║          MENÚ COMPLETO  (HU-06)             ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        List<Producto> menu = adminManager.listarMenu();
        if (menu.isEmpty()) {
            System.out.println("  (No hay platos registrados todavía)\n");
            return;
        }

        imprimirTabla(menu);
        System.out.println();
    }

    // ==================== MÉTODOS AUXILIARES DE INTERFAZ ====================

    /**
     * Imprime una lista de productos como tabla, con la categoría y la
     * disponibilidad de cada uno.
     *
     * @param productos Lista de productos a mostrar.
     */
    private void imprimirTabla(List<Producto> productos) {
        Map<Integer, String> categorias = adminManager.listarCategorias();

        System.out.println("  ─────────────────────────────────────────────────────────────────");
        System.out.printf("  %-4s %-28s %-16s %12s  %s%n",
                "ID", "NOMBRE", "CATEGORÍA", "PRECIO", "DISP.");
        System.out.println("  ─────────────────────────────────────────────────────────────────");

        for (Producto p : productos) {
            String categoria = categorias.getOrDefault(p.getCategoriaId(), "?");
            System.out.printf("  %-4d %-28s %-16s %12s  %s%n",
                    p.getId(),
                    recortar(p.getNombre(), 28),
                    recortar(categoria, 16),
                    "$" + p.getPrecio().toPlainString(),
                    p.isDisponible() ? "Sí" : "No");
        }

        System.out.println("  ─────────────────────────────────────────────────────────────────");
    }

    /**
     * Recorta un texto a una longitud máxima para mantener alineada la tabla.
     *
     * @param texto Texto original.
     * @param max   Longitud máxima.
     * @return Texto recortado (con '…' al final si fue necesario).
     */
    private String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    /**
     * Lee un número entero desde la consola.
     *
     * @param mensaje Texto que se muestra al usuario.
     * @return El entero ingresado.
     * @throws NumberFormatException Si el texto ingresado no es un entero válido.
     */
    private int leerEntero(String mensaje) {
        System.out.print(mensaje);
        String entrada = scanner.nextLine().trim();
        try {
            return Integer.parseInt(entrada);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("'" + entrada + "' no es un número entero válido.");
        }
    }

    /**
     * Lee una línea de texto desde la consola.
     *
     * @param mensaje Texto que se muestra al usuario.
     * @return El texto ingresado, sin espacios en los extremos.
     */
    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    /**
     * Lee un precio desde la consola como {@link BigDecimal}.
     * Acepta coma o punto como separador decimal (ej: "25000", "12.50", "12,50").
     *
     * @param mensaje Texto que se muestra al usuario.
     * @return El precio ingresado.
     * @throws NumberFormatException Si el texto ingresado no es un número válido.
     */
    private BigDecimal leerPrecio(String mensaje) {
        System.out.print(mensaje);
        String entrada = scanner.nextLine().trim().replace(',', '.');
        try {
            return new BigDecimal(entrada);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("'" + entrada + "' no es un precio válido.");
        }
    }

    /**
     * Muestra el encabezado de bienvenida al iniciar la aplicación.
     */
    private void mostrarBienvenida() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                          ║");
        System.out.println("║          🍽  P O L I R E S T A U R A N T E  🍽           ║");
        System.out.println("║              Sistema de Gestión de Pedidos                ║");
        System.out.println("║              Módulo del Administrador                     ║");
        System.out.println("║                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Muestra las opciones del menú principal.
     */
    private void mostrarOpciones() {
        System.out.println("═══════════════════ MENÚ PRINCIPAL ═══════════════════");
        System.out.println("  [1]  Registrar plato nuevo          (HU-06)");
        System.out.println("  [2]  Editar datos de un plato       (HU-06)");
        System.out.println("  [3]  Cambiar precio de un plato     (HU-06)");
        System.out.println("  [4]  Cambiar disponibilidad         (HU-06)");
        System.out.println("  [5]  Eliminar plato                 (HU-06)");
        System.out.println("  [6]  Ver menú completo               (HU-06)");
        System.out.println("  [7]  Salir");
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
