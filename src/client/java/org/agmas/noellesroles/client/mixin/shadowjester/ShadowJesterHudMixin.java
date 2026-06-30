package org.agmas.noellesroles.client.mixin.shadowjester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 影子小丑「影誓」技能的 HUD 提示（屏幕右下角，仿侦探）。
 * 未做完四个任务时提示暂不可用；可用后提示瞄准搭档按键缔结；已发起/已绑定显示对应状态。
 */
@Mixin(InGameHud.class)
public abstract class ShadowJesterHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void shadowJesterHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(localPlayer)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(localPlayer.getWorld());
        if (!gameWorldComponent.isRole(localPlayer, Noellesroles.SHADOW_JESTER)) return;

        ShadowJesterPlayerComponent comp = ShadowJesterPlayerComponent.KEY.get(localPlayer);

        Text line;
        if (comp.isAllied()) {
            line = Text.translatable("tip.shadow_jester.hud.bound");
        } else if (comp.isAllyProposed()) {
            line = Text.translatable("tip.shadow_jester.hud.proposed");
        } else if (!comp.isKnifeGiven()) {
            // 暂不可用：需完成四个任务
            line = Text.translatable("tip.shadow_jester.hud.locked");
        } else {
            // 可用：瞄准搭档缔结影誓
            PlayerEntity target = NoellesrolesClient.crosshairTarget;
            UUID partner = comp.getPartnerUuid();
            boolean aimingPartner = target != null && partner != null
                    && partner.equals(target.getUuid()) && NoellesrolesClient.crosshairTargetDistance <= 3.0;
            if (aimingPartner) {
                line = Text.translatable("tip.shadow_jester.hud.aim",
                        NoellesrolesClient.getDisplaySafeName(target),
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            } else {
                line = Text.translatable("tip.shadow_jester.hud.ready",
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            }
        }

        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(getTextRenderer(), line,
                context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, Noellesroles.SHADOW_JESTER.color());
    }
}
