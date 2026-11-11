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
    private String status;      // "SAFE", "SUSPICIOUS", or "FRAUD"
    private String fraudReason;
    private int    riskScore;   // 0–100 from the Risk Score Engine
    private String verdict;     // "SAFE" / "SUSPICIOUS" / "FRAUD"

    // Constructor
    public Transaction(int txnId, double amount, String location) {
        this.txnId = txnId;
        this.amount = amount;
        this.location = location;
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
        this.fraudReason = "";
        this.riskScore = 0;
        this.verdict   = "PENDING";
    }

    // Getters
    public int getTxnId()          { return txnId; }
    public double getAmount()      { return amount; }
    public String getLocation()    { return location; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus()      { return status; }
    public String getFraudReason() { return fraudReason; }
    public int    getRiskScore()   { return riskScore; }
    public String getVerdict()     { return verdict; }

    // Setters
    public void setStatus(String status)           { this.status = status; }
    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }
    public void setRiskScore(int score)            { this.riskScore = score; }
    public void setVerdict(String verdict)         { this.verdict = verdict; }

    /**
     * Restores the timestamp from a database record.
     * Used by DatabaseManager when reading transactions from MySQL.
     */
    public void setTimestampFromDb(LocalDateTime ts) { this.timestamp = ts; }

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
