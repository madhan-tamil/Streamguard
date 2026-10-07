-- ============================================================
-- streamguard.sql
-- MySQL setup script for StreamGuard Fraud Detection System
--
-- Run this file in MySQL Workbench:
--   File → Open SQL Script → streamguard.sql → Execute (lightning bolt)
--
-- Or from MySQL CLI:
--   mysql -u root -p < database/streamguard.sql
--
-- Project    : StreamGuard
-- Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
-- Department : CSBS, Chennai Institute of Technology
-- ============================================================


-- ============================================================
-- STEP 1: CREATE DATABASE
-- ============================================================

CREATE DATABASE IF NOT EXISTS streamguard;
USE streamguard;


-- ============================================================
-- STEP 2: CREATE ACCOUNTS TABLE
-- ============================================================

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    account_id      INT           PRIMARY KEY,
    account_holder  VARCHAR(100)  NOT NULL,
    account_type    VARCHAR(30)   NOT NULL,
    balance         DECIMAL(12,2) NOT NULL,
    registered_city VARCHAR(100)  NOT NULL
);


-- ============================================================
-- STEP 3: INSERT DEMO ACCOUNTS
-- (Same five accounts as in AccountManager.java)
-- ============================================================

INSERT INTO accounts (account_id, account_holder, account_type, balance, registered_city)
VALUES
    (1001, 'MADHAN T',     'SAVINGS',  75000.00,  'Chennai'),
    (1002, 'AVINASH N T',  'SAVINGS',  50000.00,  'Chennai'),
    (1003, 'RAJESH KUMAR', 'CURRENT',  120000.00, 'Coimbatore'),
    (1004, 'PRIYA S',      'SAVINGS',  30000.00,  'Bangalore'),
    (1005, 'SURESH V',     'CURRENT',  200000.00, 'Mumbai');


-- ============================================================
-- STEP 4: CREATE TRANSACTIONS TABLE
-- ============================================================

CREATE TABLE transactions (
    txn_id         INT              PRIMARY KEY,
    account_id     INT              NOT NULL,
    amount         DECIMAL(12,2)   NOT NULL,
    location       VARCHAR(100)    NOT NULL,
    timestamp      DATETIME        NOT NULL,
    status         VARCHAR(20)     NOT NULL,
    fraud_reason   VARCHAR(255)    DEFAULT NULL,
    original_class VARCHAR(50)     DEFAULT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);


-- ============================================================
-- STEP 5: INSERT SAMPLE TRANSACTIONS (for dashboard testing)
-- ============================================================

INSERT INTO transactions
    (txn_id, account_id, amount, location, timestamp, status, fraud_reason)
VALUES
    (1001, 1001, 25000.00,  'Chennai',    '2026-10-01 10:30:00', 'SAFE',  NULL),
    (1002, 1001, 60000.00,  'Chennai',    '2026-10-01 11:00:00', 'FRAUD', 'Amount exceeded limit'),
    (1003, 1002, 10000.00,  'Mumbai',     '2026-10-02 09:00:00', 'FRAUD', 'Location mismatch'),
    (1004, 1003, 15000.00,  'Coimbatore', '2026-10-02 23:30:00', 'FRAUD', 'Suspicious transaction time'),
    (1005, 1004, 35000.00,  'Bangalore',  '2026-10-03 14:00:00', 'FRAUD', 'Insufficient balance'),
    (1006, 1005, 50000.00,  'Mumbai',     '2026-10-03 15:00:00', 'SAFE',  NULL),
    (1007, 1001, 20000.00,  'Chennai',    '2026-10-04 10:00:00', 'SAFE',  NULL),
    (1008, 1002, 55000.00,  'Chennai',    '2026-10-04 11:00:00', 'FRAUD', 'Amount exceeded limit'),
    (1009, 1003, 30000.00,  'Coimbatore', '2026-10-05 12:00:00', 'SAFE',  NULL),
    (1010, 1004, 5000.00,   'Delhi',      '2026-10-05 13:00:00', 'FRAUD', 'Location mismatch');


-- ============================================================
-- STEP 6: VERIFICATION QUERIES
-- Run these to confirm setup is correct:
-- ============================================================

-- Show all accounts:
SELECT * FROM accounts;

-- Show all transactions:
SELECT * FROM transactions;

-- Show total, safe, fraud counts:
SELECT
    COUNT(*)                              AS total_transactions,
    SUM(CASE WHEN status='SAFE'  THEN 1 ELSE 0 END) AS safe_transactions,
    SUM(CASE WHEN status='FRAUD' THEN 1 ELSE 0 END) AS fraud_transactions
FROM transactions;

-- Show fraud reasons:
SELECT fraud_reason, COUNT(*) AS count
FROM transactions
WHERE status = 'FRAUD'
GROUP BY fraud_reason;

-- Show only FRAUD transactions:
SELECT * FROM transactions WHERE status = 'FRAUD' ORDER BY timestamp DESC;

-- ============================================================
-- END OF streamguard.sql
-- ============================================================
