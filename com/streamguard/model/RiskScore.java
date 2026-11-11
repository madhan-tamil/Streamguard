package com.streamguard.model;

/**
 * RiskScore.java
 * Encapsulates the numerical fraud risk score and final verdict.
 *
 * Score bands:
 *   0  – 30  → SAFE        (approved)
 *   31 – 60  → SUSPICIOUS  (flagged for review — not fully blocked)
 *   61 – 100 → FRAUD       (blocked)
 *
 * This replaces the old binary FRAUD/SAFE output of FraudChecker
 * and allows the system to give graduated responses.
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CS, Chennai Institute of Technology
 */
public class RiskScore {

    private final int    score;      // 0 to 100
    private final String verdict;    // "SAFE" / "SUSPICIOUS" / "FRAUD"
    private final String topReason;  // primary reason driving the score

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Builds a RiskScore, capping score at 100 and deriving verdict.
     *
     * @param score     Raw accumulated risk score
     * @param topReason Human-readable explanation of the highest-weight trigger
     */
    public RiskScore(int score, String topReason) {
        this.score     = Math.min(score, 100);
        this.topReason = topReason != null ? topReason : "";

        if (this.score >= 61)       this.verdict = "FRAUD";
        else if (this.score >= 31)  this.verdict = "SUSPICIOUS";
        else                        this.verdict = "SAFE";
    }

    // =========================================================
    // GETTERS
    // =========================================================

    /** Risk score 0–100. */
    public int getScore() { return score; }

    /** Final verdict: "SAFE", "SUSPICIOUS", or "FRAUD". */
    public String getVerdict() { return verdict; }

    /** Primary reason that contributed most to the score. */
    public String getTopReason() { return topReason; }

    /** True if the transaction should be fully blocked. */
    public boolean isFraud()      { return "FRAUD".equals(verdict); }

    /** True if the transaction should be flagged but not necessarily blocked. */
    public boolean isSuspicious() { return "SUSPICIOUS".equals(verdict); }

    /** True if the transaction passes all checks safely. */
    public boolean isSafe()       { return "SAFE".equals(verdict); }

    // =========================================================
    // toString
    // =========================================================

    @Override
    public String toString() {
        return String.format("RiskScore[score=%d, verdict=%s, reason=%s]",
                score, verdict, topReason);
    }
}

