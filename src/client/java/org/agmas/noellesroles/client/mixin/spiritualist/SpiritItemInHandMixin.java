package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灵魂出窍时让手臂运动和光照跟随 SpiritCamera 而非真实玩家
 */
@Mixin(HeldItemRenderer.class)
public class SpiritItemInHandMixin {

    @Unique
    private float spiritualist$tickDelta;

    // 替换 player 参数为 SpiritCamera，使手臂运动跟随灵魂相机
    @ModifyVariable(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), argsOnly = true)
    private ClientPlayerEntity spiritualist$useSpirit(ClientPlayerEntity player) {
        if (SpiritCameraHandler.isActive() && SpiritCameraHandler.getSpiritCamera() != null) {
            return SpiritCameraHandler.getSpiritCamera();
        }
        return player;
    }

    // 缓存 tickDelta 供光照计算使用
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void spiritualist$storeTickDelta(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        this.spiritualist$tickDelta = tickDelta;
    }

    // 替换 light 参数为基于 SpiritCamera 位置计算的光照值
    @ModifyVariable(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), argsOnly = true)
    private int spiritualist$useSpiritLight(int light) {
        if (SpiritCameraHandler.isActive() && SpiritCameraHandler.getSpiritCamera() != null) {
            return MinecraftClient.getInstance().getEntityRenderDispatcher().getLight(SpiritCameraHandler.getSpiritCamera(), spiritualist$tickDelta);
        }
        return light;
    }
}
