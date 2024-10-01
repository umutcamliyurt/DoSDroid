package com.nemesis.dosdroid;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST_CODE = 1; // Unique request code for file picker
    private static final int LOG_INTERVAL = 10000; // 10 seconds for logging interval
    private Button startButton;
    private Button stopButton;
    private Button importButton;
    private static Context context;
    private Uri selectedFileUri;
    private TextView logTextView;
    private Handler logHandler = new Handler(Looper.getMainLooper()); // For updating UI from background threads
    private Runnable logRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        importButton = findViewById(R.id.import_button);
        logTextView = findViewById(R.id.log_text_view);

        // Initialize HttpFloodAttack with this activity
        HttpFloodAttack.initialize(this);

        // Start button click listener
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!HttpFloodAttack.targetUrls.isEmpty()) {
                    startAttackService();
                    startLogUpdates(); // Start logging every 10 seconds
                } else {
                    Toast.makeText(MainActivity.this, "Please import URLs from a file first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Stop button click listener
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAttackService();
                stopLogUpdates(); // Stop periodic logging
            }
        });

        // Import button click listener (File Picker)
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(); // Open the file picker to select URLs
            }
        });
    }

    // Method to start the attack service
    private void startAttackService() {
        Intent serviceIntent = new Intent(this, AttackService.class);
        startService(serviceIntent);
    }

    // Method to stop the attack service
    private void stopAttackService() {
        Intent serviceIntent = new Intent(this, AttackService.class);
        stopService(serviceIntent);
        updateLog("Attack stopped."); // Log the stop action
    }

    // Method to open the file picker to select URLs
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allows selection of any file type
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE); // Start the file picker activity
    }

    // Handle file picker result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                String filePath = getFilePathFromUri(selectedFileUri);
                if (filePath != null) {
                    HttpFloodAttack.importUrlsFromFile(filePath); // Import URLs from the file
                    Toast.makeText(MainActivity.this, "URLs imported successfully", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Utility function to get the file path from Uri
    private String getFilePathFromUri(Uri uri) {
        String filePath = null;

        // Check if the URI scheme is "content" or "file"
        if (uri.getScheme().equals("content")) {
            // Query the content resolver to get the file path
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) {
                    String fileName = cursor.getString(index);
                    filePath = uri.toString(); // Use the URI itself
                }
                cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
            filePath = uri.getPath();
        }

        return filePath;
    }

    // Method to start logging every 10 seconds
    private void startLogUpdates() {
        logRunnable = new Runnable() {
            @Override
            public void run() {
                // Clear the logTextView before updating it
                logTextView.setText("");

                // Get the number of successful and failed requests
                int successCount = HttpFloodAttack.getSuccessfulRequests();
                int failureCount = HttpFloodAttack.getFailedRequests();

                // Update the logTextView with new data
                String logMessage = "Successful requests: " + successCount + "\nFailed requests: " + failureCount;
                logTextView.append(logMessage + "\n");

                // Schedule the next log update
                logHandler.postDelayed(this, LOG_INTERVAL);
            }
        };

        logHandler.post(logRunnable); // Start logging
    }

    // Method to stop periodic log updates
    private void stopLogUpdates() {
        if (logRunnable != null) {
            logHandler.removeCallbacks(logRunnable); // Stop future log updates
        }
    }

    // Method to update logTextView (called from HttpFloodAttack)
    public void updateLog(final String logMessage) {
        logHandler.post(new Runnable() {
            @Override
            public void run() {
                logTextView.append(logMessage + "\n");  // Append new log
                logTextView.scrollTo(0, logTextView.getBottom());  // Scroll to bottom as new logs come in
            }
        });
    }

    public static Context getContext() {
        return context;
    }
}
