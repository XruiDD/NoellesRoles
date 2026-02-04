package org.agmas.noellesroles.client.mixin.reporter;

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
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class ReporterHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void reporterHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        ReporterPlayerComponent reporterPlayerComponent = ReporterPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.REPORTER)) {
            int drawY = context.getScaledWindowHeight();

            // 第二行：已标记的目标名字（如果有）
            if (reporterPlayerComponent.hasMarkedTarget()) {
                PlayerEntity markedTarget = MinecraftClient.getInstance().player.getWorld().getPlayerByUuid(reporterPlayerComponent.getMarkedTarget());
                if (markedTarget != null) {
                    Text line2 = Text.translatable("tip.reporter.marked", markedTarget.getName());
                    drawY -= getTextRenderer().getWrappedLinesHeight(line2, 999999);
                    context.drawTextWithShadow(getTextRenderer(), line2, context.getScaledWindowWidth() - getTextRenderer().getWidth(line2), drawY, Noellesroles.REPORTER.color());
                }
            }

            // 第一行：冷却时间或当前准心目标+按键提示
            Text line1;
            if (abilityPlayerComponent.cooldown > 0) {
                // 冷却中，显示冷却时间
                line1 = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.cooldown / 20);
            } else {
                // 未冷却，显示准心目标和按键（3格内）
                PlayerEntity target = NoellesrolesClient.crosshairTarget;
                if (target != null && NoellesrolesClient.crosshairTargetDistance <= 3.0) {
                    line1 = Text.translatable("tip.reporter.target", target.getName(), NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
                } else {
                    line1 = Text.translatable("tip.reporter.no_target");
                }
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line1, 999999);
            context.drawTextWithShadow(getTextRenderer(), line1, context.getScaledWindowWidth() - getTextRenderer().getWidth(line1), drawY, Noellesroles.REPORTER.color());
        }
    }
}
