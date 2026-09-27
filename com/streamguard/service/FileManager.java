package com.streamguard.service;

import com.streamguard.model.Transaction;

import java.io.*;
import java.util.ArrayList;

/**
 * FileManager.java
 * Handles reading and writing transaction history to file.
 * Demonstrates: File Handling (FileWriter, BufferedWriter, FileReader, BufferedReader)
 */
public class FileManager {

    private static final String FILE_PATH = "TransactionHistory.txt";

    /**
     * Appends a transaction record to the history file.
     */
    public void saveTransaction(Transaction txn) {
        try (FileWriter fw = new FileWriter(FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw)) {

            bw.write("--------------------------------------------------");
            bw.newLine();
            bw.write(txn.toString());
            bw.newLine();

        } catch (IOException e) {
            System.out.println("  [FILE ERROR] Could not save transaction: " + e.getMessage());
        }
    }

    /**
     * Reads and returns all lines from the transaction history file.
     */
    public ArrayList<String> readHistory() {
        ArrayList<String> lines = new ArrayList<>();

        try (FileReader fr = new FileReader(FILE_PATH);
             BufferedReader br = new BufferedReader(fr)) {

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

        } catch (FileNotFoundException e) {
            System.out.println("  [INFO] No transaction history found yet.");
        } catch (IOException e) {
            System.out.println("  [FILE ERROR] Could not read history: " + e.getMessage());
        }

        return lines;
    }

    /**
     * Prints entire transaction history to console.
     */
    public void printHistory() {
        ArrayList<String> lines = readHistory();
        if (lines.isEmpty()) {
            System.out.println("  No transaction history available.");
            return;
        }
        System.out.println("\n  ======= TRANSACTION HISTORY =======");
        for (String line : lines) {
            System.out.println("  " + line);
        }
        System.out.println("  ===================================\n");
    }
}
