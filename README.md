# StreamGuard – Real-Time Financial Fraud Detection System
**Behavioral Anomaly Detection & Statistical Profiling Edition (MySQL + JDBC)**

Chennai Institute of Technology | Department of Cyber Security  
**Team:** MADHAN T (210425149028) · AVINASH N T (210425149006)

---

## Overview

StreamGuard is an enterprise-grade financial fraud detection engine implemented in Java with native MySQL JDBC persistence.

Based on academic evaluation, the outdated static rule-based system (fixed thresholds and binary if-else rules) has been replaced with a **Behavioral Anomaly Detection & Statistical Profiling Engine**.

### Why the Old Rule-Based Method was Replaced:
1. **Unrealistic Travel Blocks:** In rule-based systems, any swipe outside the user's home city was flagged as fraud, ignoring normal human travel.
2. **Arbitrary Amount Cutoffs:** Fixed limits (e.g. > ₹50,000) ignored personal wealth and spending habits (normal for corporate accounts, extreme for student accounts).
3. **Rigid Night Restrictions:** Blanket bans during late hours penalized night-shift workers.
4. **Vulnerability to Structuring:** Attackers could easily bypass rules by splitting transactions into multiple micro-payments (e.g. four ₹9,999 transactions).

### The New Method: Behavioral Anomaly Detection & Statistical Profiling
Instead of binary pass/fail rules, StreamGuard analyzes behavioral deviations and calculates a **Composite Risk Score (0–100)**:
* **Gaussian Z-Score Outlier Analysis ($Z = \frac{x - \mu}{\sigma}$):** Calculates standard deviations from the user's personal spending mean ($\mu$) and variance ($\sigma$).
* **Spatio-Temporal Velocity (Haversine Formula):** Computes physical distance and travel speed ($v = \frac{\Delta d}{\Delta t}$). Legitimate travel is permitted; speeds exceeding commercial aircraft limits (>850 km/h) are flagged as **Impossible Travel (Cloned Card)**.
* **Sliding-Window Burst Velocity:** Detects automated bot attacks, rapid card testing, and structured split transactions within a 10-minute window.
* **Graduated Risk Scoring:** Transactions are classified into **SAFE (0–30)**, **SUSPICIOUS (31–60)**, and **FRAUD (61–100)** with detailed scientific justifications.

---

## Technical Architecture & Core Algorithms

### 1. Statistical Amount Profiling (Z-Score Model)
Each customer profile maintains historical transaction parameters:
$$\mu = \frac{1}{N} \sum_{i=1}^{N} x_i, \quad \sigma = \sqrt{\frac{1}{N} \sum_{i=1}^{N} (x_i - \mu)^2}$$
$$Z = \frac{\text{Amount} - \mu}{\sigma}$$
* $Z \le 1.8\sigma$: Normal customer spending pattern (0 points)
* $1.8\sigma < Z \le 2.5\sigma$: Mild deviation (+15 points)
* $2.5\sigma < Z \le 3.5\sigma$: Moderate statistical anomaly (+35 points)
* $Z > 3.5\sigma$: Extreme statistical outlier ($p < 0.0002$, +55 points)

### 2. Spatio-Temporal Travel Feasibility (Haversine Formula)
Distance $d$ between previous location $(\phi_1, \lambda_1)$ and current location $(\phi_2, \lambda_2)$:
$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)$$
$$d = 2R \cdot \text{atan2}(\sqrt{a}, \sqrt{1-a}) \quad (R = 6371\text{ km})$$
$$\text{Velocity } v = \frac{d}{\Delta t \text{ (hours)}}$$
* $v \le 150\text{ km/h}$: Normal ground transit (car/train) ➔ Approved (0 points)
* $150 < v \le 850\text{ km/h}$: Feasible air travel with realistic layover ➔ Approved (0–15 points)
* $v > 850\text{ km/h}$: Impossible physical travel velocity ➔ **FRAUD / Cloned Card** (+65 points)

---

## Java Concepts Demonstrated

| Concept | Location |
|---------|----------|
| OOP – Encapsulation | `BankAccount`, `Transaction`, `RiskScore` |
| OOP – Inheritance | `SavingsAccount`, `CurrentAccount` extend `BankAccount` |
| OOP – Abstraction | `BankAccount` (abstract class) |
| Collections Framework | `HashMap`, `ArrayList`, sliding-window `List<LocalDateTime>` |
| Mathematical Modeling | Haversine spherical geometry, standard deviation, Z-score computation |
| Exception Handling | Robust `try-catch-finally` with automatic fallback mechanisms |
| Multithreading | Threaded transaction processing and concurrent web request handling |
| Database Connectivity | Native JDBC (PreparedStatements, Connection management, ResultSets) |

