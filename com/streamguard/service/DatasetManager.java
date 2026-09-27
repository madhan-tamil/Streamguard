package com.streamguard.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DatasetManager {

    private static final String FILE_NAME = "StreamGuard_Dataset.csv";

    public void loadDataset() {

        int totalRecords = 0;
        int fraudRecords = 0;
        int safeRecords = 0;

        Map<String, Integer> fraudReasons = new HashMap<>();

        try (BufferedReader br =
                     new BufferedReader(new FileReader(FILE_NAME))) {

            // Skip the CSV header
            String header = br.readLine();

            if (header == null) {
                System.out.println("[ERROR] Dataset is empty.");
                return;
            }

            String line;

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",", -1);

                if (data.length >= 9) {

                    totalRecords++;

                    // Column 7 = Status
                    // Array index 6 because Java starts from 0.
                    String status = data[6].trim();

                    if (status.equalsIgnoreCase("FRAUD")) {

                        fraudRecords++;

                        // Column 8 = FraudReason
                        // Array index 7.
                        String reason = data[7].trim();

                        if (reason.isEmpty()) {
                            reason = "Unknown";
                        }

                        fraudReasons.put(
                                reason,
                                fraudReasons.getOrDefault(reason, 0) + 1
                        );

                    } else {
                        safeRecords++;
                    }
                }
            }

            System.out.println();
            System.out.println("==========================================");
            System.out.println("       STREAMGUARD DATASET SUMMARY");
            System.out.println("==========================================");
            System.out.println("Total Transactions : " + totalRecords);
            System.out.println("Safe Transactions  : " + safeRecords);
            System.out.println("Fraud Transactions : " + fraudRecords);
            System.out.println("==========================================");

            System.out.println();
            System.out.println("           FRAUD REASONS");
            System.out.println("==========================================");

            if (fraudReasons.isEmpty()) {

                System.out.println("No fraud records found.");

            } else {

                for (Map.Entry<String, Integer> entry :
                        fraudReasons.entrySet()) {

                    System.out.println(
                            entry.getKey() + " : " + entry.getValue()
                    );
                }
            }

            System.out.println("==========================================");
            System.out.println();

        } catch (IOException e) {

            System.out.println("[ERROR] Unable to read dataset.");
            System.out.println("Reason: " + e.getMessage());
        }
    }
}