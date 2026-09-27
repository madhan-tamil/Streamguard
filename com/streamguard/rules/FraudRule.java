package com.streamguard.rules;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

/**
 * FraudRule.java
 * Abstract base class for all fraud detection rules.
 * Demonstrates: Abstraction, Interface, Polymorphism
 */
public abstract class FraudRule implements FraudCheck {

    private String ruleName;

    public FraudRule(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleName() {
        return ruleName;
    }

    @Override
    public abstract String check(Transaction transaction, BankAccount account);
}