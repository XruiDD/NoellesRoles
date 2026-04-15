package org.agmas.noellesroles.voice;

/**
 * 高质量 WSOLA 流式变调器（两级管线：时域拉伸 + Hermite 立方插值重采样）。
 * <p>
 * 优化点：
 * 1. 引入了 MAX_LATENCY，解决流式处理时因缓冲波动导致的"卡顿"断流。
 * 2. 引入了 Safe Synth Position，严格禁止读取尚未完成 OLA 叠加的波形，解决"沙哑"和周期毛刺。
 * 3. 扩大了相关性搜索窗和搜索半径，确保能捕获低频男声的完整基频，解决"不连贯"和机器音。
 * 4. 引入了 加权 SSD (Weighted SSD)，给予重叠中心更高的匹配权重，让接缝处相位对齐更丝滑。
 * 5. 使用位掩码 (Bitwise AND) 替代取模运算 (%)，大幅降低 CPU 占用。
 * <p>
 * 非线程安全——每个说话人 UUID 持有独立实例，由调用方保证串行调用。
 */
public final class HeliumPitchShifter {

    private static final int WINDOW = 960;
    private static final int HOP = 240;
    private static final int OVERLAP_LEN = 720;

    private static final int SEARCH_RADIUS = 480;
    private static final int CORR_WIN = 480;
    private static final float HANN_GAIN = 2.0f;

    private static final float[] HANN = buildHann(WINDOW);
    private static final float[] CORR_WEIGHTS = buildCorrWeights(CORR_WIN);

    private static final int IN_RING = 32768;
    private static final int IN_MASK = IN_RING - 1;
    private final float[] inRing = new float[IN_RING];
    private long inWriteCount = 0;

    private static final int STRETCH_RING = 16384;
    private static final int STRETCH_MASK = STRETCH_RING - 1;
    private final float[] stretchRing = new float[STRETCH_RING];

    private long synthFrames = 0;
    private double anaPosD = 0.0;
    private final float[] prevTailRaw = new float[CORR_WIN];

    private double resamplePos = 0.0;
    private long zeroedThrough = 0;

    private static final int MAX_LATENCY = 3072;
    private int latencyCount = 0;

    public HeliumPitchShifter() {}

    public short[] process(short[] in, float ratio) {
        ratio = Math.max(0.5f, Math.min(ratio, 2.5f));
        final short[] out = new short[in.length];

        for (int i = 0; i < in.length; i++) {
            inRing[(int) (inWriteCount & IN_MASK)] = in[i] * (1.0f / 32768.0f);
            inWriteCount++;

            while (canSynth()) {
                runSynthFrame(ratio);
            }

            if (latencyCount < MAX_LATENCY) {
                latencyCount++;
                out[i] = 0;
                continue;
            }

            long safeSynthPos = synthFrames * HOP - OVERLAP_LEN;

            if (resamplePos + 2.0 < safeSynthPos) {
                float s = hermite4(resamplePos) / HANN_GAIN;
                out[i] = toShort(s);
                resamplePos += ratio;

                while (zeroedThrough + 2 < (long) resamplePos) {
                    stretchRing[(int) (zeroedThrough & STRETCH_MASK)] = 0f;
                    zeroedThrough++;
                }
            } else {
                out[i] = 0;
            }
        }
        return out;
    }

    private boolean canSynth() {
        return (long) anaPosD + WINDOW + SEARCH_RADIUS <= inWriteCount;
    }

    private void runSynthFrame(float ratio) {
        long naive = (long) anaPosD;
        long bestA = naive;

        if (synthFrames > 0) {
            float bestScore = Float.MAX_VALUE;
            for (int d = -SEARCH_RADIUS; d <= SEARCH_RADIUS; d++) {
                long cand = naive + d;

                if (cand < 0 || cand + WINDOW > inWriteCount || inWriteCount - cand > IN_RING) {
                    continue;
                }

                float score = 0.0f;
                for (int k = 0; k < CORR_WIN; k++) {
                    float diff = inRing[(int) ((cand + k) & IN_MASK)] - prevTailRaw[k];
                    score += diff * diff * CORR_WEIGHTS[k];
                    if (score >= bestScore) break;
                }

                if (score < bestScore) {
                    bestScore = score;
                    bestA = cand;
                }
            }
        }

        long synthPos = synthFrames * HOP;
        for (int k = 0; k < WINDOW; k++) {
            stretchRing[(int) ((synthPos + k) & STRETCH_MASK)] += inRing[(int) ((bestA + k) & IN_MASK)] * HANN[k];
        }

        for (int k = 0; k < CORR_WIN; k++) {
            prevTailRaw[k] = inRing[(int) ((bestA + HOP + k) & IN_MASK)];
        }

        anaPosD += (double) HOP / ratio;
        synthFrames++;
    }

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

    private static float[] buildCorrWeights(int n) {
        float[] w = new float[n];
        double c = Math.PI / n;
        for (int k = 0; k < n; k++) {
            w[k] = (float) Math.sin(c * (k + 0.5));
        }
        return w;
    }
}
