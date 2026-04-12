package org.agmas.noellesroles.client.mixin.spiritualist;

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
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class SpiritHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void spiritualistHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        if (!gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.SPIRITUALIST)) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        int drawY = context.getScaledWindowHeight();
        Text line;

        if (spiritComp.isProjecting()) {
            // 灵魂出窍中
            line = Text.translatable("tip.spiritualist.active", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        } else if (abilityComp.cooldown > 0) {
            // 冷却中
            line = Text.translatable("tip.noellesroles.cooldown", abilityComp.cooldown / 20);
        } else {
            // 可以使用
            line = Text.translatable("tip.spiritualist", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        }

        drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.SPIRITUALIST.color());
    }
}
