package org.agmas.noellesroles.client.mixin.vulture;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class VultureHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void vultureHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player))  return;
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.VULTURE)) {
            VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(MinecraftClient.getInstance().player);
            int drawY = context.getScaledWindowHeight();

            Text line = Text.translatable("tip.vulture", vulturePlayerComponent.getBodiesEaten(), vulturePlayerComponent.getBodiesRequired());

            if (abilityPlayerComponent.getCooldown() > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.getCooldown() / 20);
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.VULTURE.color());
        }
    }
}
