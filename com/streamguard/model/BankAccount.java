package com.streamguard.model;

/**
 * BankAccount.java
 * Abstract base class for all bank account types.
 * Demonstrates: Abstraction, Encapsulation, Inheritance (base)
 */
public abstract class BankAccount {

    private int accountId;
    private String accountHolder;
    private double balance;
    private String accountType;
    private String registeredCity;

    // Constructor
    public BankAccount(int accountId, String accountHolder, double balance,
                       String accountType, String registeredCity) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.accountType = accountType;
        this.registeredCity = registeredCity;
    }

    // Abstract methods — subclasses must implement
    public abstract boolean deposit(double amount);
    public abstract boolean withdraw(double amount);

    // Getters
    public int getAccountId()         { return accountId; }
    public String getAccountHolder()  { return accountHolder; }
    public double getBalance()        { return balance; }
    public String getAccountType()    { return accountType; }
    public String getRegisteredCity() { return registeredCity; }

    // Protected setter so subclasses can update balance
    protected void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return String.format(
            "Account[%d] | Holder: %s | Type: %s | Balance: Rs.%.2f | City: %s",
            accountId, accountHolder, accountType, balance, registeredCity
        );
    }
}
