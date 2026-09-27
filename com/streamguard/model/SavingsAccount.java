package com.streamguard.model;

/**
 * SavingsAccount.java
 * Extends BankAccount with minimum balance enforcement.
 * Demonstrates: Inheritance, Polymorphism (method overriding)
 */
public class SavingsAccount extends BankAccount {

    private static final double MINIMUM_BALANCE = 1000.0;

    public SavingsAccount(int accountId, String accountHolder,
                          double balance, String registeredCity) {
        super(accountId, accountHolder, balance, "SAVINGS", registeredCity);
    }

    @Override
    public boolean deposit(double amount) {
        if (amount <= 0) {
            System.out.println("  [ERROR] Deposit amount must be positive.");
            return false;
        }
        setBalance(getBalance() + amount);
        System.out.printf("  [OK] Deposited Rs.%.2f. New Balance: Rs.%.2f%n", amount, getBalance());
        return true;
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("  [ERROR] Withdrawal amount must be positive.");
            return false;
        }
        if (getBalance() - amount < MINIMUM_BALANCE) {
            System.out.printf("  [ERROR] Cannot withdraw. Minimum balance of Rs.%.2f must be maintained.%n",
                MINIMUM_BALANCE);
            return false;
        }
        setBalance(getBalance() - amount);
        System.out.printf("  [OK] Withdrawn Rs.%.2f. New Balance: Rs.%.2f%n", amount, getBalance());
        return true;
    }
}
