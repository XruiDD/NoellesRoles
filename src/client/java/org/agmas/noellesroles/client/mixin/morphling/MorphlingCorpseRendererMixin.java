package org.agmas.noellesroles.client.mixin.morphling;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MorphlingCorpseRendererMixin {

    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V", at = @At("HEAD"), cancellable = true)
    void noellesroles$corpseTransforms(AbstractClientPlayerEntity player, MatrixStack matrices,
            float animationProgress, float bodyYaw, float tickDelta, float scale, CallbackInfo ci) {
        MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(player);
        if (comp.corpseMode) {
            // 应用尸体躺倒变换（参考 PlayerBodyEntityRenderer.setupTransforms）
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 - bodyYaw));
            matrices.translate(1F, 0f, 0f);
            matrices.translate(0F, 0.15f, 0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            ci.cancel();
        }
    }
}
