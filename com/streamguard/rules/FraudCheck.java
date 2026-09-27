package com.streamguard.rules;

import com.streamguard.model.BankAccount;
import com.streamguard.model.Transaction;

/**
 * FraudCheck.java
 * Interface for all fraud detection rules.
 * Demonstrates: Interface
 */
public interface FraudCheck {

    String check(Transaction transaction, BankAccount account);

}