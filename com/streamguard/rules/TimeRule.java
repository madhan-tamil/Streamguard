package com.streamguard.rules;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

/**
 * TimeRule.java
 * Flags transactions that occur between 11 PM and 5 AM (unusual hours).
 * Demonstrates: Inheritance, Polymorphism (overriding check())
 */
public class TimeRule extends FraudRule {

    public TimeRule() {
        super("Unusual Time Rule");
    }

    @Override
    public String check(Transaction transaction, BankAccount account) {
        int hour = transaction.getTimestamp().getHour(); // 0-23

        // Flag if hour is between 23 (11 PM) and 5 (5 AM)
        if (hour >= 23 || hour < 5) {
            return String.format("Transaction at unusual hour (%02d:00) — between 11 PM and 5 AM", hour);
        }
        return null; // SAFE
    }
}
