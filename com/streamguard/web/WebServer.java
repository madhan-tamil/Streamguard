package com.streamguard.web;

import com.streamguard.manager.AccountManager;
import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;
import com.streamguard.service.FileManager;
import com.streamguard.service.TransactionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WebServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {

        AccountManager accountManager =
                new AccountManager();

        TransactionService transactionService =
                new TransactionService();

        FileManager fileManager =
                new FileManager();

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(PORT),
                        0
                );


        // =========================================================
        // SERVE WEB PAGE
        // =========================================================

        server.createContext("/", exchange -> {

            try {

                Path file = Path.of(
                        "com",
                        "streamguard",
                        "src",
                        "main",
                        "webapp",
                        "index.html"
                );

                if (!Files.exists(file)) {

                    sendResponse(
                            exchange,
                            404,
                            "index.html not found at: "
                                    + file.toAbsolutePath()
                    );

                    return;
                }

                byte[] content =
                        Files.readAllBytes(file);

                exchange.getResponseHeaders()
                        .set(
                                "Content-Type",
                                "text/html; charset=UTF-8"
                        );

                exchange.sendResponseHeaders(
                        200,
                        content.length
                );

                try (OutputStream os =
                             exchange.getResponseBody()) {

                    os.write(content);
                }

            } catch (Exception e) {

                sendResponse(
                        exchange,
                        500,
                        "Server error: "
                                + e.getMessage()
                );
            }
        });


        // =========================================================
        // SERVER STATUS
        // =========================================================

        server.createContext("/api/status", exchange -> {

            sendJson(
                    exchange,
                    200,
                    "{\"status\":\"StreamGuard server is running\"}"
            );
        });


        // =========================================================
        // DATASET SUMMARY
        // =========================================================

        server.createContext("/api/dataset", exchange -> {

            try {

                Path datasetFile =
                        Path.of("StreamGuard_Dataset.csv");

                if (!Files.exists(datasetFile)) {

                    sendJson(
                            exchange,
                            404,
                            "{\"error\":\"StreamGuard_Dataset.csv not found\"}"
                    );

                    return;
                }

                int totalRecords = 0;
                int fraudRecords = 0;
                int safeRecords = 0;

                Map<String, Integer> fraudReasons =
                        new HashMap<>();


                try (
                        BufferedReader br =
                                Files.newBufferedReader(
                                        datasetFile,
                                        StandardCharsets.UTF_8
                                )
                ) {

                    // Skip header
                    String header = br.readLine();

                    if (header == null) {

                        sendJson(
                                exchange,
                                500,
                                "{\"error\":\"Dataset is empty\"}"
                        );

                        return;
                    }

                    String line;

                    while ((line = br.readLine()) != null) {

                        String[] data =
                                line.split(",", -1);

                        if (data.length < 9) {
                            continue;
                        }

                        totalRecords++;

                        /*
                         * Dataset columns:
                         *
                         * 0 = TxnID
                         * 1 = AccountID
                         * 2 = Amount
                         * 3 = Location
                         * 4 = RegisteredCity
                         * 5 = Balance
                         * 6 = Status
                         * 7 = FraudReason
                         * 8 = OriginalClass
                         */

                        String status =
                                data[6].trim();

                        String reason =
                                data[7].trim();


                        if (status.equalsIgnoreCase("FRAUD")) {

                            fraudRecords++;

                            if (!reason.isEmpty()
                                    && !reason.equalsIgnoreCase("None")) {

                                fraudReasons.put(
                                        reason,
                                        fraudReasons.getOrDefault(
                                                reason,
                                                0
                                        ) + 1
                                );
                            }

                        } else {

                            safeRecords++;
                        }
                    }
                }


                // =================================================
                // BUILD FRAUD REASONS JSON
                // =================================================

                StringBuilder reasonsJson =
                        new StringBuilder("{");

                boolean firstReason = true;

                for (
                        Map.Entry<String, Integer> entry
                                : fraudReasons.entrySet()
                ) {

                    if (!firstReason) {
                        reasonsJson.append(",");
                    }

                    reasonsJson
                            .append("\"")
                            .append(escape(entry.getKey()))
                            .append("\":")
                            .append(entry.getValue());

                    firstReason = false;
                }

                reasonsJson.append("}");


                // =================================================
                // FINAL DATASET JSON
                // =================================================

                String response =
                        "{"
                        + "\"success\":true,"
                        + "\"total\":" + totalRecords + ","
                        + "\"safe\":" + safeRecords + ","
                        + "\"fraud\":" + fraudRecords + ","
                        + "\"fraudReasons\":"
                        + reasonsJson
                        + "}";


                sendJson(
                        exchange,
                        200,
                        response
                );


            } catch (Exception e) {

                sendJson(
                        exchange,
                        500,
                        "{\"error\":\""
                                + escape(e.getMessage())
                                + "\"}"
                );
            }
        });


        // =========================================================
        // GET ALL ACCOUNTS
        // =========================================================

        server.createContext("/api/accounts", exchange -> {

            try {

                StringBuilder json =
                        new StringBuilder("[");

                boolean first = true;

                for (int id = 1001; id <= 1005; id++) {

                    BankAccount account =
                            accountManager.getAccount(id);

                    if (account == null) {
                        continue;
                    }

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{")

                            .append("\"accountId\":")
                            .append(account.getAccountId())

                            .append(",")

                            .append("\"holder\":\"")
                            .append(
                                    escape(
                                            account.getAccountHolder()
                                    )
                            )
                            .append("\"")

                            .append(",")

                            .append("\"type\":\"")
                            .append(
                                    escape(
                                            account.getAccountType()
                                    )
                            )
                            .append("\"")

                            .append(",")

                            .append("\"balance\":")
                            .append(account.getBalance())

                            .append(",")

                            .append("\"city\":\"")
                            .append(
                                    escape(
                                            account.getRegisteredCity()
                                    )
                            )
                            .append("\"")

                            .append("}");

                    first = false;
                }

                json.append("]");

                sendJson(
                        exchange,
                        200,
                        json.toString()
                );

            } catch (Exception e) {

                sendJson(
                        exchange,
                        500,
                        "{\"error\":\""
                                + escape(e.getMessage())
                                + "\"}"
                );
            }
        });


        // =========================================================
        // GET SINGLE ACCOUNT
        // =========================================================

        server.createContext("/api/account", exchange -> {

            try {

                String query =
                        exchange.getRequestURI()
                                .getQuery();

                if (query == null
                        || !query.startsWith("id=")) {

                    sendJson(
                            exchange,
                            400,
                            "{\"error\":\"Account ID required\"}"
                    );

                    return;
                }

                int accountId =
                        Integer.parseInt(
                                query.substring(3)
                        );

                BankAccount account =
                        accountManager.getAccount(
                                accountId
                        );

                if (account == null) {

                    sendJson(
                            exchange,
                            404,
                            "{\"error\":\"Account not found\"}"
                    );

                    return;
                }

                String response =
                        "{"
                        + "\"accountId\":"
                        + account.getAccountId()

                        + ","

                        + "\"holder\":\""
                        + escape(
                                account.getAccountHolder()
                        )
                        + "\""

                        + ","

                        + "\"type\":\""
                        + escape(
                                account.getAccountType()
                        )
                        + "\""

                        + ","

                        + "\"balance\":"
                        + account.getBalance()

                        + ","

                        + "\"city\":\""
                        + escape(
                                account.getRegisteredCity()
                        )
                        + "\""

                        + "}";

                sendJson(
                        exchange,
                        200,
                        response
                );

            } catch (Exception e) {

                sendJson(
                        exchange,
                        500,
                        "{\"error\":\""
                                + escape(e.getMessage())
                                + "\"}"
                );
            }
        });


        // =========================================================
        // PROCESS NEW TRANSACTION
        // =========================================================

        server.createContext(
                "/api/transaction",
                exchange -> {

                    try {

                        if (!exchange
                                .getRequestMethod()
                                .equalsIgnoreCase("POST")) {

                            sendJson(
                                    exchange,
                                    405,
                                    "{\"error\":\"POST method required\"}"
                            );

                            return;
                        }


                        String body =
                                new String(
                                        exchange
                                                .getRequestBody()
                                                .readAllBytes(),
                                        StandardCharsets.UTF_8
                                );


                        Map<String, String> data =
                                parseFormData(body);


                        if (!data.containsKey("accountId")
                                || !data.containsKey("amount")
                                || !data.containsKey("location")) {

                            sendJson(
                                    exchange,
                                    400,
                                    "{\"error\":\"accountId, amount and location are required\"}"
                            );

                            return;
                        }


                        int accountId =
                                Integer.parseInt(
                                        data.get("accountId")
                                );


                        double amount =
                                Double.parseDouble(
                                        data.get("amount")
                                );


                        String location =
                                data.get("location")
                                        .trim();


                        if (amount <= 0) {

                            sendJson(
                                    exchange,
                                    400,
                                    "{\"error\":\"Amount must be greater than zero\"}"
                            );

                            return;
                        }


                        if (location.isEmpty()) {

                            sendJson(
                                    exchange,
                                    400,
                                    "{\"error\":\"Location is required\"}"
                            );

                            return;
                        }


                        BankAccount account =
                                accountManager.getAccount(
                                        accountId
                                );


                        if (account == null) {

                            sendJson(
                                    exchange,
                                    404,
                                    "{\"error\":\"Account not found\"}"
                            );

                            return;
                        }


                        int blockedBefore =
                                transactionService
                                        .getBlockedTransactions()
                                        .size();


                        // REAL JAVA FRAUD ENGINE
                        boolean safe =
                                transactionService
                                        .processTransaction(
                                                account,
                                                amount,
                                                location
                                        );


                        String reason = "";


                        if (!safe) {

                            ArrayList<Transaction> blocked =
                                    transactionService
                                            .getBlockedTransactions();


                            if (blocked.size()
                                    > blockedBefore) {

                                Transaction txn =
                                        blocked.get(
                                                blocked.size() - 1
                                        );

                                reason =
                                        txn.getFraudReason();
                            }
                        }


                        String response;


                        if (safe) {

                            response =
                                    "{"
                                    + "\"success\":true,"
                                    + "\"status\":\"SAFE\","
                                    + "\"message\":\"Transaction approved successfully\","
                                    + "\"accountId\":"
                                    + accountId
                                    + ","
                                    + "\"amount\":"
                                    + amount
                                    + ","
                                    + "\"location\":\""
                                    + escape(location)
                                    + "\","
                                    + "\"newBalance\":"
                                    + account.getBalance()
                                    + "}";

                        } else {

                            response =
                                    "{"
                                    + "\"success\":false,"
                                    + "\"status\":\"FRAUD\","
                                    + "\"message\":\"Transaction blocked\","
                                    + "\"reason\":\""
                                    + escape(reason)
                                    + "\","
                                    + "\"accountId\":"
                                    + accountId
                                    + ","
                                    + "\"amount\":"
                                    + amount
                                    + ","
                                    + "\"location\":\""
                                    + escape(location)
                                    + "\""
                                    + "}";
                        }


                        sendJson(
                                exchange,
                                200,
                                response
                        );


                    } catch (NumberFormatException e) {

                        sendJson(
                                exchange,
                                400,
                                "{\"error\":\"Invalid account ID or amount\"}"
                        );


                    } catch (Exception e) {

                        sendJson(
                                exchange,
                                500,
                                "{\"error\":\""
                                        + escape(
                                                e.getMessage()
                                        )
                                        + "\"}"
                        );
                    }
                });


        // =========================================================
        // TRANSACTION HISTORY
        // =========================================================

        server.createContext(
                "/api/history",
                exchange -> {

                    try {

                        FileManager fm =
                                new FileManager();

                        ArrayList<String> lines =
                                fm.readHistory();


                        StringBuilder json =
                                new StringBuilder("[");

                        boolean first = true;


                        for (String line : lines) {

                            if (line.trim().isEmpty()
                                    || line.startsWith("---")) {

                                continue;
                            }


                            if (!first) {
                                json.append(",");
                            }


                            json.append("\"")
                                    .append(escape(line))
                                    .append("\"");


                            first = false;
                        }


                        json.append("]");


                        sendJson(
                                exchange,
                                200,
                                json.toString()
                        );


                    } catch (Exception e) {

                        sendJson(
                                exchange,
                                500,
                                "{\"error\":\""
                                        + escape(
                                                e.getMessage()
                                        )
                                        + "\"}"
                        );
                    }
                });


        // =========================================================
        // BLOCKED TRANSACTIONS
        // =========================================================

        server.createContext(
                "/api/blocked",
                exchange -> {

                    try {

                        ArrayList<Transaction> blocked =
                                transactionService
                                        .getBlockedTransactions();


                        StringBuilder json =
                                new StringBuilder("[");

                        boolean first = true;


                        for (Transaction txn : blocked) {

                            if (!first) {
                                json.append(",");
                            }


                            json.append("{")

                                    .append("\"txnId\":")
                                    .append(txn.getTxnId())

                                    .append(",")

                                    .append("\"amount\":")
                                    .append(txn.getAmount())

                                    .append(",")

                                    .append("\"location\":\"")
                                    .append(
                                            escape(
                                                    txn.getLocation()
                                            )
                                    )
                                    .append("\"")

                                    .append(",")

                                    .append("\"status\":\"")
                                    .append(
                                            escape(
                                                    txn.getStatus()
                                            )
                                    )
                                    .append("\"")

                                    .append(",")

                                    .append("\"reason\":\"")
                                    .append(
                                            escape(
                                                    txn.getFraudReason()
                                            )
                                    )
                                    .append("\"")

                                    .append(",")

                                    .append("\"time\":\"")
                                    .append(
                                            escape(
                                                    txn.getTimestamp()
                                                            .toString()
                                            )
                                    )
                                    .append("\"")

                                    .append("}");


                            first = false;
                        }


                        json.append("]");


                        sendJson(
                                exchange,
                                200,
                                json.toString()
                        );


                    } catch (Exception e) {

                        sendJson(
                                exchange,
                                500,
                                "{\"error\":\""
                                        + escape(
                                                e.getMessage()
                                        )
                                        + "\"}"
                        );
                    }
                });


        // =========================================================
        // START SERVER
        // =========================================================

        server.start();


        System.out.println();
        System.out.println(
                "=============================================="
        );
        System.out.println(
                "       STREAMGUARD WEB SERVER"
        );
        System.out.println(
                "=============================================="
        );
        System.out.println(
                "Server running at:"
        );
        System.out.println(
                "http://localhost:" + PORT
        );
        System.out.println();
        System.out.println(
                "Open this address in your browser."
        );
        System.out.println(
                "=============================================="
        );
    }


    // =============================================================
    // PARSE FORM DATA
    // =============================================================

    private static Map<String, String> parseFormData(
            String body) {

        Map<String, String> result =
                new HashMap<>();


        if (body == null || body.isEmpty()) {
            return result;
        }


        String[] pairs =
                body.split("&");


        for (String pair : pairs) {

            String[] parts =
                    pair.split("=", 2);


            if (parts.length == 2) {

                String key =
                        URLDecoder.decode(
                                parts[0],
                                StandardCharsets.UTF_8
                        );


                String value =
                        URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );


                result.put(
                        key,
                        value
                );
            }
        }


        return result;
    }


    // =============================================================
    // SEND JSON
    // =============================================================

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String response)
            throws IOException {


        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                status,
                data.length
        );


        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(data);
        }
    }


    // =============================================================
    // SEND NORMAL RESPONSE
    // =============================================================

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String response)
            throws IOException {


        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/plain; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                status,
                data.length
        );


        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(data);
        }
    }


    // =============================================================
    // ESCAPE JSON
    // =============================================================

    private static String escape(String text) {

        if (text == null) {
            return "";
        }


        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}