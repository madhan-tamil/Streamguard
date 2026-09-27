package com.streamguard.service;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;
import com.streamguard.rules.*;

import java.util.ArrayList;

/**
 * FraudChecker.java
 * Runs all registered fraud rules against a transaction.
 * Demonstrates: Collections (ArrayList), Polymorphism, Loops
 */
public class FraudChecker {

    private ArrayList<FraudRule> rules;

    public FraudChecker() {
        rules = new ArrayList<>();
        // Register all rules
        rules.add(new BalanceRule());
        rules.add(new AmountRule());
        rules.add(new LocationRule());
        rules.add(new TimeRule());
    }

    /**
     * Runs all fraud rules on the transaction.
     * Sets transaction status to SAFE or FRAUD with reason.
     * Returns true if SAFE, false if FRAUD.
     */
    public boolean runAllChecks(Transaction transaction, BankAccount account) {
        System.out.println("\n  [CHECKING] Running fraud detection rules...");

        for (FraudRule rule : rules) {
            String result = rule.check(transaction, account);
            if (result != null) {
                // FRAUD detected
                transaction.setStatus("FRAUD");
                transaction.setFraudReason(result);
                System.out.println("  [RULE] " + rule.getRuleName() + " -> FRAUD");
                return false;
            } else {
                System.out.println("  [RULE] " + rule.getRuleName() + " -> SAFE");
            }
        }

        transaction.setStatus("SAFE");
        return true;
    }
}
