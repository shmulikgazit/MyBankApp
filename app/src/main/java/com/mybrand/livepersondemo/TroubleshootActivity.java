package com.mybrand.livepersondemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.liveperson.infra.auth.LPAuthenticationParams;
import com.liveperson.infra.auth.LPAuthenticationType;
import com.liveperson.infra.callbacks.InitLivePersonCallBack;
import com.liveperson.infra.InitLivePersonProperties;
import com.liveperson.infra.MonitoringInitParams;
import com.liveperson.infra.ICallback;
import com.liveperson.infra.log.LogLevel;
import com.liveperson.messaging.sdk.api.LivePerson;

import java.util.HashSet;
import java.util.List;

public class TroubleshootActivity extends AppCompatActivity {

    private TextView logView;
    private ActivityResultLauncher<String> saveLauncher;
    private String pendingSaveText;
    private Button testBtn;
    private Button saveBtn;
    private volatile boolean isTestRunning = false;
    private volatile boolean isSaving = false;

    // The LP SDK's internal history is capped (~100 lines). To capture more,
    // we periodically sample snapshots and append only new lines into our own TestLogStore.
    private final android.os.Handler sdkLogPollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final HashSet<String> seenSdkLogLines = new HashSet<>();
    private volatile boolean sdkPolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshoot);

        setTitle("Troubleshoot");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        logView = findViewById(R.id.tv_test_log);
        testBtn = findViewById(R.id.btn_run_test);
        saveBtn = findViewById(R.id.btn_save_diagnostics);
        refreshLogView();
        updateButtons();

        saveLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
            if (uri == null) return;
            isSaving = true;
            updateButtons();
            try {
                byte[] bytes = (pendingSaveText == null ? "" : pendingSaveText).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os != null) os.write(bytes);
                }
                Toast.makeText(this, "Saved diagnostics file", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                pendingSaveText = null;
                isSaving = false;
                updateButtons();
            }
        });

        saveBtn.setOnClickListener(v -> {
            if (isTestRunning) {
                Toast.makeText(this, "Please wait for the test to finish, then save.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isSaving) return;
            isSaving = true;
            updateButtons();
            pendingSaveText = TestLogStore.readAll(this);
            saveLauncher.launch("liveperson-test-log.txt");
        });

        testBtn.setOnClickListener(v -> {
            if (isSaving) return;
            if (isTestRunning) return;
            runTest();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshLogView() {
        String text = TestLogStore.readAll(this);
        if (text.isEmpty()) {
            logView.setText("(No test log yet. Tap 'Test application' to run.)");
        } else {
            logView.setText(text);
        }
    }

    private void runTest() {
        isTestRunning = true;
        updateButtons();

        // Reset current test log (keep it small and focused per run)
        TestLogStore.clear(this);
        TestLogStore.append(this, "INFO", "=== Test started ===");
        TestLogStore.append(this, "INFO", "SDK version: " + LivePerson.getSDKVersion());
        TestLogStore.append(this, "INFO", "Diagnostics: " + Diagnostics.collect(this));

        // Configure and clear LivePerson in-memory log history so we can export a clean SDK snapshot later.
        try {
            // Use VERBOSE during the test to maximize chances of capturing retry/connectivity details
            // (still masked, and still only stored in-memory unless the user exports).
            LivePerson.Logging.setSDKLoggingLevel(LogLevel.VERBOSE);
            LivePerson.Logging.setDataMaskingEnabled(true);
            LivePerson.Logging.clearHistory();
            TestLogStore.append(this, "INFO", "LivePerson.Logging configured: level=VERBOSE, masking=ON, history cleared");
        } catch (Throwable t) {
            TestLogStore.append(this, "WARN", "LivePerson.Logging not available: " + t.getClass().getSimpleName());
        }

        refreshLogView();

        String brandId = getSharedPreferences("lp_demo", MODE_PRIVATE)
                .getString("brand_id", MainActivity.BRAND_ID);
        String appInstallId = getSharedPreferences("lp_demo", MODE_PRIVATE)
                .getString("app_install_id", MainActivity.APP_INSTALL_ID);

        TestLogStore.append(this, "INFO", "Using brandId=" + brandId + ", appInstallId=" + appInstallId);

        Toast.makeText(this, "Running test…", Toast.LENGTH_SHORT).show();

        // Run DNS checks sequentially so the log is deterministic.
        new Thread(() -> {
            try {
                dnsCheckBlocking("adminlogin.liveperson.net");
                dnsCheckBlocking("lptag.liveperson.net");
                dnsCheckBlocking("lpcdn.lpsnmedia.net");
                dnsCheckBlocking("lo.idp.liveperson.net");
                // Monitoring host varies by region; we don't hardcode it here.

                // Init (critical junction #1) – do this before any other SDK calls.
                continueAfterActiveConversationCheck(brandId, appInstallId);
            } catch (Throwable t) {
                TestLogStore.append(this, "ERROR", "Test crashed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                TestLogStore.append(this, "INFO", "=== Test finished ===");
                runOnUiThread(() -> {
                    refreshLogView();
                    markTestFinished();
                });
            }
        }).start();
    }

    private void continueAfterActiveConversationCheck(String brandId, String appInstallId) {
        // Init (critical junction #1)
        TestLogStore.append(this, "INFO", "Calling LivePerson.initialize(...) with MonitoringInitParams...");
        LivePerson.setIsDebuggable(true); // enables more SDK logging (best effort)
        LivePerson.initialize(
                getApplicationContext(),
                new InitLivePersonProperties(
                        brandId,
                        "com.mybrand.livepersondemo",
                        new MonitoringInitParams(appInstallId),
                        new InitLivePersonCallBack() {
                            @Override
                            public void onInitSucceed() {
                                TestLogStore.append(TroubleshootActivity.this, "OK", "initialize succeeded");
                                // Now that SDK is initialized, check if there is an active conversation
                                LivePerson.checkActiveConversation(new ICallback<Boolean, Exception>() {
                                    @Override
                                    public void onSuccess(Boolean active) {
                                        TestLogStore.append(TroubleshootActivity.this, "INFO", "Active conversation BEFORE showConversation: " + active);
                                        startConversationAndPostCheck();
                                    }

                                    @Override
                                    public void onError(Exception ex) {
                                        TestLogStore.append(TroubleshootActivity.this, "WARN", "checkActiveConversation (pre) error: " + (ex == null ? "" : ex.getMessage()));
                                        startConversationAndPostCheck();
                                    }

                                    private void startConversationAndPostCheck() {
                                        // Start conversation (critical junction #2)
                                        LPAuthenticationParams authParams = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
                                        com.liveperson.infra.ConversationViewParams viewParams = new com.liveperson.infra.ConversationViewParams();
                                        TestLogStore.append(TroubleshootActivity.this, "INFO", "Calling LivePerson.showConversation(UN_AUTH)...");
                                        boolean started;
                                        try {
                                            // Clear + start polling right before/after opening the conversation so
                                            // we capture the important connection/retry window within the SDK's ~100-line cap.
                                            try {
                                                LivePerson.Logging.clearHistory();
                                                TestLogStore.append(TroubleshootActivity.this, "INFO", "Cleared LivePerson log history before showConversation");
                                            } catch (Throwable ignore) {}

                                            started = LivePerson.showConversation(TroubleshootActivity.this, authParams, viewParams);
                                            TestLogStore.append(TroubleshootActivity.this, started ? "OK" : "WARN", "showConversation returned: " + started);

                                            startSdkLogPolling(15_000, 750);
                                        } catch (Exception e) {
                                            TestLogStore.append(TroubleshootActivity.this, "ERROR", "showConversation threw: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                                        }

                                        // Post-check after a short delay (gives UI/state time to settle)
                                        runOnUiThread(() -> {
                                            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());

                                            Runnable post1 = () -> postChecks("1.5s");
                                            Runnable post2 = () -> postChecks("5s");

                                            h.postDelayed(post1, 1500);
                                            h.postDelayed(post2, 5000);
                                        });

                                        runOnUiThread(TroubleshootActivity.this::refreshLogView);
                                    }

                                    private void postChecks(String label) {
                                        // Re-check DNS for IDP (often where retries happen)
                                        new Thread(() -> dnsCheckBlocking("lo.idp.liveperson.net")).start();

                                        LivePerson.checkActiveConversation(new ICallback<Boolean, Exception>() {
                                            @Override
                                            public void onSuccess(Boolean activeAfter) {
                                                TestLogStore.append(TroubleshootActivity.this, "INFO", "[" + label + "] Active conversation: " + activeAfter);
                                                if ("5s".equals(label)) {
                                                    stopSdkLogPolling();
                                                    appendLivePersonSdkSnapshot();
                                                    TestLogStore.append(TroubleshootActivity.this, "INFO", "=== Test finished ===");
                                                    markTestFinished();
                                                }
                                                refreshLogView();
                                            }

                                            @Override
                                            public void onError(Exception ex2) {
                                                TestLogStore.append(TroubleshootActivity.this, "WARN", "[" + label + "] checkActiveConversation error: " + (ex2 == null ? "" : ex2.getMessage()));
                                                if ("5s".equals(label)) {
                                                    stopSdkLogPolling();
                                                    appendLivePersonSdkSnapshot();
                                                    TestLogStore.append(TroubleshootActivity.this, "INFO", "=== Test finished ===");
                                                    markTestFinished();
                                                }
                                                refreshLogView();
                                            }
                                        });
                                    }
                                });
                            }

                            @Override
                            public void onInitFailed(Exception e) {
                                TestLogStore.append(TroubleshootActivity.this, "ERROR", "initialize failed: " + (e == null ? "" : e.getMessage()));
                                runOnUiThread(() -> {
                                    stopSdkLogPolling();
                                    appendLivePersonSdkSnapshot();
                                    TestLogStore.append(TroubleshootActivity.this, "INFO", "=== Test finished ===");
                                    refreshLogView();
                                    markTestFinished();
                                });
                            }
                        }
                )
        );

        runOnUiThread(this::refreshLogView);
    }

    private void updateButtons() {
        runOnUiThread(() -> {
            if (testBtn != null) testBtn.setEnabled(!isTestRunning && !isSaving);
            if (saveBtn != null) saveBtn.setEnabled(!isTestRunning && !isSaving);
        });
    }

    private void markTestFinished() {
        isTestRunning = false;
        updateButtons();
    }

    private void appendLivePersonSdkSnapshot() {
        try {
            String block = LivePerson.Logging.getLogSnapshotStringBlock(LogLevel.VERBOSE);
            if (block == null || block.trim().isEmpty()) {
                TestLogStore.append(this, "INFO", "LivePerson SDK log snapshot: (empty)");
                return;
            }

            TestLogStore.append(this, "INFO", "=== LivePerson SDK log snapshot (VERBOSE) ===");
            String[] lines = block.split("\\r?\\n");
            for (String line : lines) {
                if (line == null) continue;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                TestLogStore.append(this, "SDK", trimmed);
            }
            TestLogStore.append(this, "INFO", "=== End LivePerson SDK log snapshot ===");
        } catch (Throwable t) {
            TestLogStore.append(this, "WARN", "Failed to read LivePerson SDK log snapshot: " + t.getClass().getSimpleName());
        }
    }

    private void startSdkLogPolling(long durationMs, long intervalMs) {
        if (sdkPolling) return;
        sdkPolling = true;
        seenSdkLogLines.clear();

        long startAt = android.os.SystemClock.elapsedRealtime();
        TestLogStore.append(this, "INFO", "Starting SDK log polling (" + durationMs + "ms, every " + intervalMs + "ms)");

        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!sdkPolling) return;

                try {
                    List<String> lines = LivePerson.Logging.getLogSnapshotStrings(LogLevel.VERBOSE);
                    if (lines != null) {
                        for (String l : lines) {
                            if (l == null) continue;
                            String trimmed = l.trim();
                            if (trimmed.isEmpty()) continue;
                            if (seenSdkLogLines.add(trimmed)) {
                                TestLogStore.append(TroubleshootActivity.this, "SDK", trimmed);
                            }
                        }
                    }
                } catch (Throwable ignore) {
                    // best effort
                }

                if (android.os.SystemClock.elapsedRealtime() - startAt >= durationMs) {
                    sdkPolling = false;
                    TestLogStore.append(TroubleshootActivity.this, "INFO", "Stopped SDK log polling (duration reached)");
                    refreshLogView();
                    return;
                }

                sdkLogPollHandler.postDelayed(this, intervalMs);
            }
        };

        sdkLogPollHandler.post(poll);
    }

    private void stopSdkLogPolling() {
        if (!sdkPolling) return;
        sdkPolling = false;
        sdkLogPollHandler.removeCallbacksAndMessages(null);
        TestLogStore.append(this, "INFO", "Stopped SDK log polling");
    }

    private void dnsCheckBlocking(String host) {
        long start = System.currentTimeMillis();
        try {
            java.net.InetAddress.getByName(host);
            long ms = System.currentTimeMillis() - start;
            TestLogStore.append(this, "OK", "DNS OK: " + host + " (" + ms + "ms)");
        } catch (Exception e) {
            TestLogStore.append(this, "WARN", "DNS FAIL: " + host + " (" + e.getMessage() + ")");
        }
    }
}


