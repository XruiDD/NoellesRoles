package org.agmas.noellesroles.client.mixin.taotie;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class TaotieHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void taotieHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        PlayerEntity localPlayer = MinecraftClient.getInstance().player;

        if (gameWorldComponent.isRole(localPlayer, Noellesroles.TAOTIE)) {
            TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(localPlayer);
            int drawY = context.getScaledWindowHeight();
            int color = Noellesroles.TAOTIE.color();

            // Display Taotie Moment countdown if active
            if (taotieComp.isTaotieMomentActive()) {
                int secondsLeft = taotieComp.getTaotieMomentTicks() / 20;
                Text momentText = Text.translatable("tip.taotie.moment_active", secondsLeft);
                drawY -= getTextRenderer().getWrappedLinesHeight(momentText, 999999);
                context.drawTextWithShadow(getTextRenderer(), momentText,
                        context.getScaledWindowWidth() - getTextRenderer().getWidth(momentText), drawY, 0xFF0000);
                drawY -= 2;
            }

            // Display swallowed count
            int swallowedCount = taotieComp.getSwallowedCount();
            if (swallowedCount > 0) {
                Text swallowedText = Text.translatable("tip.taotie.swallowed_count", swallowedCount);
                drawY -= getTextRenderer().getWrappedLinesHeight(swallowedText, 999999);
                context.drawTextWithShadow(getTextRenderer(), swallowedText,
                        context.getScaledWindowWidth() - getTextRenderer().getWidth(swallowedText), drawY, color);
                drawY -= 2;
            }

            // Display cooldown if active
            if (taotieComp.getSwallowCooldown() > 0) {
                int cooldownSeconds = taotieComp.getSwallowCooldown() / 20;
                Text cooldownText = Text.translatable("tip.noellesroles.cooldown", cooldownSeconds);
                drawY -= getTextRenderer().getWrappedLinesHeight(cooldownText, 999999);
                context.drawTextWithShadow(getTextRenderer(), cooldownText,
                        context.getScaledWindowWidth() - getTextRenderer().getWidth(cooldownText), drawY, color);
                drawY -= 2;
            }

            // Display swallow hint if crosshair is on a valid target
            if (NoellesrolesClient.crosshairTarget != null && NoellesrolesClient.crosshairTargetDistance <= 3.0) {
                // Check if target is not already swallowed
                SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(NoellesrolesClient.crosshairTarget);
                if (!swallowed.isSwallowed() && taotieComp.getSwallowCooldown() <= 0) {
                    String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
                    String targetName = NoellesrolesClient.crosshairTarget.getName().getString();
                    Text swallowHint = Text.translatable("tip.taotie.swallow", keyName, targetName);
                    drawY -= getTextRenderer().getWrappedLinesHeight(swallowHint, 999999);
                    context.drawTextWithShadow(getTextRenderer(), swallowHint,
                            context.getScaledWindowWidth() - getTextRenderer().getWidth(swallowHint), drawY, color);
                }
            }
        }
    }
}
