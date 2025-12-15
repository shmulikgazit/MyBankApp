package com.mybrand.livepersondemo;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Simple release-friendly rolling log:
 * - Stored in app-private storage (no special permissions)
 * - Can be exported via SAF (CreateDocument)
 * - Masks obvious tokens/ids in URLs and long strings
 */
public final class TestLogStore {
    private static final String FILE_NAME = "lp_test_log.txt";
    private static final int MAX_BYTES = 200_000; // ~200KB
    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    // Mask common query params and long token-ish strings
    private static final Pattern SID = Pattern.compile("([?&]sid=)[^&\\s]+");
    private static final Pattern VID = Pattern.compile("([?&]vid=)[^&\\s]+");
    private static final Pattern JWTISH = Pattern.compile("([A-Za-z0-9_-]{20,})");

    private TestLogStore() {}

    public static synchronized void clear(Context context) {
        File f = new File(context.getFilesDir(), FILE_NAME);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    public static synchronized void append(Context context, String level, String message) {
        String line = TS.format(new Date()) + " [" + level + "] " + sanitize(message) + "\n";
        File f = new File(context.getFilesDir(), FILE_NAME);

        try (FileOutputStream fos = new FileOutputStream(f, true)) {
            fos.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignore) {
            // best effort
        }

        rotateIfNeeded(f);
    }

    public static synchronized String readAll(Context context) {
        File f = new File(context.getFilesDir(), FILE_NAME);
        if (!f.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception ignore) {
            // best effort
        }
        return sb.toString();
    }

    private static void rotateIfNeeded(File f) {
        try {
            if (!f.exists()) return;
            long len = f.length();
            if (len <= MAX_BYTES) return;

            // Keep only the last MAX_BYTES/2 bytes for simplicity
            long keep = MAX_BYTES / 2L;
            byte[] all = java.nio.file.Files.readAllBytes(f.toPath());
            int start = Math.max(0, all.length - (int) keep);
            byte[] tail = new byte[all.length - start];
            System.arraycopy(all, start, tail, 0, tail.length);
            java.nio.file.Files.write(f.toPath(), tail);
        } catch (Exception ignore) {
            // best effort
        }
    }

    private static String sanitize(String in) {
        if (in == null) return "";
        String s = in;
        s = SID.matcher(s).replaceAll("$1***");
        s = VID.matcher(s).replaceAll("$1***");
        // mask long token-like segments, but keep small words readable
        s = JWTISH.matcher(s).replaceAll("***");
        return s;
    }
}


