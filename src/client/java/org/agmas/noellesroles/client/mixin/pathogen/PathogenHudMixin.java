package org.agmas.noellesroles.client.mixin.pathogen;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
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
public abstract class PathogenHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void pathogenHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.PATHOGEN)) {
            int drawY = context.getScaledWindowHeight();
            Text line;

            if (abilityPlayerComponent.getCooldown() > 0) {
                // 冷却中
                line = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.getCooldown() / 20);
            } else if (NoellesrolesClient.pathogenNearestTarget != null) {
                // 有可感染目标
                String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
                String targetName = NoellesrolesClient.pathogenNearestTarget.getName().getString();
                line = Text.translatable("tip.pathogen.infect", keyName, targetName);
            } else {
                return;
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.PATHOGEN.color());
        }
    }
}
