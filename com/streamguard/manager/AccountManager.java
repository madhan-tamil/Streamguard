package com.streamguard.manager;

import com.streamguard.model.BankAccount;
import com.streamguard.model.CurrentAccount;
import com.streamguard.model.SavingsAccount;

import java.util.HashMap;

/**
 * AccountManager.java
 * Manages all bank accounts using a HashMap.
 *
 * Demonstrates:
 *   - Collections Framework (HashMap)
 *   - Methods
 *   - Database integration (accounts loaded from MySQL at startup)
 *
 * The HashMap remains as the fast in-memory lookup store.
 * MySQL provides persistence; the HashMap provides O(1) access during fraud checks.
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class AccountManager {

    // HashMap<AccountId, BankAccount> – Collections Framework
    private HashMap<Integer, BankAccount> accounts;

    /**
     * Constructor – initializes an empty HashMap.
     * Accounts are populated by DatabaseManager.loadAccountsFromDatabase()
     * at application startup (see Main.java).
     */
    public AccountManager() {
        accounts = new HashMap<>();
    }

    /**
     * Adds (or updates) an account in the in-memory HashMap.
     * Called by DatabaseManager after reading from MySQL.
     *
     * @param accountId The account ID (primary key)
     * @param account   The BankAccount object
     */
    public void addAccount(int accountId, BankAccount account) {
        accounts.put(accountId, account);
    }

    /**
     * Pre-loads the five demo accounts directly into the HashMap.
     * Used as a fallback if the MySQL database is unavailable.
     * This preserves backward compatibility.
     */
    public void loadDefaultAccounts() {
        accounts.clear();
        accounts.put(1001, new SavingsAccount(1001, "MADHAN T",     75000.00, "Chennai"));
        accounts.put(1002, new SavingsAccount(1002, "AVINASH N T",  50000.00, "Chennai"));
        accounts.put(1003, new CurrentAccount(1003, "RAJESH KUMAR", 120000.00, "Coimbatore", 25000.00));
        accounts.put(1004, new SavingsAccount(1004, "PRIYA S",      30000.00, "Bangalore"));
        accounts.put(1005, new CurrentAccount(1005, "SURESH V",     200000.00, "Mumbai", 50000.00));
        System.out.println("  [INFO] Loaded default in-memory accounts (no MySQL).");
    }

    /**
     * Retrieve account by ID from HashMap (O(1) lookup).
     *
     * @param accountId The account ID
     * @return BankAccount if found, null otherwise
     */
    public BankAccount getAccount(int accountId) {
        return accounts.get(accountId);
    }

    /**
     * List all accounts to console.
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
     *
     * @param accountId The account ID to check
     * @return true if the account is registered
     */
    public boolean accountExists(int accountId) {
        return accounts.containsKey(accountId);
    }

    /**
     * Returns the entire accounts map.
     * Used by WebServer /api/accounts endpoint.
     *
     * @return HashMap of all accounts
     */
    public HashMap<Integer, BankAccount> getAllAccounts() {
        return accounts;
    }
}
