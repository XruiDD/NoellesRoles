package org.agmas.noellesroles.client.mixin.assassin;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(InGameHud.class)
public abstract class AssassinHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderAssassinHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        if (!gameWorld.isRole(MinecraftClient.getInstance().player, Noellesroles.ASSASSIN)) return;
        if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) return;

        AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int drawY = screenHeight - 5; // 从底部开始，留5像素边距

        // 显示剩余次数（右下角）
        Text guessesText = Text.translatable("hud.assassin.guesses_remaining",
            assassinComp.getGuessesRemaining(), assassinComp.getMaxGuesses());

        drawY -= getTextRenderer().getWrappedLinesHeight(guessesText, 999999);

        int guessesColor = assassinComp.getGuessesRemaining() > 0 ? 0xAA00FF00 : 0xAAFF0000; // 半透明绿色/红色
        context.drawTextWithShadow(getTextRenderer(), guessesText,
            screenWidth - getTextRenderer().getWidth(guessesText) - 5, drawY, guessesColor);

        // 显示冷却时间（右下角）
        if (assassinComp.getCooldownTicks() > 0) {
            int cooldownSeconds = (assassinComp.getCooldownTicks() + 19) / 20;
            Text cooldownText = Text.translatable("hud.assassin.cooldown", cooldownSeconds);

            drawY -= getTextRenderer().getWrappedLinesHeight(cooldownText, 999999) + 2;

            context.drawTextWithShadow(getTextRenderer(), cooldownText,
                screenWidth - getTextRenderer().getWidth(cooldownText) - 5, drawY, 0xAAFFFF00); // 半透明黄色
        }

        // 显示按键提示（右下角，可用时）
        if (assassinComp.canGuess()) {
            String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            Text hintText = Text.translatable("hud.assassin.press_key_hint", keyName);

            drawY -= getTextRenderer().getWrappedLinesHeight(hintText, 999999) + 2;

            // 柔和的呼吸效果
            float time = (System.currentTimeMillis() % 2000) / 2000f;
            float pulse = (float) (Math.sin(time * Math.PI * 2) * 0.15 + 0.75); // 降低脉动幅度
            int alpha = (int) (170 * pulse); // 半透明
            int red = 255;
            int green = 200;
            int color = (alpha << 24) | (red << 16) | (green << 8);

            context.drawTextWithShadow(getTextRenderer(), hintText,
                screenWidth - getTextRenderer().getWidth(hintText) - 5, drawY, color);
        }

        // 显示状态提示（右下角，不可用时）
        if (!assassinComp.canGuess()) {
            Text statusText;
            int statusColor;

            if (assassinComp.getCooldownTicks() > 0) {
                statusText = Text.translatable("hud.assassin.on_cooldown");
                statusColor = 0x99FFAA00; // 半透明橙色
            } else if (assassinComp.getGuessesRemaining() <= 0) {
                statusText = Text.translatable("hud.assassin.no_guesses");
                statusColor = 0x99FF0000; // 半透明红色
            } else {
                return;
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(statusText, 999999) + 2;

            context.drawTextWithShadow(getTextRenderer(), statusText,
                screenWidth - getTextRenderer().getWidth(statusText) - 5, drawY, statusColor);
        }
    }
}
