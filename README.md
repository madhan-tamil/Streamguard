# StreamGuard – Real-Time Financial Fraud Detection System
**MySQL + JDBC Edition**

Chennai Institute of Technology | Department of Cyber Security  
**Team:** MADHAN T (210425149028) · AVINASH N T (210425149006)

---

## Overview

StreamGuard is a Java-based rule-driven financial fraud detection system.  
Every transaction is checked against four fraud rules in real time. If any rule fires, the transaction is **BLOCKED (FRAUD)**; otherwise it is **APPROVED (SAFE)**.

This edition adds **MySQL database connectivity via JDBC**, replacing the previous CSV and flat-file storage with a proper relational database.

---

## Java Concepts Demonstrated

| Concept | Location |
|---------|----------|
| OOP – Encapsulation | `BankAccount`, `Transaction` |
| OOP – Inheritance | `SavingsAccount`, `CurrentAccount` extend `BankAccount` |
| OOP – Abstraction | `BankAccount` (abstract), `FraudRule` (abstract) |
| OOP – Polymorphism | `check()` overridden in each rule class |
| Collections Framework | `HashMap<Integer, BankAccount>` in `AccountManager` |
| Collections Framework | `ArrayList<Transaction>` in `TransactionService` |
| Exception Handling | try/catch in all service and DB classes |
| Multithreading | Transaction runs in separate `Thread` in `Main` |
| **Database Connectivity** | **JDBC + MySQL in `DatabaseManager`, `DatabaseConnection`** |
| File Handling | Kept as `DatasetManager` stub (now delegates to MySQL) |

---

## Fraud Rules

| Rule | Description |
|------|-------------|
| `AmountRule` | Flags transactions above **Rs. 50,000** |
| `LocationRule` | Flags when transaction city ≠ registered city |
| `TimeRule` | Flags transactions between **11:00 PM – 5:00 AM** |
| `BalanceRule` | Flags amount greater than account balance |

---

## Project Structure

```
StreamGuard/
├── README.md
├── output.db                            ← Live transaction log (auto-generated)
├── StreamGuard_Dataset.csv              ← Optional reference (not used at runtime)
├── TransactionHistory.txt               ← Optional legacy file (not used at runtime)
├── lib/
│   └── mysql-connector-j-9.0.0.jar     ← MySQL JDBC Driver (required)
├── database/
│   └── streamguard.sql                 ← Run this in MySQL Workbench first!
├── out/                                 ← Compiled .class files go here
└── com/
    └── streamguard/
        ├── database/
        │   └── DatabaseConnection.java   ← JDBC connection (change credentials here)
        ├── main/
        │   └── Main.java                 ← Console entry point
        ├── manager/
        │   └── AccountManager.java       ← HashMap registry, DB-loaded accounts
        ├── model/
        │   ├── BankAccount.java          ← Abstract base class
        │   ├── CurrentAccount.java       ← Overdraft account
        │   ├── SavingsAccount.java       ← Minimum balance account
        │   └── Transaction.java          ← Transaction model
        ├── rules/
        │   ├── FraudCheck.java           ← Interface
        │   ├── FraudRule.java            ← Abstract base rule
        │   ├── AmountRule.java
        │   ├── BalanceRule.java
        │   ├── LocationRule.java
        │   └── TimeRule.java
        ├── service/
        │   ├── DatabaseManager.java      ← All MySQL operations (replaces CSV + TXT)
        │   ├── DatasetManager.java       ← Stub (delegates to DatabaseManager)
        │   ├── FraudChecker.java         ← Runs all fraud rules
        │   ├── OutputFileLogger.java     ← Writes tabular logs to output.db
        │   └── TransactionService.java   ← Transaction flow coordinator
        ├── web/
        │   └── WebServer.java            ← HTTP server + all API endpoints
        └── src/
            └── main/
                └── webapp/
                    └── index.html        ← Web dashboard UI
```

---

## MySQL Database Setup

### Prerequisites
- MySQL Server 8.x installed and running
- MySQL Workbench (optional but recommended)
- JDK 17 or later

### Step 1 – Run the SQL Script

Open **MySQL Workbench**, then:

1. `File → Open SQL Script → database/streamguard.sql`
2. Click **Execute** (⚡ lightning bolt button)

Or from command line:
```bash
mysql -u root -p < database/streamguard.sql
```

This creates:
- Database: `streamguard`
- Table: `accounts` (5 demo accounts pre-loaded)
- Table: `transactions` (10 sample transactions for dashboard testing)

### Step 2 – Configure Database Credentials

