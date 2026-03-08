package org.agmas.noellesroles.client.mixin.bartender;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.agmas.noellesroles.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 亢奋效果视觉 Mixin
 * 当玩家拥有亢奋状态效果时，在屏幕上下边缘绘制金黄色渐变光晕，
 * 带有呼吸动画（sin 波调整透明度）。
 */
@Mixin(InGameHud.class)
public class StimulationVignetteHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderStimulationVignette(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        StatusEffectInstance stimulation = client.player.getStatusEffect(ModEffects.STIMULATION);
        if (stimulation == null) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // 呼吸动画：用 sin 波在 0.3 ~ 0.6 之间变化
        double time = System.currentTimeMillis() / 1000.0;
        float breath = (float) (0.45 + 0.15 * Math.sin(time * 2.0));

        int alpha = (int) (breath * 255) & 0xFF;

        // 金黄色 0xFFD700
        int colorOpaque = (alpha << 24) | 0xFFD700;
        int colorTransparent = 0x00FFD700;

        int bandHeight = height / 5;

        // 上边缘：从不透明（顶）到透明（下）
        context.fillGradient(RenderLayer.getGuiOverlay(),
                0, 0, width, bandHeight,
                colorOpaque, colorTransparent, 0);

        // 下边缘：从透明（上）到不透明（底）
        context.fillGradient(RenderLayer.getGuiOverlay(),
                0, height - bandHeight, width, height,
                colorTransparent, colorOpaque, 0);
    }
}
