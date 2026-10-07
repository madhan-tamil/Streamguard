package com.streamguard.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection.java
 * Central JDBC connection manager for StreamGuard.
 *
 * Demonstrates:
 * - Database Connectivity (JDBC)
 * - Exception Handling
 * - Static utility methods
 *
 * CONFIGURATION:
 * Change URL, USER, and PASSWORD below to match your local MySQL setup.
 *
 * Project : StreamGuard
 * Team : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CS, Chennai Institute of Technology
 */
public class DatabaseConnection {

    // =========================================================
    // DATABASE CONFIGURATION – Change these to match your MySQL
    // =========================================================

    private static final String URL = "jdbc:mysql://localhost:3306/streamguard"
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Kolkata";
    private static final String USER = "root";
    private static final String PASSWORD = "StreamGuard07";

    // MySQL JDBC Driver class name
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // =========================================================
    // STATIC INITIALIZER – Loads driver once at startup
    // =========================================================

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println(
                    "[DATABASE ERROR] MySQL JDBC Driver not found.\n"
                            + "  Ensure mysql-connector-j JAR is on the classpath.\n"
                            + "  Download: https://dev.mysql.com/downloads/connector/j/");
        }
    }

    /**
     * Opens and returns a new JDBC Connection to the streamguard database.
     *
     * Usage (try-with-resources):
     * try (Connection conn = DatabaseConnection.getConnection()) {
     * // use conn
     * }
     *
     * @return Connection object ready for use
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Tests the MySQL connection and prints a status message.
     * Called once at application startup in Main.java.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(
                        "  [DATABASE] MySQL connection successful.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println(
                    "  [DATABASE ERROR] Unable to connect to MySQL.\n"
                            + "  Reason: " + e.getMessage() + "\n"
                            + "  Check: Is MySQL running? Are credentials correct?\n"
                            + "         Is 'streamguard' database created?\n"
                            + "         Run: database/streamguard.sql in MySQL Workbench.");
        }
        return false;
    }
}
