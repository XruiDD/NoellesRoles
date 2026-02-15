package org.agmas.noellesroles.client.mixin.coroner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 验尸官和秃鹫的 HUD 显示 Mixin
 * <p>
 * 此 Mixin 为验尸官和秃鹫角色添加查看尸体时的专属提示。
 * 显示内容包括：理智值检查提示、秃鹫吞噬提示等。
 * <p>
 * 注意：尸体的角色和死亡信息显示已由主模组通过 CanSeeBodyRole Event 实现，
 * 此 Mixin 只负责显示附属模组特有的功能提示。
 */
@Mixin(RoleNameRenderer.class)
public abstract class CoronerHudMixin {

    /**
     * 在 HUD 渲染末尾注入，显示验尸官和秃鹫的专属信息
     */
    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void renderCoronerHud(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (NoellesrolesClient.targetBody != null && WatheClient.isPlayerAliveAndInSurvival() && !SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            boolean isVulture = gameWorldComponent.isRole(player, Noellesroles.VULTURE);
            boolean isCoroner = gameWorldComponent.isRole(player, Noellesroles.CORONER);
            if (isVulture || isCoroner) {
                context.getMatrices().push();
                context.getMatrices().translate((float) context.getScaledWindowWidth() / 2.0F, (float) context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
                context.getMatrices().scale(0.6F, 0.6F, 1.0F);
                if(isCoroner){
                    PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
                    if (moodComponent.isLowerThanMid()) {
                        Text name = Text.translatable("hud.coroner.sanity_requirements");
                        context.drawTextWithShadow(renderer, name, -renderer.getWidth(name) / 2, 32, Colors.YELLOW);
                        context.getMatrices().pop();
                        return;
                    }
                }
                // 秃鹫专属提示
                if (isVulture) {
                    AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(player);
                    if (abilityPlayerComponent.getCooldown() <= 0) {
                        Text eatPrompt = Text.translatable("hud.vulture.eat", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()).withColor(Colors.RED);
                        context.drawTextWithShadow(renderer, eatPrompt, -renderer.getWidth(eatPrompt) / 2, 32, Colors.WHITE);
                    }
                }
                context.getMatrices().pop();
            }
        }
    }
}
