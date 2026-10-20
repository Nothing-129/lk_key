# 蓝牙钥匙

面向已配对的兼容设备的 Android 蓝牙钥匙 App，当前版本 **1.9.3**。支持在 App、桌面小部件或 NFC 贴片入口中操作车辆，无需网络连接。

## 功能

- **NFC 贴片交替开关锁**：在“更多控制 → NFC 贴片”中写入可写 NDEF 标签。手机开启 NFC 和蓝牙、屏幕解锁后，碰本机写入的贴片会通过透明入口启动后台服务，连接并检查设备授权，再按本机最近确认的开关锁指令执行相反操作。首次默认开锁，设备确认后才切换；失败或无确认时保持记录。App 按钮和小部件的开关锁确认共用本地记录，设置页显示下次贴片操作，首页仅显示最近确认的开关锁本地记录。不拉起钥匙首页，进度和结果通过静音通知显示；设置页可以停用，写入过程不发送车辆指令。贴片只存本机触发标识，不存车辆凭证；卸载或清除 App 数据后需重新写入。1.9.0 已写入的贴片可直接使用，无需重写。NFC 为可选功能，不影响无 NFC 手机使用原有控制。
- **开关锁尾灯**：按用户视频近似还原每侧七组成对灯片的流水效果；暂按前段为开锁（两轮由内向外点亮，约 3.2 秒）、后段为关锁（全亮后由外向内收灭，下方灯带最后熄灭，约 2.45 秒）。收到设备确认后触发，与后视镜同步；开窗和后备箱指令不会触发该灯效。
- **离线 3D 车辆**：首页内嵌 Three.js 领克 03 模型，可拖动环视、双指缩放及复位视角。模型和 Draco 解码器随 APK 打包。设备确认指令后展示后视镜、车窗和后备箱动画；动画仅为指令反馈，不代表传感器读取的实际车况。后备箱协议只支持释放，因此重复点击不会展示电动关箱。WebGL 不可用时回退到原车图；离开页面暂停渲染。网页仅接受原生状态，没有调用蓝牙的接口。
- **开锁 / 关锁**：首页横向控制条，左关锁、右开锁；关锁同时发送一次 5 秒长按指令用于升窗。
- **3D 场景构图**：App 默认使用接近桌面小部件的低机位与车身比例，远山地平线随展示区高度对齐；地面复用小部件中无车辆的路面纹理，四轮接触阴影按轮胎实际位置绘制。独立 H5 保留原有默认视角。
- **分层抽屉首页**：品牌标志、随屏幕可用高度适配的 3D 展示、最近确认的开关锁本地记录和悬浮开关锁控制条。底部“更多控制”默认收起，展开后横向滚动显示开窗、关窗、后备箱、洗车和 NFC 贴片，点击 NFC 贴片直接进入设置页。抽屉展开和收起时保持车辆画布尺寸与背景构图固定，避免车辆和远山跳动；小屏可纵向滚动。
- **洗车模式**：读取状态、设置起止时间、停止，以及蓝牙重连后自动退出选项。弹窗、日期与时间选择器采用统一深色主题，区分安全提醒、主操作与停止操作。时段由设备执行。
- **点击重连**：断开连接时点击操作，会先连接并检查设备授权，再执行本次指令；连接失败后可手动重试。
- **连接提示**：右上角固定单行显示简短状态，不挤动车辆布局；点按状态可查看完整提示。
- **桌面小部件**：可调整大小的 4×2 小部件，支持后台连接、开锁和关锁升窗，并显示操作结果。
- **暮色界面**：主界面与小部件共用完整场景素材，保持较低的车辆视角、统一的光影与接地投影；图片等比缩放，天空和前景自然延伸至页面。暮光呼吸、流动薄雾、闪烁光点与地面微光更清晰可见；缓存的柔边遮罩让特效绕开车身与按钮，支持主题色应用图标。背景动画在离开页面、锁屏或窗口失去焦点时暂停，关闭系统动画时保持静态。

## 使用

1. 使用 Android 12 及以上、支持低功耗蓝牙的手机。
2. 先在原小程序完成设备授权，并确保手机已配对目标设备。
3. 安装后打开 App，授予“附近设备”权限，开启蓝牙并靠近车辆。
4. 点击对应按钮操作；需要桌面控制时，在小部件列表中添加本应用的小部件。

小部件操作期间可能显示前台服务通知。收到“设备已确认”仅表示设备接受了指令，车辆实际状态需现场确认。

### NFC 贴片设置

