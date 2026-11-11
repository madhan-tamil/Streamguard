package com.streamguard.service;

import com.streamguard.model.BankAccount;
import com.streamguard.model.RiskScore;
import com.streamguard.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * ScoredFraudChecker.java
 * Weighted Risk Score Engine for StreamGuard.
 *
 * Replaces the binary FraudChecker with a probabilistic model.
 * Each signal adds a weighted number of risk points (0–100 total).
 *
 * Score bands:
 *   0  – 30  →  SAFE         (transaction approved)
 *   31 – 60  →  SUSPICIOUS   (flagged but not fully blocked)
 *   61 – 100 →  FRAUD        (transaction blocked)
 *
 * Why this is better than pure rule-based detection:
 *   - A ₹49,999 transaction at 2 AM from a different city gets flagged
 *     even though no single rule triggers.
 *   - Fraudsters cannot game a single threshold (e.g. splitting ₹60k
 *     into two ₹29k transactions still accumulates risk from other signals).
 *   - Three risk levels (SAFE / SUSPICIOUS / FRAUD) allow graduated
 *     responses rather than hard blocks.
 *
 * Demonstrates:
 *   - OOP (encapsulation, return type RiskScore)
 *   - Collections (List for velocity window)
 *   - Lambda / removeIf
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CS, Chennai Institute of Technology
 */
public class ScoredFraudChecker {

    // =========================================================
    // WEIGHTS — each signal's contribution to the risk score
    // =========================================================

    /** Insufficient balance — highest weight, almost certainly suspicious. */
    private static final int W_BALANCE   = 40;

    /** Amount > ₹50,000 — high-value transaction signal. */
    private static final int W_AMOUNT    = 30;

    /** High velocity — ≥3 transactions in a 10-minute window. */
    private static final int W_VELOCITY  = 35;

    /** City mismatch — transaction city differs from registered city. */
    private static final int W_LOCATION  = 25;

    /** High frequency — ≥5 transactions today. */
    private static final int W_FREQUENCY = 25;

    /** Unusual time — 11 PM to 5 AM. */
    private static final int W_TIME      = 20;

    /** Round number — exact multiples of ₹1,000 ≥ ₹10,000. */
    private static final int W_ROUND     = 10;

    // =========================================================
    // THRESHOLDS
    // =========================================================

    private static final double AMOUNT_LIMIT     = 50_000.0;
    private static final int    VELOCITY_LIMIT   = 3;         // txns per 10-min window
    private static final int    FREQUENCY_LIMIT  = 5;         // txns per day
    private static final long   VELOCITY_WINDOW  = 10 * 60 * 1000L; // 10 minutes in ms

    // =========================================================
    // STATE — velocity / frequency tracking
    // =========================================================

    /** Timestamps of recent transactions for velocity checking. */
    private final List<Long> recentTimestamps = new ArrayList<>();

    /** Count of transactions processed today (session-based). */
    private int dailyCount = 0;

    // =========================================================
    // MAIN METHOD
    // =========================================================

