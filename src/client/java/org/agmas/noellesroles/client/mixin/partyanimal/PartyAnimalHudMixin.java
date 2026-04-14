package org.agmas.noellesroles.client.mixin.partyanimal;

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
import org.agmas.noellesroles.partyanimal.PartyAnimalPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.voice.HeliumBuzzPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class PartyAnimalHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void partyAnimalHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(mc.player)) return;
        if (SwallowedPlayerComponent.isPlayerSwallowed(mc.player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(mc.player.getWorld());
        PlayerEntity localPlayer = mc.player;

        if (!gameWorldComponent.isRole(localPlayer, Noellesroles.PARTY_ANIMAL)) return;

        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(localPlayer);
        PartyAnimalPlayerComponent partyComp = PartyAnimalPlayerComponent.KEY.get(localPlayer);
        int drawY = context.getScaledWindowHeight();
        int color = Noellesroles.PARTY_ANIMAL.color();

        if (abilityComp.getCooldown() > 0) {
            int cooldownSeconds = abilityComp.getCooldown() / 20;
            Text cooldownText = Text.translatable("tip.noellesroles.cooldown", cooldownSeconds);
            drawY -= getTextRenderer().getWrappedLinesHeight(cooldownText, 999999);
            context.drawTextWithShadow(getTextRenderer(), cooldownText,
                    context.getScaledWindowWidth() - getTextRenderer().getWidth(cooldownText), drawY, color);
        } else if (partyComp.hasMarkedTarget()) {
            String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            int remainingSeconds = partyComp.getMarkTicksRemaining() / 20;
            Text confirmHint = Text.translatable("tip.partyanimal.confirm", keyName, partyComp.getMarkedTargetName(), remainingSeconds);
            drawY -= getTextRenderer().getWrappedLinesHeight(confirmHint, 999999);
            context.drawTextWithShadow(getTextRenderer(), confirmHint,
                    context.getScaledWindowWidth() - getTextRenderer().getWidth(confirmHint), drawY, color);
        } else {
            String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            Text hint;
            if (NoellesrolesClient.crosshairTarget != null && NoellesrolesClient.crosshairTargetDistance <= 3.0) {
                PlayerEntity crosshair = NoellesrolesClient.crosshairTarget;
                HeliumBuzzPlayerComponent buzz = HeliumBuzzPlayerComponent.KEY.get(crosshair);
                int lvl = buzz.isActive() ? buzz.getAmplifier() + 1 : 0;
                if (lvl >= 3) {
                    hint = Text.translatable("tip.partyanimal.maxed", crosshair.getName().getString());
                } else {
                    hint = Text.translatable("tip.partyanimal.mark", keyName, crosshair.getName().getString());
                }
            } else {
                hint = Text.translatable("tip.partyanimal.ready_or_self", keyName);
            }
            drawY -= getTextRenderer().getWrappedLinesHeight(hint, 999999);
            context.drawTextWithShadow(getTextRenderer(), hint,
                    context.getScaledWindowWidth() - getTextRenderer().getWidth(hint), drawY, color);
        }
    }
}
