package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // La URL asume que tu base de datos se llama 'polirestaurante'
    private static final String URL = "jdbc:mysql://localhost:3306/polirestaurante";
    private static final String USER = "root";
    // ¡RECUERDA CAMBIAR ESTO POR LA CONTRASEÑA QUE PUSISTE EN EL INSTALADOR!
    private static final String PASSWORD = "Uch@2005";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}