package com.streamguard.service;

import com.streamguard.database.DatabaseConnection;
import com.streamguard.manager.AccountManager;
import com.streamguard.model.BankAccount;
import com.streamguard.model.CurrentAccount;
import com.streamguard.model.SavingsAccount;
import com.streamguard.model.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DatabaseManager.java
 * Handles all MySQL database interactions for StreamGuard.
 *
 * Replaces:
 *   - DatasetManager (CSV-based stats) → now reads from MySQL transactions table
 *   - FileManager    (file-based history) → now reads/writes transactions table
 *
 * Demonstrates:
 *   - JDBC (PreparedStatement, ResultSet, try-with-resources)
 *   - SQL queries (SELECT, INSERT, UPDATE)
 *   - Exception Handling
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class DatabaseManager {

    // =========================================================
    // TRANSACTION ID MANAGEMENT
    // =========================================================

    private int nextTxnId;

    /**
     * Constructor: Determines the next safe transaction ID
     * by reading the current MAX(txn_id) from MySQL.
     * This prevents duplicate primary keys after restarts.
     */
    public DatabaseManager() {
        this.nextTxnId = loadNextTxnId();
    }

    /**
     * Reads MAX(txn_id) from transactions table.
     * If table is empty, starts from 1001.
     */
    private int loadNextTxnId() {
        int startId = 1001;
        String sql = "SELECT MAX(txn_id) FROM transactions";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                int max = rs.getInt(1);
                if (max >= startId) {
                    startId = max + 1;
                }
            }

        } catch (SQLException e) {
            System.out.println(
                "  [DB WARN] Could not read max txn_id, starting from 1001. "
                + e.getMessage()
            );
        }
        return startId;
    }

    /**
     * Returns and increments the next transaction ID.
     */
    public synchronized int getNextTxnId() {
        return nextTxnId++;
    }

    // =========================================================
    // DATASET SUMMARY  (replaces DatasetManager.loadDataset)
    // =========================================================

    /**
     * Queries MySQL for transaction statistics and prints a summary
     * in the same style as the original DatasetManager CSV output.
     */
    public void printDatabaseSummary() {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("       STREAMGUARD DATABASE SUMMARY");
        System.out.println("==========================================");

        int total = 0, safe = 0, fraud = 0;
        Map<String, Integer> fraudReasons = new HashMap<>();

        // -- Total transactions
        String sqlTotal = "SELECT COUNT(*) FROM transactions";

        // -- Safe transactions
        String sqlSafe = "SELECT COUNT(*) FROM transactions WHERE status = 'SAFE'";

        // -- Fraud transactions
        String sqlFraud = "SELECT COUNT(*) FROM transactions WHERE status = 'FRAUD'";

        // -- Fraud reasons grouped
        String sqlReasons =
            "SELECT fraud_reason, COUNT(*) AS cnt "
            + "FROM transactions "
            + "WHERE status = 'FRAUD' AND fraud_reason IS NOT NULL AND fraud_reason <> '' "
            + "GROUP BY fraud_reason";

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Total
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlTotal)) {
                if (rs.next()) total = rs.getInt(1);
            }

            // Safe
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlSafe)) {
                if (rs.next()) safe = rs.getInt(1);
            }

            // Fraud
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlFraud)) {
                if (rs.next()) fraud = rs.getInt(1);
            }

            // Fraud reasons
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlReasons)) {
                while (rs.next()) {
                    String reason = rs.getString("fraud_reason");
                    int cnt = rs.getInt("cnt");
                    if (reason != null && !reason.isEmpty()) {
                        fraudReasons.put(reason, cnt);
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println(
                "  [DB ERROR] Could not load database summary: " + e.getMessage()
            );
            System.out.println("==========================================\n");
            return;
        }

        System.out.println("Total Transactions : " + total);
        System.out.println("Safe Transactions  : " + safe);
        System.out.println("Fraud Transactions : " + fraud);
        System.out.println("==========================================");
        System.out.println();
        System.out.println("           FRAUD REASONS");
        System.out.println("==========================================");

        if (fraudReasons.isEmpty()) {
            System.out.println("  No fraud records found in database.");
        } else {
            for (Map.Entry<String, Integer> entry : fraudReasons.entrySet()) {
                System.out.println("  " + entry.getKey() + " : " + entry.getValue());
            }
        }

        System.out.println("==========================================");
        System.out.println();
    }

    /**
     * Returns a JSON-formatted summary for the web API.
     * Used by WebServer /api/dataset endpoint.
     */
    public String getDatasetSummaryJson() {

        int total = 0, safe = 0, fraud = 0;
        Map<String, Integer> fraudReasons = new HashMap<>();

        String sqlTotal   = "SELECT COUNT(*) FROM transactions";
        String sqlSafe    = "SELECT COUNT(*) FROM transactions WHERE status = 'SAFE'";
        String sqlFraud   = "SELECT COUNT(*) FROM transactions WHERE status = 'FRAUD'";
        String sqlReasons =
            "SELECT fraud_reason, COUNT(*) AS cnt "
            + "FROM transactions "
            + "WHERE status = 'FRAUD' AND fraud_reason IS NOT NULL AND fraud_reason <> '' "
            + "GROUP BY fraud_reason";

        try (Connection conn = DatabaseConnection.getConnection()) {

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlTotal)) {
                if (rs.next()) total = rs.getInt(1);
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlSafe)) {
                if (rs.next()) safe = rs.getInt(1);
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlFraud)) {
                if (rs.next()) fraud = rs.getInt(1);
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlReasons)) {
                while (rs.next()) {
                    String reason = rs.getString("fraud_reason");
                    int cnt = rs.getInt("cnt");
                    if (reason != null && !reason.isEmpty()) {
                        fraudReasons.put(reason, cnt);
                    }
                }
            }

        } catch (SQLException e) {
            return "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }

        // Determine top fraud reason
        String mainFraudReason = "Not available";
        int maxCnt = 0;
        for (Map.Entry<String, Integer> entry : fraudReasons.entrySet()) {
            if (entry.getValue() > maxCnt) {
                maxCnt = entry.getValue();
                mainFraudReason = entry.getKey();
            }
        }

        // Build fraud reasons JSON
        StringBuilder reasonsJson = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : fraudReasons.entrySet()) {
            if (!first) reasonsJson.append(",");
            reasonsJson.append("\"")
                       .append(escapeJson(entry.getKey()))
                       .append("\":")
                       .append(entry.getValue());
            first = false;
        }
        reasonsJson.append("}");

        return "{"
            + "\"success\":true,"
            + "\"total\":" + total + ","
            + "\"safe\":" + safe + ","
            + "\"fraud\":" + fraud + ","
            + "\"fraudReason\":\"" + escapeJson(mainFraudReason) + "\","
            + "\"fraudReasons\":" + reasonsJson
            + "}";
    }

    // =========================================================
    // SAVE TRANSACTION TO MYSQL
    // =========================================================

    /**
     * Inserts a processed transaction into the transactions table.
     * Uses PreparedStatement to prevent SQL injection.
     *
     * @param txn       The Transaction object (must have status set)
     * @param accountId The owning account's ID
     */
    public void saveTransaction(Transaction txn, int accountId) {

        String sql =
            "INSERT INTO transactions "
            + "(txn_id, account_id, amount, location, timestamp, status, fraud_reason) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, txn.getTxnId());
            ps.setInt(2, accountId);
            ps.setDouble(3, txn.getAmount());
            ps.setString(4, txn.getLocation());
            // Convert LocalDateTime → java.sql.Timestamp
            ps.setTimestamp(5, Timestamp.valueOf(txn.getTimestamp()));
            ps.setString(6, txn.getStatus());
            ps.setString(7,
                txn.getFraudReason() == null || txn.getFraudReason().isEmpty()
                    ? null
                    : txn.getFraudReason()
            );

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                "  [DB ERROR] Failed to save transaction TXN"
                + String.format("%04d", txn.getTxnId())
                + ": " + e.getMessage()
            );
        }
    }

    // =========================================================
    // UPDATE ACCOUNT BALANCE IN MYSQL
    // =========================================================

    /**
     * Updates the account balance in MySQL after a SAFE transaction.
     *
     * @param accountId  The account to update
     * @param newBalance The new balance to store
     */
    public void updateAccountBalance(int accountId, double newBalance) {

        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, newBalance);
            ps.setInt(2, accountId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                "  [DB ERROR] Failed to update balance for account "
                + accountId + ": " + e.getMessage()
            );
        }
    }

    // =========================================================
    // TRANSACTION HISTORY FROM MYSQL
    // =========================================================

    /**
     * Retrieves all transactions from MySQL ordered by timestamp desc.
     * Replaces FileManager.readHistory() / printHistory().
     *
     * @return ArrayList of Transaction objects
     */
    public ArrayList<Transaction> getTransactionHistory() {

        ArrayList<Transaction> list = new ArrayList<>();

        String sql =
            "SELECT txn_id, amount, location, timestamp, status, fraud_reason "
            + "FROM transactions ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int    txnId       = rs.getInt("txn_id");
                double amount      = rs.getDouble("amount");
                String location    = rs.getString("location");
                Timestamp ts       = rs.getTimestamp("timestamp");
                String status      = rs.getString("status");
                String fraudReason = rs.getString("fraud_reason");

                Transaction txn = new Transaction(txnId, amount, location);
                // Overwrite the auto-set timestamp with the DB value
                txn.setTimestampFromDb(ts.toLocalDateTime());
                txn.setStatus(status);
                txn.setFraudReason(fraudReason == null ? "" : fraudReason);

                list.add(txn);
            }

        } catch (SQLException e) {
            System.out.println(
                "  [DB ERROR] Could not load transaction history: " + e.getMessage()
            );
        }

        return list;
    }

    /**
     * Prints transaction history to console.
     * Replaces FileManager.printHistory().
     */
    public void printTransactionHistory() {

        ArrayList<Transaction> history = getTransactionHistory();

        System.out.println("\n  ======= TRANSACTION HISTORY (MySQL) =======");

        if (history.isEmpty()) {
            System.out.println("  No transactions recorded in database yet.");
        } else {
            for (Transaction txn : history) {
                System.out.println("  --------------------------------------------------");
                System.out.println("  " + txn);
            }
        }

        System.out.println("  ============================================\n");
    }

    // =========================================================
    // BLOCKED (FRAUD) TRANSACTIONS FROM MYSQL
    // =========================================================

    /**
     * Retrieves only FRAUD transactions from MySQL.
     * Used for "View Blocked Transactions" menu and /api/blocked.
     *
     * @return ArrayList of blocked Transaction objects
     */
    public ArrayList<Transaction> getBlockedTransactions() {

        ArrayList<Transaction> list = new ArrayList<>();

        String sql =
            "SELECT txn_id, amount, location, timestamp, status, fraud_reason "
            + "FROM transactions WHERE status = 'FRAUD' ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int    txnId       = rs.getInt("txn_id");
                double amount      = rs.getDouble("amount");
                String location    = rs.getString("location");
                Timestamp ts       = rs.getTimestamp("timestamp");
                String status      = rs.getString("status");
                String fraudReason = rs.getString("fraud_reason");

                Transaction txn = new Transaction(txnId, amount, location);
                txn.setTimestampFromDb(ts.toLocalDateTime());
                txn.setStatus(status);
                txn.setFraudReason(fraudReason == null ? "" : fraudReason);

                list.add(txn);
            }

        } catch (SQLException e) {
            System.out.println(
                "  [DB ERROR] Could not load blocked transactions: " + e.getMessage()
            );
        }

        return list;
    }

    // =========================================================
    // LOAD ACCOUNTS FROM MYSQL
    // =========================================================

    /**
     * Reads all accounts from MySQL accounts table and loads them
     * into the AccountManager's HashMap.
     *
     * Preserves the HashMap as the in-memory fast-access store
     * while MySQL provides persistent storage.
     *
     * @param accountManager The AccountManager whose map will be populated
     */
    public void loadAccountsFromDatabase(AccountManager accountManager) {

        String sql =
            "SELECT account_id, account_holder, account_type, balance, registered_city "
            + "FROM accounts";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int    accountId   = rs.getInt("account_id");
                String holder      = rs.getString("account_holder");
                String type        = rs.getString("account_type");
                double balance     = rs.getDouble("balance");
                String city        = rs.getString("registered_city");

                BankAccount account;
                if ("CURRENT".equalsIgnoreCase(type)) {
                    // Default overdraft limit for current accounts
                    double overdraft = 50000.00;
                    if (accountId == 1003) overdraft = 25000.00;
                    account = new CurrentAccount(accountId, holder, balance, city, overdraft);
                } else {
                    account = new SavingsAccount(accountId, holder, balance, city);
                }

                accountManager.addAccount(accountId, account);
            }

            System.out.println(
                "  [DATABASE] Accounts loaded from MySQL successfully."
            );

        } catch (SQLException e) {
            System.out.println(
                "  [DB ERROR] Could not load accounts from MySQL: " + e.getMessage()
                + "\n  Falling back to default in-memory accounts."
            );
            // Fall back to default hardcoded accounts
            accountManager.loadDefaultAccounts();
        }
    }

    // =========================================================
    // JSON HISTORY (for /api/history)
    // =========================================================

    /**
     * Returns all transactions as a JSON array for the web API.
     */
    public String getHistoryJson() {

        ArrayList<Transaction> list = getTransactionHistory();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Transaction txn : list) {
            if (!first) json.append(",");
            json.append("\"")
                .append(escapeJson(txn.toString()))
                .append("\"");
            first = false;
        }

        json.append("]");
        return json.toString();
    }

    /**
     * Returns blocked transactions as a JSON array for the web API.
     */
    public String getBlockedJson() {

        ArrayList<Transaction> list = getBlockedTransactions();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Transaction txn : list) {
            if (!first) json.append(",");
            json.append("{")
                .append("\"txnId\":").append(txn.getTxnId())
                .append(",\"amount\":").append(txn.getAmount())
                .append(",\"location\":\"").append(escapeJson(txn.getLocation())).append("\"")
                .append(",\"status\":\"").append(escapeJson(txn.getStatus())).append("\"")
                .append(",\"reason\":\"").append(escapeJson(txn.getFraudReason())).append("\"")
                .append(",\"time\":\"").append(escapeJson(txn.getTimestamp().toString())).append("\"")
                .append("}");
            first = false;
        }

        json.append("]");
        return json.toString();
    }

    // =========================================================
    // HELPER
    // =========================================================

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\r", "\\r")
                   .replace("\n", "\\n");
    }
}
