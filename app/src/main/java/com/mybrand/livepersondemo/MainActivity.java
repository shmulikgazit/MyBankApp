package com.mybrand.livepersondemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.liveperson.infra.ConversationViewParams;
import com.liveperson.infra.InitLivePersonProperties;
import com.liveperson.infra.MonitoringInitParams;
import com.liveperson.infra.PushUnregisterType;
import com.liveperson.infra.auth.LPAuthenticationParams;
import com.liveperson.infra.auth.LPAuthenticationType;
import com.liveperson.infra.callbacks.InitLivePersonCallBack;
import com.liveperson.infra.callbacks.PKCEParamsCallBack;
import com.liveperson.infra.model.PKCEParams;
import com.liveperson.infra.model.errors.PkceGenerateError;
import com.liveperson.messaging.sdk.api.LivePerson;
import com.liveperson.messaging.sdk.api.callbacks.LogoutLivePersonCallback;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.CodeVerifierUtil;
import net.openid.appauth.ResponseTypeValues;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LivePersonDemo";
    private static final String APP_ID = "com.mybrand.livepersondemo";

    public static final String BRAND_ID = "45975138";
    public static final String APP_INSTALL_ID = "8be7d84c-a6e1-45a1-80f3-c70dfc118d09";

    public static final String DEFAULT_ISSUER = "https://trial-2210774.okta.com/oauth2/aussz3u8wyZoGwLyu697";
    public static final String DEFAULT_REDIRECT_URI = "com.mybrand.livepersondemo://callback";

    private Button startUnauthButton;
    private Button startAuthButton;
    private TextView statusTextView;
    private AuthorizationService authService;

    private volatile boolean pkceLoginInProgress;
    private boolean authRedirectConsumed;

    @Nullable
    private PKCEParams storedLpPkceParams;

    private static final String PREF_NEED_LOGOUT_BEFORE_UNAUTH = "lp_need_logout_before_unauth";
    private static final long DELAY_MS_AFTER_INIT_BEFORE_UNAUTH_CONVERSATION = 4000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        authService = new AuthorizationService(this);

        initViews();
        setupClickListeners();
        processReturnFromBrowser(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processReturnFromBrowser(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        processReturnFromBrowser(getIntent());
    }

    @Override
    protected void onDestroy() {
        if (authService != null) {
            authService.dispose();
            authService = null;
        }
        super.onDestroy();
    }

    private static int authFlowPendingIntentFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            f |= PendingIntent.FLAG_MUTABLE;
        }
        return f;
    }

    private void initViews() {
        startUnauthButton = findViewById(R.id.btn_start_unauth);
        startAuthButton = findViewById(R.id.btn_start_auth);
        statusTextView = findViewById(R.id.tv_status);
        findViewById(R.id.btn_open_settings).setOnClickListener(v ->
                startActivity(new android.content.Intent(MainActivity.this, SettingsActivity.class)));
        findViewById(R.id.btn_open_troubleshoot).setOnClickListener(v ->
                startActivity(new android.content.Intent(MainActivity.this, TroubleshootActivity.class)));
    }

    private void setupClickListeners() {
        startUnauthButton.setOnClickListener(v -> startConversation(false));
        startAuthButton.setOnClickListener(v -> startConversation(true));
    }

    private void setStartButtonsEnabled(boolean enabled) {
        if (startUnauthButton != null) startUnauthButton.setEnabled(enabled);
        if (startAuthButton != null) startAuthButton.setEnabled(enabled);
    }

    private void startConversation(boolean authenticatedFlow) {
        Log.d(TAG, authenticatedFlow ? "Starting authenticated (Okta) flow..." : "Starting unauthenticated flow...");
        setStartButtonsEnabled(false);
        setStatus("Initializing...");

        SharedPreferences prefs = getSharedPreferences("lp_demo", MODE_PRIVATE);
        final String brandId = prefs.getString("brand_id", BRAND_ID).trim();
        final String appInstallId = prefs.getString("app_install_id", APP_INSTALL_ID).trim();

        String issuerRaw = prefs.getString("issuer", DEFAULT_ISSUER);
        final String issuer = (issuerRaw == null || issuerRaw.trim().isEmpty()) ? DEFAULT_ISSUER : issuerRaw.trim();

        String redirectRaw = prefs.getString("redirect_uri", DEFAULT_REDIRECT_URI);
        final String redirectUriStr = (redirectRaw == null || redirectRaw.trim().isEmpty())
                ? DEFAULT_REDIRECT_URI : redirectRaw.trim();

        if (authenticatedFlow) {
            String clientId = prefs.getString("okta_client_id", "").trim();
            if (clientId.isEmpty()) {
                Log.w(TAG, "AUTH aborted: empty Okta client id (enter in Settings)");
                setStatus("Add Okta Client ID in Settings");
                setStartButtonsEnabled(true);
                return;
            }
        }

        if (!authenticatedFlow) {
            if (prefs.getBoolean(PREF_NEED_LOGOUT_BEFORE_UNAUTH, false)) {
                setStatus("Clearing session (visitor chat)...");
                Log.i(TAG, "logOut before UN_AUTH (prior AUTH conversation in this app)");
                LivePerson.logOut(getApplicationContext(), brandId, APP_ID, true, PushUnregisterType.NONE,
                        new LogoutLivePersonCallback() {
                            @Override
                            public void onLogoutSucceed() {
                                runOnUiThread(() -> initializeLivePersonForFlow(brandId, appInstallId, issuer, redirectUriStr, false));
                            }

                            @Override
                            public void onLogoutFailed() {
                                Log.w(TAG, "logOut failed; still attempting initialize for UN_AUTH");
                                runOnUiThread(() -> initializeLivePersonForFlow(brandId, appInstallId, issuer, redirectUriStr, false));
                            }
                        });
            } else {
                Log.i(TAG, "UN_AUTH: skip logOut (visitor-only; avoids LP engagement cold-load errors)");
                initializeLivePersonForFlow(brandId, appInstallId, issuer, redirectUriStr, false);
            }
            return;
        }

        setStatus("Preparing sign-in...");
        Log.i(TAG, "AUTH step 1/4: LivePerson.logOut before init");
        LivePerson.logOut(getApplicationContext(), brandId, APP_ID, true, PushUnregisterType.NONE,
                new LogoutLivePersonCallback() {
                    @Override
                    public void onLogoutSucceed() {
                        Log.i(TAG, "AUTH step 2/4: logOut OK → LivePerson.initialize");
                        runOnUiThread(() -> initializeLivePersonForFlow(brandId, appInstallId, issuer, redirectUriStr, true));
                    }

                    @Override
                    public void onLogoutFailed() {
                        Log.w(TAG, "AUTH: logOut failed → still calling initialize");
                        runOnUiThread(() -> initializeLivePersonForFlow(brandId, appInstallId, issuer, redirectUriStr, true));
                    }
                });
    }

    private void initializeLivePersonForFlow(final String brandId, final String appInstallId,
            final String issuer, final String redirectUriStr, final boolean authenticatedFlow) {
        InitLivePersonCallBack initCallback = new InitLivePersonCallBack() {
            @Override
            public void onInitSucceed() {
                Log.i(TAG, "SDK init OK for brand " + brandId + " (authenticatedFlow=" + authenticatedFlow + ")");
                runOnUiThread(() -> {
                    if (authenticatedFlow) {
                        Log.i(TAG, "AUTH step 3/4: LP ready → getPKCEParams + Okta authorize (no app /token)");
                        setStatus("Generating PKCE (LivePerson SDK)…");
                        beginOktaPkceLogin(issuer, redirectUriStr);
                    } else {
                        setStatus("Opening conversation...");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            openUnauthenticatedConversation();
                        }, DELAY_MS_AFTER_INIT_BEFORE_UNAUTH_CONVERSATION);
                    }
                });
            }

            @Override
            public void onInitFailed(Exception e) {
                Log.e(TAG, "SDK Init Failed", e);
                runOnUiThread(() -> {
                    String msg = e.getMessage() != null ? e.getMessage() : String.valueOf(e);
                    setStatus("Init failed: " + msg);
                    setStartButtonsEnabled(true);
                    pkceLoginInProgress = false;
                });
            }
        };

        Log.i(TAG, "Calling LivePerson.initialize for brand: " + brandId + " appInstallId=" + appInstallId);
        LivePerson.initialize(getApplicationContext(), new InitLivePersonProperties(
                brandId, APP_ID, new MonitoringInitParams(appInstallId), initCallback));
    }

    private void beginOktaPkceLogin(String issuerUrl, String redirectUriStr) {
        SharedPreferences prefs = getSharedPreferences("lp_demo", MODE_PRIVATE);
        final String clientId = prefs.getString("okta_client_id", "").trim();
        final Uri redirectUri = Uri.parse(redirectUriStr);

        pkceLoginInProgress = true;
        authRedirectConsumed = false;
        storedLpPkceParams = null;

        LPAuthenticationParams pkceTemplate = buildLpAuthTemplateForPkce(redirectUriStr);
        Log.i(TAG, "Calling LivePerson.getPKCEParams");

        LivePerson.getPKCEParams(getApplicationContext(), pkceTemplate, new PKCEParamsCallBack() {
            @Override
            public void onPKCEGenerateSuccess(PKCEParams params) {
                if (params == null || params.getCodeVerifier() == null || params.getCodeChallenge() == null) {
                    Log.e(TAG, "getPKCEParams returned null fields");
                    runOnUiThread(() -> {
                        pkceLoginInProgress = false;
                        setStartButtonsEnabled(true);
                        setStatus("PKCE params from SDK were incomplete");
                    });
                    return;
                }
                storedLpPkceParams = params;
                Log.i(TAG, "getPKCEParams OK: method=" + params.getCodeChallengeMethod()
                        + " verifier_len=" + params.getCodeVerifier().length()
                        + " challenge_len=" + params.getCodeChallenge().length());
                runOnUiThread(() -> fetchOidcConfigAndOpenBrowser(issuerUrl, redirectUriStr, clientId, redirectUri, params));
            }

            @Override
            public void onPKCEGenerateFailed(@Nullable PkceGenerateError error) {
                Log.e(TAG, "getPKCEParams failed: " + (error != null ? error.toString() : "null"));
                runOnUiThread(() -> {
                    pkceLoginInProgress = false;
                    setStartButtonsEnabled(true);
                    setStatus("PKCE generation failed (see Logcat)");
                });
            }
        });
    }

    private LPAuthenticationParams buildLpAuthTemplateForPkce(String redirectUriStr) {
        SharedPreferences prefs = getSharedPreferences("lp_demo", MODE_PRIVATE);
        LPAuthenticationParams p = new LPAuthenticationParams(LPAuthenticationType.AUTH);
        if (redirectUriStr != null && !redirectUriStr.isEmpty()) {
            p.setHostAppRedirectUri(redirectUriStr);
        }
        String idpDisplay = prefs.getString("idp_display_name", "").trim();
        if (!idpDisplay.isEmpty()) {
            p.setIssuerDisplayName(idpDisplay);
        }
        return p;
    }

    private void fetchOidcConfigAndOpenBrowser(String issuerUrl, String redirectUriStr, String clientId, Uri redirectUri,
            PKCEParams lpPkce) {
        Log.i(TAG, "fetchFromIssuer: " + issuerUrl);
        AuthorizationServiceConfiguration.fetchFromIssuer(
                Uri.parse(issuerUrl),
                (@Nullable AuthorizationServiceConfiguration serviceConfig, @Nullable AuthorizationException ex) -> {
                    if (serviceConfig == null) {
                        String err = ex != null && ex.error != null ? ex.error : "Could not load OIDC configuration";
                        Log.e(TAG, "OIDC discovery failed: " + err, ex);
                        runOnUiThread(() -> {
                            storedLpPkceParams = null;
                            pkceLoginInProgress = false;
                            setStartButtonsEnabled(true);
                            setStatus("Okta discovery failed: " + err);
                        });
                        return;
                    }

                    /*
                     * LP may return code_verifier longer than RFC 7636 max (128). AppAuth validates length;
                     * authorize only sends code_challenge (from LP). Use a short dummy verifier for AppAuth;
                     * pass LP's real verifier to the SDK via setCodeVerifier.
                     */
                    String lpVerifier = lpPkce.getCodeVerifier();
                    if (lpVerifier != null && lpVerifier.length() > 128) {
                        Log.w(TAG, "LP code_verifier length=" + lpVerifier.length()
                                + " > 128; AppAuth uses dummy verifier for build; authorize uses LP code_challenge");
                    }
                    String appAuthVerifierForLibrary = CodeVerifierUtil.generateRandomCodeVerifier();

                    AuthorizationRequest request = new AuthorizationRequest.Builder(
                            serviceConfig,
                            clientId,
                            ResponseTypeValues.CODE,
                            redirectUri)
                            .setScopes(Arrays.asList("openid", "profile", "email"))
                            .setCodeVerifier(
                                    appAuthVerifierForLibrary,
                                    lpPkce.getCodeChallenge(),
                                    lpPkce.getCodeChallengeMethod())
                            .build();

                    Intent completion = new Intent(this, MainActivity.class);
                    completion.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    Intent cancel = new Intent(this, MainActivity.class);
                    cancel.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    cancel.putExtra("lp_auth_cancelled", true);

                    PendingIntent ok = PendingIntent.getActivity(this, 0, completion, authFlowPendingIntentFlags());
                    PendingIntent cancelled = PendingIntent.getActivity(this, 1, cancel, authFlowPendingIntentFlags());

                    runOnUiThread(() -> {
                        try {
                            setStatus("Opening sign-in… (browser tab)");
                            Log.i(TAG, "AUTH step 4/4: Okta authorize → return code + verifier to LP (LP calls /token)");
                            authService.performAuthorizationRequest(
                                    request,
                                    ok,
                                    cancelled,
                                    authService.createCustomTabsIntentBuilder().build());
                        } catch (Throwable t) {
                            Log.e(TAG, "performAuthorizationRequest failed", t);
                            storedLpPkceParams = null;
                            pkceLoginInProgress = false;
                            setStartButtonsEnabled(true);
                            String m = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                            setStatus("Could not open sign-in: " + m);
                        }
                    });
                });
    }

    private void processReturnFromBrowser(Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra("lp_auth_cancelled", false)) {
            if (pkceLoginInProgress) {
                pkceLoginInProgress = false;
                storedLpPkceParams = null;
                setStartButtonsEnabled(true);
                setStatus("Sign-in cancelled");
            }
            intent.removeExtra("lp_auth_cancelled");
            return;
        }

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException authEx = AuthorizationException.fromIntent(intent);

        if (response == null && authEx == null) {
            if (pkceLoginInProgress) {
                Log.w(TAG, "OAuth return: no response in Intent yet");
            }
            return;
        }

        if (!pkceLoginInProgress && response == null) {
            return;
        }

        if (authEx != null) {
            Log.e(TAG, "Authorization failed", authEx);
            pkceLoginInProgress = false;
            storedLpPkceParams = null;
            authRedirectConsumed = false;
            setStartButtonsEnabled(true);
            setStatus("Okta authorization failed");
            Toast.makeText(this,
                    authEx.error != null ? authEx.error : String.valueOf(authEx.getMessage()),
                    Toast.LENGTH_LONG).show();
            clearAuthIntent(intent);
            return;
        }

        if (response == null) {
            return;
        }

        if (authRedirectConsumed) {
            return;
        }
        authRedirectConsumed = true;

        logOktaAuthorizationRedirect(response);

        final String codeVerifierForLp;
        if (storedLpPkceParams != null && storedLpPkceParams.getCodeVerifier() != null
                && !storedLpPkceParams.getCodeVerifier().isEmpty()) {
            codeVerifierForLp = storedLpPkceParams.getCodeVerifier();
            Log.i(TAG, "code_verifier from LivePerson.getPKCEParams, length=" + codeVerifierForLp.length());
        } else {
            codeVerifierForLp = response.request != null ? response.request.codeVerifier : null;
            Log.w(TAG, "LP PKCE params missing; fallback to AppAuth request.codeVerifier");
        }
        storedLpPkceParams = null;

        if (response.authorizationCode == null || response.authorizationCode.isEmpty()) {
            pkceLoginInProgress = false;
            authRedirectConsumed = false;
            setStartButtonsEnabled(true);
            setStatus("Okta returned no authorization code");
            Toast.makeText(this, "Missing authorization code from Okta", Toast.LENGTH_LONG).show();
            clearAuthIntent(intent);
            return;
        }

        SharedPreferences prefs = getSharedPreferences("lp_demo", MODE_PRIVATE);
        String redirectUriStr = prefs.getString("redirect_uri", DEFAULT_REDIRECT_URI).trim();
        setStatus("Opening conversation (LivePerson IdP completes token)...");
        openConversationWithAuthKey(response.authorizationCode, null, redirectUriStr, codeVerifierForLp);
        pkceLoginInProgress = false;
        setStartButtonsEnabled(true);
        clearAuthIntent(intent);
    }

    private static void clearAuthIntent(Intent intent) {
        intent.setData(null);
        intent.removeCategory(Intent.CATEGORY_BROWSABLE);
    }

    private void openUnauthenticatedConversation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            LPAuthenticationParams authParams = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
            ConversationViewParams viewParams = new ConversationViewParams();
            boolean started = LivePerson.showConversation(MainActivity.this, authParams, viewParams);
            Log.i(TAG, "showConversation (UN_AUTH) result: " + started);
            if (started) {
                getSharedPreferences("lp_demo", MODE_PRIVATE).edit()
                        .putBoolean(PREF_NEED_LOGOUT_BEFORE_UNAUTH, false)
                        .apply();
            }
            setStatus(started ? "Conversation opened" : "showConversation returned false");
            setStartButtonsEnabled(true);
        }, 500);
    }

    private void openConversationWithAuthKey(String authKey, @Nullable String idpDisplay, String redirectUriStr,
            @Nullable String oktaCodeVerifier) {
        SharedPreferences prefs = getSharedPreferences("lp_demo", MODE_PRIVATE);
        if (idpDisplay == null) {
            idpDisplay = prefs.getString("idp_display_name", "").trim();
        }

        String display = idpDisplay;
        logLivePersonAuthInput(authKey, redirectUriStr, display, oktaCodeVerifier);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            getSharedPreferences("lp_demo", MODE_PRIVATE).edit().putBoolean(PREF_NEED_LOGOUT_BEFORE_UNAUTH, true).apply();
            LPAuthenticationParams authParams = new LPAuthenticationParams(LPAuthenticationType.AUTH);
            authParams.setAuthKey(authKey);
            applyOktaCodeVerifierForLp(authParams, oktaCodeVerifier);
            if (redirectUriStr != null && !redirectUriStr.isEmpty()) {
                authParams.setHostAppRedirectUri(redirectUriStr);
            }
            if (display != null && !display.isEmpty()) {
                authParams.setIssuerDisplayName(display);
            }
            boolean started = LivePerson.showConversation(MainActivity.this, authParams, new ConversationViewParams(false));
            Log.d(TAG, "showConversation (authKey) result: " + started);
            setStatus(started ? "Conversation opened" : "showConversation returned false");
        }, 500);
    }

    private void setStatus(String text) {
        if (statusTextView != null) {
            statusTextView.setText(text);
        }
    }

    private static void applyOktaCodeVerifierForLp(LPAuthenticationParams authParams, @Nullable String oktaCodeVerifier) {
        if (authParams == null || oktaCodeVerifier == null || oktaCodeVerifier.isEmpty()) {
            return;
        }
        authParams.setCodeVerifier(oktaCodeVerifier);
    }

    private void logOktaAuthorizationRedirect(AuthorizationResponse r) {
        Log.i(TAG, "--- Okta redirect (no in-app /token) ---");
        if (r.authorizationCode != null) {
            Log.i(TAG, "authorization_code length=" + r.authorizationCode.length()
                    + (BuildConfig.DEBUG ? (" value=" + r.authorizationCode) : ""));
        } else {
            Log.w(TAG, "authorization_code: null");
        }
        Log.i(TAG, "state: " + r.state);
        if (r.request != null && r.request.codeVerifier != null) {
            Log.i(TAG, "AppAuth internal code_verifier length=" + r.request.codeVerifier.length());
        }
    }

    private void logLivePersonAuthInput(String authorizationCode, String redirectUri, String idpDisplay,
            @Nullable String oktaCodeVerifier) {
        Log.i(TAG, "--- LivePerson SDK input ---");
        Log.i(TAG, "AUTH: setAuthKey authorization_code len=" + (authorizationCode != null ? authorizationCode.length() : 0)
                + (BuildConfig.DEBUG && authorizationCode != null ? (" value=" + authorizationCode) : ""));
        if (oktaCodeVerifier != null && !oktaCodeVerifier.isEmpty()) {
            Log.i(TAG, "setCodeVerifier len=" + oktaCodeVerifier.length()
                    + (BuildConfig.DEBUG ? (" value=" + oktaCodeVerifier) : ""));
        } else {
            Log.w(TAG, "setCodeVerifier missing (LP PKCE IdP may error 1057)");
        }
        Log.i(TAG, "setHostAppRedirectUri: " + redirectUri);
        Log.i(TAG, "setIssuerDisplayName: " + (idpDisplay == null || idpDisplay.isEmpty() ? "(empty)" : idpDisplay));
    }
}
