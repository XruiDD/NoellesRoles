package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通灵者灵魂出窍黑白滤镜
 * 使用 MC 内置的后处理着色器系统，加载自定义灰度 shader
 */
@Mixin(GameRenderer.class)
public abstract class SpiritVisionMixin {

    @Shadow PostEffectProcessor postProcessor;
    @Shadow private boolean postProcessorEnabled;

    @Shadow protected abstract void loadPostProcessor(Identifier id);

    @Unique
    private static final Identifier SPIRIT_SHADER = Identifier.ofVanilla("shaders/post/spirit_grayscale.json");

    @Unique
    private boolean spiritShaderActive = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void spiritualist$manageShader(CallbackInfo ci) {
        if (SpiritCameraHandler.isActive() && !spiritShaderActive) {
            // 灵魂出窍刚激活，加载灰度 shader
            loadPostProcessor(SPIRIT_SHADER);
            spiritShaderActive = true;
        } else if (!SpiritCameraHandler.isActive() && spiritShaderActive) {
            // 灵魂出窍结束，卸载 shader
            if (this.postProcessor != null) {
                this.postProcessor.close();
            }
            this.postProcessor = null;
            this.postProcessorEnabled = false;
            spiritShaderActive = false;
        }
    }
}
