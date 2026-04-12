package org.agmas.noellesroles.client.mixin.detective;

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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class DetectiveHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void detectiveHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.DETECTIVE)) {
            int drawY = context.getScaledWindowHeight();

            Text line;
            if (abilityPlayerComponent.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.cooldown / 20);
            } else {
                PlayerEntity target = NoellesrolesClient.crosshairTarget;
                if (target != null && NoellesrolesClient.crosshairTargetDistance <= 3.0) {
                    line = Text.translatable("tip.detective.target", NoellesrolesClient.getDisplaySafeName(target), NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
                } else {
                    line = Text.translatable("tip.detective.no_target");
                }
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.DETECTIVE.color());
        }
    }
}
