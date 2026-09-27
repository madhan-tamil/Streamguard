package com.streamguard.rules;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

/**
 * AmountRule.java
 * Flags transactions where amount exceeds Rs.50,000.
 * Demonstrates: Inheritance, Polymorphism (overriding check())
 */
public class AmountRule extends FraudRule {

    private static final double LIMIT = 50000.0;

    public AmountRule() {
        super("Amount Limit Rule");
    }

    @Override
    public String check(Transaction transaction, BankAccount account) {
        if (transaction.getAmount() > LIMIT) {
            return String.format("Transaction amount Rs.%.2f exceeds limit of Rs.%.2f",
                transaction.getAmount(), LIMIT);
        }
        return null; // SAFE
    }
}
