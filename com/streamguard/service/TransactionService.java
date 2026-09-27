package com.streamguard.service;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

import java.util.ArrayList;

/**
 * TransactionService.java
 * Coordinates transaction processing: validate → fraud check → update balance → save.
 * Demonstrates: Methods, Exception Handling, Collections
 */
public class TransactionService {

    private FraudChecker fraudChecker;
    private FileManager fileManager;
    private ArrayList<Transaction> blockedTransactions;
    private int txnCounter;

    public TransactionService() {
        this.fraudChecker = new FraudChecker();
        this.fileManager = new FileManager();
        this.blockedTransactions = new ArrayList<>();
        this.txnCounter = 1001;
    }

    /**
     * Main method to process a transaction.
     * Returns true if transaction is approved (SAFE), false if blocked (FRAUD).
     */
    public boolean processTransaction(BankAccount account, double amount, String location) {
        // Create transaction object
        Transaction txn = new Transaction(txnCounter++, amount, location);

        System.out.println("\n  ----------------------------------------");
        System.out.printf("  Processing Transaction TXN%04d%n", txn.getTxnId());
        System.out.printf("  Account: %s | Amount: Rs.%.2f | Location: %s%n",
            account.getAccountHolder(), amount, location);
        System.out.println("  ----------------------------------------");

        // Run fraud checks
        boolean isSafe = fraudChecker.runAllChecks(txn, account);

        // Display result
        System.out.println("\n  ========================================");
        if (isSafe) {
            System.out.println("  RESULT  :  ✔  TRANSACTION APPROVED (SAFE)");
            account.withdraw(amount);
        } else {
            System.out.println("  RESULT  :  ✘  TRANSACTION BLOCKED (FRAUD)");
            System.out.println("  REASON  :  " + txn.getFraudReason());
            blockedTransactions.add(txn);
        }
        System.out.println("  ========================================\n");

        // Save to file
        fileManager.saveTransaction(txn);

        return isSafe;
    }

    public ArrayList<Transaction> getBlockedTransactions() {
        return blockedTransactions;
    }

    public void validateAmount(String input) throws Exception {
        double amount = Double.parseDouble(input);
        if (amount <= 0) throw new Exception("Amount must be greater than zero.");
    }
}
