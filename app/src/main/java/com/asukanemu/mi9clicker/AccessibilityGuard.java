package com.asukanemu.mi9clicker;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AccessibilityGuard {
    interface GrantCallback {
        void onComplete(boolean granted, String message);
    }

    private static final ExecutorService ROOT_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AccessibilityGuard() {}

    static boolean hasSecureSettingsPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isGlobalAccessibilityEnabled(Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
        ) == 1;
    }

    static boolean isOwnServiceListed(Context context) {
        String ownService = ownServiceName(context);
        for (String service : enabledServices(context)) {
            if (ownService.equals(service)) {
                return true;
            }
        }
        return false;
    }

    static boolean removeOwnService(Context context) {
        if (!hasSecureSettingsPermission(context)) {
            return false;
        }
        String ownService = ownServiceName(context);
        List<String> remaining = new ArrayList<>();
        for (String service : enabledServices(context)) {
            if (!ownService.equals(service)) {
                remaining.add(service);
            }
        }
        return Settings.Secure.putString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                joinServices(remaining)
        );
    }

    static boolean ensureOwnServiceEnabled(Context context) {
        if (!hasSecureSettingsPermission(context)) {
            return false;
        }
        String ownService = ownServiceName(context);
        List<String> services = enabledServices(context);
        if (!services.contains(ownService)) {
            services.add(ownService);
            if (!Settings.Secure.putString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    joinServices(services)
            )) {
                return false;
            }
        }
        return Settings.Secure.putInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1
        );
    }

    static void requestRootGrant(Context context, GrantCallback callback) {
        Context appContext = context.getApplicationContext();
        if (hasSecureSettingsPermission(appContext)) {
            MAIN_HANDLER.post(() -> callback.onComplete(true, "Root 自愈已授权"));
            return;
        }
        ROOT_EXECUTOR.execute(() -> {
            boolean granted = false;
            String message;
            try {
                String command = "pm grant " + appContext.getPackageName() + " "
                        + Manifest.permission.WRITE_SECURE_SETTINGS;
                Process process = new ProcessBuilder("su", "-c", command)
                        .redirectErrorStream(true)
                        .start();
                Runnable timeout = process::destroy;
                MAIN_HANDLER.postDelayed(timeout, 60_000L);
                int exitCode = process.waitFor();
                MAIN_HANDLER.removeCallbacks(timeout);
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (output.length() < 500) {
                            output.append(line).append('\n');
                        }
                    }
                }
                granted = exitCode == 0 && hasSecureSettingsPermission(appContext);
                message = granted
                        ? "Root 自愈授权成功"
                        : "授权失败（退出码 " + exitCode + "）";
                if (!granted && output.length() > 0) {
                    message += "：" + output.toString().trim();
                }
            } catch (Exception error) {
                message = "无法调用 Root：" + error.getMessage();
            }
            boolean result = granted;
            String resultMessage = message;
            MAIN_HANDLER.post(() -> callback.onComplete(result, resultMessage));
        });
    }

    private static String ownServiceName(Context context) {
        return new ComponentName(context, ClickAccessibilityService.class).flattenToString();
    }

    private static List<String> enabledServices(Context context) {
        String value = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        List<String> services = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return services;
        }
        for (String service : value.split(":")) {
            String trimmed = service.trim();
            if (!trimmed.isEmpty() && !services.contains(trimmed)) {
                services.add(trimmed);
            }
        }
        return services;
    }

    private static String joinServices(List<String> services) {
        StringBuilder result = new StringBuilder();
        for (String service : services) {
            if (result.length() > 0) {
                result.append(':');
            }
            result.append(service);
        }
        return result.toString();
    }
}
