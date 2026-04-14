package org.agmas.noellesroles.client.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.voice.HeliumBuzzPlayerComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接收端客户端变调插件：根据 {@link HeliumBuzzPlayerComponent}（CCA 自动同步到所有
 * 追踪者）判断说话人是否该变调，在 SVC 解码 PCM 后、SPR 的 OpenAL 空间化之前处理。
 */
public class HeliumBuzzVoicechatPlugin implements VoicechatPlugin {

    private static final float RAMP_OUT_TICKS = 10f;

    private final Map<UUID, HeliumPitchShifter> shifters = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "noellesroles_helium_buzz";
    }

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(ClientReceiveSoundEvent.EntitySound.class, this::onReceiveEntity);
    }

    private void onReceiveEntity(ClientReceiveSoundEvent.EntitySound event) {
        if (event.isCancelled()) return;

        short[] pcm = event.getRawAudio();
        if (pcm == null || pcm.length == 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        UUID speaker = event.getEntityId();
        if (speaker == null) return;

        PlayerEntity p = mc.world.getPlayerByUuid(speaker);
        if (p == null) {
            shifters.remove(speaker);
            return;
        }

        HeliumBuzzPlayerComponent comp = HeliumBuzzPlayerComponent.KEY.getNullable(p);
        if (comp == null || !comp.isActive()) {
            shifters.remove(speaker);
            return;
        }

        float ratio = pitchRatioFor(comp);
        HeliumPitchShifter shifter = shifters.computeIfAbsent(speaker, k -> new HeliumPitchShifter());
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
