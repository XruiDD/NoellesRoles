package org.agmas.noellesroles.client.mixin.criminalreasoner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
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

        String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
        Text hintText = Text.translatable("hud.criminal_reasoner.press_key_hint", keyName);

        // 将提示固定绘制在右下角，并与现有角色 HUD 的边距保持一致。
        int drawX = context.getScaledWindowWidth() - getTextRenderer().getWidth(hintText) - 5;
        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(hintText, 999999);

        // 文本颜色使用角色色 RGB(112, 75, 75)。
        context.drawTextWithShadow(getTextRenderer(), hintText, drawX, drawY, 0x704B4B);
    }
}
