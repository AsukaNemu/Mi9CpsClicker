package com.asukanemu.mi9clicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

public final class ClickAccessibilityService extends AccessibilityService {
    public interface ClickListener {
        void onStateChanged(boolean running, String message);
        void onProgress(int completed, int total);
    }

    private static volatile ClickAccessibilityService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private int session;
    private int targetX;
    private int targetY;
    private int cps;
    private int total;
    private int completed;
    private ClickListener listener;

    public static ClickAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isConnected() {
        return instance != null;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Window contents are intentionally not observed. The service only dispatches user-configured taps.
    }

    @Override
    public void onInterrupt() {
        stopClicking("点击服务被系统中断");
    }

    @Override
    public void onDestroy() {
        stopClicking("点击服务已关闭");
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    public void startClicking(int x, int y, int clicksPerSecond, int clickCount, ClickListener clickListener) {
        stopInternal(null);
        targetX = x;
        targetY = y;
        cps = ClickConfig.clamp(clicksPerSecond, 1, 50);
        total = ClickConfig.clamp(clickCount, 0, 1_000_000);
        completed = 0;
        listener = clickListener;
        running = true;
        session++;
        int activeSession = session;
        notifyState(true, "点击中");
        handler.post(() -> dispatchNext(activeSession));
    }

    public void stopClicking(String reason) {
        stopInternal(reason == null ? "已暂停" : reason);
    }

    public boolean isRunning() {
        return running;
    }

    public void clearListener(ClickListener candidate) {
        if (listener == candidate) {
            listener = null;
        }
    }

    private void dispatchNext(int activeSession) {
        if (!running || activeSession != session) {
            return;
        }
        if (total > 0 && completed >= total) {
            stopInternal("已完成 " + total + " 次点击");
            return;
        }

        Path path = new Path();
        path.moveTo(targetX, targetY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 1))
                .build();
        long startedAt = SystemClock.uptimeMillis();

        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                if (!running || activeSession != session) {
                    return;
                }
                completed++;
                if (listener != null) {
                    listener.onProgress(completed, total);
                }
                if (total > 0 && completed >= total) {
                    stopInternal("已完成 " + total + " 次点击");
                    return;
                }
                long intervalMs = Math.max(1L, Math.round(1000.0 / cps));
                long elapsed = SystemClock.uptimeMillis() - startedAt;
                handler.postDelayed(() -> dispatchNext(activeSession), Math.max(0L, intervalMs - elapsed));
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                if (running && activeSession == session) {
                    stopInternal("手势被系统取消");
                }
            }
        }, handler);

        if (!accepted) {
            stopInternal("系统拒绝了点击手势");
        }
    }

    private void stopInternal(String reason) {
        boolean wasRunning = running;
        running = false;
        session++;
        handler.removeCallbacksAndMessages(null);
        if (reason != null && (wasRunning || listener != null)) {
            notifyState(false, reason);
        }
    }

    private void notifyState(boolean isRunning, String message) {
        if (listener != null) {
            listener.onStateChanged(isRunning, message);
        }
    }
}