1. 先确认 App 手动开锁正常，再开启手机 NFC。
2. 打开“更多控制 → NFC 贴片”，点击“写入控制贴片”。写入会替换标签原有内容，使用空白或专用可写标签；1.9.0 已写入的贴片无需重写。
3. 将贴片靠近手机背面的 NFC 感应区并保持贴近，直到显示写入成功；已格式化的 NDEF 标签会现场回读验证。
4. 移开贴片，返回钥匙或桌面，在手机解锁状态下重新碰贴片。收到本次指令确认后，移开贴片再碰即可执行相反操作，无需固定等待；也可先移开贴片，等确认后再碰。执行期间的重复触碰被忽略且不排队，失败后移开再碰可重试。首次出现系统 NFC 启动许可时允许该 App；各厂商系统的弹窗或后台限制需实机确认。
5. 在车辆旁验证后再固定贴片；金属位置使用抗金属标签，车外使用适合室外环境的封装。

NFC 入口只接受实时读取的、本机启用的控制标签。URL或伪造启动 Intent 本身不能操作车辆；读取后通过进程内一次性票据移交未导出的 NFC 后台服务。使用 `NfcAdapter.ignore` 的移出回调与本次 BLE 操作结束两个条件控制下次触发，250 ms 用于移出检测防抖；持续贴着或执行中重复碰不会再次操作。被忽略的过早触碰也需移开再碰。一次性票据的 5 秒有效期仅用于防止启动信息过期，不是操作间隔。NFC 使用 connectedDevice 前台服务持有独立 BLE 监听到确认或失败，完成后释放连接和服务。锁屏/息屏操作不作为支持范围。多张贴片可使用同一本机标识；其他手机需自行设置，设置页停用会停用本机全部贴片入口。

静默操作仍要求手机解锁、NFC 与蓝牙开启。已有贴片无需重写，系统 NFC 提示或启动许可由手机控制。首次可在“NFC 贴片 → 开启操作结果通知”中授权通知；未授权不影响后台操作，但可能看不到进度和结果。结果通知保留约一分钟，点击通知才会打开钥匙首页。透明入口使用独立任务，避免将后台的钥匙首页带到前台。

本地记录不是车辆实际锁状态。实体钥匙、其他手机、车辆自动锁车和洗车等操作可能使记录与车辆不一致。仅本机收到普通开锁/关锁升窗指令的成功回复时更新记录；开窗、关窗、后备箱、时间同步和洗车回复不参与交替。收到成功回复也只代表设备确认指令。“最多两次”只适用于连续两次指令确实执行成功、期间没有其他操作改变车锁状态的情况；连接超时、未确认或重复触碰被忽略时不成立。

NFC 验证（设备测试只打开设置页和测试隔离的标签/票据，不连接蓝牙、不发送车辆指令）：

```sh
javac -d build/test app/src/com/zcy/lkkey/NfcTrigger.java tests/NfcTriggerTest.java
java -cp build/test com.zcy.lkkey.NfcTriggerTest
javac -d build/test app/src/com/zcy/lkkey/LocalLockState.java tests/LocalLockStateTest.java
java -cp build/test com.zcy.lkkey.LocalLockStateTest
javac -d build/test app/src/com/zcy/lkkey/NfcTapGate.java tests/NfcTapGateTest.java
java -cp build/test com.zcy.lkkey.NfcTapGateTest
./tests/build-device-test.sh
adb install --no-incremental -r build/device-test.apk
adb shell am instrument -w -e mode nfc com.zcy.lkkey.test/.DeviceTest
```

真实贴片的写入、系统分发到 App 和实车开锁仍需现场确认。

