package com.fengshui.luopan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/**
 * 风水罗盘绘制视图。
 *
 * 盘面（由外向内，全部随内盘一体旋转，可手动拖动）：
 *  1. 360° 度数刻度环（每2°小格 / 10°中格 / 30°大格带数字，四正位标北东南西）
 *  2. 六十四卦层（伏羲先天圆图，复起子位顺时针，每卦 5.625°）
 *  3. 九星数字环（洛书数配八宫：1坎 8艮 3震 4巽 9离 2坤 7兑 6乾）
 *  4. 二十八宿层（角宿起辰巽，逆时针排布）
 *  5. 天盘缝针二十四山（顺时针错半位 7.5°，纳水）
 *  6. 人盘中针二十四山（逆时针错半位 7.5°，消砂）
 *  7. 地盘正针二十四山（大字红字，格龙定向之基准）
 *  8. 七十二龙（每山三龙、正中空亡，六十甲子配纳音五色）
 *  9. 一百二十分金（每山五分金，丙丁可用金色、甲乙戊暗标）
 * 10. 十二地支环 / 十天干环
 * 11. 八卦层（三线爻符 + 卦名，后天八卦）
 * 12. 中央天池（白盘 + 红色子午基准线 + 两颗红点 + 红黑磁针）
 *
 * 天心十道：贯穿盘心的固定红色十字线。手势：按住盘面拖动旋转内盘（石头摩擦音效），
 * 双击回正；松手时若叠针叠反（红针指午）自动转正。磁针恒指磁北。
 */
public class LuopanView extends View {

    // ---------- 数据 ----------
    public static final String[] MOUNTAINS = {
            "子", "癸", "丑", "艮", "寅", "甲", "卯", "乙", "辰", "巽", "巳", "丙",
            "午", "丁", "未", "坤", "申", "庚", "酉", "辛", "戌", "乾", "亥", "壬"
    };

    /** 各坐山对应的朝向（索引同 MOUNTAINS） */
    public static final String[] XIANG = {
            "午", "丁", "未", "坤", "申", "庚", "酉", "辛",
            "戌", "乾", "亥", "壬", "子", "癸", "丑", "艮",
            "寅", "甲", "卯", "乙", "辰", "巽", "巳", "丙"
    };

    /** 九运（2024–2043）坐向理气：方位简述 / 吉凶简评 / 民俗要点（民俗参考） */
    public static final String[] LI_FANGWEI = {
            "坐正北，向正南", "坐北偏东，向南偏西", "坐东北偏北，向西南偏南", "坐正东北，向正西南",
            "坐东北偏东，向西南偏西", "坐东偏北，向西偏南", "坐正东，向正西", "坐东偏南，向西偏北",
            "坐东南偏东，向西北偏西", "坐正东南，向正西北", "坐东南偏南，向西北偏北", "坐南偏东，向北偏西",
            "坐正南，向正北", "坐南偏西，向北偏东", "坐西南偏南，向东北偏北", "坐正西南，向正东北",
            "坐西南偏西，向东北偏东", "坐西偏南，向东偏北", "坐正西，向正东", "坐西偏北，向东偏南",
            "坐西北偏西，向东南偏东", "坐正西北，向正东南", "坐西北偏北，向东南偏南", "坐北偏西，向东南"
    };

    public static final String[] LI_JIXIONG = {
            "上吉", "平稳中等", "偏弱", "偏弱慎用", "中等平稳", "中等", "中上吉", "中等偏吉",
            "中等", "中上吉", "中等", "中上吉", "偏弱慎用", "中等平稳", "偏弱", "偏弱慎用",
            "中等", "中等", "中上吉", "中等偏吉", "中等", "上吉", "中等", "中上吉"
    };

