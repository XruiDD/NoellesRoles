package org.agmas.noellesroles.client.voice;

import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
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
        if (comp == null || !comp.isActive()) {
            SHIFTERS.remove(speaker);
            return;
        }

        float ratio = pitchRatioFor(comp);
        HeliumPitchShifter shifter = SHIFTERS.computeIfAbsent(speaker, k -> new HeliumPitchShifter());
        short[] shifted = shifter.process(pcm, ratio);
        event.setRawAudio(shifted);
    }

    private static float pitchRatioFor(HeliumBuzzPlayerComponent comp) {
        float base = switch (comp.getAmplifier()) {
            case 0 -> 1.35f;
            case 1 -> 1.55f;
            default -> 1.75f;
        };
        int remaining = comp.getTicksRemaining();
        if (remaining >= RAMP_OUT_TICKS) return base;
        float ramp = Math.max(0f, remaining / RAMP_OUT_TICKS);
        return 1f + (base - 1f) * ramp;
    }
}
