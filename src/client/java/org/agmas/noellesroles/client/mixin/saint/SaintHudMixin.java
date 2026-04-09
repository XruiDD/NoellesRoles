package org.agmas.noellesroles.client.mixin.saint;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.util.HudRenderHelper;
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
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(player);
        if (!saintComponent.isKarmaLocked()) return;

        Text line = Text.translatable("tip.saint.karma_locked", Math.max(1, saintComponent.getKarmaLockTicks() / 20));
        int drawY = context.getScaledWindowHeight() - getTextRenderer().fontHeight;
        context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, 0xF29A64);
    }
}
