package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.spiritualist.SpiritCamera;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 灵魂出窍时让 MC.player 的本体可见
 *
 * 原版 WorldRenderer 实体渲染条件第 977 行：
 *   (!(entity instanceof ClientPlayerEntity) || camera.getFocusedEntity() == entity)
 *
 * 当 camera focus 是 SpiritCamera 时，MC.player 因为不等于 focusedEntity 被跳过。
 * 这里拦截 instanceof 检查：灵魂出窍期间让 MC.player 不被认为是 ClientPlayerEntity，
 * 使条件短路为 true，允许渲染。
 */
@Mixin(WorldRenderer.class)
public class SpiritWorldRendererMixin {

    /**
     * 拦截 WorldRenderer.render 方法中第 977 行的 camera.getFocusedEntity() 调用
     * 条件：!(entity instanceof ClientPlayerEntity) || camera.getFocusedEntity() == entity
     * 灵魂出窍时返回 MC.player，使 == 比较对 MC.player 为 true，允许本体渲染
     *
     * render 方法中 getFocusedEntity 调用顺序：
     *   ordinal 0 = 973行 entity != camera.getFocusedEntity()
     *   ordinal 1 = 975行 camera.getFocusedEntity() instanceof LivingEntity
     *   ordinal 2 = 975行 ((LivingEntity)camera.getFocusedEntity()).isSleeping()
     *   ordinal 3 = 977行 camera.getFocusedEntity() == entity  ← 目标
     */
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;getFocusedEntity()Lnet/minecraft/entity/Entity;",
            ordinal = 3))
    private Entity spiritualist$allowPlayerRendering(Camera camera) {
        Entity focused = camera.getFocusedEntity();
        if (SpiritCameraHandler.isActive() && focused instanceof SpiritCamera) {
            // 返回 MC.player，使 camera.getFocusedEntity() == entity 对 MC.player 为 true
            return MinecraftClient.getInstance().player;
        }
        return focused;
    }
}
