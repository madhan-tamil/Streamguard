package com.streamguard.anomaly;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UserProfileStatistics.java
 * Maintains an individualized behavioral profile for each bank account.
 *
 * Replaces fixed threshold rules (e.g., "amount > 50,000") with
 * dynamic statistical profiling (Z-score calculation based on each
 * user's personal Gaussian spending distribution).
 *
 * Formula:
 *   Mean (μ) = (Σ x_i) / N
 *   Variance (σ^2) = (Σ (x_i - μ)^2) / N
 *   Z-Score = (amount - μ) / σ
 *
 * Interpretation:
 *   Z < 2.0 : Normal spending behavior for this specific customer
 *   2.0 <= Z < 3.0 : Moderate deviation (unusual spending)
 *   Z >= 3.0 : Extreme statistical outlier (>99.7% abnormal under normal distribution)
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class UserProfileStatistics {

    private final int accountId;
    private final String accountHolder;
    private final List<Double> historicalAmounts;
    private String lastLocation;
    private LocalDateTime lastTimestamp;

    public UserProfileStatistics(int accountId, String accountHolder, String initialCity, double defaultMean, double defaultStdDev) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.historicalAmounts = new ArrayList<>();
        this.lastLocation = initialCity;
        this.lastTimestamp = LocalDateTime.now().minusHours(6); // Default 6 hours ago

        // Seed with realistic initial data points around defaultMean
        seedInitialHistory(defaultMean, defaultStdDev);
    }

    private void seedInitialHistory(double mean, double stdDev) {
        historicalAmounts.add(mean);
        historicalAmounts.add(mean + stdDev * 0.5);
        historicalAmounts.add(mean - stdDev * 0.4);
        historicalAmounts.add(mean + stdDev * 0.8);
        historicalAmounts.add(mean - stdDev * 0.7);
        historicalAmounts.add(mean);
    }

    /**
     * Adds a newly approved transaction amount to update the customer's baseline.
     */
    public synchronized void recordTransaction(double amount, String location, LocalDateTime timestamp) {
        historicalAmounts.add(amount);
        // Keep window of last 50 transactions to adapt to recent habit shifts
        if (historicalAmounts.size() > 50) {
            historicalAmounts.remove(0);
        }
        this.lastLocation = location;
        this.lastTimestamp = timestamp;
    }

    /**
     * Computes the statistical mean (μ) of this account's transactions.
     */
    public synchronized double getMean() {
        if (historicalAmounts.isEmpty()) return 5000.0;
        double sum = 0;
        for (double a : historicalAmounts) sum += a;
        return sum / historicalAmounts.size();
    }

    /**
     * Computes the standard deviation (σ) of this account's transactions.
     */
    public synchronized double getStandardDeviation() {
        if (historicalAmounts.size() < 2) return 1000.0;
        double mean = getMean();
        double sumSquaredDiff = 0;
        for (double a : historicalAmounts) {
            double diff = a - mean;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / historicalAmounts.size());
        // Enforce minimum stdDev (15% of mean) to avoid division by zero or extreme sensitivity
        return Math.max(stdDev, mean * 0.15);
    }

    /**
     * Calculates the statistical Z-score for a candidate transaction amount.
     * Z = (amount - μ) / σ
     *
     * @param amount The candidate amount to evaluate
     * @return Z-score (number of standard deviations away from personal mean)
     */
    public synchronized double calculateZScore(double amount) {
        double mean = getMean();
        double stdDev = getStandardDeviation();
        return (amount - mean) / stdDev;
    }

    public int getAccountId() { return accountId; }
    public String getAccountHolder() { return accountHolder; }
    public synchronized String getLastLocation() { return lastLocation; }
    public synchronized LocalDateTime getLastTimestamp() { return lastTimestamp; }
    public synchronized int getHistoryCount() { return historicalAmounts.size(); }
}

