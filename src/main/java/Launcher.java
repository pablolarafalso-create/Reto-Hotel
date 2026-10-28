/**
 * Clase de arranque que NO extiende Application.
 * Evita el error "el método principal debe devolver void" / "JavaFX runtime components are missing"
 * al ejecutar con el botón Run de VS Code con las librerías JavaFX en el classpath.
 */
public class Launcher {
    public static void main(String[] args) {
        HotelApp.main(args);
    }
}
