package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.agmas.noellesroles.client.spiritualist.SpiritCamera;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵魂出窍相机修正：
 * 1. 切换相机时立即更新眼高，避免过渡动画跳动
 * 2. 移除水下/岩浆/粉雪的浸没覆盖层
 */
@Mixin(Camera.class)
public class SpiritCameraMixin {

    @Shadow private Entity focusedEntity;
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;

    @Inject(method = "update", at = @At("HEAD"))
    private void spiritualist$fixEyeHeight(BlockView area, Entity newFocusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (newFocusedEntity == null || this.focusedEntity == null || newFocusedEntity.equals(this.focusedEntity)) {
            return;
        }
        if (newFocusedEntity instanceof SpiritCamera || this.focusedEntity instanceof SpiritCamera) {
            this.lastCameraY = this.cameraY = newFocusedEntity.getStandingEyeHeight();
        }
    }

    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    private void spiritualist$noSubmersion(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
