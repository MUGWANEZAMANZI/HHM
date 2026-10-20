package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {


    /**
     * This method is used to get a connection to the database.
     * @return Connection object
     */
    public static Connection getConnection() {
        try(Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hhm", "root", "root")) {
            return con;
        } catch (SQLException e) {
            IO.println("Error connecting to database: " + e.getMessage());
            return null;
        }
    }
}
