package com.streamguard.service;

import com.streamguard.database.DatabaseConnection;
import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * OutputFileLogger.java
 * Formats and writes transactions to "output.db" in root directory as an ASCII tabular column.
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class OutputFileLogger {

    public static final String DEFAULT_FILE_PATH = "output.db";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Table border and separator matching column widths:
    // Col 1: TXN ID        (8 chars  -> 10 with spaces)
    // Col 2: ACCOUNT ID    (10 chars -> 12 with spaces)
    // Col 3: ACC HOLDER    (20 chars -> 22 with spaces)
    // Col 4: AMOUNT (Rs)   (13 chars -> 15 with spaces)
    // Col 5: LOCATION      (15 chars -> 17 with spaces)
    // Col 6: DATE & TIME   (19 chars -> 21 with spaces)
    // Col 7: STATUS        (8 chars  -> 10 with spaces)
    // Col 8: REASON/REMARKS(75 chars -> 77 with spaces)
    private static final String SEPARATOR =
        "+----------+------------+----------------------+---------------+-----------------+---------------------+----------+-----------------------------------------------------------------------------+";

    private static final String ROW_FORMAT =
        "| %-8s | %-10s | %-20s | %-13s | %-15s | %-19s | %-8s | %-75s |";

    /**
     * Record representation for table output
     */
    public static class TransactionRecord {
        public int txnId;
        public int accountId;
        public String accountHolder;
        public double amount;
        public String location;
        public LocalDateTime timestamp;
        public String status;
        public String reason;

        public TransactionRecord(int txnId, int accountId, String accountHolder, double amount,
                                 String location, LocalDateTime timestamp, String status, String reason) {
            this.txnId = txnId;
            this.accountId = accountId;
            this.accountHolder = accountHolder != null ? accountHolder : "Unknown";
            this.amount = amount;
            this.location = location != null ? location : "";
            this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
            this.status = status != null ? status : "UNKNOWN";
            this.reason = reason;
        }
    }

    /**
     * Writes all transactions from MySQL into the specified file in tabular format.
     * If MySQL is unavailable, falls back to the provided fallback records.
     */
    public static synchronized void updateOutputFile(String filePath, List<TransactionRecord> fallbackList) {
        List<TransactionRecord> records = loadTransactionsFromDatabase();
        if (records.isEmpty() && fallbackList != null && !fallbackList.isEmpty()) {
            records = fallbackList;
        }

        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(SEPARATOR);
            writer.newLine();

            // Header line
            String header = String.format(
                ROW_FORMAT,
                "TXN ID", "ACCOUNT ID", "ACCOUNT HOLDER", "AMOUNT (Rs)",
                "LOCATION", "DATE & TIME", "STATUS", "REASON / REMARKS"
            );
            writer.write(header);
            writer.newLine();

            writer.write(SEPARATOR);
            writer.newLine();

            for (TransactionRecord rec : records) {
                String row = formatRecordRow(rec);
                writer.write(row);
                writer.newLine();
                writer.write(SEPARATOR);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            System.err.println("  [OUTPUT.DB ERROR] Failed to write to " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Appends or records a single transaction.
     * When database is connected, rewrites the full updated table to maintain total consistency.
     * When database is not connected, appends the row to the file directly.
     */
    public static synchronized void recordTransaction(Transaction txn, BankAccount account, String filePath) {
        boolean dbSuccess = false;

        // Try full refresh from database
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                updateOutputFile(filePath, null);
                dbSuccess = true;
            }
        } catch (Exception ignored) {
            // MySQL unavailable, fall back to direct file append
        }

        if (!dbSuccess) {
            appendSingleRow(txn, account, filePath);
        }
    }

    public static synchronized void recordTransaction(Transaction txn, BankAccount account) {
        recordTransaction(txn, account, DEFAULT_FILE_PATH);
    }

    public static synchronized void updateOutputFile() {
        updateOutputFile(DEFAULT_FILE_PATH, null);
    }

    /**
     * Appends a single row directly to file (fallback when DB is offline).
     */
    private static void appendSingleRow(Transaction txn, BankAccount account, String filePath) {
        File file = new File(filePath);
        boolean needHeader = !file.exists() || file.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (needHeader) {
                writer.write(SEPARATOR);
                writer.newLine();
                String header = String.format(
                    ROW_FORMAT,
                    "TXN ID", "ACCOUNT ID", "ACCOUNT HOLDER", "AMOUNT (Rs)",
                    "LOCATION", "DATE & TIME", "STATUS", "REASON / REMARKS"
                );
                writer.write(header);
                writer.newLine();
                writer.write(SEPARATOR);
                writer.newLine();
            }

            TransactionRecord rec = new TransactionRecord(
                txn.getTxnId(),
                account != null ? account.getAccountId() : 0,
                account != null ? account.getAccountHolder() : "Unknown",
                txn.getAmount(),
                txn.getLocation(),
                txn.getTimestamp(),
                txn.getStatus(),
                txn.getFraudReason()
            );

            writer.write(formatRecordRow(rec));
            writer.newLine();
            writer.write(SEPARATOR);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("  [OUTPUT.DB ERROR] Could not append to " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Formats a record into a tabular row string.
     */
    private static String formatRecordRow(TransactionRecord rec) {
        String txnIdStr = String.format("TXN%04d", rec.txnId);
        String accIdStr = String.valueOf(rec.accountId);
        String holderStr = truncate(rec.accountHolder, 20);
        String amountStr = String.format("%.2f", rec.amount);
        String locStr = truncate(rec.location, 15);
        String timeStr = rec.timestamp.format(DATE_FORMATTER);
        String statusStr = truncate(rec.status, 8);

        String remarks = rec.reason;
        if (remarks == null || remarks.trim().isEmpty()) {
            remarks = "SAFE".equalsIgnoreCase(rec.status) ? "Approved (Safe)" : "No reason specified";
        }
        String reasonStr = truncate(remarks.trim(), 75);

        return String.format(
            ROW_FORMAT,
            txnIdStr, accIdStr, holderStr, amountStr, locStr, timeStr, statusStr, reasonStr
        );
    }

    /**
     * Reads all transactions with account holder names from MySQL.
     */
    private static List<TransactionRecord> loadTransactionsFromDatabase() {
        List<TransactionRecord> records = new ArrayList<>();
        String sql =
            "SELECT t.txn_id, t.account_id, COALESCE(a.account_holder, 'Unknown') AS account_holder, "
            + "       t.amount, t.location, t.timestamp, t.status, t.fraud_reason "
            + "FROM transactions t "
            + "LEFT JOIN accounts a ON t.account_id = a.account_id "
            + "ORDER BY t.txn_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int txnId = rs.getInt("txn_id");
                int accId = rs.getInt("account_id");
                String holder = rs.getString("account_holder");
                double amount = rs.getDouble("amount");
                String loc = rs.getString("location");
                Timestamp ts = rs.getTimestamp("timestamp");
                String status = rs.getString("status");
                String reason = rs.getString("fraud_reason");

                LocalDateTime dt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

                records.add(new TransactionRecord(txnId, accId, holder, amount, loc, dt, status, reason));
            }

        } catch (SQLException e) {
            System.err.println("  [OUTPUT.DB DB ERROR] Could not load transactions: " + e.getMessage());
        }

        return records;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static void main(String[] args) {
        System.out.println("Updating " + DEFAULT_FILE_PATH + " with all transactions...");
        updateOutputFile();
        System.out.println("Done! Check " + DEFAULT_FILE_PATH);
    }
}
