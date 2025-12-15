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
import com.liveperson.messaging.sdk.api.LivePerson;

public class TroubleshootActivity extends AppCompatActivity {

    private TextView logView;
    private ActivityResultLauncher<String> saveLauncher;
    private String pendingSaveText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshoot);

        setTitle("Troubleshoot");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        logView = findViewById(R.id.tv_test_log);
        Button testBtn = findViewById(R.id.btn_run_test);
        Button saveBtn = findViewById(R.id.btn_save_diagnostics);
        refreshLogView();

        saveLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
            if (uri == null) return;
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
            }
        });

        saveBtn.setOnClickListener(v -> {
            pendingSaveText = TestLogStore.readAll(this);
            saveLauncher.launch("liveperson-test-log.txt");
        });

        testBtn.setOnClickListener(v -> runTest());
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
        // Reset current test log (keep it small and focused per run)
        TestLogStore.clear(this);
        TestLogStore.append(this, "INFO", "=== Test started ===");
        TestLogStore.append(this, "INFO", "SDK version: " + LivePerson.getSDKVersion());
        TestLogStore.append(this, "INFO", "Diagnostics: " + Diagnostics.collect(this));
        refreshLogView();

        String brandId = getSharedPreferences("lp_demo", MODE_PRIVATE)
                .getString("brand_id", MainActivity.BRAND_ID);
        String appInstallId = getSharedPreferences("lp_demo", MODE_PRIVATE)
                .getString("app_install_id", MainActivity.APP_INSTALL_ID);

        TestLogStore.append(this, "INFO", "Using brandId=" + brandId + ", appInstallId=" + appInstallId);

        Toast.makeText(this, "Running test…", Toast.LENGTH_SHORT).show();

        // Run DNS checks sequentially so the log is deterministic.
        new Thread(() -> {
            dnsCheckBlocking("adminlogin.liveperson.net");
            dnsCheckBlocking("lptag.liveperson.net");
            dnsCheckBlocking("lpcdn.lpsnmedia.net");
            dnsCheckBlocking("lo.idp.liveperson.net");
            // Monitoring host varies by region; we don't hardcode it here.

            // Init (critical junction #1) – do this before any other SDK calls.
            continueAfterActiveConversationCheck(brandId, appInstallId);
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
                                            started = LivePerson.showConversation(TroubleshootActivity.this, authParams, viewParams);
                                            TestLogStore.append(TroubleshootActivity.this, started ? "OK" : "WARN", "showConversation returned: " + started);
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
                                                    TestLogStore.append(TroubleshootActivity.this, "INFO", "=== Test finished ===");
                                                }
                                                refreshLogView();
                                            }

                                            @Override
                                            public void onError(Exception ex2) {
                                                TestLogStore.append(TroubleshootActivity.this, "WARN", "[" + label + "] checkActiveConversation error: " + (ex2 == null ? "" : ex2.getMessage()));
                                                if ("5s".equals(label)) {
                                                    TestLogStore.append(TroubleshootActivity.this, "INFO", "=== Test finished ===");
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
                                    TestLogStore.append(TroubleshootActivity.this, "INFO", "=== Test finished ===");
                                    refreshLogView();
                                });
                            }
                        }
                )
        );

        runOnUiThread(this::refreshLogView);
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


