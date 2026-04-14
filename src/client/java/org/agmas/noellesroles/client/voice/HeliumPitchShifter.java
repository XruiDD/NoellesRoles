package org.agmas.noellesroles.client.voice;

/**
 * 高质量 WSOLA 流式变调器（两级管线：时域拉伸 + Hermite 立方插值重采样）。
 * <p>
 * 优化点：
 * 1. 引入了 MAX_LATENCY，解决流式处理时因缓冲波动导致的“卡顿”断流。
 * 2. 引入了 Safe Synth Position，严格禁止读取尚未完成 OLA 叠加的波形，解决“沙哑”和周期毛刺。
 * 3. 扩大了相关性搜索窗和搜索半径，确保能捕获低频男声的完整基频，解决“不连贯”和机器音。
 * 4. 引入了 加权 SSD (Weighted SSD)，给予重叠中心更高的匹配权重，让接缝处相位对齐更丝滑。
 * 5. 使用位掩码 (Bitwise AND) 替代取模运算 (%)，大幅降低 CPU 占用。
 * <p>
 * 非线程安全——每个说话人 UUID 持有独立实例，由调用方保证串行调用。
 */
public final class HeliumPitchShifter {

    // --- 时频参数 (针对 48kHz 严格计算) ---
    private static final int WINDOW = 960;           // Hann 窗长 20ms
    private static final int HOP = 240;              // 合成跳 5ms（75% overlap）
    private static final int OVERLAP_LEN = 720;      // WINDOW - HOP (15ms)

    private static final int SEARCH_RADIUS = 480;    // ±10ms 搜索范围，捕捉低频波段
    private static final int CORR_WIN = 480;         // 10ms 匹配窗，覆盖 100Hz 基频周期
    private static final float HANN_GAIN = 2.0f;     // 4 窗 75% overlap 依然适用

    // 预计算的窗函数与权重
    private static final float[] HANN = buildHann(WINDOW);
    private static final float[] CORR_WEIGHTS = buildCorrWeights(CORR_WIN);

    // --- 环形缓冲与位掩码（要求必须是 2 的幂） ---
    private static final int IN_RING = 32768;
    private static final int IN_MASK = IN_RING - 1;
    private final float[] inRing = new float[IN_RING];
    private long inWriteCount = 0;

    private static final int STRETCH_RING = 16384;   // 容纳算法延迟
    private static final int STRETCH_MASK = STRETCH_RING - 1;
    private final float[] stretchRing = new float[STRETCH_RING];

    // --- WSOLA 状态 ---
    private long synthFrames = 0;                    // 已完成合成帧数
    private double anaPosD = 0.0;                    // 下一帧搜索中心（input 坐标，分数）
    private final float[] prevTailRaw = new float[CORR_WIN];

    // --- 重采样与延迟控制 ---
    private double resamplePos = 0.0;                // 当前拉伸环上的分数读位置（绝对）
    private long zeroedThrough = 0;                  // 已清零并不再读取的绝对位置

    // 流式固定延迟，确保任何变调倍率下都不会发生缓冲欠载 (Underrun)
    private static final int MAX_LATENCY = 3072;     // ~64ms 初始化缓冲
    private int latencyCount = 0;

    public HeliumPitchShifter() {}

    /**
     * 处理一帧 PCM。输入输出长度一致。
     *
     * @param in    16-bit PCM 单声道采样
     * @param ratio 变调倍率，>1 升调；夹到 [0.5, 2.5]
     * @return 变调后新 short 数组
     */
    public short[] process(short[] in, float ratio) {
        // 限制倍率
        ratio = Math.max(0.5f, Math.min(ratio, 2.5f));
        final short[] out = new short[in.length];

        for (int i = 0; i < in.length; i++) {
            // 写入输入环（使用快速位掩码）
            inRing[(int) (inWriteCount & IN_MASK)] = in[i] * (1.0f / 32768.0f);
            inWriteCount++;

            // 只要输入攒够了，就立刻合成 WSOLA 帧
            while (canSynth()) {
                runSynthFrame(ratio);
            }

            // 1. 启动期预缓冲（防止刚开始出声时卡顿断流）
            if (latencyCount < MAX_LATENCY) {
                latencyCount++;
                out[i] = 0;
                continue;
            }

            // 2. 计算"安全读取水位线"：退回 OVERLAP_LEN，确保只读取 100% 叠加完的波形
            long safeSynthPos = synthFrames * HOP - OVERLAP_LEN;

            // 3. 读取插值
            if (resamplePos + 2.0 < safeSynthPos) {
                float s = hermite4(resamplePos) / HANN_GAIN;
                out[i] = toShort(s);
                resamplePos += ratio;

                // 及时清理已经被读取过的旧数据
                while (zeroedThrough + 2 < (long) resamplePos) {
                    stretchRing[(int) (zeroedThrough & STRETCH_MASK)] = 0f;
                    zeroedThrough++;
                }
            } else {
                // 如果严格维持了 MAX_LATENCY，理论上绝对不会走到这里
                out[i] = 0;
            }
        }
        return out;
    }

