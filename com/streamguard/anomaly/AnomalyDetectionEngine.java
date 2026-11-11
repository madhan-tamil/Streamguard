package com.streamguard.anomaly;

import com.streamguard.model.BankAccount;
import com.streamguard.model.RiskScore;
import com.streamguard.model.Transaction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AnomalyDetectionEngine.java
 * Replaces hardcoded rule-based checks with Behavioral Anomaly Detection & Statistical Outlier Analysis.
 *
 * How this scientifically answers the professor's logical questions:
 *
 * 1. "Why is a transaction outside the registered city marked fraud? People travel!"
 *    -> Solved by Haversine Travel Velocity Check:
 *       Calculates distance and speed between consecutive transactions.
 *       If you swipe in Chennai and 4 hours later swipe in Bangalore (290 km, 72 km/h),
 *       it is mathematically feasible travel -> APPROVED (0 risk points).
 *       If you swipe in Chennai and 20 minutes later swipe in Delhi (1,760 km, 5,280 km/h),
 *       it is physically impossible travel -> FLAGGED as Cloned Card (Impossible Travel Anomaly).
 *
 * 2. "Why is an amount over Rs. 50,000 marked fraud? What if the user is wealthy?"
 *    -> Solved by Individual Statistical Z-Score Profiling:
 *       Calculates Z = (Amount - μ) / σ for each account's historical spending.
 *       A ₹40,000 transaction for a business account with μ=₹35,000 has Z ≈ 0.3σ -> APPROVED!
 *       A ₹40,000 transaction for a student account with μ=₹1,500 has Z ≈ 25σ -> Extreme Outlier!
 *
 * 3. "Why is late night (11 PM - 5 AM) marked fraud? People work night shifts!"
 *    -> Solved by Contextual Anomaly Weighting:
 *       Time is no longer an automatic standalone block. A late night food order with normal
 *       Z-score (< 1.5) and local coordinates is approved as SAFE.
 *
 * 4. "What if a fraudster splits Rs. 60,000 into multiple Rs. 9,999 transactions?"
 *    -> Solved by Sliding-Window Burst Velocity Detection:
 *       Detects rapid micro-transactions in small time windows (card testing & structuring).
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class AnomalyDetectionEngine {

    // Account behavioral profiles: accountId -> UserProfileStatistics
    private final Map<Integer, UserProfileStatistics> userProfiles = new HashMap<>();

    // Sliding time window for burst velocity: accountId -> List of recent timestamps
    private final Map<Integer, List<LocalDateTime>> recentTxnTimes = new HashMap<>();

    public AnomalyDetectionEngine() {
        initDefaultProfiles();
    }

    /**
     * Seeds initial realistic customer statistical baselines.
     * Demonstrates dynamic Gaussian parameters per user persona.
     */
    private void initDefaultProfiles() {
        // Account 1001: Madhan T (Chennai) - Regular salaried personal account
        userProfiles.put(1001, new UserProfileStatistics(1001, "MADHAN T", "Chennai", 8000.0, 2500.0));

        // Account 1002: Avinash N T (Chennai) - Moderate personal spender
        userProfiles.put(1002, new UserProfileStatistics(1002, "AVINASH N T", "Chennai", 6000.0, 1800.0));

        // Account 1003: Rajesh Kumar (Coimbatore) - Commercial Current Account
        userProfiles.put(1003, new UserProfileStatistics(1003, "RAJESH KUMAR", "Coimbatore", 25000.0, 8000.0));

        // Account 1004: Priya S (Bangalore) - Student/Young Professional Savings
        userProfiles.put(1004, new UserProfileStatistics(1004, "PRIYA S", "Bangalore", 3500.0, 1200.0));

        // Account 1005: Suresh V (Mumbai) - High Net-Worth Corporate Current Account
        userProfiles.put(1005, new UserProfileStatistics(1005, "SURESH V", "Mumbai", 50000.0, 15000.0));
    }

    /**
     * Gets or creates a profile for an account.
     */
    public synchronized UserProfileStatistics getProfile(BankAccount account) {
        return userProfiles.computeIfAbsent(account.getAccountId(), id -> {
            // Default baseline based on account type
            double defaultMean = "CURRENT".equalsIgnoreCase(account.getAccountType()) ? 20000.0 : 5000.0;
            double defaultStd = defaultMean * 0.35;
            return new UserProfileStatistics(id, account.getAccountHolder(), account.getRegisteredCity(), defaultMean, defaultStd);
        });
    }

    /**
     * Evaluates a transaction using Behavioral Anomaly & Statistical Outlier Detection.
     *
     * @param txn The transaction to evaluate
     * @param account The bank account
     * @return RiskScore (0-100) with verdict and scientific explanation
     */
    public RiskScore evaluate(Transaction txn, BankAccount account) {
        int totalRiskScore = 0;
        String topReason = "Normal customer transaction pattern";
        int topWeight = 0;

        UserProfileStatistics profile = getProfile(account);

        System.out.println("\n  [ANOMALY ENGINE] Evaluating transaction via Statistical & Behavioral Profiling...");
        System.out.println("  ------------------------------------------------------------------------");

        // =====================================================================
        // 1. STATISTICAL AMOUNT ANOMALY (Z-Score Analysis)
        // =====================================================================
        double zScore = profile.calculateZScore(txn.getAmount());
        System.out.printf("  [STATISTICS] Personal Mean μ = Rs.%.2f, StdDev σ = Rs.%.2f%n",
                profile.getMean(), profile.getStandardDeviation());
        System.out.printf("  [STATISTICS] Candidate Amount Rs.%.2f -> Z-Score = %.2fσ%n",
                txn.getAmount(), zScore);

        if (zScore > 3.5) {
            // Extreme statistical outlier (>99.98% deviation from this customer's habit)
            int pts = 55;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Extreme Amount Anomaly (Z = %.2fσ exceeds 3.5σ threshold)%n", pts, zScore);
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("Statistical spending outlier: Amount Rs.%.2f is %.1fσ above personal average (Rs.%.0f)",
                        txn.getAmount(), zScore, profile.getMean());
            }
        } else if (zScore > 2.5) {
            // Moderate statistical deviation
            int pts = 35;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Moderate Amount Anomaly (Z = %.2fσ exceeds 2.5σ threshold)%n", pts, zScore);
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("Unusual amount: Rs.%.2f deviates %.1fσ from customer baseline (Rs.%.0f)",
                        txn.getAmount(), zScore, profile.getMean());
            }
        } else if (zScore > 1.8) {
            // Mild deviation
            int pts = 15;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Mild Amount Deviation (Z = %.2fσ)%n", pts, zScore);
        } else {
            System.out.printf("  [ 0 pts] Amount is within standard personal spending deviation (Z = %.2fσ)%n", zScore);
        }

        // =====================================================================
        // 2. SPATIO-TEMPORAL TRAVEL ANOMALY (Haversine Impossible Speed)
        // =====================================================================
        String prevCity = profile.getLastLocation();
        LocalDateTime prevTime = profile.getLastTimestamp();
        LocalDateTime currTime = txn.getTimestamp();

        double distanceKm = GeoLocationService.calculateDistanceKm(prevCity, txn.getLocation());
        long minutesElapsed = Math.max(Duration.between(prevTime, currTime).toMinutes(), 1);
        double speedKmh = GeoLocationService.calculateSpeedKmh(distanceKm, minutesElapsed);

        System.out.printf("  [GEOLOCATION] Previous: %s (%s) -> Current: %s (%s)%n",
                prevCity, prevTime.toLocalTime(), txn.getLocation(), currTime.toLocalTime());
        System.out.printf("  [GEOLOCATION] Distance = %.1f km, Time elapsed = %d mins -> Velocity = %.1f km/h%n",
                distanceKm, minutesElapsed, speedKmh);

        if (distanceKm > 50.0 && speedKmh > GeoLocationService.MAX_FEASIBLE_SPEED_KMH) {
            // Impossible physical travel (Speed > commercial aircraft ~850 km/h) -> Card Cloned!
            int pts = 65;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] IMPOSSIBLE TRAVEL ANOMALY! Velocity %.1f km/h exceeds maximum aircraft speed (850 km/h)%n",
                    pts, speedKmh);
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("Impossible Travel Anomaly: %s to %s (%.0f km in %d mins = %.0f km/h exceeds feasible aircraft speed - Cloned Card Indicator)",
                        prevCity, txn.getLocation(), distanceKm, minutesElapsed, speedKmh);
            }
        } else if (distanceKm > 200.0 && speedKmh > 350.0 && minutesElapsed < 60) {
            // Unlikely high-speed relocation
            int pts = 30;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Rapid Relocation Anomaly (%.1f km/h)%n", pts, speedKmh);
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("Suspicious relocation velocity: %.0f km/h between %s and %s",
                        speedKmh, prevCity, txn.getLocation());
            }
        } else if (distanceKm > 0) {
            System.out.printf("  [ 0 pts] Valid travel: %s to %s within normal travel speed (%.1f km/h)%n",
                    prevCity, txn.getLocation(), speedKmh);
        } else {
            System.out.println("  [ 0 pts] Local transaction in registered/last activity city");
        }

        // =====================================================================
        // 3. SLIDING-WINDOW BURST VELOCITY ANOMALY (Card Testing / Bot Attacks)
        // =====================================================================
        List<LocalDateTime> history = recentTxnTimes.computeIfAbsent(account.getAccountId(), k -> new ArrayList<>());
        LocalDateTime tenMinsAgo = currTime.minusMinutes(10);
        history.removeIf(t -> t.isBefore(tenMinsAgo));
        history.add(currTime);

        if (history.size() >= 5) {
            int pts = 45;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Severe Transaction Burst Anomaly: %d transactions in 10 minutes%n", pts, history.size());
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("High-frequency burst anomaly: %d transactions in 10 minutes (Bot / Card-Testing pattern)",
                        history.size());
            }
        } else if (history.size() >= 3) {
            int pts = 25;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Moderate Velocity Burst: %d transactions in 10 minutes%n", pts, history.size());
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("Rapid transaction velocity: %d transactions in 10-minute window", history.size());
            }
        }

        // =====================================================================
        // 4. FINANCIAL SOLVENCY CONSTRAINT
        // =====================================================================
        if (account.getBalance() < txn.getAmount()) {
            int pts = 40;
            totalRiskScore += pts;
            System.out.printf("  [+%d pts] Insufficient Account Balance (Available: Rs.%.2f, Required: Rs.%.2f)%n",
                    pts, account.getBalance(), txn.getAmount());
            if (pts > topWeight) {
                topWeight = pts;
                topReason = String.format("Insufficient balance: Available Rs.%.2f, Required Rs.%.2f",
                        account.getBalance(), txn.getAmount());
            }
        }

        // =====================================================================
        // 5. CONTEXTUAL CIRCADIAN TIME ANOMALY
        // =====================================================================
        int hour = currTime.getHour();
        if ((hour >= 23 || hour < 5) && totalRiskScore > 20) {
            // Only add risk points if another anomaly was ALREADY detected
            totalRiskScore += 15;
            System.out.printf("  [+15 pts] Contextual Late-Night Anomaly (%02d:00) paired with existing deviations%n", hour);
        }

        // Capping total score to 100
        int finalScore = Math.min(totalRiskScore, 100);

        System.out.println("  ------------------------------------------------------------------------");
        System.out.printf("  [ANOMALY RESULT] Composite Risk Score: %d / 100%n", finalScore);

        RiskScore result = new RiskScore(finalScore, topReason);

        // If transaction is safe, update baseline profile
        if (result.isSafe()) {
            profile.recordTransaction(txn.getAmount(), txn.getLocation(), txn.getTimestamp());
        }

        return result;
    }
}

