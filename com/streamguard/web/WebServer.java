package com.streamguard.web;

import com.streamguard.database.DatabaseConnection;
import com.streamguard.manager.AccountManager;
import com.streamguard.model.BankAccount;
import com.streamguard.service.DatabaseManager;
import com.streamguard.service.OutputFileLogger;
import com.streamguard.service.TransactionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * WebServer.java
 * Embedded Java HTTP server for the StreamGuard web dashboard.
 *
 * Changes from original:
 *   - /api/dataset  now reads from MySQL (not StreamGuard_Dataset.csv)
 *   - /api/history  now reads from MySQL transactions table
 *   - /api/blocked  now reads from MySQL (FRAUD records)
 *   - DatabaseManager is shared with TransactionService
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class WebServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {

        // =========================================================
        // DATABASE INITIALIZATION
        // =========================================================

        System.out.println("  [WEB SERVER] Connecting to MySQL...");
        boolean dbConnected = DatabaseConnection.testConnection();

        DatabaseManager   databaseManager   = new DatabaseManager();
        AccountManager    accountManager    = new AccountManager();
        TransactionService transactionService = new TransactionService(databaseManager);

        if (dbConnected) {
            databaseManager.loadAccountsFromDatabase(accountManager);
        } else {
            System.out.println(
                "  [INFO] No MySQL connection. Using fallback in-memory accounts."
            );
            accountManager.loadDefaultAccounts();
        }

        // Initialize output.db with all existing transactions in tabular format
        OutputFileLogger.updateOutputFile();

        // =========================================================
        // HTTP SERVER SETUP
        // =========================================================

        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0
        );


        // =========================================================
        // SERVE WEB PAGE
        // =========================================================

        server.createContext("/", exchange -> {

            try {

                Path file = Path.of(
                    "com", "streamguard", "src", "main", "webapp", "index.html"
                );

                if (!Files.exists(file)) {
                    sendResponse(exchange, 404,
                        "index.html not found at: " + file.toAbsolutePath());
                    return;
                }

                byte[] content = Files.readAllBytes(file);

                exchange.getResponseHeaders()
                        .set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, content.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }

            } catch (Exception e) {
                sendResponse(exchange, 500, "Server error: " + e.getMessage());
            }
        });


        // =========================================================
        // SERVER STATUS
        // =========================================================

        server.createContext("/api/status", exchange ->
            sendJson(exchange, 200,
                "{\"status\":\"StreamGuard server is running\","
                + "\"database\":\"" + (dbConnected ? "MySQL Connected" : "No DB") + "\"}"
            )
        );


        // =========================================================
        // DATASET SUMMARY  ← now from MySQL, not CSV
        // =========================================================

        server.createContext("/api/dataset", exchange -> {

            try {
                String json;
                if (dbConnected) {
                    json = databaseManager.getDatasetSummaryJson();
                } else {
                    // Fallback to real dataset figures when MySQL is offline
                    json = "{"
                         + "\"success\":true,"
                         + "\"total\":58679,"
                         + "\"safe\":55227,"
                         + "\"fraud\":3452,"
                         + "\"fraudReason\":\"Location mismatch\","
                         + "\"fraudReasons\":{\"Location mismatch\":3452}"
                         + "}";
                }
                sendJson(exchange, 200, json);

            } catch (Exception e) {
                sendJson(exchange, 500,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });


        // =========================================================
        // GET ALL ACCOUNTS
        // =========================================================

        server.createContext("/api/accounts", exchange -> {

            try {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                for (Map.Entry<Integer, BankAccount> entry
                        : accountManager.getAllAccounts().entrySet()) {

                    BankAccount account = entry.getValue();

                    if (!first) json.append(",");

                    json.append("{")
                        .append("\"accountId\":").append(account.getAccountId())
                        .append(",\"holder\":\"").append(escape(account.getAccountHolder())).append("\"")
                        .append(",\"type\":\"").append(escape(account.getAccountType())).append("\"")
                        .append(",\"balance\":").append(account.getBalance())
                        .append(",\"city\":\"").append(escape(account.getRegisteredCity())).append("\"")
                        .append("}");

                    first = false;
                }

                json.append("]");
                sendJson(exchange, 200, json.toString());

            } catch (Exception e) {
                sendJson(exchange, 500,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });


        // =========================================================
        // GET SINGLE ACCOUNT
        // =========================================================

        server.createContext("/api/account", exchange -> {

            try {

                String query = exchange.getRequestURI().getQuery();

                if (query == null || !query.startsWith("id=")) {
                    sendJson(exchange, 400, "{\"error\":\"Account ID required\"}");
                    return;
                }

                int accountId = Integer.parseInt(query.substring(3));
                BankAccount account = accountManager.getAccount(accountId);

                if (account == null) {
                    sendJson(exchange, 404, "{\"error\":\"Account not found\"}");
                    return;
                }

                String response = "{"
                    + "\"accountId\":"   + account.getAccountId()
                    + ",\"holder\":\""   + escape(account.getAccountHolder()) + "\""
                    + ",\"type\":\""     + escape(account.getAccountType()) + "\""
                    + ",\"balance\":"    + account.getBalance()
                    + ",\"city\":\""     + escape(account.getRegisteredCity()) + "\""
                    + "}";

                sendJson(exchange, 200, response);

            } catch (Exception e) {
                sendJson(exchange, 500,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });


        // =========================================================
        // PROCESS NEW TRANSACTION  (POST)
        // =========================================================

        server.createContext("/api/transaction", exchange -> {

            try {

                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    sendJson(exchange, 405, "{\"error\":\"POST method required\"}");
                    return;
                }

                String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                );

                Map<String, String> data = parseFormData(body);

                if (!data.containsKey("accountId")
                        || !data.containsKey("amount")
                        || !data.containsKey("location")) {

                    sendJson(exchange, 400,
                        "{\"error\":\"accountId, amount and location are required\"}");
                    return;
                }

                int    accountId = Integer.parseInt(data.get("accountId"));
                double amount    = Double.parseDouble(data.get("amount"));
                String location  = data.get("location").trim();

                if (amount <= 0) {
                    sendJson(exchange, 400,
                        "{\"error\":\"Amount must be greater than zero\"}");
                    return;
                }

                if (location.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"Location is required\"}");
                    return;
                }

                BankAccount account = accountManager.getAccount(accountId);

                if (account == null) {
                    sendJson(exchange, 404, "{\"error\":\"Account not found\"}");
                    return;
                }

                int blockedBefore = transactionService.getBlockedTransactions().size();

                // Run the fraud engine
                boolean safe = transactionService.processTransaction(
                    account, amount, location
                );

                String reason = "";
                if (!safe) {
                    var blocked = transactionService.getBlockedTransactions();
                    if (blocked.size() > blockedBefore) {
                        reason = blocked.get(blocked.size() - 1).getFraudReason();
                    }
                }

                String response;
                if (safe) {
                    response = "{"
                        + "\"success\":true,"
                        + "\"status\":\"SAFE\","
                        + "\"message\":\"Transaction approved successfully\","
                        + "\"accountId\":" + accountId + ","
                        + "\"amount\":" + amount + ","
                        + "\"location\":\"" + escape(location) + "\","
                        + "\"newBalance\":" + account.getBalance()
                        + "}";
                } else {
                    response = "{"
                        + "\"success\":false,"
                        + "\"status\":\"FRAUD\","
                        + "\"message\":\"Transaction blocked\","
                        + "\"reason\":\"" + escape(reason) + "\","
                        + "\"accountId\":" + accountId + ","
                        + "\"amount\":" + amount + ","
                        + "\"location\":\"" + escape(location) + "\""
                        + "}";
                }

                sendJson(exchange, 200, response);

            } catch (NumberFormatException e) {
                sendJson(exchange, 400,
                    "{\"error\":\"Invalid account ID or amount\"}");

            } catch (Exception e) {
                sendJson(exchange, 500,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });


        // =========================================================
        // TRANSACTION HISTORY  ← now from MySQL
        // =========================================================

        server.createContext("/api/history", exchange -> {

            try {
                String json;
                if (dbConnected) {
                    json = databaseManager.getHistoryJson();
                } else {
                    json = "[]";
                }
                sendJson(exchange, 200, json);

            } catch (Exception e) {
                sendJson(exchange, 500,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });


        // =========================================================
        // BLOCKED TRANSACTIONS  ← now from MySQL
        // =========================================================

        server.createContext("/api/blocked", exchange -> {

            try {
                String json;
                if (dbConnected) {
                    json = databaseManager.getBlockedJson();
                } else {
                    // Fall back to in-session in-memory list
                    var blocked = transactionService.getBlockedTransactions();
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    for (var txn : blocked) {
                        if (!first) sb.append(",");
                        sb.append("{")
                          .append("\"txnId\":").append(txn.getTxnId())
                          .append(",\"amount\":").append(txn.getAmount())
                          .append(",\"location\":\"").append(escape(txn.getLocation())).append("\"")
                          .append(",\"status\":\"").append(escape(txn.getStatus())).append("\"")
                          .append(",\"reason\":\"").append(escape(txn.getFraudReason())).append("\"")
                          .append(",\"time\":\"").append(escape(txn.getTimestamp().toString())).append("\"")
                          .append("}");
                        first = false;
                    }
                    sb.append("]");
                    json = sb.toString();
                }
                sendJson(exchange, 200, json);

            } catch (Exception e) {
                sendJson(exchange, 500,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        });


        // =========================================================
        // START SERVER
        // =========================================================

        server.start();

        System.out.println();
        System.out.println("==============================================");
        System.out.println("       STREAMGUARD WEB SERVER");
        System.out.println("==============================================");
        System.out.println("Server running at:");
        System.out.println("http://localhost:" + PORT);
        System.out.println();
        System.out.println("Database : " + (dbConnected ? "MySQL Connected" : "No DB (in-memory fallback)"));
        System.out.println("Open this address in your browser.");
        System.out.println("==============================================");
        System.out.println("Press ENTER or Ctrl+C to stop the server.");

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n  [WEB SERVER] Shutting down...");
            server.stop(1);
        }));

        // Automatically open the web dashboard in the default browser
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI("http://localhost:" + PORT));
                }
            }
        } catch (Exception ignored) {
            // Silently fall back to manual browser access
        }

        // Keep the main thread alive so the server keeps running
        System.in.read();
        server.stop(0);
    }


    // =============================================================
    // PARSE FORM DATA
    // =============================================================

    private static Map<String, String> parseFormData(String body) {

        Map<String, String> result = new HashMap<>();

        if (body == null || body.isEmpty()) return result;

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                String key   = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }

        return result;
    }


    // =============================================================
    // SEND JSON
    // =============================================================

    private static void sendJson(HttpExchange exchange, int status, String response)
            throws IOException {

        byte[] data = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }


    // =============================================================
    // SEND PLAIN RESPONSE
    // =============================================================

    private static void sendResponse(HttpExchange exchange, int status, String response)
            throws IOException {

        byte[] data = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }


    // =============================================================
    // ESCAPE JSON STRINGS
    // =============================================================

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\r", "\\r")
                   .replace("\n", "\\n");
    }
}