package com.streamguard.manager;

import com.streamguard.model.BankAccount;
import com.streamguard.model.CurrentAccount;
import com.streamguard.model.SavingsAccount;

import java.util.HashMap;

/**
 * AccountManager.java
 * Manages all bank accounts using a HashMap.
 * Demonstrates: Collections Framework (HashMap), Methods
 */
public class AccountManager {

    private HashMap<Integer, BankAccount> accounts;

    public AccountManager() {
        accounts = new HashMap<>();
        loadDefaultAccounts();
    }

    /**
     * Pre-loads sample accounts for demo purposes.
     */
    private void loadDefaultAccounts() {
        accounts.put(1001, new SavingsAccount(1001, "MADHAN T",      75000.00, "Chennai"));
        accounts.put(1002, new SavingsAccount(1002, "AVINASH N T",   50000.00, "Chennai"));
        accounts.put(1003, new CurrentAccount(1003, "RAJESH KUMAR",  120000.00, "Coimbatore", 25000.00));
        accounts.put(1004, new SavingsAccount(1004, "PRIYA S",       30000.00, "Bangalore"));
        accounts.put(1005, new CurrentAccount(1005, "SURESH V",      200000.00, "Mumbai", 50000.00));
    }

    /**
     * Retrieve account by ID.
     */
    public BankAccount getAccount(int accountId) {
        return accounts.get(accountId);
    }

    /**
     * List all accounts.
     */
    public void listAccounts() {
        System.out.println("\n  ======= REGISTERED ACCOUNTS =======");
        for (BankAccount acc : accounts.values()) {
            System.out.println("  " + acc);
        }
        System.out.println("  ====================================\n");
    }

    /**
     * Check if an account exists.
     */
    public boolean accountExists(int accountId) {
        return accounts.containsKey(accountId);
    }
}