    /**
     * Evaluates the transaction and returns a RiskScore.
     *
     * @param txn     The transaction to evaluate
     * @param account The owning bank account
     * @return        RiskScore with score (0–100), verdict, and top reason
     */
    public RiskScore evaluate(Transaction txn, BankAccount account) {

        int    total     = 0;
        String topReason = "";
        int    topWeight = 0;

        System.out.println("\n  [RISK ENGINE] Calculating weighted risk score...");
        System.out.println("  ─────────────────────────────────────────────");

        // ── 1. INSUFFICIENT BALANCE ──────────────────────────
        if (account.getBalance() < txn.getAmount()) {
            total += W_BALANCE;
            printSignal(W_BALANCE, "Insufficient balance");
            if (W_BALANCE > topWeight) {
                topWeight = W_BALANCE;
                topReason = String.format(
                    "Insufficient balance — available ₹%.2f, required ₹%.2f",
                    account.getBalance(), txn.getAmount());
            }
        }

        // ── 2. HIGH VELOCITY (≥3 txns in 10-min window) ─────
        long now = System.currentTimeMillis();
        recentTimestamps.add(now);
        recentTimestamps.removeIf(t -> now - t > VELOCITY_WINDOW);

        if (recentTimestamps.size() >= VELOCITY_LIMIT) {
            total += W_VELOCITY;
            printSignal(W_VELOCITY,
                String.format("High velocity: %d transactions in 10 min",
                    recentTimestamps.size()));
            if (W_VELOCITY > topWeight) {
                topWeight = W_VELOCITY;
                topReason = String.format(
                    "High velocity: %d transactions submitted in a 10-minute window",
                    recentTimestamps.size());
            }
        }

        // ── 3. AMOUNT LIMIT ──────────────────────────────────
        if (txn.getAmount() > AMOUNT_LIMIT) {
            total += W_AMOUNT;
            printSignal(W_AMOUNT,
                String.format("Amount ₹%.2f exceeds limit ₹%.0f",
                    txn.getAmount(), AMOUNT_LIMIT));
            if (W_AMOUNT > topWeight) {
                topWeight = W_AMOUNT;
                topReason = String.format(
                    "Transaction amount ₹%.2f exceeds limit ₹%.0f",
                    txn.getAmount(), AMOUNT_LIMIT);
            }
        }

        // ── 4. LOCATION MISMATCH ─────────────────────────────
        String txnCity = txn.getLocation().trim().toLowerCase();
        String regCity = account.getRegisteredCity().trim().toLowerCase();

        if (!txnCity.equals(regCity)) {
            total += W_LOCATION;
            printSignal(W_LOCATION,
                String.format("Location '%s' ≠ registered city '%s'",
                    txn.getLocation(), account.getRegisteredCity()));
            if (W_LOCATION > topWeight) {
                topWeight = W_LOCATION;
                topReason = String.format(
                    "Transaction in '%s' but account registered in '%s'",
                    txn.getLocation(), account.getRegisteredCity());
            }
        }

        // ── 5. HIGH DAILY FREQUENCY ──────────────────────────
        dailyCount++;
        if (dailyCount >= FREQUENCY_LIMIT) {
            total += W_FREQUENCY;
            printSignal(W_FREQUENCY,
                String.format("High frequency: %d transactions today", dailyCount));
            if (W_FREQUENCY > topWeight) {
                topWeight = W_FREQUENCY;
                topReason = String.format(
                    "High transaction frequency: %d transactions processed today",
                    dailyCount);
            }
        }

        // ── 6. UNUSUAL TIME ───────────────────────────────────
        int hour = txn.getTimestamp().getHour();
        if (hour >= 23 || hour < 5) {
            total += W_TIME;
            printSignal(W_TIME,
                String.format("Unusual hour: %02d:00 (11 PM – 5 AM)", hour));
            if (W_TIME > topWeight) {
                topWeight = W_TIME;
                topReason = String.format(
                    "Transaction at unusual hour %02d:00 (between 11 PM and 5 AM)", hour);
            }
        }

        // ── 7. ROUND AMOUNT ───────────────────────────────────
        if (txn.getAmount() >= 10_000 && txn.getAmount() % 1_000 == 0) {
            total += W_ROUND;
            printSignal(W_ROUND,
                String.format("Suspicious round amount: ₹%.0f", txn.getAmount()));
            if (topWeight == 0) {
                topReason = String.format(
                    "Suspicious round number amount ₹%.0f", txn.getAmount());
            }
        }

        // ── FINAL RESULT ──────────────────────────────────────
        System.out.println("  ─────────────────────────────────────────────");

        RiskScore result = new RiskScore(total, topReason);
        System.out.printf("  [RISK SCORE] %d / 100  →  %s%n%n",
            result.getScore(), result.getVerdict());

        return result;
    }

    // =========================================================
    // HELPER
    // =========================================================

    private void printSignal(int weight, String description) {
        System.out.printf("  [+%2d pts] %s%n", weight, description);
    }
}