Open [`com/streamguard/database/DatabaseConnection.java`](com/streamguard/database/DatabaseConnection.java) and update:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/streamguard"
                                     + "?useSSL=false&allowPublicKeyRetrieval=true"
                                     + "&serverTimezone=Asia/Kolkata";
private static final String USER     = "root";
private static final String PASSWORD = "your_password";   // ← change this
```

### Step 3 – Verify Setup (MySQL Workbench / CLI)

```sql
USE streamguard;

-- Check accounts
SELECT * FROM accounts;

-- Check all transactions
SELECT * FROM transactions;

-- Summary counts
SELECT
    COUNT(*) AS total,
    SUM(CASE WHEN status='SAFE'  THEN 1 ELSE 0 END) AS safe,
    SUM(CASE WHEN status='FRAUD' THEN 1 ELSE 0 END) AS fraud
FROM transactions;

-- Fraud reasons breakdown
SELECT fraud_reason, COUNT(*) AS count
FROM transactions
WHERE status = 'FRAUD'
GROUP BY fraud_reason;

-- All fraud (blocked) transactions
SELECT * FROM transactions WHERE status = 'FRAUD' ORDER BY timestamp DESC;
```

---

## How to Compile & Run

### Compile (PowerShell – run from `D:\StreamGuard`)

```powershell
javac -d out -cp "lib\mysql-connector-j-9.0.0.jar" (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

### Run Console Version (Main.java)

```powershell
java -cp "out;lib\mysql-connector-j-9.0.0.jar" com.streamguard.main.Main
```

### Run Web Dashboard (WebServer.java)

```powershell
java -cp "out;lib\mysql-connector-j-9.0.0.jar" com.streamguard.web.WebServer
```

Then open: **http://localhost:8080**

> **Troubleshooting: `java.net.BindException: Address already in use: bind`**  
> If you see this error when running the Web Server, it means another instance is already running on port 8080. You must stop it first:
> - **PowerShell**: Run `Stop-Process -Name java -Force -ErrorAction SilentlyContinue`
> - Or close the terminal window where it is currently running.

---

## Default Demo Accounts (in MySQL and HashMap)

| ID | Holder | Type | Balance | City |
|----|--------|------|---------|------|
| 1001 | MADHAN T | Savings | Rs. 75,000 | Chennai |
| 1002 | AVINASH N T | Savings | Rs. 50,000 | Chennai |
| 1003 | RAJESH KUMAR | Current | Rs. 1,20,000 | Coimbatore |
| 1004 | PRIYA S | Savings | Rs. 30,000 | Bangalore |
| 1005 | SURESH V | Current | Rs. 2,00,000 | Mumbai |

---

## Sample Test Cases (Account 1001 – MADHAN T, Chennai)

| Amount | Location | Expected Result | Rule Triggered |
|--------|----------|-----------------|----------------|
| Rs. 25,000 | Chennai | ✔ SAFE | None |
| Rs. 60,000 | Chennai | ✘ FRAUD | AmountRule (> Rs. 50,000) |
| Rs. 10,000 | Mumbai | ✘ FRAUD | LocationRule (city mismatch) |
| Rs. 10,000 | Chennai | ✘ FRAUD | TimeRule (if run at midnight) |
| Rs. 80,000 | Chennai | ✘ FRAUD | BalanceRule (> balance) |

---

## How a Transaction Flows Through the System

```
User Input (amount, location, account ID)
          ↓
  TransactionService.processTransaction()
          ↓
  DatabaseManager.getNextTxnId()     ← avoid duplicate IDs after restart
          ↓
  FraudChecker.runAllChecks()        ← runs all 4 rules
          ↓
   SAFE                  FRAUD
     ↓                     ↓
account.withdraw()    blockedTransactions.add()
     ↓
DatabaseManager.updateAccountBalance()   ← UPDATE accounts SET balance = ?
          ↓
DatabaseManager.saveTransaction()        ← INSERT INTO transactions ...
          ↓
  Result displayed to user
```

---

## API Endpoints (Web Server)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `http://localhost:8080/` | GET | Web dashboard UI |
| `/api/status` | GET | Server and DB status |
| `/api/dataset` | GET | Summary from MySQL (total/safe/fraud/reasons) |
| `/api/accounts` | GET | All accounts JSON |
| `/api/account?id=1001` | GET | Single account JSON |
| `/api/transaction` | POST | Process new transaction |
| `/api/history` | GET | All transactions from MySQL (DESC by time) |
| `/api/blocked` | GET | FRAUD transactions from MySQL |

---

## output.db – Live Transaction Log File

Every transaction processed through the web dashboard (or console) is **automatically appended** to `output.db` in the project root as a formatted ASCII table.

### Sample output.db contents

```
===========================================================================================================================================
| TXN ID | ACCOUNT ID | HOLDER NAME           | AMOUNT        | CITY       | STATUS | FRAUD REASON                        | TIMESTAMP          |
===========================================================================================================================================
| 1011   | 1001       | MADHAN T              | Rs. 25000.00  | Chennai    | SAFE   | -                                   | 2026-10-07 10:05:12 |
| 1012   | 1002       | AVINASH N T           | Rs. 60000.00  | Chennai    | FRAUD  | Amount exceeds Rs. 50,000 limit     | 2026-10-07 10:07:45 |
| 1013   | 1003       | RAJESH KUMAR          | Rs. 10000.00  | Mumbai     | FRAUD  | Transaction from different city     | 2026-10-07 10:09:31 |
===========================================================================================================================================
```

- **On startup**, existing transactions from MySQL are loaded into `output.db` automatically.
- **On each new transaction**, the record is appended live.
- **Managed by**: `com/streamguard/service/OutputFileLogger.java`

---

## How Java Connects to MySQL

```java
// 1. Load driver (once, in static block)
Class.forName("com.mysql.cj.jdbc.Driver");

// 2. Get connection
Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

// 3. PreparedStatement – prevents SQL injection
PreparedStatement ps = conn.prepareStatement(
    "INSERT INTO transactions (txn_id, account_id, amount, ...) VALUES (?, ?, ?, ...)"
);
ps.setInt(1, txn.getTxnId());
ps.setDouble(3, txn.getAmount());
ps.executeUpdate();

// 4. try-with-resources – auto-closes connection
try (Connection conn = DatabaseConnection.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // ...
}
```

---

## What Changed vs Original

| Feature | Before | After |
|---------|--------|-------|
| Dataset stats | Read `StreamGuard_Dataset.csv` | MySQL `SELECT COUNT(*)` queries |
| Transaction storage | Write to `TransactionHistory.txt` | `INSERT INTO transactions` |
| Transaction logging | Console output only | Synchronized ASCII table logging in `output.db` |
| Transaction history | Read `TransactionHistory.txt` | `SELECT * FROM transactions` |
| Blocked transactions | In-memory `ArrayList` (lost on restart) | MySQL `WHERE status='FRAUD'` |
| Account loading | Hardcoded `HashMap` | Loaded from MySQL `accounts` table |
| Account balance sync | Java object only | Java object + MySQL `UPDATE accounts` |
| Transaction ID | Restarted at 1001 each run | Continues from `MAX(txn_id) + 1` |

---

## Files Created / Modified / Removed

### Created (New)
- `com/streamguard/database/DatabaseConnection.java` – JDBC connection manager
- `com/streamguard/service/DatabaseManager.java` – All MySQL operations
- `com/streamguard/service/OutputFileLogger.java` – Writes tabular logs to `output.db`
- `database/streamguard.sql` – Database setup script
- `lib/mysql-connector-j-9.0.0.jar` – MySQL JDBC driver
- `output.db` – Live tabular transaction log file
- `README.md` – This file

### Modified
- `com/streamguard/main/Main.java` – DB init, accounts from MySQL, triggers `output.db` sync
- `com/streamguard/manager/AccountManager.java` – `addAccount()`, `loadDefaultAccounts()`, `getAllAccounts()`
- `com/streamguard/model/Transaction.java` – Added `setTimestampFromDb()`
- `com/streamguard/service/TransactionService.java` – Uses `DatabaseManager` and logs to `output.db`
- `com/streamguard/service/DatasetManager.java` – Converted to DB-delegating stub
- `com/streamguard/web/WebServer.java` – All API endpoints use MySQL, triggers `output.db` sync

### Removed (no longer used at runtime)
- `StreamGuard_Dataset.csv` – Still present as reference, not loaded at runtime
- `TransactionHistory.txt` – Still present as reference, not written or read
- `com/streamguard/service/FileManager.java` – Removed legacy text file handling

---

## Notes

- The **CSV file** and **TXT file** still exist in the project folder but are no longer used at runtime.
- If MySQL is **not available**, the application falls back to the hardcoded in-memory accounts and prints a clear error message instead of crashing.
- This project does **not** use Spring Boot, Hibernate, JPA, or any ORM. It uses plain Java + JDBC.
- The MySQL Connector/J JAR (`lib/mysql-connector-j-9.0.0.jar`) must be on the classpath when compiling and running.
