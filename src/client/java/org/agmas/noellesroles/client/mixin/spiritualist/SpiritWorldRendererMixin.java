package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.tick.TickManager;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILHARD;

/**
 * 灵魂出窍时渲染真实玩家本体
 * 照搬 Freecam LevelRendererMixin 方案
 */
@Mixin(WorldRenderer.class)
public abstract class SpiritWorldRendererMixin {

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Shadow protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V", ordinal = 0), locals = CAPTURE_FAILHARD)
    private void spiritualist$renderPlayerBody(RenderTickCounter tickCounter,
                                               boolean renderBlockOutline,
                                               Camera camera,
                                               GameRenderer gameRenderer,
                                               LightmapTextureManager lightmapTextureManager,
                                               Matrix4f matrix4f,
                                               Matrix4f matrix4f2,
                                               CallbackInfo ci,
                                               // Local capture
                                               TickManager tickManager,
                                               float tickDelta,
                                               Profiler profiler,
                                               Vec3d cameraPosition,
                                               double x,
                                               double y,
                                               double z,
                                               boolean hasCapturedFrustum,
                                               Frustum frustum,
                                               float viewDistance,
                                               boolean thickFog,
                                               Matrix4fStack modelViewStack,
                                               boolean hasOutline,
                                               MatrixStack matrices,
                                               VertexConsumerProvider.Immediate immediate) {
        if (SpiritCameraHandler.isActive()) {
            renderEntity(MinecraftClient.getInstance().player, x, y, z, tickDelta, matrices, this.bufferBuilders.getEntityVertexConsumers());
        }
    }
}
