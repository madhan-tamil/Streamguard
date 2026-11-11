package com.streamguard.service;

import com.streamguard.anomaly.AnomalyDetectionEngine;
import com.streamguard.model.BankAccount;
import com.streamguard.model.RiskScore;
import com.streamguard.model.Transaction;

import java.util.ArrayList;

/**
 * TransactionService.java
 * Coordinates transaction processing: create → fraud check → update → save to MySQL.
 *
 * Transaction Flow:
 *   1. Create Transaction object
 *   2. Run Fraud Rules (FraudChecker)
 *   3. If SAFE  → deduct balance (Java object + MySQL accounts table)
 *   4. If FRAUD → no deduction
 *   5. Save Transaction to MySQL (DatabaseManager.saveTransaction)
 *
 * Demonstrates:
 *   - Methods, Exception Handling, Collections
 *   - JDBC integration via DatabaseManager
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class TransactionService {

    private AnomalyDetectionEngine anomalyEngine;
    private DatabaseManager        databaseManager;
    private Transaction            lastTransaction;

    // In-memory list for blocked transactions this session
    // (also persisted in MySQL; kept here for quick access without extra DB query)
    private ArrayList<Transaction> blockedTransactions;

    /**
     * Constructor – accepts DatabaseManager for database operations.
     *
     * @param databaseManager Shared DatabaseManager instance
     */
    public TransactionService(DatabaseManager databaseManager) {
        this.databaseManager     = databaseManager;
        this.anomalyEngine       = new AnomalyDetectionEngine();
        this.blockedTransactions = new ArrayList<>();
        initProfilesFromDatabase();
    }

    /**
     * Initializes behavioral profiles with existing MySQL transaction data.
     */
    private void initProfilesFromDatabase() {
        try {
            int[] demoAccounts = {1001, 1002, 1003, 1004, 1005};
            for (int accId : demoAccounts) {
                var txns = databaseManager.getTransactionsForAccount(accId);
                for (Transaction t : txns) {
                    if ("SAFE".equalsIgnoreCase(t.getStatus())) {
                        var profile = anomalyEngine.getProfile(new com.streamguard.model.SavingsAccount(accId, "", 0, t.getLocation()));
                        profile.recordTransaction(t.getAmount(), t.getLocation(), t.getTimestamp());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Main transaction processing method.
     *
     * @param account  The BankAccount to debit
     * @param amount   Transaction amount in Rs.
     * @param location Transaction city
     * @return true if SAFE, false if FRAUD
     */
    public boolean processTransaction(BankAccount account, double amount, String location) {

        // 1. Create Transaction object with a DB-safe ID
        int txnId = databaseManager.getNextTxnId();
        Transaction txn = new Transaction(txnId, amount, location);

        System.out.println("\n  ----------------------------------------");
        System.out.printf("  Processing Transaction TXN%04d%n", txn.getTxnId());
        System.out.printf("  Account: %s | Amount: Rs.%.2f | Location: %s%n",
            account.getAccountHolder(), amount, location);
        System.out.println("  ----------------------------------------");

        // 2. Run the Behavioral Anomaly & Statistical Outlier Engine
        RiskScore risk = anomalyEngine.evaluate(txn, account);
        txn.setRiskScore(risk.getScore());
        txn.setVerdict(risk.getVerdict());

        // 3. Display result
        System.out.println("\n  ========================================");
        if (risk.isSafe()) {
            txn.setStatus("SAFE");
            System.out.println("  RESULT  :  ✔  TRANSACTION APPROVED (SAFE)");
            System.out.printf( "  SCORE   :  %d / 100%n", risk.getScore());

            // 4a. Deduct balance from Java object
            account.withdraw(amount);

            // 4b. Sync new balance to MySQL accounts table
            databaseManager.updateAccountBalance(
                account.getAccountId(),
                account.getBalance()
            );

        } else if (risk.isSuspicious()) {
            txn.setStatus("SUSPICIOUS");
            txn.setFraudReason(risk.getTopReason());
            System.out.println("  RESULT  :  ⚠  TRANSACTION FLAGGED (SUSPICIOUS)");
            System.out.printf( "  SCORE   :  %d / 100%n", risk.getScore());
            System.out.println("  REASON  :  " + risk.getTopReason());
            blockedTransactions.add(txn);

        } else {
            txn.setStatus("FRAUD");
            txn.setFraudReason(risk.getTopReason());
            System.out.println("  RESULT  :  ✘  TRANSACTION BLOCKED (FRAUD)");
            System.out.printf( "  SCORE   :  %d / 100%n", risk.getScore());
            System.out.println("  REASON  :  " + risk.getTopReason());

            // Keep in-memory list for this session
            blockedTransactions.add(txn);
        }
        System.out.println("  ========================================\n");

        // Determine return value (SAFE only → true)
        boolean isSafe = risk.isSafe();

        // Keep reference to last processed transaction
        this.lastTransaction = txn;

        // 5. Save transaction to MySQL (SAFE or FRAUD)
        databaseManager.saveTransaction(txn, account.getAccountId());

        // 6. Record transaction in tabular column format to output.db
        OutputFileLogger.recordTransaction(txn, account);

        return isSafe;
    }

    /**
     * Returns in-memory blocked transactions for this session.
     * For a complete persistent list, use databaseManager.getBlockedTransactions().
     *
     * @return ArrayList of FRAUD transactions this session
     */
    public ArrayList<Transaction> getBlockedTransactions() {
        return blockedTransactions;
    }

    /**
     * Returns the most recently processed transaction.
     */
    public Transaction getLastTransaction() {
        return lastTransaction;
    }

    /**
     * Validates an amount string.
     * Demonstrates Exception Handling.
     *
     * @param input String representation of the amount
     * @throws Exception if amount is not positive
     */
    public void validateAmount(String input) throws Exception {
        double amount = Double.parseDouble(input);
        if (amount <= 0) throw new Exception("Amount must be greater than zero.");
    }
}
