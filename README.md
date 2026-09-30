# 杨公风水罗盘（Android）

一款参照传统杨公盘圈层绘制的 Android 风水罗盘应用：磁北定向、内盘手动旋转叠针、石头摩擦音效、坐向双定位与「山向解读」。

> 免责声明：罗盘测向功能真实可用（磁北）；应用内的九运吉凶等内容仅为**传统民俗说法，没有科学依据**，仅供参考娱乐。现实选房请优先采光、通风、噪音、户型、消防、地质安全。

## 目录结构

```
yanggong-luopan-apk/
├── AndroidManifest.xml            # 清单（minSdk 21 / targetSdk 34，无任何权限）
├── yanggong-fengshui-luopan.apk   # 已签名的成品 APK（v1.38，可直接安装）
├── java/com/fengshui/luopan/
│   ├── MainActivity.java          # 入口：传感器采集 + 使用说明/山向解读弹窗
│   └── LuopanView.java            # 核心：13 层盘面绘制 + 磁针 + 水平仪 + 触控
├── res/
│   ├── values/                    # strings.xml（应用名）、styles.xml（主题）
│   ├── raw/stone_drag.wav         # 内盘旋转石头摩擦音效（2.2s 无缝循环）
│   └── mipmap-*/ic_launcher.png   # 启动图标（山鬼花钱，5 档密度）
└── scripts/
    ├── build.ps1                  # 一键构建脚本（无 Gradle 手工管线）
    └── release.keystore           # 签名密钥（storepass/keypass 均为 luopan2026，alias=luopan）
```

## 功能特性

- **13 层圈层**（自外向内）：360° 度数刻度（四正位红字北东南西）→ 六十四卦（伏羲先天圆图）→ 洛书九星配宫 → 二十八宿（角起辰巽）→ 天盘缝针 24 山（顺错 7.5°）→ 人盘中针 24 山（逆错 7.5°）→ 地盘正针 24 山（红大字）→ 七十二龙（龟甲空亡 + 纳音五色）→ 一百二十分金（细刻度）→ 地支环 → 天干环 → 八卦（三线爻符+卦名）→ 天池
- **天池**：白底、红色子午基准线 + 两颗对称红点，红头黑尾三角磁针恒指磁北
- **叠针/纳针**：按住盘面拖动旋转内盘（外盘+天池一体），把海底线对齐磁针；双击回正；松手偏移 ≥150° 自动转正（红针=子）
- **交互细节**：石头摩擦音效音量随转速变化、右下角圆形水平仪（居中变绿）、磁场偏弱提示、屏幕常亮
- **坐向双定位**：底栏「坐X山 · 向Y山」+ 度数/方位/吉凶速览
- **山向解读弹窗**（左下角按钮）：九运理气、七十二龙（落龙/空亡/纳音）、一百二十分金（丙丁可用/甲乙戊不用）、正向与兼向判定、24 组宅向对照表
- **性能**：盘面静态内容一次渲染成位图缓存，每帧仅旋转贴图，低端机流畅

## 构建方法

### 依赖

| 工具 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | javac 编译（-source/-target 1.8） |
| Android build-tools | 34 | aapt2 / d8 / zipalign / apksigner |
| android.jar | platform-34 | 编译 classpath |
| PowerShell | 5.1+ | 构建脚本 |

默认 SDK 路径在 `scripts/build.ps1` 头部：`C:\android-build\android-14`（build-tools）与 `C:\android-build\android-34\android.jar`，按需修改。

### 一键构建

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1
```

管线：`aapt2 compile` → `aapt2 link` → `javac` → `d8` → 追加 classes.dex → `zipalign` → `apksigner`（v1+v2）。产物输出到 `build\luopan-v1.38.apk`（脚本内自动打印 BUILD-OK 路径）。

### 签名

使用仓库内 `scripts/release.keystore`（测试密钥，密码 `luopan2026`，别名 `luopan`）。**正式发布前请务必更换为自己的密钥**：

```powershell
keytool -genkeypair -keystore my-release.keystore -alias <你的别名> -keyalg RSA -keysize 2048 -validity 10950
```

并修改 `scripts/build.ps1` 中对应的 `--ks` / `--ks-key-alias` / 密码参数。

### 直接安装

不想自己编译的话，仓库里附带已签名的 `yanggong-fengshui-luopan.apk`（v1.38，623KB），Android 5.0+ 手机直接安装即可（首次需允许"未知来源"）。

## 版本历史（v1.0 → v1.38 摘要）

| 版本 | 主要变化 |
| --- | --- |
| v1.0 | 初版：单层二十四山 + 六十四卦 + 九星 + 二十八宿 + 八卦爻线 + 天池 |
| v1.1–1.4 | 天干地支环、天池样式迭代、八卦三线爻符 |
| v1.5 | 内置「使用说明」弹窗（叠针/纳针教学） |
| v1.6–1.7 | 圆形水平仪、海底线移入天池 |
| v1.8–1.9 | 天池基准线网（按用户参考图）、外圈四正标注 |
| v1.10–1.13 | 二十四山详细用法文档、内盘手动旋转、宅向对照表 |
| v1.14–1.16 | 九运吉凶简评、坐向双定位、山向解读弹窗 |
| v1.17–1.24 | 篆书标题、山鬼花钱图标、石门机关音效多轮调优 |
| v1.25–1.29 | 用户上传音效替换、叠针防呆自动转正、天池精简 |
| v1.30–1.33 | 天心十道贯穿盘心、全盘文字排序勘误、三层二十四山 + 七十二龙 + 一百二十分金 |
| v1.34–1.38 | 标题「楊公風水羅盤」、分金细刻度、山向解读扩展（龙/分金/正兼向）、叠针自动转正 |

## License

仅供学习与个人使用。
