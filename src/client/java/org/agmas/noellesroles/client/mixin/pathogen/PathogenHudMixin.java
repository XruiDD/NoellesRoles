package org.agmas.noellesroles.client.mixin.pathogen;

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
public abstract class PathogenHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void pathogenHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player))  return;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        PlayerEntity localPlayer = MinecraftClient.getInstance().player;

        if (gameWorldComponent.isRole(localPlayer, Noellesroles.PATHOGEN)) {
            int drawY = context.getScaledWindowHeight();
            Text line = null;
            Text actionBarText = null;

            if (abilityPlayerComponent.getCooldown() > 0) {
                // 冷却中
                line = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.getCooldown() / 20);
            }

            if (NoellesrolesClient.pathogenNearestTarget != null) {
                // 检查目标是否在3格内且可见（可以感染）
                double distanceSquared = localPlayer.squaredDistanceTo(NoellesrolesClient.pathogenNearestTarget);
                boolean canInfect = distanceSquared < 9.0 && localPlayer.canSee(NoellesrolesClient.pathogenNearestTarget);

                if (canInfect && abilityPlayerComponent.getCooldown() <= 0) {
                    // 可以感染 - 显示感染提示
                    String keyName = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
                    String targetName = NoellesrolesClient.pathogenNearestTarget.getName().getString();
                    line = Text.translatable("tip.pathogen.infect", keyName, targetName);
                }

                // ActionBar 显示方向和距离（始终显示）
                String direction = NoellesrolesClient.pathogenTargetDirection;
                String vertical = NoellesrolesClient.pathogenTargetVertical;
                int distance = (int) Math.round(NoellesrolesClient.pathogenNearestTargetDistance);

                // 构建 ActionBar 文本
                String verticalText = vertical != null && !vertical.isEmpty() ? " " + vertical : "";
                actionBarText = Text.translatable("tip.pathogen.compass", direction, distance, verticalText);
            } else {
                // 没有未感染目标
                actionBarText = Text.translatable("tip.pathogen.no_target");
            }

            // 显示 ActionBar
            if (actionBarText != null) {
                MinecraftClient.getInstance().inGameHud.setOverlayMessage(actionBarText, false);
            }

            // 显示右下角提示（冷却或感染提示）
            if (line != null) {
                drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
                context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.PATHOGEN.color());
            }
        }
    }
}
