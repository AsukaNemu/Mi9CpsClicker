# 米9 连点器（Mi9 CPS Clicker）

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/AsukaNemu/Mi9CpsClicker?display_name=tag)](https://github.com/AsukaNemu/Mi9CpsClicker/releases/latest)
[![Device](https://img.shields.io/badge/device-Xiaomi%20Mi%209%20%2F%20cepheus-FF6900)](https://github.com/AsukaNemu/Mi9CpsClicker)

专为小米 9（`cepheus`）制作的轻量级单点 CPS 连点器。通过 Android 无障碍手势执行点击，提供桌面应用入口、可拖动悬浮准星以及悬浮开始/暂停控制；日常运行不依赖 Root。

> [下载最新 APK](https://github.com/AsukaNemu/Mi9CpsClicker/releases/latest) · 当前版本：`v1.0.0`

## 项目信息

| 项目 | 说明 |
| --- | --- |
| 主要设备 | Xiaomi Mi 9 / cepheus |
| 实机验证 | Android 11（API 30）、1080 × 2340、440 dpi |
| 最低系统 | Android 7.0（API 24） |
| Target SDK | Android 15（API 35） |
| 点击模式 | 单点循环点击 |
| 点击速度 | 1–50 CPS |
| 点击次数 | 1–1,000,000，填写 `0` 为无限循环 |
| Root | 不需要 |

## 功能

- 桌面/应用列表中的“米9 连点器”入口
- 深色参数设置页
- 可拖动的绿色悬浮准星
- 1–50 CPS 速度调节
- 有限次数与无限循环
- 悬浮开始、暂停、关闭按钮
- 点击进度显示
- 前台通知，便于返回设置页
- 点击开始后准星自动变为不可触摸状态，使手势落到下方目标应用

## 安装

1. 前往 [Releases](https://github.com/AsukaNemu/Mi9CpsClicker/releases/latest) 下载 `Mi9-CPS-Clicker-v1.0.0.apk`。
2. 在手机上允许当前文件管理器或浏览器“安装未知应用”。
3. 安装 APK。
4. 从桌面或应用列表打开“米9 连点器”。

也可以在连接 ADB 后安装：

```bash
adb install -r Mi9-CPS-Clicker-v1.0.0.apk
```

## 首次设置

应用需要两项由用户手动开启的系统权限：

1. 点击“打开无障碍设置”，启用“米9 连点器点击服务”。
2. 点击“允许悬浮窗权限”，允许应用显示在其他应用上层。

完成后回到主界面，两项状态会显示为绿色。

## 使用方法

1. 输入点击位置、CPS 和点击次数。
2. 点击“保存并启动悬浮控制”。
3. 返回桌面或进入目标应用。
4. 将绿色准星拖到需要点击的位置。
5. 在悬浮面板中点击“开始点击”。
6. 需要停止时点击“暂停点击”或“关闭悬浮控制”。

参数说明：

| 参数 | 范围 | 说明 |
| --- | --- | --- |
| X / Y | 屏幕有效坐标 | 可以直接输入，也可以通过拖动准星调整 |
| CPS | 1–50 | 每秒期望点击次数，建议先从 10 CPS 测试 |
| 点击次数 | 0–1,000,000 | `0` 表示持续点击，直到手动暂停 |

## 权限与隐私

| 权限/服务 | 用途 |
| --- | --- |
| 无障碍服务 | 发送用户配置的点击手势 |
| 悬浮窗 | 显示准星和开始/暂停面板 |
| 前台服务 | 保持悬浮控制在切换应用后继续运行 |
| 通知 | 显示前台服务状态并返回设置页 |

- 无障碍配置明确设置为 `canRetrieveWindowContent=false`，应用不读取其他应用的窗口内容。
- Manifest 未声明网络权限，应用不会上传点击坐标或使用数据。
- 应用不会请求、调用或依赖 Root 权限。
- 设置只保存在本机的 `SharedPreferences` 中。

## 兼容性与限制

- 已针对小米 9 / Android 11 实机编译、安装和服务注册验证。
- 点击由 Android `AccessibilityService.dispatchGesture()` 执行，实际可达到的 CPS 会受系统负载、ROM 和目标应用影响。
- 部分游戏、银行应用或使用特殊渲染/安全策略的应用可能拒绝无障碍手势。
- 锁屏或屏幕关闭时不会按预期点击。
- 当前版本只支持单点点击，不包含多点序列、滑动或脚本录制。
- 在游戏或第三方平台中使用前，请确认符合相应服务条款。

## MIUI 排错

如果悬浮窗消失或点击没有反应，请依次检查：

1. 无障碍服务是否仍处于启用状态。
2. “显示在其他应用上层”权限是否开启。
3. MIUI 电量管理是否限制了本应用的后台运行。
4. 准星是否位于系统导航栏、通知栏或悬浮面板自身上方。
5. 将 CPS 降到 10 后重新测试。

更新或重装 APK 后，MIUI 可能要求重新确认无障碍与悬浮窗权限。

## 从源码构建

要求：

- JDK 17
- Android SDK Platform 36
- Android Build Tools 36.0.0 或兼容版本

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

Linux / macOS：

```bash
./gradlew :app:assembleDebug
```

构建结果位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 技术实现

- Java 17
- Android 原生 View UI，无第三方运行时依赖
- `AccessibilityService` 顺序派发点击手势
- `TYPE_APPLICATION_OVERLAY` 悬浮准星与控制面板
- 前台 `Service` 保持悬浮控制生命周期
- `SharedPreferences` 保存坐标、CPS 与次数

## v1.0.0 校验值

```text
SHA-256
F00D2609348B85FC78F9AA6A6724F9AA2975677DE320109306D2859914218B70
```

## 许可证

当前仓库未附加开源许可证。代码可供查看与个人测试；未经许可，请勿重新分发、销售或以其他项目名义发布。
