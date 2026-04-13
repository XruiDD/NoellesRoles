package org.agmas.noellesroles.client.demonhunter;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.particle.HandParticle;
import dev.doctor4t.wathe.client.render.WatheRenderLayers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.demonhunter.DemonHunterPistolItem;
import org.agmas.noellesroles.demonhunter.DemonHunterShootC2SPacket;

/**
 * 猎魔手枪的客户端射击逻辑：发送射击包 + 播放手部粒子 + 后坐力。
 * <p>
 * 此类仅在客户端 source set 中编译和使用。
 */
public final class DemonHunterClientHelper {

    private DemonHunterClientHelper() {
    }

    /**
     * 在客户端初始化时调用，注入射击回调。
     */
    public static void init() {
        DemonHunterPistolItem.clientShootHandler = DemonHunterClientHelper::handleClientShoot;
    }

    private static void handleClientShoot(PlayerEntity user, int bullets) {
        HitResult collision = DemonHunterPistolItem.getGunTarget(user);
        int targetId = DemonHunterPistolItem.resolveTargetFromHitResult(user.getWorld(), collision);
        ClientPlayNetworking.send(new DemonHunterShootC2SPacket(targetId));
        if (bullets > 0) {
            user.setPitch(user.getPitch() - 4);
            spawnHandParticle();
        }
    }

    private static void spawnHandParticle() {
        HandParticle handParticle = new HandParticle()
                .setTexture(Wathe.id("textures/particle/gunshot.png"))
                .setPos(0.1f, 0.2f, -0.2f)
                .setMaxAge(3)
                .setSize(0.5f)
                .setVelocity(0f, 0f, 0f)
                .setLight(15, 15)
                .setAlpha(1f, 0.1f)
                .setRenderLayer(WatheRenderLayers::additive);
        WatheClient.handParticleManager.spawn(handParticle);
    }
}
