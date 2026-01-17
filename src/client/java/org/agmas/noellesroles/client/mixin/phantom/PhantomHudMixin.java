package org.agmas.noellesroles.client.mixin.phantom;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class PhantomHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void phantomHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player))  return;
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.PHANTOM)) {
            int drawY = context.getScaledWindowHeight();

            Text line = Text.translatable("tip.phantom", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());

            // 检查是否处于隐身状态
            StatusEffectInstance invisEffect = MinecraftClient.getInstance().player.getStatusEffect(StatusEffects.INVISIBILITY);
            if (invisEffect != null) {
                // 隐身中，显示剩余时间
                line = Text.translatable("tip.phantom.active", invisEffect.getDuration() / 20);
            } else if (abilityPlayerComponent.cooldown > 0) {
                // 冷却中
                line = Text.translatable("tip.noellesroles.cooldown", abilityPlayerComponent.cooldown/20);
            }

            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Colors.RED);
        }
    }
}