    public static final String[] LI_YAODIAN = {
            "天地定位，九运双星会向，丁财皆宜；大众优选，忌讳兼癸丁，犯二女同居",
            "正向可用，属于温和宅局；水法合局则旺，最怕向方有高大逼压建筑",
            "九运整体不算旺；传统需外局山水配合，若无好峦头，易是非耗财",
            "九运典型上山下水格局；民俗认为容易损丁、多病、破财，峦头不好尽量避开",
            "普通正向，不特大吉也非大凶；重点看外局砂水，忌西南有凹风、破碎建筑",
            "古法合格正向；九运属于平局，利稳守，不易大发，也不容易大破",
            "木局清秀，传统利文教、读书；九运平中带吉，忌西方高楼高压",
            "合格正向；古法有名贵格【辛入乾宫百万庄】，但九运不是旺山旺向，外局好才好用",
            "正向可用；最看重水口，外局不合容易破财，西北方忌突兀高楼",
            "九运双星会坐，主人丁健康；传统利贵人，坐后方宜有靠山，忌后方空缺",
            "普通正向；水法合局则吉，最怕西北有路冲、尖角煞",
            "九运双星会向，利求财；前方宜空旷见水、大路，坐后要有靠山",
            "上山下水格局；民俗易家庭不和、损耗财运，除非外局形势极佳，一般不推荐",
            "温和平局，适合居家守成；忌北方高楼高压",
            "九运二黑病符临向；峦头不好，家人多病、是非较多",
            "上山下水；民俗主健康问题、人丁不稳，若无极佳外部山水，尽量不用",
            "普通正向，平稳守成；东北忌断路、大坑、破败建筑",
            "平局，适合稳定居住；东方不宜有高压、巨石",
            "和卯山酉互为反向；传统利学业文采，东方宜开阔，忌东方高楼压宅",
            "乙山辛向的反向；古法贵局，九运平局，前方宜开阔，忌东南方凹风",
            "普通正向；东南方忌急水、直冲大路",
            "传统经典贵局，九运双星会坐；利贵人事业，坐后必须有靠山，后方虚空则减力",
            "合格正向；水局合则旺，忌东南有破碎、尖角建筑物",
            "九运双星会向，主利财运；杨公古法属贵局，宜前方开阔，忌坐后空缺"
    };

    private static final String[] MANSIONS = {
            "角", "亢", "氐", "房", "心", "尾", "箕",
            "斗", "牛", "女", "虚", "危", "室", "壁",
            "奎", "娄", "胃", "昴", "毕", "觜", "参",
            "井", "鬼", "柳", "星", "张", "翼", "轸"
    };

    // 伏羲先天六十四卦圆图：复起于子（0°），顺时针至乾（约南），姤起于午，终坤（约北）
    private static final String[] HEX64 = {
            "复", "颐", "屯", "益", "震", "噬嗑", "随", "无妄", "明夷", "贲", "既济", "家人",
            "丰", "离", "革", "同人", "临", "损", "节", "中孚", "归妹", "睽", "兑", "履",
            "泰", "大畜", "需", "小畜", "大壮", "大有", "夬", "乾",
            "姤", "大过", "鼎", "恒", "巽", "井", "蛊", "升", "讼", "困", "未济", "解",
            "涣", "坎", "蒙", "师", "遯", "咸", "旅", "小过", "渐", "蹇", "艮", "谦",
            "否", "萃", "晋", "豫", "观", "比", "剥", "坤"
    };

    // 十二地支：子居正北，每支 30° 顺时针
    private static final String[] BRANCHES = {
            "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    };

    // 十天干：每干 36° 顺时针，甲起于 75°（二十四山甲位）
    private static final String[] STEMS = {
            "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
    };
    private static final float STEM_START = 75f;

    // 后天八卦：方位（顺时针自北）、卦名与三爻 {初,中,上}（1=阳爻）
    private static final int[] TRIGRAM_ANGLE = {0, 45, 90, 135, 180, 225, 270, 315};
    private static final String[] TRIGRAM_NAME = {"坎", "艮", "震", "巽", "离", "坤", "兑", "乾"};
    private static final int[][] TRIGRAM_LINES = {
            {0, 1, 0}, {0, 0, 1}, {1, 0, 0}, {0, 1, 1},
            {1, 0, 1}, {0, 0, 0}, {1, 1, 0}, {1, 1, 1}
    };

    // 玄空洛书九星配宫：每宫一字，数字 = 该宫洛书数（索引同 TRIGRAM_ANGLE）
    private static final int[] STAR_NUM = {1, 8, 3, 4, 9, 2, 7, 6};
    private static final int[] STAR_COLOR = {
            0xFFE8E8E8, 0xFFE8E8E8, 0xFF3ECF8E, 0xFF37B34A,
            0xFFB07CE8, 0xFF9A9AA2, 0xFFE04A3F, 0xFFE8E8E8
    };

    // 七十二龙：六十甲子 + 十二空亡（索引 d%3==1 为空亡，每山三龙正中）
    public static final String[] GAN10 = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    public static final String[] ZHI12 = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    public static final String[] JIAZI60 = buildJiazi();
    // 三十纳音（每两龙一纳音）
    public static final String[] NAYIN = {
            "金", "火", "木", "土", "金", "火", "水", "土", "金", "木",
            "水", "土", "火", "木", "水", "金", "火", "木", "土", "金",
            "火", "水", "土", "金", "木", "水", "土", "火", "木", "水"
    };

    private static String[] buildJiazi() {
        String[] a = new String[60];
        for (int k = 0; k < 60; k++) a[k] = GAN10[k % 10] + ZHI12[k % 12];
        return a;
    }

    private static int nayinColor(String nayin) {
        switch (nayin) {
            case "金": return 0xFFE8E8E8;
            case "木": return 0xFF37B34A;
            case "水": return 0xFF4A90D9;
            case "火": return 0xFFE04A3F;
            default:  return 0xFFC8A96E; // 土
        }
    }

    private static final String[] DIR8 = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};

