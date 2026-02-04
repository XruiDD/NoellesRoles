package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MorphlingHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void morphlingHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        MorphlingPlayerComponent morphlingPlayerComponent = MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        if (!GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player)) return;
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.MORPHLING)) {
            int drawY = context.getScaledWindowHeight();
            int morphTicks = morphlingPlayerComponent.getMorphTicks();

            Text line;
            if (morphTicks > 0) {
                // 变形中，显示变形剩余时间
                line = Text.translatable("tip.morphling.active", morphTicks / 20);
            } else if (morphTicks < 0) {
                // 冷却中，显示冷却倒计时
                line = Text.translatable("tip.noellesroles.cooldown", (-morphTicks) / 20);
            } else {
                // 准备就绪，显示提示
                line = Text.translatable("tip.morphling");
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.MORPHLING.color());
        }
    }
}
