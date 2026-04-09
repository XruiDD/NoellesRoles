package org.agmas.noellesroles.client.mixin.criminalreasoner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.criminalreasoner.CriminalReasonerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class CriminalReasonerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderCriminalReasonerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        if (!gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.CRIMINAL_REASONER)) return;

        CriminalReasonerPlayerComponent criminalReasonerComponent = CriminalReasonerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        Text cooldownText;
        if (abilityPlayerComponent.getCooldown() > 0) {
            cooldownText = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.getCooldown() / 20);
        } else {
            String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            cooldownText = Text.translatable("hud.criminal_reasoner.press_key_hint", keyName);
        }

        int requiredReasoningCount = Math.floorDiv(gameWorldComponent.getAllPlayers().size(), 3);
        Text progressText = Text.translatable(
                "hud.criminal_reasoner.progress",
                criminalReasonerComponent.getSuccessfulReasoningCount(),
                requiredReasoningCount
        );

        int progressY = context.getScaledWindowHeight() - 25;
        int cooldownY = context.getScaledWindowHeight() - 15;
        int progressX = context.getScaledWindowWidth() - getTextRenderer().getWidth(progressText);
        int cooldownX = context.getScaledWindowWidth() - getTextRenderer().getWidth(cooldownText);
        context.drawTextWithShadow(getTextRenderer(), progressText, progressX, progressY, 0xA37D7D);
        context.drawTextWithShadow(getTextRenderer(), cooldownText, cooldownX, cooldownY, 0x704B4B);
    }
}
