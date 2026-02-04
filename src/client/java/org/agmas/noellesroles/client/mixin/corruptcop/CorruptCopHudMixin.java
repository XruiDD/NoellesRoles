package org.agmas.noellesroles.client.mixin.corruptcop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 黑警HUD Mixin
 * 在游戏界面右下角渲染黑警透视倒计时
 */
@Mixin(InGameHud.class)
public abstract class CorruptCopHudMixin {
    @Shadow
    public abstract TextRenderer getTextRenderer();

    private static final int VISION_OFF_DURATION = 20 * 20; // 20秒 = 400 ticks
    private static final int VISION_ON_DURATION = 10 * 20;  // 10秒 = 200 ticks
    private static final int VISION_CYCLE_TOTAL = VISION_OFF_DURATION + VISION_ON_DURATION; // 600 ticks

    @Inject(method = "render", at = @At("TAIL"))
    public void corruptCopVisionTimeHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());

        if (!gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.CORRUPT_COP)) {
            return;
        }

        CorruptCopPlayerComponent corruptCopComponent = CorruptCopPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        // 只有在黑警时刻激活时才显示
        if (!corruptCopComponent.isCorruptCopMomentActive()) {
            return;
        }

        int drawY = context.getScaledWindowHeight();
        int visionCycleTimer = corruptCopComponent.getVisionCycleTimer();
        boolean canSeeThrough = corruptCopComponent.canSeePlayersThroughWalls();

        Text line;
        if (canSeeThrough) {
            // 正在透视中，显示剩余透视时间
            int timeRemaining = (VISION_CYCLE_TOTAL - visionCycleTimer) / 20; // 转换为秒
            line = Text.translatable("tip.corrupt_cop.vision_active", timeRemaining);
        } else {
            // 不能透视，显示距离下次透视的时间
            int timeRemaining = (VISION_OFF_DURATION - visionCycleTimer) / 20; // 转换为秒
            line = Text.translatable("tip.corrupt_cop.vision_inactive", timeRemaining);
        }

        drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.CORRUPT_COP.color());
    }
}