---

## Project Structure

```
StreamGuard/
├── README.md
├── output.db                                ← Live tabular ASCII transaction log
├── lib/
│   └── mysql-connector-j-9.0.0.jar          ← MySQL JDBC Driver
├── database/
│   └── streamguard.sql                      ← MySQL schema & initial seed data
├── out/                                     ← Compiled bytecode (.class files)
└── com/
    └── streamguard/
        ├── anomaly/                         ← NEW: Behavioral Anomaly Detection Engine
        │   ├── AnomalyDetectionEngine.java  ← Composite risk engine & burst tracker
        │   ├── GeoLocationService.java      ← Indian city geocodes & Haversine formula
        │   └── UserProfileStatistics.java   ← Gaussian dynamic baseline (μ, σ, Z-score)
        ├── database/
        │   └── DatabaseConnection.java      ← JDBC connection manager
        ├── main/
        │   └── Main.java                    ← Interactive console interface
        ├── manager/
        │   └── AccountManager.java          ← Account registry & cache
        ├── model/
        │   ├── BankAccount.java             ← Abstract account model
        │   ├── CurrentAccount.java          ← Current account with overdraft
        │   ├── SavingsAccount.java          ← Savings account with min balance
        │   ├── Transaction.java             ← Model with risk score & verdict
        │   └── RiskScore.java               ← Score (0-100) and classification model
        ├── service/
        │   ├── DatabaseManager.java         ← MySQL queries & profile data loader
        │   ├── OutputFileLogger.java        ← Live sync to output.db
        │   └── TransactionService.java      ← Transaction orchestrator
        ├── web/
        │   └── WebServer.java               ← Embedded HTTP server & JSON APIs
        └── src/main/webapp/
            └── index.html                   ← Dashboard with animated risk bar & pills
```

---

## Step-by-Step MySQL / MariaDB Server Setup Guide

Follow these steps to connect MySQL/MariaDB to the StreamGuard project:

### Step 1: Start Database Server
Open your terminal and ensure the database service is running:

**Fedora / RHEL / MariaDB:**
```bash
sudo systemctl start mariadb
sudo systemctl status mariadb
```

**Ubuntu / Debian (MySQL):**
```bash
sudo systemctl start mysql
sudo systemctl status mysql
```

> **Tip:** To enable MariaDB to start automatically on every boot:
> ```bash
> sudo systemctl enable mariadb
> ```

---

### Step 2: Initialize Database and Tables (Run ONCE)
Import `database/streamguard.sql` into MariaDB / MySQL:

```bash
cd /home/madhan/Documents/Streamguard
sudo mariadb -u root -pStreamGuard07 < database/streamguard.sql
```
*(If using standard MySQL, replace `mariadb` with `mysql`)*.

> ⚠️ **IMPORTANT WARNING:**  
> `streamguard.sql` contains `DROP TABLE IF EXISTS`. It resets the database and seeds the 10 initial sample records. **Run this command ONLY ONCE during initial setup.**  
> **Do NOT run this command to inspect transactions**, or it will erase any new transactions you submitted!

---

### Step 3: Check Live Database Records (Without Resetting)

Use these commands anytime to view real-time changes reflected from transactions:

#### A. View Latest Transactions (Live Stream)
```bash
sudo mariadb -u root -pStreamGuard07 -e "USE streamguard; SELECT txn_id, account_id, amount, location, timestamp, status, risk_score, verdict FROM transactions ORDER BY txn_id DESC LIMIT 5;"
```

#### B. Check Live Account Balances
```bash
sudo mariadb -u root -pStreamGuard07 -e "USE streamguard; SELECT account_id, account_holder, account_type, balance, registered_city FROM accounts;"
```

#### C. Open Interactive MariaDB Shell
```bash
sudo mariadb -u root -pStreamGuard07
```
Once inside the prompt (`MariaDB [(none)]>`):
```sql
USE streamguard;
SELECT * FROM transactions ORDER BY txn_id DESC LIMIT 10;
SELECT * FROM accounts;
EXIT;
```

---

### Step 4: Configure Credentials in Code
Open [`com/streamguard/database/DatabaseConnection.java`](com/streamguard/database/DatabaseConnection.java):

