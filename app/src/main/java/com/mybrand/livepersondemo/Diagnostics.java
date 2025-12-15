package com.mybrand.livepersondemo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Diagnostics {

    private Diagnostics() {}

    public static String collect(Context context) {
        JSONObject root = new JSONObject();
        try {
            root.put("timestamp", isoNow());
            root.put("app", appBlock(context));
            root.put("device", deviceBlock());
            root.put("liveperson", livePersonBlock(context));
        } catch (JSONException ignore) {
            // best effort
        }
        try {
            return root.toString(2);
        } catch (JSONException ignore) {
            return root.toString();
        }
    }

    private static JSONObject appBlock(Context context) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("package", context.getPackageName());
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            o.put("versionName", pi.versionName);
            o.put("versionCode", pi.getLongVersionCode());
        } catch (Exception e) {
            o.put("versionName", JSONObject.NULL);
            o.put("versionCode", JSONObject.NULL);
        }
        return o;
    }

    private static JSONObject deviceBlock() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("model", Build.MODEL);
        o.put("sdkInt", Build.VERSION.SDK_INT);
        o.put("release", Build.VERSION.RELEASE);
        return o;
    }

    private static JSONObject livePersonBlock(Context context) throws JSONException {
        JSONObject o = new JSONObject();
        // what the app is configured to use
        String brandId = context.getSharedPreferences("lp_demo", Context.MODE_PRIVATE)
                .getString("brand_id", MainActivity.BRAND_ID);
        String appInstallId = context.getSharedPreferences("lp_demo", Context.MODE_PRIVATE)
                .getString("app_install_id", MainActivity.APP_INSTALL_ID);
        o.put("brandId", brandId);
        o.put("appInstallId", appInstallId);
        o.put("appId", "com.mybrand.livepersondemo");
        // dependency pinned in build.gradle
        o.put("sdkDependency", "com.liveperson.android:lp_messaging_sdk:5.25.1");
        return o;
    }

    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
    }
}


