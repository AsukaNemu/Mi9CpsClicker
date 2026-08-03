package com.asukanemu.mi9clicker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int COLOR_BG = Color.rgb(11, 15, 20);
    private static final int COLOR_CARD = Color.rgb(24, 32, 40);
    private static final int COLOR_ACCENT = Color.rgb(85, 230, 193);
    private static final int COLOR_TEXT = Color.rgb(245, 248, 250);
    private static final int COLOR_MUTED = Color.rgb(157, 177, 188);

    private TextView overlayStatus;
    private TextView accessibilityStatus;
    private EditText xInput;
    private EditText yInput;
    private EditText cpsInput;
    private EditText countInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(COLOR_BG);
        window.setNavigationBarColor(Color.rgb(6, 9, 12));
        window.getDecorView().setSystemUiVisibility(0);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    @Override
    protected void onPause() {
        saveConfig(false);
        super.onPause();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(36));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = text("CEPHEUS · ANDROID 11", 12, COLOR_ACCENT);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow, matchWrap());

        TextView title = text("米9 连点器", 32, COLOR_TEXT);
        title.setPadding(0, dp(4), 0, 0);
        root.addView(title, matchWrap());

        TextView subtitle = text("拖动悬浮准星到目标位置，再按开始。无需 Root，关闭时不会执行任何手势。", 15, COLOR_MUTED);
        subtitle.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(8);
        subtitleParams.bottomMargin = dp(22);
        root.addView(subtitle, subtitleParams);

        LinearLayout permissionCard = card();
        root.addView(permissionCard, cardParams());
        permissionCard.addView(sectionTitle("01  必要权限"), matchWrap());

        accessibilityStatus = text("正在检查无障碍服务…", 15, COLOR_MUTED);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(12);
        permissionCard.addView(accessibilityStatus, statusParams);
        Button accessibilityButton = secondaryButton("打开无障碍设置");
        permissionCard.addView(accessibilityButton, buttonParams());
        accessibilityButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        overlayStatus = text("正在检查悬浮窗权限…", 15, COLOR_MUTED);
        LinearLayout.LayoutParams overlayStatusParams = matchWrap();
        overlayStatusParams.topMargin = dp(14);
        permissionCard.addView(overlayStatus, overlayStatusParams);
        Button overlayPermissionButton = secondaryButton("允许悬浮窗权限");
        permissionCard.addView(overlayPermissionButton, buttonParams());
        overlayPermissionButton.setOnClickListener(v -> openOverlayPermission());

        LinearLayout configCard = card();
        root.addView(configCard, cardParams());
        configCard.addView(sectionTitle("02  点击参数"), matchWrap());

        LinearLayout positionRow = new LinearLayout(this);
        positionRow.setOrientation(LinearLayout.HORIZONTAL);
        xInput = numberInput("X", ClickConfig.getX(this));
        yInput = numberInput("Y", ClickConfig.getY(this));
        positionRow.addView(labeledInput("屏幕 X", xInput), weightedColumn(1f, 0, dp(5)));
        positionRow.addView(labeledInput("屏幕 Y", yInput), weightedColumn(1f, dp(5), 0));
        LinearLayout.LayoutParams rowParams = matchWrap();
        rowParams.topMargin = dp(12);
        configCard.addView(positionRow, rowParams);

        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        cpsInput = numberInput("1–50", ClickConfig.getCps(this));
        countInput = numberInput("0 = 无限", ClickConfig.getCount(this));
        speedRow.addView(labeledInput("CPS / 每秒点击", cpsInput), weightedColumn(1f, 0, dp(5)));
        speedRow.addView(labeledInput("点击次数", countInput), weightedColumn(1f, dp(5), 0));
        LinearLayout.LayoutParams speedParams = matchWrap();
        speedParams.topMargin = dp(10);
        configCard.addView(speedRow, speedParams);

        TextView hint = text("建议先用 10 CPS 测试。0 次表示持续点击，直到你在悬浮窗按暂停。", 13, COLOR_MUTED);
        hint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.topMargin = dp(12);
        configCard.addView(hint, hintParams);

        Button launchButton = primaryButton("保存并启动悬浮控制");
        LinearLayout.LayoutParams launchParams = matchWrap();
        launchParams.height = dp(54);
        launchParams.topMargin = dp(8);
        root.addView(launchButton, launchParams);
        launchButton.setOnClickListener(v -> launchOverlay());

        TextView footer = text("使用流程：开启两项权限 → 启动悬浮控制 → 拖动准星 → 开始点击", 13, COLOR_MUTED);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(14);
        root.addView(footer, footerParams);

        return scrollView;
    }

    private void launchOverlay() {
        if (!saveConfig(true)) {
            return;
        }
        hideKeyboard();
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "先允许悬浮窗权限", Toast.LENGTH_SHORT).show();
            openOverlayPermission();
            return;
        }
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "悬浮控制已启动，可返回桌面或打开目标应用", Toast.LENGTH_LONG).show();
        moveTaskToBack(true);
    }

    private boolean saveConfig(boolean showError) {
        if (xInput == null) {
            return true;
        }
        try {
            int x = Integer.parseInt(xInput.getText().toString().trim());
            int y = Integer.parseInt(yInput.getText().toString().trim());
            int cps = Integer.parseInt(cpsInput.getText().toString().trim());
            int count = Integer.parseInt(countInput.getText().toString().trim());
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
            x = ClickConfig.clamp(x, 0, metrics.widthPixels - 1);
            y = ClickConfig.clamp(y, 0, metrics.heightPixels - 1);
            cps = ClickConfig.clamp(cps, 1, 50);
            count = ClickConfig.clamp(count, 0, 1_000_000);
            ClickConfig.save(this, x, y, cps, count);
            xInput.setText(String.valueOf(x));
            yInput.setText(String.valueOf(y));
            cpsInput.setText(String.valueOf(cps));
            countInput.setText(String.valueOf(count));
            return true;
        } catch (NumberFormatException error) {
            if (showError) {
                Toast.makeText(this, "请填写有效的整数参数", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private void updatePermissionStatus() {
        boolean accessibilityEnabled = isAccessibilityEnabled();
        accessibilityStatus.setText(accessibilityEnabled ? "● 无障碍点击服务已开启" : "○ 无障碍点击服务未开启");
        accessibilityStatus.setTextColor(accessibilityEnabled ? COLOR_ACCENT : Color.rgb(255, 171, 112));
        boolean overlayEnabled = Settings.canDrawOverlays(this);
        overlayStatus.setText(overlayEnabled ? "● 悬浮窗权限已允许" : "○ 悬浮窗权限未允许");
        overlayStatus.setTextColor(overlayEnabled ? COLOR_ACCENT : Color.rgb(255, 171, 112));
    }

    private boolean isAccessibilityEnabled() {
        int enabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
        );
        if (enabled != 1) {
            return false;
        }
        String services = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (services == null) {
            return false;
        }
        ComponentName component = new ComponentName(this, ClickAccessibilityService.class);
        return services.contains(component.flattenToString()) || services.contains(component.flattenToShortString());
    }

    private void openOverlayPermission() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(rounded(COLOR_CARD, dp(18)));
        return layout;
    }

    private LinearLayout labeledInput(String label, EditText input) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = text(label, 12, COLOR_MUTED);
        column.addView(labelView, matchWrap());
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.height = dp(48);
        inputParams.topMargin = dp(5);
        column.addView(input, inputParams);
        return column;
    }

    private EditText numberInput(String hint, int value) {
        EditText input = new EditText(this);
        input.setText(String.valueOf(value));
        input.setHint(hint);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(Color.rgb(100, 119, 130));
        input.setTextSize(17);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(rounded(Color.rgb(13, 19, 25), dp(10)));
        return input;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.rgb(6, 38, 31));
        button.setAllCaps(false);
        button.setBackground(rounded(COLOR_ACCENT, dp(14)));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(COLOR_TEXT);
        button.setAllCaps(false);
        button.setBackground(rounded(Color.rgb(42, 55, 65), dp(11)));
        return button;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 13, COLOR_ACCENT);
        title.setLetterSpacing(0.08f);
        return title;
    }

    private TextView text(String value, float sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(14);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.height = dp(44);
        params.topMargin = dp(8);
        return params;
    }

    private LinearLayout.LayoutParams weightedColumn(float weight, int left, int right) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.leftMargin = left;
        params.rightMargin = right;
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

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
