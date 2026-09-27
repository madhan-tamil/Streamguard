============================================================
  STREAMGUARD – Real-Time Financial Fraud Detection
  Chennai Institute of Technology | Dept. of Cyber Security
  Project Review
============================================================

TEAM MEMBERS:
  1. MADHAN T      (Reg No: 210425149028)
  2. AVINASH N T   (Reg No: 210425149006)

------------------------------------------------------------
PROJECT OVERVIEW
------------------------------------------------------------

StreamGuard is a Java-based financial fraud detection system that
checks transactions in real time using a rule-driven engine.
The application evaluates every transaction against a set of fraud
rules such as excessive amount, location mismatch, unusual time,
and insufficient balance. If a transaction violates one or more
rules, it is blocked and marked as FRAUD.

This project demonstrates key Java concepts including:
- Object-Oriented Programming (OOP)
- Encapsulation, Abstraction, Inheritance, Polymorphism
- Collections Framework
- Exception Handling
- File Handling
- Multithreading
- Rule-based decision making

------------------------------------------------------------
OBJECTIVE
------------------------------------------------------------

The objective of StreamGuard is to simulate a banking fraud guard
that protects customer accounts by identifying risky transactions
before money leaves the account. The system is designed for:
- real-time transaction evaluation
- blocked transaction tracking
- dataset-based fraud analysis
- both console and web-based interfaces

------------------------------------------------------------
FEATURES
------------------------------------------------------------

1. Transaction validation against fraud rules
2. Balance-based fraud detection
3. Amount-based fraud detection
4. Location mismatch detection
5. Time-based fraud detection (midnight risk window)
6. Transaction history storage in text file
7. Dashboard summary from CSV dataset
8. Browser-based UI served by embedded Java HTTP server
9. Multiple sample bank accounts and transactions

------------------------------------------------------------
RULES IMPLEMENTED
------------------------------------------------------------

1. AmountRule
   - flags transactions above Rs.50,000

2. LocationRule
   - flags transaction location different from registered city

3. TimeRule
   - flags transactions between 11:00 PM and 5:00 AM

4. BalanceRule
   - flags transactions where amount exceeds current balance

5. FraudChecker
   - runs all rules and returns SAFE or FRAUD

------------------------------------------------------------
PROJECT STRUCTURE
------------------------------------------------------------

StreamGuard/
├── README.txt
├── StreamGuard_Dataset.csv
├── TransactionHistory.txt
├── out/
├── com/
│   └── streamguard/
│       ├── GUI/
│       │   └── StreamGuardGUI.java
│       ├── main/
│       │   └── Main.java
│       ├── manager/
│       │   └── AccountManager.java
│       ├── model/
│       │   ├── BankAccount.java
│       │   ├── CurrentAccount.java
│       │   ├── SavingsAccount.java
│       │   └── Transaction.java
│       ├── rules/
│       │   ├── AmountRule.java
│       │   ├── BalanceRule.java
│       │   ├── FraudCheck.java
│       │   ├── FraudRule.java
│       │   ├── LocationRule.java
│       │   └── TimeRule.java
│       ├── service/
│       │   ├── DatasetManager.java
│       │   ├── FileManager.java
│       │   ├── FraudChecker.java
│       │   └── TransactionService.java
│       ├── src/
│       │   └── main/
│       │       └── webapp/
│       │           └── index.html
│       └── web/
│           └── WebServer.java

------------------------------------------------------------
KEY JAVA CONCEPTS DEMONSTRATED
------------------------------------------------------------

1. OOP - Encapsulation
   - private fields + getters/setters in BankAccount and Transaction

2. OOP - Inheritance
   - SavingsAccount extends BankAccount
   - CurrentAccount extends BankAccount
   - all rules extend FraudRule

3. OOP - Abstraction
   - BankAccount is abstract
   - FraudRule is abstract

4. OOP - Polymorphism
   - check() is overridden in each rule class

5. Collections Framework
   - HashMap<Integer, BankAccount> in AccountManager
   - ArrayList<Transaction> in TransactionService
   - ArrayList<FraudRule> in FraudChecker

6. Exception Handling
   - try/catch for invalid input, file issues, parse errors

7. File Handling
   - FileWriter, BufferedWriter, FileReader, BufferedReader

8. Multithreading
   - transaction processing runs in separate Thread in Main.java

9. Loops and Conditionals
   - menu loop, switch-case, if-else checks

10. Methods and Encapsulation
   - processTransaction(), runAllChecks(), saveTransaction(), listAccounts()

