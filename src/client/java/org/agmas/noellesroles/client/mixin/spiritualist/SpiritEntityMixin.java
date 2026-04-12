package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通灵者灵魂出窍：
 * 1. 将鼠标视角输入从 MC.player 重定向到 SpiritCamera
 * 2. 防止 SpiritCamera 和其他实体互相推挤
 */
@Mixin(Entity.class)
public class SpiritEntityMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void spiritualist$redirectMouseToSpirit(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (SpiritCameraHandler.isActive() && (Object) this == MinecraftClient.getInstance().player) {
            SpiritCameraHandler.getSpiritCamera().changeLookDirection(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void spiritualist$noPush(Entity entity, CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            var spirit = SpiritCameraHandler.getSpiritCamera();
            if (spirit != null && (entity == spirit || (Object) this == spirit)) {
                ci.cancel();
            }
        }
    }
}