    // ---------- 配色 ----------
    private static final int COL_BG = 0xFF0A0A0C;
    private static final int COL_DISK = 0xFF101014;
    private static final int COL_RING_ALT = 0xFF16161B;
    private static final int COL_LINE = 0xFF2E2E36;
    private static final int COL_SEG = 0xFF232329;
    private static final int COL_RED = 0xFFE0392E;
    private static final int COL_RED_DIM = 0xFFD0453A;
    private static final int COL_GOLD = 0xFFC8A96E;
    private static final int COL_STEM = 0xFFD9B45C;
    private static final int COL_BRANCH = 0xFFE8E8E8;
    private static final int COL_TICK = 0xFF8E8E96;
    private static final int COL_NUM = 0xFFC6C6CE;
    private static final int COL_VOID = 0xFF1A1A20;
    private static final int COL_BOTTOM_BG = 0xFF121217;
    private static final int COL_BOTTOM_TEXT = 0xFFE8C86E;
    private static final int COL_BOTTOM_SUB = 0xFF8E8E96;

    // 天池（纯白底）
    private static final int COL_POOL_BG = 0xFFFFFFFF;
    private static final int COL_POOL_RIM = 0xFF7A5C1E;
    private static final int COL_POOL_LINE = 0xFFD8D8DC;
    private static final int COL_NEEDLE_RED = 0xFFD32A1C;
    private static final int COL_NEEDLE_TAIL = 0xFF111114;

    // ---------- 状态 ----------
    private volatile float azimuth = 0f;
    private volatile int accuracy = 3;
    private volatile float tiltX = 0f;
    private volatile float tiltY = 0f;
    private float manualOffset = 0f; // 内盘手动旋转偏移（顺时针为正，-180..180）
    private GestureDetector gesture;
    private boolean dragging;
    private float lastTouchAngle;

    // 拖动石头摩擦音效
    private SoundPool soundPool;
    private int stoneSoundId;
    private boolean stoneLoaded;
    private int stoneStream;
    private float stoneVol;

    // 顶部标题（篆书“二十四山風水羅盤”，白色透明底）
    private Bitmap titleImg;

    private Bitmap disk;
    private float cx, cy, R;
    private final float bottomBar;
    private final float density;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface tfMedium;

