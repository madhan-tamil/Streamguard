package com.streamguard.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction.java
 * Represents a single banking transaction.
 * Demonstrates: Encapsulation, Constructors
 */
public class Transaction {

    private int txnId;
    private double amount;
    private String location;
    private LocalDateTime timestamp;
    private String status; // "SAFE" or "FRAUD"
    private String fraudReason;

    // Constructor
    public Transaction(int txnId, double amount, String location) {
        this.txnId = txnId;
        this.amount = amount;
        this.location = location;
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
        this.fraudReason = "";
    }

    // Getters
    public int getTxnId()          { return txnId; }
    public double getAmount()      { return amount; }
    public String getLocation()    { return location; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus()      { return status; }
    public String getFraudReason() { return fraudReason; }

    // Setters
    public void setStatus(String status)           { this.status = status; }
    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return String.format(
            "TXN%04d | Amount: Rs.%.2f | Location: %s | Time: %s | Status: %s%s",
            txnId, amount, location,
            timestamp.format(fmt),
            status,
            fraudReason.isEmpty() ? "" : " | Reason: " + fraudReason
        );
    }
}
