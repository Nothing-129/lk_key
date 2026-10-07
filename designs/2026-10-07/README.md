# 蓝牙钥匙：四套视觉方案

日期：2026-10-07。用途：供用户选择视觉方向，尚未应用到 Android App 或安装到手机。

每张设计图左侧为 App 首页，右侧为匹配的桌面小部件。图中的连接状态仅为界面示例，不代表当前车辆连接或锁车状态。

| 方案 | 文件 | 设计方向 |
| --- | --- | --- |
| A 曜石黑 | A-obsidian.png | 深色车库、冷光线条、双区操作台 |
| B 瓷白极简 | B-porcelain.png | 暖白留白、黑白圆形按钮、简洁列表 |
| C 钛灰按键 | C-titanium.png | 哑光金属、实体按键质感、小部件竖向按键 |
| D 冰蓝轻透 | D-ice-blue.png | 浅蓝卡片、上下胶囊按钮、小部件双竖键 |

## 沿用的功能约束

- 首页突出开锁、关锁；“关”继续关锁并升窗。
- 开窗、关窗、后备箱、洗车处于次级区域。
- 小部件仅提供开、关，点击后后台执行并在桌面显示结果。
- 车辆图以用户已选定的灰色领克 03 素材为参考。
- 不增加续航、油量、空调、引擎等尚未实现的车辆能力。
- 图中的装饰、投影、细小文字和图标为视觉探索。实施时沿用原始 car_render.png，保证车型一致；用原生文字和统一图标重建界面，并校正窗口图标方向。连接信息保持低调。

## GitHub 视觉参考

以下仓库的公开 README 和截图已查看。这些方案借鉴布局和视觉语言，未复制其代码或车辆素材。

- [Hamad-Anwar/Car-Controller-Getx-Flutter](https://github.com/Hamad-Anwar/Car-Controller-Getx-Flutter)：车辆主视觉、主要操作和次级控制分组。
- [enoch-aik/tesla_app](https://github.com/enoch-aik/tesla_app)：深色面板、柔和层次、凸起圆形控制的质感。
- [saqrelfirgany/Tesla-Flutter-App](https://github.com/saqrelfirgany/Tesla-Flutter-App)：深色背景、细线图标和车辆周围的直接操作。

## 生成记录

使用内置 image_gen 工具，四套分别生成。输入参考为 app/res/drawable-nodpi/car_render.png。完整最终提示词保存在 prompts.json。应用代码、蓝牙行为与已安装版本未修改。

