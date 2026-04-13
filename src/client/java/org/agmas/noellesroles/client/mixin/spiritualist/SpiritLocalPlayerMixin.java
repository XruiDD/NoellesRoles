package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 照搬 Freecam LocalPlayerMixin
 * 灵魂出窍时让 MC.player.isCamera() 返回 true，
 * 使 sendMovementPackets / tickNewAi 继续工作，保持客户端-服务器位置同步
 */
@Mixin(ClientPlayerEntity.class)
public class SpiritLocalPlayerMixin {

    @Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
    private void spiritualist$keepCamera(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritCameraHandler.isActive() && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
