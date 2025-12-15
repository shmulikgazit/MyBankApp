package com.mybrand.livepersondemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.liveperson.infra.ConversationViewParams;
import com.liveperson.infra.InitLivePersonProperties;
import com.liveperson.infra.MonitoringInitParams;
import com.liveperson.infra.auth.LPAuthenticationParams;
import com.liveperson.infra.auth.LPAuthenticationType;
import com.liveperson.infra.callbacks.InitLivePersonCallBack;
import com.liveperson.infra.statemachine.InitProcess;
import com.liveperson.messaging.sdk.api.LivePerson;
import com.liveperson.messaging.Messaging;
import com.liveperson.messaging.MessagingInitData;
import com.liveperson.messaging.MessagingFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LivePersonDemo";
    private static final String APP_ID = "com.mybrand.livepersondemo";
    
    // Brand ID for your LivePerson account
    public static final String BRAND_ID = "45975138";
    // App Install ID (not needed for AUTH flow)
    public static final String APP_INSTALL_ID = "8be7d84c-a6e1-45a1-80f3-c70dfc118d09";
    // Auth Code (Code Flow) - set this to your brand's auth code to avoid Monitoring
    private static final String AUTH_CODE = ""; // TODO: paste your auth code here

    private Button startConversationButton;
    private Button openSettingsButton;
    private Button openTroubleshootButton;
    private android.widget.TextView statusTextView;
    private Messaging messaging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        startConversationButton = findViewById(R.id.btn_start_conversation);
        openSettingsButton = findViewById(R.id.btn_open_settings);
        openTroubleshootButton = findViewById(R.id.btn_open_troubleshoot);
        statusTextView = findViewById(R.id.tv_status);
    }

    private void setupClickListeners() {
        startConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUnauthenticatedConversation();
            }
        });

        openSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        openTroubleshootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(MainActivity.this, TroubleshootActivity.class));
            }
        });
    }

    /**
     * Starts an unauthenticated conversation using LivePerson SDK
     * This is perfect for demo purposes and learning
     */
    private void startUnauthenticatedConversation() {
        Log.d(TAG, "Starting unauthenticated conversation flow");
        
        // Disable button to prevent multiple clicks
        startConversationButton.setEnabled(false);
        startConversationButton.setText(R.string.initializing);
        setStatus("Initializing...");

        // Read settings (allow override via Settings screen)
        final String brandId = getSharedPreferences("lp_demo", MODE_PRIVATE)
                .getString("brand_id", BRAND_ID);
        final String appInstallId = getSharedPreferences("lp_demo", MODE_PRIVATE)
                .getString("app_install_id", APP_INSTALL_ID);
        setStatus("Initializing for brand " + brandId);

        // Initialize Infra with Monitoring (required for UnAuth flow)
        InitLivePersonCallBack initCallback = new InitLivePersonCallBack() {
            @Override
            public void onInitSucceed() {
                Log.d(TAG, "Messaging SDK initialized successfully");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startConversationButton.setEnabled(true);
                        startConversationButton.setText(R.string.start_conversation);
                        setStatus("Initialized");
                    }
                });

                // Start UN_AUTH conversation (uses Monitoring engagement)
                LPAuthenticationParams authParams = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
                Messaging controller = MessagingFactory.getInstance().getController();
                if (controller == null) {
                    Log.e(TAG, "Messaging controller is null after init");
                    Toast.makeText(MainActivity.this, "Messaging not ready yet, please try again", Toast.LENGTH_LONG).show();
                    return;
                }
                ConversationViewParams viewParams = new ConversationViewParams();
                controller.setConversationViewParams(viewParams);
                setStatus("Connecting...");
                controller.connect(BRAND_ID, authParams, viewParams);
                // Bring the conversation UI to the foreground via SDK API
                controller.moveToForeground(BRAND_ID, authParams, viewParams);
                setStatus("Conversation opening...");
            }

            @Override
            public void onInitFailed(Exception e) {
                Log.e(TAG, "Messaging SDK initialization failed", e);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startConversationButton.setEnabled(true);
                        startConversationButton.setText(R.string.start_conversation);
                        Toast.makeText(MainActivity.this, "Failed to initialize LivePerson: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setStatus("Init failed");
                    }
                });
            }
        };

        // Stage 1: Initialize Monitoring (required for UnAuth)
        LivePerson.initialize(
                getApplicationContext(),
                new InitLivePersonProperties(
                        brandId,
                        APP_ID,
                        new MonitoringInitParams(appInstallId),
                        new InitLivePersonCallBack() {
                            @Override
                            public void onInitSucceed() {
                                // Stage 2: Open conversation UI using SDK facade
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startConversationButton.setEnabled(true);
                                        startConversationButton.setText(R.string.start_conversation);
                                        setStatus("Monitoring initialized");
                                    }
                                });

                                LPAuthenticationParams authParams = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
                                ConversationViewParams viewParams = new ConversationViewParams();
                                LivePerson.showConversation(MainActivity.this, authParams, viewParams);
                            }

                            @Override
                            public void onInitFailed(Exception e) {
                                Log.e(TAG, "Monitoring init failed", e);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startConversationButton.setEnabled(true);
                                        startConversationButton.setText(R.string.start_conversation);
                                        Toast.makeText(MainActivity.this, "Monitoring init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        setStatus("Monitoring init failed");
                                    }
                                });
                            }
                        }
                )
        );
    }

    private void setStatus(String text) {
        if (statusTextView != null) {
            statusTextView.setText(text);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity paused");
    }
}
