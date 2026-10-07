package com.streamguard.main;

import com.streamguard.database.DatabaseConnection;
import com.streamguard.manager.AccountManager;
import com.streamguard.model.BankAccount;
import com.streamguard.service.DatabaseManager;
import com.streamguard.service.OutputFileLogger;
import com.streamguard.service.TransactionService;

import java.util.Scanner;

/**
 * Main.java
 * Entry point of StreamGuard – Real-Time Financial Fraud Detection System.
 *
 * Demonstrates:
 * - Classes & Objects
 * - Exception Handling
 * - Multithreading
 * - Database Connectivity (MySQL / JDBC)
 * - Collections Framework (HashMap, ArrayList)
 * - Loops & Switch Case
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        printBanner();

        // ==========================================
        // DATABASE INITIALIZATION
        // ==========================================

        System.out.println("  Connecting to MySQL database...");

        boolean dbConnected = DatabaseConnection.testConnection();

        // Create DatabaseManager (manages txnId, SQL queries)
        DatabaseManager databaseManager = new DatabaseManager();

        // Create AccountManager (HashMap-based in-memory registry)
        AccountManager accountManager = new AccountManager();

        if (dbConnected) {
            // Load accounts from MySQL into HashMap
            databaseManager.loadAccountsFromDatabase(accountManager);
        } else {
            // Fallback: load default hardcoded accounts
            System.out.println(
                "  [INFO] Running without MySQL. Using in-memory accounts."
            );
            accountManager.loadDefaultAccounts();
        }

        // Create TransactionService with database support
        TransactionService txnService = new TransactionService(databaseManager);

        // Display database/transaction summary at startup
        if (dbConnected) {
            databaseManager.printDatabaseSummary();
        } else {
            System.out.println(
                "\n  [INFO] Database summary unavailable (no MySQL connection).\n"
            );
        }

        // Initialize output.db with all transactions in tabular format
        OutputFileLogger.updateOutputFile();

        // ==========================================
        // MAIN APPLICATION LOOP
        // ==========================================
        while (true) {

            printMenu();

            int choice = -1;

            try {

                System.out.print("  Enter your choice: ");
                choice = Integer.parseInt(sc.nextLine().trim());

            } catch (NumberFormatException e) {

                System.out.println(
                    "  [ERROR] Please enter a valid number.\n"
                );
                continue;
            }

            switch (choice) {

                // ==========================================
                // CASE 1 – NEW TRANSACTION
                // ==========================================
                case 1:

                    System.out.println("\n  --- NEW TRANSACTION ---");

                    accountManager.listAccounts();

                    int accId = 0;
                    BankAccount account = null;

                    // Get valid account
                    while (account == null) {

                        try {

                            System.out.print("  Enter Account ID: ");
                            accId = Integer.parseInt(sc.nextLine().trim());
                            account = accountManager.getAccount(accId);

                            if (account == null) {
                                System.out.println(
                                    "  [ERROR] Account not found. Try again."
                                );
                            }

                        } catch (NumberFormatException e) {
                            System.out.println(
                                "  [ERROR] Invalid Account ID. Enter a number."
                            );
                        }
                    }

                    // Get valid amount
                    double amount = 0;

                    while (amount <= 0) {

                        try {

                            System.out.print("  Enter Transaction Amount (Rs.): ");
                            amount = Double.parseDouble(sc.nextLine().trim());

                            if (amount <= 0) {
                                System.out.println(
                                    "  [ERROR] Amount must be greater than zero."
                                );
                            }

                        } catch (NumberFormatException e) {
                            System.out.println(
                                "  [ERROR] Invalid amount. Enter a numeric value."
                            );
                            amount = 0;
                        }
                    }

                    // Get location
                    System.out.print("  Enter Transaction Location (City): ");
                    String location = sc.nextLine().trim();
                    if (location.isEmpty()) location = "Unknown";

                    /*
                     * Final copies for the lambda (multithreading).
                     */
                    final BankAccount finalAccount  = account;
                    final double      finalAmount   = amount;
                    final String      finalLocation = location;

                    // ==========================================
                    // MULTITHREADING – Transaction runs in separate thread
                    // ==========================================
                    Thread transactionThread = new Thread(() ->
                        txnService.processTransaction(
                            finalAccount, finalAmount, finalLocation
                        )
                    );

                    transactionThread.start();

                    try {
                        transactionThread.join();
                    } catch (InterruptedException e) {
                        System.out.println("  [ERROR] Transaction thread interrupted.");
                        Thread.currentThread().interrupt();
                    }

                    break;

                // ==========================================
                // CASE 2 – TRANSACTION HISTORY (from MySQL)
                // ==========================================
                case 2:

                    System.out.println("\n  --- TRANSACTION HISTORY ---");

                    if (dbConnected) {
                        databaseManager.printTransactionHistory();
                    } else {
                        System.out.println(
                            "  [INFO] Transaction history unavailable (no MySQL connection)."
                        );
                        // Show in-memory blocked list as fallback
                        if (!txnService.getBlockedTransactions().isEmpty()) {
                            System.out.println("  In-session transactions:");
                            txnService.getBlockedTransactions()
                                      .forEach(t -> System.out.println("  " + t));
                        }
                    }
                    break;

                // ==========================================
                // CASE 3 – ALL ACCOUNTS
                // ==========================================
                case 3:

                    System.out.println("\n  --- ALL ACCOUNTS ---");
                    accountManager.listAccounts();
                    break;

                // ==========================================
                // CASE 4 – BLOCKED TRANSACTIONS (from MySQL)
                // ==========================================
                case 4:

                    System.out.println("\n  --- BLOCKED TRANSACTIONS ---");

                    if (dbConnected) {
                        // Query MySQL for all FRAUD transactions
                        var blocked = databaseManager.getBlockedTransactions();
                        if (blocked.isEmpty()) {
                            System.out.println(
                                "  No fraud transactions found in database.\n"
                            );
                        } else {
                            System.out.println(
                                "\n  ======= FRAUD TRANSACTIONS (MySQL) ======="
                            );
                            for (var t : blocked) {
                                System.out.println("  --------------------------------------------------");
                                System.out.println("  " + t);
                            }
                            System.out.println(
                                "  ==========================================\n"
                            );
                        }
                    } else {
                        // Fallback: in-memory blocked list
                        if (txnService.getBlockedTransactions().isEmpty()) {
                            System.out.println(
                                "  No fraud transactions recorded in this session.\n"
                            );
                        } else {
                            txnService.getBlockedTransactions()
                                      .forEach(t -> System.out.println("  " + t));
                            System.out.println();
                        }
                    }
                    break;

                // ==========================================
                // CASE 0 – EXIT
                // ==========================================
                case 0:

                    System.out.println("\n  Thank you for using StreamGuard!");
                    sc.close();
                    return;

                // ==========================================
                // INVALID OPTION
                // ==========================================
                default:
                    System.out.println(
                        "  [ERROR] Invalid choice. Please select from the menu.\n"
                    );
            }
        }
    }

    // ==========================================
    // BANNER
    // ==========================================
    private static void printBanner() {

        System.out.println("====================================================");
        System.out.println("          STREAMGUARD FRAUD DETECTION SYSTEM");
        System.out.println("          Powered by MySQL + JDBC");
        System.out.println("====================================================");
        System.out.println();
    }

    // ==========================================
    // MENU
    // ==========================================
    private static void printMenu() {

        System.out.println("  ┌─────────────────────────────────┐");
        System.out.println("  │           MAIN MENU             │");
        System.out.println("  ├─────────────────────────────────┤");
        System.out.println("  │  1. New Transaction             │");
        System.out.println("  │  2. View Transaction History    │");
        System.out.println("  │  3. View All Accounts            │");
        System.out.println("  │  4. View Blocked Transactions   │");
        System.out.println("  │  0. Exit                        │");
        System.out.println("  └─────────────────────────────────┘");
        System.out.println();
    }
}