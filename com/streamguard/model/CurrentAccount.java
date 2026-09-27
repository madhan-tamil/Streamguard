package com.streamguard.model;

/**
 * CurrentAccount.java
 * Extends BankAccount with overdraft limit support.
 * Demonstrates: Inheritance, Polymorphism (method overriding)
 */
public class CurrentAccount extends BankAccount {

    private double overdraftLimit;

    public CurrentAccount(int accountId, String accountHolder,
                          double balance, String registeredCity, double overdraftLimit) {
        super(accountId, accountHolder, balance, "CURRENT", registeredCity);
        this.overdraftLimit = overdraftLimit;
    }

    public double getOverdraftLimit() { return overdraftLimit; }

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
        if (getBalance() - amount < -overdraftLimit) {
            System.out.printf("  [ERROR] Exceeds overdraft limit of Rs.%.2f.%n", overdraftLimit);
            return false;
        }
        setBalance(getBalance() - amount);
        System.out.printf("  [OK] Withdrawn Rs.%.2f. New Balance: Rs.%.2f%n", amount, getBalance());
        return true;
    }
}
