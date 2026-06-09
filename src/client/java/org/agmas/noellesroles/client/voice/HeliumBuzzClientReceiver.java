package org.agmas.noellesroles.client.voice;

import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.MinecraftClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.jester.JesterMomentClient;
import org.agmas.noellesroles.voice.HeliumBuzzPlayerComponent;
import org.agmas.noellesroles.voice.HeliumPitchShifter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接收端客户端变调实际处理类（仅 client sourceSet）：
 * 在 SVC 解码 PCM 后、SPR 的 OpenAL 空间化之前对说话人音频做变调处理。
 */
public final class HeliumBuzzClientReceiver {

    private static final float RAMP_OUT_TICKS = 10f;

    private static final Map<UUID, HeliumPitchShifter> SHIFTERS = new ConcurrentHashMap<>();

    private HeliumBuzzClientReceiver() {}

    public static void register(EventRegistration r) {
        r.registerEvent(ClientReceiveSoundEvent.EntitySound.class, HeliumBuzzClientReceiver::onReceiveEntity);
    }

    private static void onReceiveEntity(ClientReceiveSoundEvent.EntitySound event) {
        if (event.isCancelled()) return;

        short[] pcm = event.getRawAudio();
        if (pcm == null || pcm.length == 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        UUID speaker = event.getEntityId();
        if (speaker == null) return;

        PlayerEntity p = mc.world.getPlayerByUuid(speaker);
        if (p == null) {
            SHIFTERS.remove(speaker);
            return;
        }

        HeliumBuzzPlayerComponent comp = HeliumBuzzPlayerComponent.KEY.getNullable(p);
        boolean compActive = comp != null && comp.isActive();
        // 小丑时刻：所有 playing-and-alive 玩家嗓音至少按一级(amplifier 0)变调（状态驱动，不改 buzz 组件）
        boolean jesterBuzz = JesterMomentClient.isActive() && GameFunctions.isPlayerPlayingAndAlive(p);
        if (!compActive && !jesterBuzz) {
            SHIFTERS.remove(speaker);
            return;
        }

        float ratio = compActive ? pitchRatioFor(comp) : baseRatioFor(0);
        if (jesterBuzz) ratio = Math.max(ratio, baseRatioFor(0)); // 小丑时刻保底一级，与派对狂叠加时取更高音
        HeliumPitchShifter shifter = SHIFTERS.computeIfAbsent(speaker, k -> new HeliumPitchShifter());
        short[] shifted = shifter.process(pcm, ratio);
        event.setRawAudio(shifted);
    }

    /** amplifier → 目标音高倍率（0 = 一级）。 */
    private static float baseRatioFor(int amplifier) {
        return switch (amplifier) {
            case 0 -> 1.35f;
            case 1 -> 1.55f;
            default -> 1.75f;
        };
    }

    private static float pitchRatioFor(HeliumBuzzPlayerComponent comp) {
        float base = baseRatioFor(comp.getAmplifier());
        int remaining = comp.getTicksRemaining();
        if (remaining >= RAMP_OUT_TICKS) return base;
        float ramp = Math.max(0f, remaining / RAMP_OUT_TICKS);
        return 1f + (base - 1f) * ramp;
    }
}