    public LuopanView(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        bottomBar = 84f * density;
        tfMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        gesture = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                manualOffset = 0f;
                postInvalidateOnAnimation();
                return true;
            }
        });

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attrs).build();
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool sp, int sampleId, int status) {
                stoneLoaded = (status == 0);
            }
        });
        stoneSoundId = soundPool.load(c, com.fengshui.luopan.R.raw.stone_drag, 1);
    }

    // ---------- 公共接口 ----------
    public void setAzimuth(float az, int acc) {
        azimuth = az;
        if (acc >= 0) accuracy = acc;
        postInvalidateOnAnimation();
    }

    public void setAccuracy(int acc) {
        accuracy = acc;
        postInvalidateOnAnimation();
    }

    public void setTilt(float tx, float ty) {
        tiltX = tx;
        tiltY = ty;
        postInvalidateOnAnimation();
    }

    /** 竖线朝外一端所指之山（向）的索引 0..23 */
    public int getXiangIndex() {
        float effective = norm(azimuth - manualOffset);
        int idx = Math.round(effective / 15f) % 24;
        return (idx % 24 + 24) % 24;
    }

    /** 对端坐山索引 0..23 */
    public int getZuoIndex() {
        return (getXiangIndex() + 12) % 24;
    }

    /** 内盘读数（含手动偏移）：竖线朝外端的方位角 */
    public float getEffectiveAzimuth() {
        return norm(azimuth - manualOffset);
    }

    /** 当前读数落在七十二龙的哪一龙（0..71，d%3==1 为龟甲空亡） */
    public int getLongIndex() {
        int d = (int) Math.floor((getEffectiveAzimuth() + 7.5f) / 5f);
        return ((d % 72) + 72) % 72;
    }

    /** 当前读数落在一百二十分金的哪一分金（0..119，s%5: 甲乙丙丁戊） */
    public int getFenJinIndex() {
        int s = (int) Math.floor((getEffectiveAzimuth() + 7.5f) / 3f);
        return ((s % 120) + 120) % 120;
    }

    /** 七十二龙序号 -> 六十甲子序号（跳过空亡） */
    public static int jiaziIndexForDragon(int d) {
        return d - (d + 2) / 3;
    }

    public static boolean isVoidDragon(int d) {
        return d % 3 == 1;
    }

    // ================= 尺寸 =================

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        cx = w / 2f;
        float availH = h - bottomBar;
        cy = availH / 2f;
        R = Math.min(w, availH) / 2f * 0.965f;
        buildDisk();
        titleImg = BitmapFactory.decodeResource(getResources(), com.fengshui.luopan.R.drawable.title_white);
    }

    // ================= 静态盘面 =================

    private void buildDisk() {
        if (R <= 0) return;
        int size = (int) (2f * R + 4f);
        disk = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(disk);
        c.translate(size / 2f, size / 2f);
        float r = size / 2f - 2f;

        p.reset();
        p.setAntiAlias(true);

        // 盘底
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_DISK);
        c.drawCircle(0, 0, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.4f * density);
        p.setColor(0xFF3A3A44);
        c.drawCircle(0, 0, r - 1f, p);

        drawTickRing(c, r);
        drawHex64Ring(c, r);
        drawStarRing(c, r);
        drawMansionRing(c, r);
        drawFengZhen24(c, r);
        drawRenZhen24(c, r);
        drawDiPan24(c, r);
        drawLong72(c, r);
        drawFenJin120(c, r);
        drawBranchRing(c, r);
        drawStemRing(c, r);
        drawTrigramRing(c, r);
        drawCenterPool(c, r);

        // 圈层分隔圆
        ringCircle(c, 0.958f * r, COL_LINE, density, p);
        ringCircle(c, 0.862f * r, COL_LINE, density, p);
        ringCircle(c, 0.795f * r, COL_LINE, density, p);
        ringCircle(c, 0.712f * r, COL_LINE, density, p);
        ringCircle(c, 0.648f * r, COL_LINE, density, p);
        ringCircle(c, 0.575f * r, COL_LINE, density, p);
        ringCircle(c, 0.478f * r, COL_LINE, density, p);
        ringCircle(c, 0.405f * r, COL_LINE, density, p);
        ringCircle(c, 0.349f * r, COL_LINE, density, p);
        ringCircle(c, 0.290f * r, COL_LINE, density, p);
        ringCircle(c, 0.244f * r, COL_LINE, density, p);
        ringCircle(c, 0.168f * r, COL_LINE, density, p);
    }

    /** 1. 度数刻度环 */
    private void drawTickRing(Canvas c, float r) {
        for (int d = 0; d < 360; d += 2) {
            float len = 0.016f * r;
            float wd = density;
            int col = COL_TICK;
            if (d % 30 == 0) {
                len = 0.040f * r;
                wd = 2f * density;
                col = COL_NUM;
            } else if (d % 10 == 0) {
                len = 0.026f * r;
                wd = 1.4f * density;
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(wd);
            p.setColor(col);
            radialLine(c, d, r - len, r - 0.004f * r, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        String[] cardinal = {"北", "东", "南", "西"};
        for (int d = 0; d < 360; d += 30) {
            if (d % 90 == 0) {
                p.setTypeface(tfMedium);
                p.setColor(COL_RED);
                p.setTextSize(0.050f * r);
                drawCharCentered(c, cardinal[d / 90], d, 0.928f * r, p);
            } else {
                p.setTypeface(Typeface.DEFAULT);
                p.setColor(COL_NUM);
                p.setTextSize(0.042f * r);
                drawCharCentered(c, String.valueOf(d), d, 0.928f * r, p);
            }
        }
        p.setTypeface(Typeface.DEFAULT);
    }

    /** 2. 六十四卦层：卦名单行切向排布（让出度数数字带） */
    private void drawHex64Ring(Canvas c, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int i = 0; i < 64; i++) {
            radialLine(c, i * 5.625f + 2.8125f, 0.862f * r, 0.905f * r, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_GOLD);
        p.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 64; i++) {
            String name = HEX64[i];
            float ang = i * 5.625f;
            p.setTextSize((name.length() == 1 ? 0.034f : 0.028f) * r);
            c.save();
            c.rotate(ang);
            Paint.FontMetrics fm = p.getFontMetrics();
            float voff = (fm.ascent + fm.descent) / 2f;
            c.drawText(name, 0, -0.8835f * r - voff, p);
            c.restore();
        }
    }

    /** 3. 九星数字环：洛书数配八宫（1坎 8艮 3震 4巽 9离 2坤 7兑 6乾），每宫 45° */
    private void drawStarRing(Canvas c, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int i = 0; i < 8; i++) {
            radialLine(c, TRIGRAM_ANGLE[i] + 22.5f, 0.795f * r, 0.855f * r, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(tfMedium);
        for (int i = 0; i < 8; i++) {
            p.setColor(STAR_COLOR[i]);
            p.setTextSize(0.048f * r);
            drawCharCentered(c, String.valueOf(STAR_NUM[i]), TRIGRAM_ANGLE[i], 0.825f * r, p);
        }
        p.setTypeface(Typeface.DEFAULT);
    }

    /** 4. 二十八宿层：角宿起于辰巽（135°），逆时针排布，七宿组心对四正宫 */
    private void drawMansionRing(Canvas c, float r) {
        float step = 360f / 28f;
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int i = 0; i < 28; i++) {
            float ang = norm(135f - (i + 0.5f) * step);
            radialLine(c, ang, 0.715f * r, 0.788f * r, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_RED_DIM);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(tfMedium);
        for (int i = 0; i < 28; i++) {
            float ang = norm(135f - i * step);
            p.setTextSize(0.044f * r);
            drawCharCentered(c, MANSIONS[i], ang, 0.7515f * r, p);
        }
        p.setTypeface(Typeface.DEFAULT);
    }

    /** 通用二十四山绘制（offset 为该盘子山中心的角度偏移） */
    private void drawMountain24(Canvas c, float r, float offset, float rIn, float rOut,
                                int color, float fontSize, boolean bold, boolean altBg) {
        if (altBg) {
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 24; i++) {
                if (i % 2 == 1) {
                    fillSector(c, offset + i * 15f - 7.5f, offset + i * 15f + 7.5f, rIn, rOut, COL_RING_ALT, p);
                }
            }
        }
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int i = 0; i < 24; i++) {
            radialLine(c, offset + i * 15f + 7.5f, rIn, rOut, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        if (bold) p.setTypeface(tfMedium);
        p.setTextSize(fontSize * r);
        for (int i = 0; i < 24; i++) {
            drawCharCentered(c, MOUNTAINS[i], offset + i * 15f, (rIn + rOut) / 2f, p);
        }
        if (bold) p.setTypeface(Typeface.DEFAULT);
    }

    /** 5. 天盘缝针二十四山：顺时针错半位（+7.5°），用于纳水 */
    private void drawFengZhen24(Canvas c, float r) {
        drawMountain24(c, r, 7.5f, 0.652f * r, 0.706f * r, 0xFFD9B45C, 0.046f, true, false);
    }

    /** 6. 人盘中针二十四山：逆时针错半位（-7.5°），用于消砂 */
    private void drawRenZhen24(Canvas c, float r) {
        drawMountain24(c, r, -7.5f, 0.580f * r, 0.642f * r, 0xFFE8E8E8, 0.046f, true, false);
    }

    /** 7. 地盘正针二十四山：大字红字，格龙定向基准 */
    private void drawDiPan24(Canvas c, float r) {
        drawMountain24(c, r, 0f, 0.482f * r, 0.568f * r, COL_RED, 0.074f, true, true);
    }

    /** 8. 七十二龙：每山三龙、正中空亡，六十甲子配纳音五色 */
    private void drawLong72(Canvas c, float r) {
        float rIn = 0.408f * r, rOut = 0.470f * r, rMid = (rIn + rOut) / 2f;
        int jiazi = 0;
        for (int d = 0; d < 72; d++) {
            float a0 = -7.5f + d * 5f;
            float a1 = a0 + 5f;
            boolean voidDragon = (d % 3 == 1);
            if (voidDragon) {
                fillSector(c, a0, a1, rIn, rOut, COL_VOID, p);
                continue;
            }
            String nayin = NAYIN[jiazi / 2];
            fillSector(c, a0, a1, rIn, rOut, (nayinColor(nayin) & 0x00FFFFFF) | 0x50000000, p);
            String name = JIAZI60[jiazi];
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE8E8E8);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(0.024f * r);
            drawCharCentered(c, name.substring(0, 1), a0 + 2.5f, 0.447f * r, p);
            drawCharCentered(c, name.substring(1), a0 + 2.5f, 0.423f * r, p);
            jiazi++;
        }
    }

    /** 9. 一百二十分金：细密刻度环（均匀暗色，丙丁可用分金在「山向解读」中说明） */
    private void drawFenJin120(Canvas c, float r) {
        float rIn = 0.246f * r, rOut = 0.284f * r;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f * density);
        p.setColor(0xFF3A3A42);
        for (int s = 0; s < 120; s++) {
            float a = -7.5f + s * 3f;
            radialLine(c, a, rIn, rOut, p);
        }
    }

    /** 10. 十二地支环：子居正北 */
    private void drawBranchRing(Canvas c, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int i = 0; i < 12; i++) {
            radialLine(c, i * 30f + 15f, 0.295f * r, 0.342f * r, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_BRANCH);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(tfMedium);
        p.setTextSize(0.038f * r);
        for (int i = 0; i < 12; i++) {
            drawCharCentered(c, BRANCHES[i], i * 30f, 0.3185f * r, p);
        }
        p.setTypeface(Typeface.DEFAULT);
    }

    /** 11. 十天干环：甲起于 75°，在地支环外圈 */
    private void drawStemRing(Canvas c, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int i = 0; i < 10; i++) {
            radialLine(c, norm(STEM_START + i * 36f + 18f), 0.356f * r, 0.398f * r, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_STEM);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(tfMedium);
        p.setTextSize(0.038f * r);
        for (int i = 0; i < 10; i++) {
            drawCharCentered(c, STEMS[i], norm(STEM_START + i * 36f), 0.377f * r, p);
        }
        p.setTypeface(Typeface.DEFAULT);
    }

    /** 12. 八卦层（三线爻符 + 卦名左右并排） */
    private void drawTrigramRing(Canvas c, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(COL_SEG);
        p.setStrokeWidth(0.7f * density);
        for (int k = 0; k < 8; k++) {
            radialLine(c, TRIGRAM_ANGLE[k] + 22.5f, 0.175f * r, 0.238f * r, p);
        }
        float barW = 0.062f * r;
        float barT = 0.013f * r;
        float gap = 0.007f * r;
        float yinGap = 0.012f * r;
        float yTop = -0.236f * r;
        float rr = barT / 2f;

        for (int k = 0; k < 8; k++) {
            int[] lines = TRIGRAM_LINES[k];
            c.save();
            c.rotate(TRIGRAM_ANGLE[k] - 8f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(COL_GOLD);
            for (int j = 0; j < 3; j++) {
                int yang = lines[2 - j];
                float y0 = yTop + j * (barT + gap);
                float y1 = y0 + barT;
                if (yang == 1) {
                    c.drawRoundRect(-barW / 2f, y0, barW / 2f, y1, rr, rr, p);
                } else {
                    c.drawRoundRect(-barW / 2f, y0, -yinGap / 2f, y1, rr, rr, p);
                    c.drawRoundRect(yinGap / 2f, y0, barW / 2f, y1, rr, rr, p);
                }
            }
            c.restore();
        }

        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_GOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(tfMedium);
        p.setTextSize(0.030f * r);
        for (int k = 0; k < 8; k++) {
            drawCharCentered(c, TRIGRAM_NAME[k], TRIGRAM_ANGLE[k] + 10f, 0.203f * r, p);
        }
        p.setTypeface(Typeface.DEFAULT);
    }

    /** 13. 中央天池：纯白底 + 红色子午基准线 + 两颗对称红点 */
    private void drawCenterPool(Canvas c, float r) {
        float poolR = 0.150f * r;
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_POOL_BG);
        c.drawCircle(0, 0, poolR, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.6f * density);
        p.setColor(COL_POOL_RIM);
        c.drawCircle(0, 0, poolR - 0.004f * r, p);
        p.setStrokeWidth(0.8f * density);
        p.setColor(COL_POOL_LINE);
        c.drawCircle(0, 0, 0.132f * r, p);

        float edge = 0.130f * r;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f * density);
        p.setColor(COL_RED);
        c.drawLine(0, -edge, 0, edge, p);
        c.drawLine(-0.016f * r, edge, 0.016f * r, edge, p);

        p.setStyle(Paint.Style.FILL);
        float dotR = 0.009f * r;
        c.drawCircle(-0.022f * r, -0.108f * r, dotR, p);
        c.drawCircle(0.022f * r, -0.114f * r, dotR, p);
    }

    /** 磁针：红头黑尾三角（白描边），恒指磁北（不随内盘手动偏转） */
    private void drawNeedle(Canvas canvas) {
        canvas.save();
        canvas.rotate(-azimuth, cx, cy);
        float L = 0.121f * R;
        float W = 0.0267f * R;
        Path n1 = new Path();
        n1.moveTo(cx, cy - L);
        n1.lineTo(cx - W, cy + 0.0127f * R);
        n1.lineTo(cx + W, cy + 0.0127f * R);
        n1.close();
        Path n2 = new Path();
        n2.moveTo(cx, cy + L);
        n2.lineTo(cx - W, cy - 0.0127f * R);
        n2.lineTo(cx + W, cy - 0.0127f * R);
        n2.close();
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.2f * density);
        p.setColor(0xFFFFFFFF);
        canvas.drawPath(n1, p);
        canvas.drawPath(n2, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_NEEDLE_RED);
        canvas.drawPath(n1, p);
        p.setColor(COL_NEEDLE_TAIL);
        canvas.drawPath(n2, p);
        canvas.restore();

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF141414);
        canvas.drawCircle(cx, cy, 0.0134f * R, p);
        p.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, 0.0057f * R, p);
    }

    // ================= 动态层 =================

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_BG);
        canvas.drawRect(0, 0, w, h, p);

        if (disk != null) {
            canvas.save();
            canvas.rotate(-azimuth + manualOffset, cx, cy);
            canvas.drawBitmap(disk, cx - disk.getWidth() / 2f, cy - disk.getHeight() / 2f, null);
            canvas.restore();
        }

        drawCrosshairLines(canvas);
        drawNeedle(canvas);

        // 顶部居中标题：篆书“二十四山風水羅盤”（白色，固定不转）
        if (titleImg != null) {
            float freeTop = 14f * density;
            float freeBottom = cy - R - 30f * density;
            float bandH = freeBottom - freeTop;
            if (bandH > 22f * density) {
                float th = Math.min(34f * density, bandH * 0.6f);
                float tw = titleImg.getWidth() * (th / titleImg.getHeight());
                if (tw > w * 0.72f) {
                    tw = w * 0.72f;
                    th = tw / titleImg.getWidth() * titleImg.getHeight();
                }
                float ty = freeTop + (bandH - th) * 0.66f;
                p.reset();
                p.setAntiAlias(true);
                p.setFilterBitmap(true);
                canvas.drawBitmap(titleImg, null,
                        new RectF(cx - tw / 2f, ty, cx + tw / 2f, ty + th), p);
            }
        }

        drawCrosshairEnds(canvas);
        drawLevel(canvas, w, h);
        drawBottomBar(canvas, w, h);
    }

    /** 天心十道：贯穿盘心的固定红色十字线，四正位字符处开让位缺口 */
    private void drawCrosshairLines(Canvas canvas) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f * density);
        p.setColor(COL_RED);
        p.setAlpha(216);
        float g1 = 0.958f * R, g2 = 0.898f * R; // 北/南、东/西字符让位区
        canvas.drawLine(cx, cy - R - 4f * density, cx, cy - g1, p);
        canvas.drawLine(cx, cy - g2, cx, cy + g2, p);
        canvas.drawLine(cx, cy + g1, cx, cy + R + 4f * density, p);
        canvas.drawLine(cx - R - 4f * density, cy, cx - g1, cy, p);
        canvas.drawLine(cx - g2, cy, cx + g2, cy, p);
        canvas.drawLine(cx + g1, cy, cx + R + 4f * density, cy, p);
        p.setAlpha(255);
    }

    /** 天心十道端部：顶部红色指示三角 + 校准提示（画在磁针之上） */
    private void drawCrosshairEnds(Canvas canvas) {
        Path tri = new Path();
        tri.moveTo(cx, cy - R + 1.5f * density);
        tri.lineTo(cx - 7f * density, cy - R - 11f * density);
        tri.lineTo(cx + 7f * density, cy - R - 11f * density);
        tri.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_RED);
        canvas.drawPath(tri, p);

        if (accuracy >= 0 && accuracy < 2) {
            p.setColor(0xFFE6A23C);
            p.setTextSize(12.5f * density);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("磁场偏弱，请远离金属并画\"8\"字校准", cx, cy - R - 17f * density, p);
        }
    }

    /** 底部信息栏：坐X山 · 向Y山 + 度数/方位/吉凶 */
    private void drawBottomBar(Canvas canvas, float w, float h) {
        float top = h - bottomBar;
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_BOTTOM_BG);
        canvas.drawRect(0, top, w, h, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(density);
        p.setColor(0xFF2A2A32);
        canvas.drawLine(0, top, w, top, p);

        float effective = norm(azimuth - manualOffset);
        int xiangIdx = Math.round(effective / 15f) % 24;
        if (xiangIdx < 0) xiangIdx += 24;
        int zuoIdx = (xiangIdx + 12) % 24;
        int dir = ((Math.round(effective / 45f)) % 8 + 8) % 8;

        p.setStyle(Paint.Style.FILL);
        p.setColor(COL_BOTTOM_TEXT);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(23f * density);
        p.setTypeface(tfMedium);
        String main = String.format(Locale.US, "坐%s山 · 向%s山", MOUNTAINS[zuoIdx], MOUNTAINS[xiangIdx]);
        canvas.drawText(main, cx, top + bottomBar * 0.58f, p);

        p.setColor(COL_BOTTOM_SUB);
        p.setTextSize(11.5f * density);
        p.setTypeface(Typeface.DEFAULT);
        String offsetTxt = Math.abs(manualOffset) > 0.05f
                ? String.format(Locale.US, " · 手动偏移 %+.1f°", manualOffset) : "";
        String sub = String.format(Locale.US, "%.2f° · %s · %s%s",
                effective, DIR8[dir], LI_JIXIONG[zuoIdx], offsetTxt);
        canvas.drawText(sub, cx, top + bottomBar * 0.85f, p);
    }

    /** 圆形水平仪：右下角，气泡居中变绿表示已水平 */
    private void drawLevel(Canvas canvas, float w, float h) {
        float radius = 34f * density;
        float lcx = w - 16f * density - radius;
        float lcy = h - bottomBar - 16f * density - radius;

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCC101014);
        canvas.drawCircle(lcx, lcy, radius, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.6f * density);
        p.setColor(0xFF6E6E78);
        canvas.drawCircle(lcx, lcy, radius, p);
        p.setStrokeWidth(density);
        p.setColor(0xFF3C3C46);
        canvas.drawCircle(lcx, lcy, radius * 0.55f, p);
        canvas.drawLine(lcx - radius, lcy, lcx - radius * 0.78f, lcy, p);
        canvas.drawLine(lcx + radius * 0.78f, lcy, lcx + radius, lcy, p);
        canvas.drawLine(lcx, lcy - radius, lcx, lcy - radius * 0.78f, p);
        canvas.drawLine(lcx, lcy + radius * 0.78f, lcx, lcy + radius, p);

        boolean level = Math.abs(tiltX) < 0.055f && Math.abs(tiltY) < 0.055f;
        float bx = lcx + tiltX * radius * 0.62f;
        float by = lcy + tiltY * radius * 0.62f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(level ? 0xFF35C46A : COL_RED);
        canvas.drawCircle(bx, by, radius * 0.26f, p);
    }

    // ================= 触控 =================

    /** 盘面触控：按住拖动旋转内盘（外盘+天池一体），双击回正；磁针不受影响 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gesture.onTouchEvent(event);
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (dist <= R + 20f * density && event.getY() < getHeight() - bottomBar) {
                    dragging = true;
                    lastTouchAngle = touchAngle(dx, dy);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    startStoneSound();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float a = touchAngle(dx, dy);
                    float delta = a - lastTouchAngle;
                    if (delta > 180f) delta -= 360f;
                    if (delta < -180f) delta += 360f;
                    manualOffset = norm(manualOffset + delta);
                    if (manualOffset > 180f) manualOffset -= 360f;
                    lastTouchAngle = a;
                    updateStoneVolume(Math.abs(delta));
                    postInvalidateOnAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging && Math.abs(manualOffset) >= 150f) {
                    // 叠针叠反了（红针指午）：松手自动转正为红针指子
                    snapToCorrect();
                }
                dragging = false;
                stopStoneSound();
                break;
            default:
                break;
        }
        return true;
    }

    /** 触点相对盘心的角度（自正北顺时针） */
    private static float touchAngle(float dx, float dy) {
        return (float) Math.toDegrees(Math.atan2(dx, -dy));
    }

    private void snapToCorrect() {
        ValueAnimator ani = ValueAnimator.ofFloat(manualOffset, 0f);
        ani.setDuration(400L);
        ani.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                manualOffset = (Float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            }
        });
        ani.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                manualOffset = 0f;
                postInvalidateOnAnimation();
            }
        });
        ani.start();
    }

    // ================= 音效 =================

    private void startStoneSound() {
        if (!stoneLoaded || soundPool == null) return;
        stoneVol = 1.0f;
        if (stoneStream == 0) {
            stoneStream = soundPool.play(stoneSoundId, stoneVol, stoneVol, 1, -1, 1.0f);
        }
    }

    private void updateStoneVolume(float deltaDeg) {
        if (stoneStream == 0) return;
        float target = Math.max(0.8f, Math.min(1f, 0.85f + deltaDeg * 0.04f));
        stoneVol += 0.35f * (target - stoneVol);
        soundPool.setVolume(stoneStream, stoneVol, stoneVol);
    }

    private void stopStoneSound() {
        if (stoneStream != 0 && soundPool != null) {
            soundPool.stop(stoneStream);
            stoneStream = 0;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopStoneSound();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    // ================= 几何小工具 =================

    private static float norm(float deg) {
        float v = deg % 360f;
        if (v < 0) v += 360f;
        return v;
    }

    private static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }

    /** 角度制径向线：ang 为自正北顺时针角度 */
    private void radialLine(Canvas c, float ang, float r1, float r2, Paint paint) {
        float s = (float) Math.sin(rad(ang));
        float co = -(float) Math.cos(rad(ang));
        c.drawLine(s * r1, co * r1, s * r2, co * r2, paint);
    }

    /** 在角度 ang、半径 radius 处画一个径向朝向、垂直居中的字符 */
    private void drawCharCentered(Canvas c, String s, float ang, float radius, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        float voff = (fm.ascent + fm.descent) / 2f;
        c.save();
        c.rotate(ang);
        c.drawText(s, 0, -radius - voff, paint);
        c.restore();
    }

    private void ringCircle(Canvas c, float radius, int color, float wd, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(wd);
        c.drawCircle(0, 0, radius, paint);
    }

    /** 填充扇环：a0->a1 自北顺时针角度，r0 内半径，r1 外半径 */
    private void fillSector(Canvas c, float a0, float a1, float r0, float r1, int color, Paint paint) {
        Path path = new Path();
        path.moveTo((float) (Math.sin(rad(a0)) * r0), (float) (-Math.cos(rad(a0)) * r0));
        RectF outer = new RectF(-r1, -r1, r1, r1);
        RectF inner = new RectF(-r0, -r0, r0, r0);
        path.arcTo(outer, a0 - 90f, a1 - a0);
        path.arcTo(inner, a1 - 90f, -(a1 - a0));
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(path, paint);
    }
}
