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

「山向解读」弹窗新增「圈层详解（由外向内）」一节，13 条逐层解读：

* 圈层	解读要点（均动态引用当前坐向）
* 度数刻度环	360° 标尺、四正位定义，精确记录度数
* 六十四卦层	伏羲先天圆图，复起子位顺时针至乾南，玄空大卦用途
* 九星数字环	洛书数配八宫对照，标注当前坐山属哪一宫（如“一白坎宫”）
* 二十八宿	28 宿、角起辰巽逆时针、天星派用途
* 天盘缝针	顺错半位 7.5°，双山五行属纳水，看水流用此层
* 人盘中针	逆错半位 7.5°，属消砂，看山峰用此层
* 地盘正针	红大字定向基准层，标注“当前坐 X 山”
* 七十二龙	三龙一山、龟甲空亡、六十甲子纳音——标注当前向线压哪条龙、纳音何属性
* 一百二十分金	五分金只取丙丁、避甲乙戊的原理
* 地支环	标注当前坐山属地支哪一系
* 天干环	甲起 75°、戊己不入盘的排布原理
* 八卦层	后天八卦镇方 + 当前坐山所属宫卦
* 天池	子午基准线与磁针、叠针定位起点
* 样式为金色◆标题 + 灰白正文，位于「正向/兼向判定」之后、「二十四山对照表」之前，转动手机或拖动内盘后重新点开，解读内容会随坐向实时变化。
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

## 版本历史（v1.0 → v1.39 摘要）

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
| v1.38–1.39 | 山向解读弹窗现在包含你要的全部 11 个圈层的详细解读，且每条都结合当前定位出的坐向动态生成.

## License

仅供学习与个人使用。
