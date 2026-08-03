package com.asukanemu.mi9clicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class OverlayService extends Service implements ClickAccessibilityService.ClickListener {
    private static final String CHANNEL_ID = "clicker_overlay";
    private static final int NOTIFICATION_ID = 9;

    private WindowManager windowManager;
    private LinearLayout panel;
    private TargetView target;
    private WindowManager.LayoutParams panelParams;
    private WindowManager.LayoutParams targetParams;
    private TextView statusText;
    private TextView configText;
    private Button startStopButton;
    private int screenWidth;
    private int screenHeight;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        refreshScreenSize();
        showOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        ClickAccessibilityService service = ClickAccessibilityService.getInstance();
        if (service != null) {
            service.stopClicking("悬浮控制已关闭");
            service.clearListener(this);
        }
        if (windowManager != null) {
            if (panel != null) {
                windowManager.removeView(panel);
            }
            if (target != null) {
                windowManager.removeView(target);
            }
        }
        panel = null;
        target = null;
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void showOverlay() {
        createTarget();
        createPanel();
        windowManager.addView(target, targetParams);
        windowManager.addView(panel, panelParams);
        updateConfigText();
    }

    private void createTarget() {
        int size = dp(58);
        int centerX = ClickConfig.getX(this);
        int centerY = ClickConfig.getY(this);
        target = new TargetView(this);
        target.setContentDescription("点击位置准星，可拖动");
        targetParams = new WindowManager.LayoutParams(
                size,
                size,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = ClickConfig.clamp(centerX - size / 2, -size / 2, screenWidth - size / 2);
        targetParams.y = ClickConfig.clamp(centerY - size / 2, -size / 2, screenHeight - size / 2);

        target.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private int downX;
            private int downY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        downX = targetParams.x;
                        downY = targetParams.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int nextX = downX + Math.round(event.getRawX() - downRawX);
                        int nextY = downY + Math.round(event.getRawY() - downRawY);
                        targetParams.x = ClickConfig.clamp(nextX, -size / 2, screenWidth - size / 2);
                        targetParams.y = ClickConfig.clamp(nextY, -size / 2, screenHeight - size / 2);
                        windowManager.updateViewLayout(target, targetParams);
                        saveTargetCenter();
                        updateConfigText();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        saveTargetCenter();
                        updateConfigText();
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            view.performClick();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void createPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(10), dp(14), dp(12));
        panel.setBackground(rounded(Color.argb(242, 20, 27, 34), dp(16)));
        panel.setElevation(dp(8));

        TextView handle = text("米9 连点器   拖动这里移动", 14, Color.rgb(180, 197, 207));
        handle.setGravity(Gravity.CENTER_VERTICAL);
        handle.setPadding(0, dp(3), 0, dp(8));
        panel.addView(handle, matchWrap());
        makePanelDraggable(handle);

        statusText = text("已暂停", 20, Color.WHITE);
        statusText.setGravity(Gravity.CENTER);
        panel.addView(statusText, matchWrap());

        configText = text("", 13, Color.rgb(158, 178, 189));
        configText.setGravity(Gravity.CENTER);
        configText.setPadding(0, dp(4), 0, dp(8));
        panel.addView(configText, matchWrap());

        LinearLayout cpsRow = new LinearLayout(this);
        cpsRow.setOrientation(LinearLayout.HORIZONTAL);
        cpsRow.setGravity(Gravity.CENTER);
        Button slower = smallButton("− CPS");
        Button faster = smallButton("+ CPS");
        cpsRow.addView(slower, weighted());
        cpsRow.addView(faster, weighted());
        panel.addView(cpsRow, matchWrap());

        slower.setOnClickListener(v -> changeCps(-1));
        faster.setOnClickListener(v -> changeCps(1));

        startStopButton = smallButton("开始点击");
        startStopButton.setTextColor(Color.rgb(7, 32, 27));
        startStopButton.setBackground(rounded(Color.rgb(85, 230, 193), dp(12)));
        LinearLayout.LayoutParams primaryParams = matchWrap();
        primaryParams.topMargin = dp(8);
        primaryParams.height = dp(44);
        panel.addView(startStopButton, primaryParams);
        startStopButton.setOnClickListener(v -> toggleClicking());

        Button close = smallButton("关闭悬浮控制");
        close.setTextColor(Color.rgb(209, 219, 225));
        LinearLayout.LayoutParams closeParams = matchWrap();
        closeParams.topMargin = dp(5);
        panel.addView(close, closeParams);
        close.setOnClickListener(v -> stopSelf());

        panelParams = new WindowManager.LayoutParams(
                dp(282),
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = dp(18);
        panelParams.y = dp(90);
    }

    private void makePanelDraggable(View handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private int downX;
            private int downY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        downX = panelParams == null ? dp(18) : panelParams.x;
                        downY = panelParams == null ? dp(90) : panelParams.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        panelParams.x = ClickConfig.clamp(
                                downX + Math.round(event.getRawX() - downRawX),
                                0,
                                Math.max(0, screenWidth - dp(80))
                        );
                        panelParams.y = ClickConfig.clamp(
                                downY + Math.round(event.getRawY() - downRawY),
                                0,
                                Math.max(0, screenHeight - dp(60))
                        );
                        windowManager.updateViewLayout(panel, panelParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.performClick();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void toggleClicking() {
        ClickAccessibilityService service = ClickAccessibilityService.getInstance();
        if (service == null) {
            Toast.makeText(this, "请先在无障碍设置中启用“米9 连点器点击服务”", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        if (service.isRunning()) {
            service.stopClicking("已暂停");
            return;
        }
        saveTargetCenter();
        service.startClicking(
                ClickConfig.getX(this),
                ClickConfig.getY(this),
                ClickConfig.getCps(this),
                ClickConfig.getCount(this),
                this
        );
    }

    private void changeCps(int delta) {
        ClickAccessibilityService service = ClickAccessibilityService.getInstance();
        if (service != null && service.isRunning()) {
            Toast.makeText(this, "请先暂停，再调整速度", Toast.LENGTH_SHORT).show();
            return;
        }
        int cps = ClickConfig.clamp(ClickConfig.getCps(this) + delta, 1, 50);
        ClickConfig.save(this, ClickConfig.getX(this), ClickConfig.getY(this), cps, ClickConfig.getCount(this));
        updateConfigText();
    }

    private void saveTargetCenter() {
        if (targetParams == null || target == null) {
            return;
        }
        int x = targetParams.x + targetParams.width / 2;
        int y = targetParams.y + targetParams.height / 2;
        ClickConfig.save(this, x, y, ClickConfig.getCps(this), ClickConfig.getCount(this));
    }

    private void updateConfigText() {
        if (configText == null) {
            return;
        }
        int count = ClickConfig.getCount(this);
        String countLabel = count == 0 ? "无限" : String.valueOf(count);
        configText.setText(getString(
                R.string.config_summary,
                ClickConfig.getX(this),
                ClickConfig.getY(this),
                ClickConfig.getCps(this),
                countLabel
        ));
    }

    private void setTargetTouchable(boolean touchable) {
        if (target == null || targetParams == null) {
            return;
        }
        if (touchable) {
            targetParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            target.setAlpha(1f);
        } else {
            targetParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            target.setAlpha(0.48f);
        }
        windowManager.updateViewLayout(target, targetParams);
    }

    @Override
    public void onStateChanged(boolean running, String message) {
        if (statusText == null) {
            return;
        }
        statusText.setText(message);
        statusText.setTextColor(running ? Color.rgb(85, 230, 193) : Color.WHITE);
        startStopButton.setText(running ? "暂停点击" : "开始点击");
        setTargetTouchable(!running);
        updateConfigText();
    }

    @Override
    public void onProgress(int completed, int total) {
        if (statusText == null) {
            return;
        }
        statusText.setText(total == 0 ? "点击中 · " + completed : "点击中 · " + completed + "/" + total);
    }

    private void refreshScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private TextView text(String value, float sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private Button smallButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setTextColor(Color.rgb(225, 234, 239));
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(rounded(Color.rgb(42, 54, 64), dp(10)));
        return button;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class TargetView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TargetView(Context context) {
            super(context);
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.31f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(70, 85, 230, 193));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(3f, getResources().getDisplayMetrics().density * 2f));
            paint.setColor(Color.rgb(85, 230, 193));
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.drawLine(cx - radius - 10, cy, cx + radius + 10, cy, paint);
            canvas.drawLine(cx, cy - radius - 10, cx, cy + radius + 10, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, Math.max(3f, paint.getStrokeWidth()), paint);
        }
    }
}
