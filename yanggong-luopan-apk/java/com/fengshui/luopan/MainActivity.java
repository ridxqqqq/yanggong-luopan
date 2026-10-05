package com.fengshui.luopan;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import java.util.Locale;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 风水罗盘主界面：采集罗盘传感器（旋转矢量优先，加速度+地磁兜底），
 * 得到手机指向的磁北方位角交给 LuopanView 渲染。
 * 右上角"使用说明"按钮弹出注解弹窗；首次启动自动弹一次。
 */
public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private LuopanView view;
    private boolean useRotationVector;

    // 旋转矢量低通滤波状态（四元数连续，无 359<->0 跳变问题）
    private final float[] rotVecFilt = new float[4];
    private boolean haveRot;

    // 加速度+地磁兜底
    private final float[] gravity = new float[3];
    private final float[] geomag = new float[3];
    private boolean hasGravity, hasMag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        view = new LuopanView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(buildHelpButton(), buildHelpButtonLayout());
        root.addView(buildLiQiButton(), buildLiQiButtonLayout());
        setContentView(root);

        if (!getSharedPreferences("luopan", MODE_PRIVATE)
                .getBoolean("help_shown_v1", false)) {
            showHelp();
            getSharedPreferences("luopan", MODE_PRIVATE)
                    .edit().putBoolean("help_shown_v1", true).apply();
        }
    }

    // ================= 使用说明弹窗 =================

    private TextView buildHelpButton() {
        TextView btn = new TextView(this);
        btn.setText("ⓘ 使用说明");
        btn.setTextColor(0xFFE8C86E);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB314141A);
        bg.setCornerRadius(dp(16));
        bg.setStroke(1, 0xFF4A4A55);
        btn.setBackground(bg);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelp();
            }
        });
        return btn;
    }

    private FrameLayout.LayoutParams buildHelpButtonLayout() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.topMargin = dp(34);
        lp.rightMargin = dp(12);
        return lp;
    }

    // ================= 坐向理气弹窗 =================

    private TextView buildLiQiButton() {
        TextView btn = new TextView(this);
        btn.setText("山向解读");
        btn.setTextColor(0xFFE8C86E);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB314141A);
        bg.setCornerRadius(dp(14));
        bg.setStroke(1, 0xFF4A4A55);
        btn.setBackground(bg);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLiQi();
            }
        });
        return btn;
    }

    private FrameLayout.LayoutParams buildLiQiButtonLayout() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.START;
        lp.bottomMargin = dp(16);
        lp.leftMargin = dp(12);
        return lp;
    }

    /** 按当前定位出的坐向，显示对应的九运理气吉凶说明与二十四山对照 */
    private void showLiQi() {
        int zuo = view.getZuoIndex();
        int xiang = view.getXiangIndex();

        float eff = view.getEffectiveAzimuth();
        int longIdx = view.getLongIndex();
        boolean longVoid = LuopanView.isVoidDragon(longIdx);
        int ki = LuopanView.jiaziIndexForDragon(longIdx);
        String longName = LuopanView.JIAZI60[ki];
        String longNayin = LuopanView.NAYIN[ki / 2];
        int fj = view.getFenJinIndex();
        int fjPos = fj % 5;
        String fjName = new String[]{"甲", "乙", "丙", "丁", "戊"}[fjPos];
        boolean fjOk = (fjPos == 2 || fjPos == 3);
        float diff = eff - xiang * 15f;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        float adiff = Math.abs(diff);
        String zheng = adiff <= 3f ? "正向" : (adiff <= 6f ? "兼向" : "近界缝（空亡边缘）");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(16));

        TextView cur = new TextView(this);
        cur.setText(String.format("坐%s山 · 向%s山", LuopanView.MOUNTAINS[zuo], LuopanView.MOUNTAINS[xiang]));
        cur.setTextColor(0xFFE8C86E);
        cur.setTextSize(20f);
        cur.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        cur.setPadding(0, dp(6), 0, dp(2));
        box.addView(cur);

        box.addView(bullet("俗称：" + LuopanView.MOUNTAINS[zuo] + "山" + LuopanView.XIANG[zuo] + "向"));
        TextView jx = new TextView(this);
        jx.setText("吉凶：" + LuopanView.LI_JIXIONG[zuo]);
        jx.setTextColor(liQiColor(LuopanView.LI_JIXIONG[zuo]));
        jx.setTextSize(14f);
        jx.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        jx.setPadding(dp(6), dp(2), 0, dp(2));
        box.addView(jx);
        box.addView(bullet("方位：" + LuopanView.LI_FANGWEI[zuo]));
        box.addView(bullet("理气简评：" + LuopanView.LI_YAODIAN[zuo]));
        box.addView(note("九运（2024–2043）民俗简评，仅供参考；正向口径，兼向另论；风水吉凶无科学依据。"));

        box.addView(section("七十二龙解读"));
        if (longVoid) {
            box.addView(bullet("向线正压【龟甲空亡】（" + LuopanView.MOUNTAINS[xiang]
                    + "山正中 5°），六十甲子不到此线，传统认为不可定向，宜前后挪动重测。"));
        } else {
            box.addView(bullet("向线落在【" + longName + "】龙（纳音" + longNayin
                    + "），为" + LuopanView.MOUNTAINS[xiang] + "山三龙之一，可依纳音五行论生克。"));
        }
        box.addView(bullet("七十二龙每山三龙、各 5°，正中一线为龟甲空亡不用，余六十甲子跳空顺排。"));

        box.addView(section("一百二十分金解读"));
        if (fjOk) {
            box.addView(bullet("向线正压【" + fjName + "分金】，属每山五分金中的可用吉线，宜立向。"));
        } else if (fjPos == 4) {
            box.addView(bullet("向线正压【戊分金】，空亡不用之线，宜前后微调避开。"));
        } else {
            box.addView(bullet("向线正压【" + fjName + "分金】，孤虚不用，传统避之，宜微调至丙丁分金。"));
        }
        box.addView(bullet("一百二十分金每山五分金（甲乙丙丁戊），丙丁居中可用，甲乙与戊避之。"));

        box.addView(section("正向 / 兼向判定"));
        box.addView(bullet(String.format(Locale.US, "距向山%s中心线 %.1f°，判定：%s。",
                LuopanView.MOUNTAINS[xiang], adiff, zheng)));
        box.addView(bullet("≤3° 为正向；3–6° 为兼向（如子兼癸）；6–7.5° 近界缝犯空亡，宜重测。"));

        box.addView(section("圈层详解（由外向内）"));
        for (String[] l : ringGuide(zuo, xiang)) {
            box.addView(ringBullet(l[0], l[1]));
        }

        box.addView(section("二十四山坐向理气对照"));
        for (int i = 0; i < 24; i++) {
            String prefix = (i == zuo) ? "▶ " : "";
            TextView tv = bullet(String.format(Locale.US, "%d. %s山%s向 · %s · %s",
                    i + 1, LuopanView.MOUNTAINS[i], LuopanView.XIANG[i],
                    LuopanView.LI_JIXIONG[i], LuopanView.LI_YAODIAN[i]));
            if (i == zuo) {
                tv.setTextColor(0xFFE8C86E);
                tv.setText(prefix + tv.getText());
            }
            box.addView(tv);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("山向解读 · 九运理气与分金（民俗参考）")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    /** 圈层详解：逐层说明，含当前坐向在每层的对应内容 */
    private java.util.List<String[]> ringGuide(int zuo, int xiang) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        int zhi = Math.round(zuo * 15f / 30f) % 12;
        String zhiName = LuopanView.ZHI12[zhi];
        String mountain = LuopanView.MOUNTAINS[zuo];
        String starName = new String[]{"一白坎", "八白艮", "三碧震", "四绿巽",
                "九紫离", "二黑坤", "七赤兑", "六白乾"}[zuo % 8];
        int longIdx = view.getLongIndex();
        int ki = LuopanView.jiaziIndexForDragon(longIdx);
        String longName = LuopanView.JIAZI60[ki];
        String longNayin = LuopanView.NAYIN[ki / 2];

        out.add(new String[]{"度数刻度环（外圈）",
                "现代 360° 角度标尺，正北 0°、正东 90°、正南 180°、正西 270°，每 30° 标数。用于精确记录坐向度数。"});
        out.add(new String[]{"六十四卦层（伏羲先天圆图）",
                "每卦 5.625°，复卦起子位顺时针，经东方至乾居南，姤卦续起，坤终北位。用于玄空大卦择日与卦理断事。"});
        out.add(new String[]{"九星数字环（洛书配宫）",
                "洛书数配后天八卦：1坎北、8艮东北、3震东、4巽东南、9离南、2坤西南、7兑西、6乾西北。当前坐山属【" + starName + "】宫，玄空飞星以此宫排盘。"});
        out.add(new String[]{"二十八宿层",
                "角亢氐房…井鬼柳星张翼轸共 28 宿，每宿约 12.86°，角宿起辰巽（135°）逆时针排布。天星派用于消砂纳水与择日。"});
        out.add(new String[]{"天盘缝针（二十四山）",
                "整体顺时针错半位 7.5°（子山对地盘子癸之界），双山五行属【纳水】——看水流来去定吉凶，以缝针为准。"});
        out.add(new String[]{"人盘中针（二十四山）",
                "整体逆时针错半位 7.5°（子山对地盘壬子之界），双山五行属【消砂】——看山峰位置定生克，以中针为准。"});
        out.add(new String[]{"地盘正针（二十四山）",
                "红色大字，每山 15°，子正北起。这是【格龙定向】的基准层：叠针后十字竖线所指即坐向，底栏读数即来源于此层。当前坐" + mountain + "山。"});
        out.add(new String[]{"七十二龙（地盘分金）",
                "每山三龙、各 5°，正中一线为【龟甲空亡】不用，余六十甲子跳空顺排，配纳音五行定坐穴。当前向线压【" + longName + "】龙（纳音" + longNayin + "）。"});
        out.add(new String[]{"一百二十分金",
                "每山五分金（甲乙丙丁戊各 3°），传统只用居中的【丙、丁】分金（避甲乙孤虚、戊空亡）。盘面以均匀细刻度呈现，具体落线由本弹窗实时判定。"});
        out.add(new String[]{"地支环（十二支）",
                "十二地支每支 30°，子居正北顺时针至亥。地支是二十四山的骨架，坐" + mountain + "山属地支【" + zhiName + "】系，三合六合关系由支系推。"});
        out.add(new String[]{"天干环（十干）",
                "十天干每干 36°，甲起东北偏东（75°）。戊己不入盘；四正与四隅的天干围绕地支，构成干支组合的方位意义。"});
        out.add(new String[]{"八卦层（三线爻符+卦名）",
                "后天八卦各镇一方（坎北、艮东北、震东、巽东南、离南、坤西南、兑西、乾西北），爻画阴阳即卦象。当前坐山属【" + starName + "】宫卦。"});
        out.add(new String[]{"天池（中心）",
                "白底红色子午基准线 + 两颗红点，磁针红头指子（北）、黑尾指午（南）。叠针即让红线与磁针重合——这是全盘定位的起点。"});
        return out;
    }

    /** 圈层详解条目：名称金色加粗 + 说明正文 */
    private LinearLayout ringBullet(String name, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(6), dp(4), 0, dp(4));

        TextView tvName = new TextView(this);
        tvName.setText("◆ " + name);
        tvName.setTextColor(0xFFD9B45C);
        tvName.setTextSize(13.5f);
        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        row.addView(tvName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(0xFFDDDDDD);
        tvDesc.setTextSize(13f);
        tvDesc.setLineSpacing(dp(2), 1f);
        row.addView(tvDesc);
        return row;
    }

    private int liQiColor(String jixiong) {
        if (jixiong.contains("上吉")) return 0xFF35C46A;
        if (jixiong.contains("偏弱")) return 0xFFE05A4E;
        return 0xFFE6C34A;
    }

    private void showHelp() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(16));

        box.addView(section("一、测量步骤（叠针 / 纳针法）"));
        box.addView(bullet("前置准备：让外盘天心十道竖线对准大门中轴线后锁死不动；罗盘放平、水平仪气泡居中；摘掉金属物品、远离钢筋铁门；耐心等磁针完全静止。"));
        box.addView(bullet("只转动内盘：按住盘面拖动（外盘+天池海底线一体旋转），让红针（北端）对准海底线北端两个小红点，黑针（南端）自然对准另一端（午），整条磁针与海底红线完全重合、无夹角。"));
        box.addView(bullet("这一步叫叠针 / 纳针，是定向最关键的一步。双击盘面回正归零，底栏会显示「手动偏移 X°」。"));
        box.addView(bullet("合格标准：俯视针线重合成一条线，红针头精准压在两个小红点上，磁针稳定不抖。"));
        box.addView(bullet("若松手时红针指在午端（叠反了），App 会自动转正：红针恒指子端、黑针恒指午端。"));

        box.addView(section("二、常见错误"));
        box.addView(bullet("转动外盘去对磁针——外盘对准大门后不能动，只能转天池。"));
        box.addView(bullet("指针还在晃就强行对齐——磁场干扰读数不准，换位置。"));
        box.addView(bullet("只对一端、针是斜的——整条磁针必须贴合海底线。"));
        box.addView(bullet("盘面倾斜——用右下角水平仪检查，气泡居中变绿为水平。"));

        box.addView(section("三、对齐完成之后"));
        box.addView(bullet("内外盘全部不动，看天心十道竖线读地盘二十四山：靠近身体（屋内）= 坐山，朝外（门外）= 朝向。"));
        box.addView(bullet("底栏「X山：角度」即竖线当前所指之山；面向大门读得的是朝向，背靠读得的是坐山。"));
        box.addView(bullet("底栏同时定位显示「坐X山 · 向Y山」与吉凶简评；左下角「坐向理气」按钮可查看当前坐向的九运理气说明与二十四山对照。"));

        box.addView(section("四、小提醒：红北黑南"));
        box.addView(bullet("红 = 北（子）= 磁针 N 极；黑 = 南（午）= 磁针 S 极，红指子黑指午属正常设计。"));
        box.addView(bullet("调盘一律以红针对齐海底线北端为准。"));

        box.addView(section("五、二十四山速查（坐 ⇄ 向，相差 180°）"));
        box.addView(bullet("壬⇄丙　子⇄午　癸⇄丁　丑⇄未　艮⇄坤　寅⇄申"));
        box.addView(bullet("甲⇄庚　卯⇄酉　乙⇄辛　辰⇄戌　巽⇄乾　巳⇄亥"));
        box.addView(bullet("速记口诀：壬子癸，丑艮寅，甲卯乙，辰巽巳，丙午丁，未坤申，庚酉辛，戌乾亥。"));
        box.addView(bullet("正向 = 读数在山中间区域；靠近 7.5° 分界线为兼向（如子兼癸），各派规则不一。"));
        box.addView(bullet("完整 24 组宅向对照表见随附使用说明文档。"));

        box.addView(section("六、圈层注解（由外向内）"));
        box.addView(bullet("度数刻度环：360° 角度标尺，正北 0°、正东 90°、正南 180°、正西 270°，四正位标北东南西。"));
        box.addView(bullet("六十四卦层：伏羲先天圆图，每卦 5.625°，供卦理参考。"));
        box.addView(bullet("九星数字环：洛书数配八宫（1坎8艮3震4巽9离2坤7兑6乾）。"));
        box.addView(bullet("二十八宿层：28 宿均分，角宿起辰巽。"));
        box.addView(bullet("地盘正针：红色大字二十四山，格龙定向基准层。"));
        box.addView(bullet("人盘中针 / 天盘缝针：二十四山各逆、顺错半位 7.5°，消砂纳水。"));
        box.addView(bullet("七十二龙：每山三龙、正中空亡，六十甲子纳音五色。"));
        box.addView(bullet("一百二十分金：每山五分金，丙丁可用。"));
        box.addView(bullet("地支环 / 天干环：十二地支每支 30°（子居北）、十天干每干 36°。"));
        box.addView(bullet("八卦层：三线爻符 + 卦名，后天八卦各镇一方。"));
        box.addView(bullet("天池：白盘含红色基准线网（海底线），与外盘一体可手动旋转叠针对线；磁针红头指北。"));
        box.addView(bullet("天心十道：固定红色十字线，对准测量方位、读取各圈信息。"));
        box.addView(bullet("右下角水平仪：气泡居中变绿表示手机已水平。"));

        box.addView(section("七、九运吉凶速查（民俗参考）"));
        box.addView(bullet("上吉（九运优选）：子山午向、乾山巽向。"));
        box.addView(bullet("中上吉：壬山丙向、卯山酉向、巽山乾向、丙山壬向、酉山卯向、乙山辛向、辛山乙向。"));
        box.addView(bullet("中等平稳（最常见）：癸山丁向、寅山申向、甲山庚向、辰山戌向、巳山亥向、丁山癸向、申山寅向、庚山甲向、戌山辰向、亥山巳向。"));
        box.addView(bullet("偏弱慎用：丑山未向、艮山坤向、午山子向、未山丑向、坤山艮向。"));
        box.addView(bullet("上表均为正向；落两山交界成兼向后吉凶会反转。峦头优先：门前路冲、背后无靠则吉向减力。"));
        box.addView(bullet("九运为 2024–2043，换运后旺衰重排；逐向详评见随附使用说明文档。"));

        box.addView(note("提示：本罗盘为磁北定向（与实物罗盘一致），未叠加磁偏角；风水吉凶仅为传统民俗说法，没有科学依据。"));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("风水罗盘 · 注解与使用说明")
                .setView(scroll)
                .setPositiveButton("知道了", null)
                .show();
    }

    private TextView section(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFE8C86E);
        tv.setTextSize(15f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setPadding(0, dp(14), 0, dp(4));
        return tv;
    }

    private TextView bullet(String text) {
        TextView tv = new TextView(this);
        tv.setText("• " + text);
        tv.setTextColor(0xFFDDDDDD);
        tv.setTextSize(13.5f);
        tv.setLineSpacing(dp(3), 1f);
        tv.setPadding(dp(6), dp(2), 0, dp(2));
        return tv;
    }

    private TextView note(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF8E8E96);
        tv.setTextSize(12f);
        tv.setPadding(0, dp(12), 0, 0);
        return tv;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ================= 传感器 =================

    @Override
    protected void onResume() {
        super.onResume();
        Sensor rv = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        useRotationVector = (rv != null);
        if (useRotationVector) {
            sensorManager.registerListener(this, rv, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Sensor mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (mag != null) sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);
        }
        // 加速度计始终注册：兼容模式定向 + 右下角水平仪
        Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acc != null) sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int t = event.sensor.getType();
        if (t == Sensor.TYPE_ROTATION_VECTOR && useRotationVector) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float w = event.values.length > 3 ? event.values[3] : 0f;
            if (!haveRot) {
                rotVecFilt[0] = x; rotVecFilt[1] = y; rotVecFilt[2] = z; rotVecFilt[3] = w;
                haveRot = true;
            } else {
                // 处理四元数双覆盖符号翻转，避免滤波值突然绕远路
                float dot = rotVecFilt[0] * x + rotVecFilt[1] * y + rotVecFilt[2] * z + rotVecFilt[3] * w;
                if (dot < 0) { x = -x; y = -y; z = -z; w = -w; }
                final float a = 0.18f;
                rotVecFilt[0] += a * (x - rotVecFilt[0]);
                rotVecFilt[1] += a * (y - rotVecFilt[1]);
                rotVecFilt[2] += a * (z - rotVecFilt[2]);
                rotVecFilt[3] += a * (w - rotVecFilt[3]);
            }
            float[] r = new float[9];
            SensorManager.getRotationMatrixFromVector(r, rotVecFilt);
            float az = azimuthFromMatrix(r);
            if (az >= 0) view.setAzimuth(az, 3);
        } else if (t == Sensor.TYPE_ACCELEROMETER) {
            for (int i = 0; i < 3; i++) gravity[i] += 0.2f * (event.values[i] - gravity[i]);
            hasGravity = true;
            // 水平仪倾角：以重力矢量在屏幕平面上的偏移归一化，平放时为 0
            float gm = (float) Math.sqrt(gravity[0] * gravity[0]
                    + gravity[1] * gravity[1] + gravity[2] * gravity[2]);
            if (gm > 1f) {
                float tx = clamp(gravity[0] / gm, -1f, 1f);
                float ty = clamp(gravity[1] / gm, -1f, 1f);
                view.setTilt(tx, ty);
            }
            tryFallback();
        } else if (t == Sensor.TYPE_MAGNETIC_FIELD) {
            for (int i = 0; i < 3; i++) geomag[i] += 0.2f * (event.values[i] - geomag[i]);
            hasMag = true;
            tryFallback();
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private void tryFallback() {
        if (!hasGravity || !hasMag) return;
        float[] r = new float[9];
        if (SensorManager.getRotationMatrix(r, null, gravity, geomag)) {
            float az = azimuthFromMatrix(r);
            if (az >= 0) view.setAzimuth(az, 3);
        }
    }

    /** 把旋转矩阵换算成屏幕正上方指向的方位角（0-360，顺时针，磁北）。 */
    private float azimuthFromMatrix(float[] r) {
        float[] remap = new float[9];
        int screenRot;
        try {
            screenRot = getWindowManager().getDefaultDisplay().getRotation();
        } catch (Exception e) {
            screenRot = Surface.ROTATION_0;
        }
        boolean ok;
        switch (screenRot) {
            case Surface.ROTATION_90:
                ok = SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remap);
                break;
            case Surface.ROTATION_270:
                ok = SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remap);
                break;
            case Surface.ROTATION_180:
                ok = SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z, remap);
                break;
            default:
                ok = SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X, SensorManager.AXIS_Z, remap);
        }
        if (!ok) return -1f;
        float[] ori = new float[3];
        SensorManager.getOrientation(remap, ori);
        float az = (float) Math.toDegrees(ori[0]);
        if (az < 0) az += 360f;
        return az;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        int t = sensor.getType();
        if (t == Sensor.TYPE_MAGNETIC_FIELD || t == Sensor.TYPE_ROTATION_VECTOR) {
            view.setAccuracy(accuracy);
        }
    }
}
