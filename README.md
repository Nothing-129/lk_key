# 蓝牙钥匙

面向已配对的兼容设备的 Android 蓝牙钥匙 App，当前版本 **1.7.2**。支持在 App 或桌面小部件中操作车辆，无需网络连接。

## 功能

- **开锁 / 关锁**：首页横向控制条，左关锁、右开锁；关锁同时发送一次 5 秒长按指令用于升窗。
- **更多控制**：开窗、关窗、后备箱和洗车模式。
- **洗车模式**：读取状态、设置起止时间、停止，以及蓝牙重连后自动退出选项。时段由设备执行。
- **点击重连**：断开连接时点击操作，会先连接并检查设备授权，再执行本次指令；连接失败后可手动重试。
- **桌面小部件**：可调整大小的 4×2 小部件，支持后台连接、开锁和关锁升窗，并显示操作结果。
- **暮色界面**：深色车库背景与车辆场景，支持主题色应用图标。

## 使用

1. 使用 Android 12 及以上、支持低功耗蓝牙的手机。
2. 先在原小程序完成设备授权，并确保手机已配对目标设备。
3. 安装后打开 App，授予“附近设备”权限，开启蓝牙并靠近车辆。
4. 点击对应按钮操作；需要桌面控制时，在小部件列表中添加本应用的小部件。

小部件操作期间可能显示前台服务通知。收到“设备已确认”仅表示设备接受了指令，车辆实际状态需现场确认。

洗车模式运行时车辆会解锁，离车不会自动锁车，请保管好车内物品。修改洗车设置前需先成功读取设备状态。

## 构建与安装

现有构建脚本适用于 macOS，使用 Homebrew OpenJDK 17、Android SDK 35 和 Build Tools 35.0.0。默认 SDK 路径为 `$HOME/Library/Android/sdk`，JDK 路径为 `/opt/homebrew/opt/openjdk@17`；其他环境需调整脚本路径。

```sh
./build.sh
adb install --no-incremental -r build/lk-key.apk
```

输出为本地调试签名 APK。构建产物和签名文件位于 `build/`，不提交到仓库。

## 开发与验证

- `app/src/com/zcy/lkkey/`：原生 Java 界面、蓝牙通信、协议及小部件。
- `app/res/`：布局、图标及车辆场景素材。
- `tests/`：离线协议测试及可选设备测试组件。
- `designs/`：界面设计参考与生成记录。

协议基于原小程序静态分析，使用 FFE0 服务、FFE2 写入和 FFE1 通知。连接后同步时间并检查设备授权；命令包含时间与 MD5 校验。不导出微信凭证、不重置配对，也不申请网络权限。

离线测试不连接车辆：

```sh
mkdir -p build/test
/opt/homebrew/opt/openjdk@17/bin/javac -d build/test app/src/com/zcy/lkkey/Protocol.java app/src/com/zcy/lkkey/WashMode.java tests/ProtocolTest.java tests/WashModeTest.java
/opt/homebrew/opt/openjdk@17/bin/java -cp build/test ProtocolTest
/opt/homebrew/opt/openjdk@17/bin/java -cp build/test WashModeTest
```

已有验证记录覆盖构建与签名、离线协议、模拟器界面及小部件流程，并曾在实车确认普通开锁和普通关锁。关锁升窗、开窗、后备箱、洗车模式，以及实机小部件后台控制和断线重连执行，仍需现场验证；其他设备或固件的兼容性尚未确认。