    /** 是否有足够输入做下一个合成帧（含搜索余量）。 */
    private boolean canSynth() {
        return (long) anaPosD + WINDOW + SEARCH_RADIUS <= inWriteCount;
    }

    /**
     * 执行一个 WSOLA 合成帧
     */
    private void runSynthFrame(float ratio) {
        long naive = (long) anaPosD;
        long bestA = naive;

        if (synthFrames > 0) {
            float bestScore = Float.MAX_VALUE;
            // 在 ±SEARCH_RADIUS 内寻找最平滑的接缝
            for (int d = -SEARCH_RADIUS; d <= SEARCH_RADIUS; d++) {
                long cand = naive + d;

                // 确保候选位置在合法输入范围内
                if (cand < 0 || cand + WINDOW > inWriteCount || inWriteCount - cand > IN_RING) {
                    continue;
                }

                float score = 0.0f;
                // 加权 SSD 评估
                for (int k = 0; k < CORR_WIN; k++) {
                    float diff = inRing[(int) ((cand + k) & IN_MASK)] - prevTailRaw[k];

                    // 乘以权重：使中间波形对接更精准，边缘容错更高
                    score += diff * diff * CORR_WEIGHTS[k];

                    // Early exit：如果当前累计误差已经超过历史最佳，立刻中止内循环
                    if (score >= bestScore) break;
                }

                if (score < bestScore) {
                    bestScore = score;
                    bestA = cand;
                }
            }
        }

        // OLA 相加叠加到合成环
        long synthPos = synthFrames * HOP;
        for (int k = 0; k < WINDOW; k++) {
            stretchRing[(int) ((synthPos + k) & STRETCH_MASK)] += inRing[(int) ((bestA + k) & IN_MASK)] * HANN[k];
        }

        // 提取重叠区开头用于下一次的匹配
        for (int k = 0; k < CORR_WIN; k++) {
            prevTailRaw[k] = inRing[(int) ((bestA + HOP + k) & IN_MASK)];
        }

        anaPosD += (double) HOP / ratio;
        synthFrames++;
    }

    /** 4 点 Catmull-Rom / Hermite 立方插值 */
    private float hermite4(double pos) {
        long base = (long) pos;
        float t = (float) (pos - base);

        float y0 = stretchRing[(int) ((base - 1) & STRETCH_MASK)];
        float y1 = stretchRing[(int) (base & STRETCH_MASK)];
        float y2 = stretchRing[(int) ((base + 1) & STRETCH_MASK)];
        float y3 = stretchRing[(int) ((base + 2) & STRETCH_MASK)];

        float c0 = y1;
        float c1 = 0.5f * (y2 - y0);
        float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
        float c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2);
        return ((c3 * t + c2) * t + c1) * t + c0;
    }

    private static short toShort(float v) {
        float s = v * 32767f;
        if (s > 32767f) return Short.MAX_VALUE;
        if (s < -32768f) return Short.MIN_VALUE;
        return (short) s;
    }

    private static float[] buildHann(int n) {
        float[] w = new float[n];
        double c = 2.0 * Math.PI / n;
        for (int k = 0; k < n; k++) {
            w[k] = (float) (0.5 - 0.5 * Math.cos(c * k));
        }
        return w;
    }

    /**
     * 构建 SSD 匹配权重窗
     * 使用 Sine 窗，中心权重最高（为1），边缘略大于0。
     */
    private static float[] buildCorrWeights(int n) {
        float[] w = new float[n];
        double c = Math.PI / n;
        for (int k = 0; k < n; k++) {
            // 使用 (k + 0.5) 避免首尾出现绝对的 0，从而确保 Early Exit 不会失效
            w[k] = (float) Math.sin(c * (k + 0.5));
        }
        return w;
    }
}