package org.agmas.noellesroles.client.mixin.scavenger;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.scavenger.ScavengerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 清道夫尸体隐藏Mixin
 * 使清道夫杀死的尸体对大部分玩家不可见
 * 但秃鹫、中立角色和旁观者可以看到
 */
@Mixin(PlayerBodyEntityRenderer.class)
public class ScavengerBodyHideMixin {

    @Inject(method = "render(Ldev/doctor4t/trainmurdermystery/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void hideScavengerBodies(PlayerBodyEntity body, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null || localPlayer.getWorld() == null) return;

        // 旁观者和创造模式始终可以看到所有尸体
        if (TMMClient.isPlayerSpectatingOrCreative()) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(localPlayer.getWorld());
        UUID deadPlayerUuid = body.getPlayerUuid();

        // 检查是否有清道夫杀死了这个玩家
        boolean isHiddenByScavenger = false;
        for (UUID scavengerUuid : gameWorldComponent.getAllWithRole(Noellesroles.SCAVENGER)) {
            PlayerEntity scavengerPlayer = localPlayer.getWorld().getPlayerByUuid(scavengerUuid);
            if (scavengerPlayer != null) {
                ScavengerPlayerComponent scavengerComp = ScavengerPlayerComponent.KEY.get(scavengerPlayer);
                if (scavengerComp.isBodyHidden(deadPlayerUuid)) {
                    isHiddenByScavenger = true;
                    break;
                }
            }
        }

        if (!isHiddenByScavenger) {
            return; // 不是清道夫的尸体，正常渲染
        }

        // 尸体是清道夫杀的，检查当前玩家是否有查看权限
        // 秃鹫可以看到
        if (gameWorldComponent.isRole(localPlayer, Noellesroles.VULTURE)) {
            return;
        }

        // 中立角色可以看到（小丑、黑警、病原体）
        if (gameWorldComponent.isRole(localPlayer, Noellesroles.JESTER) ||
            gameWorldComponent.isRole(localPlayer, Noellesroles.CORRUPT_COP) ||
            gameWorldComponent.isRole(localPlayer, Noellesroles.PATHOGEN)) {
            return;
        }

        // 其他玩家（包括杀手、无辜者、验尸官）无法看到清道夫的尸体
        ci.cancel();
    }
}