1.9.2 已通过离线触碰状态机测试及 Android 15 模拟器 NFC 集成测试：操作结束与贴片移出的两种先后顺序、持续贴住、过早重碰、不排队、过期移出回调、失败重试、本地记录持久化、一次性票据，以及隔离模拟 BLE 回复的匹配/截断/重复/失败和完成回调顺序。模拟器验证不连接蓝牙、不发送车辆指令，也无法验证真实 NFC 天线的移出检测；该部分需手机现场验证。移出检测使用 Android 官方 [`NfcAdapter.ignore`](https://developer.android.com/reference/android/nfc/NfcAdapter#ignore(android.nfc.Tag,int,android.nfc.NfcAdapter.OnTagRemovedListener,android.os.Handler))。

1.9.3 静默 NFC 已在 Android 15 模拟器验证：由独立任务中的透明 NFC 入口异步转交私有前台服务，外部系统设置页面保持前台，未启动钥匙首页；伪造及重放票据被拒绝，缺少蓝牙权限时通过无声音、无振动通知报告失败，保留本地记录并停止服务。验证只在模拟器中模拟已验证标签的内部交接，不写贴片、不建立蓝牙连接、不发送车辆指令；小米实机的后台执行和通知显示仍需现场验证。前台服务按 Android 的 [connectedDevice 类型](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device)执行。

静默服务测试仅运行于模拟器，测试前撤销模拟器蓝牙权限以保证不会连接车辆：

```sh
adb -s emulator-5562 shell pm revoke com.zcy.lkkey android.permission.BLUETOOTH_CONNECT
adb -s emulator-5562 shell pm grant com.zcy.lkkey android.permission.POST_NOTIFICATIONS
adb -s emulator-5562 shell am instrument -w -e mode nfc-silent com.zcy.lkkey.test/.DeviceTest
```

洗车模式运行时车辆会解锁，离车不会自动锁车，请保管好车内物品。修改洗车设置前需先成功读取设备状态。

## 构建与安装

现有构建脚本适用于 macOS，使用 Homebrew OpenJDK 17、Android SDK 35 和 Build Tools 35.0.0。默认 SDK 路径为 `$HOME/Library/Android/sdk`，JDK 路径为 `/opt/homebrew/opt/openjdk@17`；其他安装路径可通过 `JAVA_HOME` 和 `ANDROID_HOME`（或 `ANDROID_SDK_ROOT`）指定。

```sh
cd h5-car-key && npm ci && cd ..
./build.sh
adb install --no-incremental -r build/lk-key.apk
```

构建还需要 Node.js/npm。`build.sh` 先构建独立的 Android 车辆展示入口到 `app/assets/vehicle3d/`，再打包 APK；该目录为生成产物。原独立 H5 页面仍可单独运行，App 保留原生控制按钮、蓝牙逻辑和洗车弹窗。桌面小部件继续使用静态车辆场景。

输出为本地调试签名 APK，构建产物位于 `build/`。应用签名固定保存在 `~/.android/lk-key/debug.keystore`，也可用 `LK_KEYSTORE_PATH` 指定；首次使用会迁移已有的 `build/debug.keystore`。签名文件不提交到仓库，换电脑时需单独复制。缺失签名时构建会停止，不会自动生成不同的签名。

## 开发与验证

- `app/src/com/zcy/lkkey/`：原生 Java 界面、蓝牙通信、协议及小部件。
- `app/res/`：布局、图标及车辆场景素材。
- `tests/`：离线协议测试及可选设备测试组件。
- `designs/`：界面设计参考与生成记录。
- `h5-car-key/android.html`、`src/android.js`：App 专用车辆展示入口。
- `Vehicle3DView.java`、`VehicleVisualState.java`：本地 WebView 资源映射、生命周期、静态回退及确认指令到动画的映射。

协议基于原小程序静态分析，使用 FFE0 服务、FFE2 写入和 FFE1 通知。连接后同步时间并检查设备授权；命令包含时间与 MD5 校验。不导出微信凭证、不重置配对，也不申请网络权限。

离线测试不连接车辆：

```sh
mkdir -p build/test
/opt/homebrew/opt/openjdk@17/bin/javac -d build/test app/src/com/zcy/lkkey/Protocol.java app/src/com/zcy/lkkey/WashMode.java tests/ProtocolTest.java tests/WashModeTest.java
/opt/homebrew/opt/openjdk@17/bin/java -cp build/test ProtocolTest
/opt/homebrew/opt/openjdk@17/bin/java -cp build/test WashModeTest
```

动画状态映射与尾灯时序测试：

```sh
javac -d build/test app/src/com/zcy/lkkey/VehicleVisualState.java tests/VehicleVisualStateTest.java
java -cp build/test com.zcy.lkkey.VehicleVisualStateTest
node h5-car-key/scripts/test-tail-lights.mjs
```

模拟器专用 3D 集成测试（只模拟确认回调，不发送车辆指令）：

```sh
./tests/build-device-test.sh
adb install --no-incremental -r build/device-test.apk
adb shell am instrument -w -e mode 3d com.zcy.lkkey.test/.DeviceTest
```

只检查主界面场景、不点击车辆控制的设备测试（首次安装测试组件需手机允许）：

```sh
./tests/build-device-test.sh
adb install --no-incremental -r build/device-test.apk
adb shell am instrument -w -e mode scene com.zcy.lkkey.test/.DeviceTest
```

该测试检查主界面复用相同的小部件素材、不同宽度下图片不被拉伸，以及特效遮罩保护车身与按钮而不裁掉两侧暮光。

模拟器上可用 `-e mode wash-style` 检查洗车弹窗的深色样式、读取前禁用编辑、日期与时间选择器和关闭清理，并保存截图；该模式不会点击保存或停止按钮。

已有验证记录覆盖构建与签名、离线协议、模拟器界面及小部件流程，并曾在实车确认普通开锁和普通关锁。关锁升窗、开窗、后备箱、洗车模式，以及实机小部件后台控制和断线重连执行，仍需现场验证；其他设备或固件的兼容性尚未确认。

1.8.1 的离线 3D 已在 Android 15 模拟器（硬件加速、WebView 124）验证：本地 GLB/Draco 加载、模拟确认回调驱动后视镜/车窗/后备箱与两套开关锁尾灯时序、重复释放后备箱不误作关箱、外部请求阻断和静态图片回退。此测试未发送车辆指令；手机实际帧率及实车联动仍待验证。