```java
private static final String URL = "jdbc:mysql://localhost:3306/streamguard"
        + "?useSSL=false&allowPublicKeyRetrieval=true"
        + "&serverTimezone=Asia/Kolkata";
private static final String USER = "root";
private static final String PASSWORD = "StreamGuard07"; // <-- Match your local MySQL password
```
*If your database root password differs, update `PASSWORD` in this file.*

---

## How to Compile & Run

### 1. Compile All Files
Compile all Java sources with the included MySQL JDBC connector:

**Linux / macOS (Bash):**
```bash
cd /home/madhan/Documents/Streamguard

javac -cp "lib/mysql-connector-j-9.0.0.jar" -d out \
    com/streamguard/model/*.java \
    com/streamguard/anomaly/*.java \
    com/streamguard/rules/*.java \
    com/streamguard/manager/*.java \
    com/streamguard/service/*.java \
    com/streamguard/database/*.java \
    com/streamguard/web/WebServer.java \
    com/streamguard/main/Main.java
```

**Windows (PowerShell):**
```powershell
javac -cp "lib\mysql-connector-j-9.0.0.jar" -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

---

### 2. Run the Web Dashboard (Recommended for Demo)
```bash
java -cp "out:lib/mysql-connector-j-9.0.0.jar" com.streamguard.web.WebServer
```
*(On Windows PowerShell, replace `:` with `;`: `java -cp "out;lib\mysql-connector-j-9.0.0.jar" com.streamguard.web.WebServer`)*

* Open your browser and navigate to: **`http://localhost:8080`**
* The server status indicator will show: **● ONLINE** and **MySQL Connected**.

---

### 3. Run the Console CLI Version
```bash
java -cp "out:lib/mysql-connector-j-9.0.0.jar" com.streamguard.main.Main
```

---

## Academic Demonstration Scenarios (Show Your Professor)

Run these scenarios during review to demonstrate the superiority of the Anomaly Detection engine:

| Scenario | Input | Engine Result | Scientific Justification |
|---|---|---|---|
| **1. Normal Spending** | Account `1001`<br>₹5,000 in `Chennai` | ✅ **APPROVED (SAFE)**<br>Risk Score: `0/100` | Within normal baseline ($\mu = ₹8,000, \sigma = ₹2,500$). $Z = 0.4\sigma$. |
| **2. Feasible Human Travel** | Account `1001`<br>₹4,000 in `Bangalore` | ✅ **APPROVED (SAFE)**<br>Risk Score: `0/100` | Haversine distance = 290 km, time elapsed = 5 hrs ➔ Velocity = 58 km/h. Feasible highway transit. |
| **3. Impossible Travel (Cloned Card)** | Account `1001`<br>₹5,000 in `Delhi` *(immediately after)* | 🚫 **BLOCKED (FRAUD)**<br>Risk Score: `65/100` | Distance = 1,740 km, time < 2 mins ➔ Velocity > 50,000 km/h (> 850 km/h commercial aircraft limit). |
| **4. Statistical Spending Outlier** | Account `1004 (Priya S)`<br>₹45,000 in `Bangalore` | 🚫 **BLOCKED (FRAUD)**<br>Risk Score: `55/100` | Account average is ₹3,500. A ₹45,000 transaction is $>34\sigma$ standard deviations away (extreme outlier). |
| **5. High-Frequency Burst Testing** | Any Account<br>5 transactions in < 2 mins | ⚠️ **SUSPICIOUS / FRAUD**<br>Risk Score: `45–70/100` | Sliding-window burst detector flags automated card testing / bot activity. |

---

## Default Demo Accounts

| ID | Holder | Type | Initial Balance | Registered City | Spending Persona |
|:---:|:---|:---:|:---:|:---:|:---|
| **1001** | MADHAN T | Savings | ₹75,000 | Chennai | Regular Salaried ($\mu = ₹8,000$) |
| **1002** | AVINASH N T | Savings | ₹50,000 | Chennai | Moderate Personal ($\mu = ₹6,000$) |
| **1003** | RAJESH KUMAR | Current | ₹1,20,000 | Coimbatore | Commercial Business ($\mu = ₹25,000$) |
| **1004** | PRIYA S | Savings | ₹30,000 | Bangalore | Student / Entry-Level ($\mu = ₹3,500$) |
| **1005** | SURESH V | Current | ₹2,00,000 | Mumbai | High-Net-Worth Corporate ($\mu = ₹50,000$) |
