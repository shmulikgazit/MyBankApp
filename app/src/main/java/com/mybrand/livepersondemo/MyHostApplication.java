package com.mybrand.livepersondemo;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import com.liveperson.infra.log.LogLevel;
import com.liveperson.messaging.sdk.api.LivePerson;

public class MyHostApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

            LivePerson.setIsDebuggable(isDebuggable);
            LivePerson.Logging.setSDKLoggingLevel(isDebuggable ? LogLevel.DEBUG : LogLevel.INFO);
            LivePerson.Logging.setDataMaskingEnabled(!isDebuggable);
        } catch (Throwable ignore) {
        }
    }
}
