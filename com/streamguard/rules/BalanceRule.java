package com.streamguard.rules;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

/**
 * BalanceRule.java
 * Flags transactions where account balance is insufficient.
 * Demonstrates: Inheritance, Polymorphism (overriding check())
 */
public class BalanceRule extends FraudRule {

    public BalanceRule() {
        super("Insufficient Balance Rule");
    }

    @Override
    public String check(Transaction transaction, BankAccount account) {
        if (account.getBalance() < transaction.getAmount()) {
            return String.format("Insufficient balance. Available: Rs.%.2f, Required: Rs.%.2f",
                account.getBalance(), transaction.getAmount());
        }
        return null; // SAFE
    }
}
