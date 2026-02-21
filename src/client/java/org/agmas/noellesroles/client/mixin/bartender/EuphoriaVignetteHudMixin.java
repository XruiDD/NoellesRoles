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
 * 当玩家拥有亢奋状态效果时，在屏幕四边绘制金黄色渐变光晕，
 * 带有呼吸动画（sin 波调整透明度）。
 */
@Mixin(InGameHud.class)
public class EuphoriaVignetteHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderEuphoriaVignette(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        StatusEffectInstance euphoria = client.player.getStatusEffect(ModEffects.EUPHORIA);
        if (euphoria == null) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // 呼吸动画：用 sin 波在 0.3 ~ 0.6 之间变化
        double time = System.currentTimeMillis() / 1000.0;
        float breath = (float) (0.45 + 0.15 * Math.sin(time * 2.0));

        int alpha = (int) (breath * 255) & 0xFF;

        // 金黄色 0xFFD700
        int colorOpaque = (alpha << 24) | 0xFFD700;
        int colorTransparent = 0x00FFD700;

        int bandHeight = height / 6;
        int bandWidth = width / 8;

        // 上边缘：从不透明（顶）到透明（下）
        context.fillGradient(RenderLayer.getGuiOverlay(),
                0, 0, width, bandHeight,
                colorOpaque, colorTransparent, 0);

        // 下边缘：从透明（上）到不透明（底）
        context.fillGradient(RenderLayer.getGuiOverlay(),
                0, height - bandHeight, width, height,
                colorTransparent, colorOpaque, 0);

        // 左边缘：用多条竖带模拟水平渐变（从左侧不透明到右侧透明）
        int strips = 8;
        for (int i = 0; i < strips; i++) {
            int stripAlpha = (int) (alpha * (1.0f - (float) i / strips)) & 0xFF;
            int stripColor = (stripAlpha << 24) | 0xFFD700;
            int x0 = i * bandWidth / strips;
            int x1 = (i + 1) * bandWidth / strips;
            context.fillGradient(RenderLayer.getGuiOverlay(),
                    x0, 0, x1, height,
                    stripColor, stripColor, 0);
        }

        // 右边缘：用多条竖带模拟水平渐变（从左侧透明到右侧不透明）
        for (int i = 0; i < strips; i++) {
            int stripAlpha = (int) (alpha * ((float) (i + 1) / strips)) & 0xFF;
            int stripColor = (stripAlpha << 24) | 0xFFD700;
            int x0 = width - bandWidth + i * bandWidth / strips;
            int x1 = width - bandWidth + (i + 1) * bandWidth / strips;
            context.fillGradient(RenderLayer.getGuiOverlay(),
                    x0, 0, x1, height,
                    stripColor, stripColor, 0);
        }
    }
}
