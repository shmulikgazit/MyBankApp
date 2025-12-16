package com.mybrand.livepersondemo;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import com.liveperson.infra.log.LogLevel;
import com.liveperson.messaging.sdk.api.LivePerson;

/**
 * Applies LivePerson SDK logging defaults early and consistently.
 *
 * Based on LivePerson "Advanced Features: Logging":
 * - LivePerson.Logging.setSDKLoggingLevel(...)
 * - LivePerson.Logging.setDataMaskingEnabled(...)
 */
public class MyHostApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

            // Best effort: ensure debug builds emit more SDK detail, while keeping masking ON for safety.
            LivePerson.setIsDebuggable(isDebuggable);
            LivePerson.Logging.setSDKLoggingLevel(isDebuggable ? LogLevel.DEBUG : LogLevel.INFO);
            LivePerson.Logging.setDataMaskingEnabled(true);
        } catch (Throwable ignore) {
            // If anything changes between SDK versions, don't crash the host app.
        }
    }
}


