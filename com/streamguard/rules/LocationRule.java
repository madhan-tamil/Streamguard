package com.streamguard.rules;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

/**
 * LocationRule.java
 * Flags transactions from a city different from the account's registered city.
 * Demonstrates: Inheritance, Polymorphism (overriding check())
 */
public class LocationRule extends FraudRule {

    public LocationRule() {
        super("Location Change Rule");
    }

    @Override
    public String check(Transaction transaction, BankAccount account) {
        String txnCity = transaction.getLocation().trim().toLowerCase();
        String regCity = account.getRegisteredCity().trim().toLowerCase();

        if (!txnCity.equals(regCity)) {
            return String.format("Transaction location '%s' differs from registered city '%s'",
                transaction.getLocation(), account.getRegisteredCity());
        }
        return null; // SAFE
    }
}
