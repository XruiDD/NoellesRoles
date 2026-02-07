package org.agmas.noellesroles.client.mixin.silencer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.silencer.SilencerPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class SilencerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void silencerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;
        if (SwallowedPlayerComponent.isPlayerSwallowed(MinecraftClient.getInstance().player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        PlayerEntity localPlayer = MinecraftClient.getInstance().player;

        if (gameWorldComponent.isRole(localPlayer, Noellesroles.SILENCER)) {
            AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(localPlayer);
            SilencerPlayerComponent silencerComp = SilencerPlayerComponent.KEY.get(localPlayer);
            int drawY = context.getScaledWindowHeight();
            int color = Noellesroles.SILENCER.color();

            if (abilityComp.getCooldown() > 0) {
                int cooldownSeconds = abilityComp.getCooldown() / 20;
                Text cooldownText = Text.translatable("tip.noellesroles.cooldown", cooldownSeconds);
                drawY -= getTextRenderer().getWrappedLinesHeight(cooldownText, 999999);
                context.drawTextWithShadow(getTextRenderer(), cooldownText,
                        context.getScaledWindowWidth() - getTextRenderer().getWidth(cooldownText), drawY, color);
            } else if (silencerComp.hasMarkedTarget()) {
                // 已标记目标 → 显示确认释放提示
                String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
                int remainingSeconds = silencerComp.getMarkTicksRemaining() / 20;
                Text confirmHint = Text.translatable("tip.silencer.confirm", keyName, silencerComp.getMarkedTargetName(), remainingSeconds);
                drawY -= getTextRenderer().getWrappedLinesHeight(confirmHint, 999999);
                context.drawTextWithShadow(getTextRenderer(), confirmHint,
                        context.getScaledWindowWidth() - getTextRenderer().getWidth(confirmHint), drawY, color);
            } else {
                // 没有标记 → 显示标记提示
                if (NoellesrolesClient.crosshairTarget != null && NoellesrolesClient.crosshairTargetDistance <= 3.0) {
                    String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
                    String targetName = NoellesrolesClient.crosshairTarget.getName().getString();
                    Text markHint = Text.translatable("tip.silencer.mark", keyName, targetName);
                    drawY -= getTextRenderer().getWrappedLinesHeight(markHint, 999999);
                    context.drawTextWithShadow(getTextRenderer(), markHint,
                            context.getScaledWindowWidth() - getTextRenderer().getWidth(markHint), drawY, color);
                } else {
                    String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
                    Text readyHint = Text.translatable("tip.silencer.ready", keyName);
                    drawY -= getTextRenderer().getWrappedLinesHeight(readyHint, 999999);
                    context.drawTextWithShadow(getTextRenderer(), readyHint,
                            context.getScaledWindowWidth() - getTextRenderer().getWidth(readyHint), drawY, color);
                }
            }
        }
    }
}