------------------------------------------------------------
REQUIREMENTS
------------------------------------------------------------

- Java Development Kit (JDK) installed
- Command Prompt / PowerShell / Terminal access
- Optional: IntelliJ IDEA or VS Code with Java support

------------------------------------------------------------
HOW TO RUN (CONSOLE VERSION)
------------------------------------------------------------

Using IntelliJ IDEA:
  1. Open IntelliJ IDEA
  2. Click File -> Open
  3. Select the StreamGuard folder
  4. Ensure the project is recognized as a Java project
  5. Run the class: com.streamguard.main.Main

Using Command Line (Windows PowerShell):
  1. Open PowerShell in the project root
  2. Compile:
       javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
  3. Run:
       java -cp out com.streamguard.main.Main

------------------------------------------------------------
HOW TO RUN (WEB VERSION)
------------------------------------------------------------

The project also includes a browser-based dashboard served by the
built-in Java HTTP server.

1. Open PowerShell in the StreamGuard folder
2. Compile the project:
       javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
3. Start the web server:
       java -cp out com.streamguard.web.WebServer
4. Open the browser and visit:
       http://localhost:8080

The web server exposes these endpoints:
- http://localhost:8080/                  -> main dashboard page
- http://localhost:8080/api/status       -> server status
- http://localhost:8080/api/dataset      -> dataset summary JSON
- http://localhost:8080/api/accounts     -> all accounts JSON
- http://localhost:8080/api/account?id=1001 -> single account JSON
- http://localhost:8080/api/transaction  -> POST new transaction
- http://localhost:8080/api/history      -> transaction history JSON
- http://localhost:8080/api/blocked      -> blocked transactions JSON

------------------------------------------------------------
DEFAULT SAMPLE ACCOUNTS
------------------------------------------------------------

The application preloads the following demo accounts:
- 1001: MADHAN T      | Savings | Balance: Rs.75,000 | Chennai
- 1002: AVINASH N T   | Savings | Balance: Rs.50,000 | Chennai
- 1003: RAJESH KUMAR  | Current | Balance: Rs.120,000 | Coimbatore
- 1004: PRIYA S       | Savings | Balance: Rs.30,000 | Bangalore
- 1005: SURESH V      | Current | Balance: Rs.200,000 | Mumbai

------------------------------------------------------------
SAMPLE TEST CASES
------------------------------------------------------------

Account 1001 (MADHAN T, Chennai, Balance: Rs.75,000)

+--------------------------+-----------+-----------+--------+
| Amount                   | Location  | Time      | Result |
+--------------------------+-----------+-----------+--------+
| Rs.25,000                | Chennai   | 10:00 AM  | SAFE   |
| Rs.60,000 (> limit)     | Chennai   | 10:00 AM  | FRAUD  |
| Rs.10,000               | Mumbai    | 10:00 AM  | FRAUD  |
| Rs.10,000               | Chennai   | Midnight  | FRAUD  |
| Rs.80,000 (> balance)   | Chennai   | 10:00 AM  | FRAUD  |
+--------------------------+-----------+-----------+--------+

These cases are meant to demonstrate how the fraud rules behave.

------------------------------------------------------------
DATASET DESCRIPTION
------------------------------------------------------------

The project includes a CSV dataset named StreamGuard_Dataset.csv.
It contains transaction records with columns such as:
- Transaction ID
- Account ID
- Amount
- Location
- Registered City
- Balance
- Status
- FraudReason
- Original Class

This dataset is used by DatasetManager to calculate fraud totals,
fraud reasons, and summary statistics.

------------------------------------------------------------
FILES OF INTEREST
------------------------------------------------------------

- com.streamguard.main.Main
  Main console entry point

- com.streamguard.web.WebServer
  Starts HTTP server and serves the dashboard

- com.streamguard.service.TransactionService
  Processes transactions and decides safety

- com.streamguard.service.FraudChecker
  Runs all fraud rules

- com.streamguard.manager.AccountManager
  Maintains the in-memory account registry

- com.streamguard.service.FileManager
  Reads and writes transaction history

- com.streamguard.src.main.webapp.index.html
  Web interface for dashboard and transaction checks

------------------------------------------------------------
NOTES
------------------------------------------------------------

- The console version uses a menu-based interface.
- The web version is the preferred way to view the dashboard.
- Transaction history is saved to TransactionHistory.txt.
- If the server is not running, the browser page will not load.

------------------------------------------------------------
END
------------------------------------------------------------
