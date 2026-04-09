package org.agmas.noellesroles.client.mixin.saint;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class SaintHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderSaintHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(client.player)) {
            return;
        }

        SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(client.player);
        if (!saintComponent.isKarmaLocked()) {
            return;
        }

        Text line = Text.translatable("tip.saint.karma_locked", Math.max(1, saintComponent.getKarmaLockTicks() / 20));
        int drawY = context.getScaledWindowHeight() - getTextRenderer().fontHeight;
        context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, 0xF29A64);
    }
}
