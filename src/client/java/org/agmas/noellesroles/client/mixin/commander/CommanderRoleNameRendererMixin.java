package org.agmas.noellesroles.client.mixin.commander;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RoleNameRenderer.class)
public abstract class CommanderRoleNameRendererMixin {

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void noellesroles$renderCommanderLabel(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (player == null || player.getWorld() == null) return;
        GameWorldComponent gwc = GameWorldComponent.KEY.get(player.getWorld());
        if (!gwc.canUseKillerFeatures(player)) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;

        float tickDelta = tickCounter.getTickDelta(true);
        float range = WatheClient.canSeeSpectatorInformation() ? 8f : 2f;
        Vec3d start = player.getCameraPosVec(tickDelta);
        Vec3d dir = player.getRotationVec(tickDelta);
        Vec3d end = start.add(dir.multiply(range));
        Box box = player.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, start, end, box,
                e -> e instanceof PlayerEntity pe && pe != player && !pe.isSpectator(),
                range * range);
        if (hit == null || !(hit.getEntity() instanceof PlayerEntity target)) return;
        if (target == player) return;
        if (!gwc.isRole(target, Noellesroles.COMMANDER)) return;

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f + 6f, 0f);
        context.getMatrices().scale(0.6f, 0.6f, 1f);
        Text label = Text.translatable("label.commander.title");
        int width = renderer.getWidth(label);
        int y = 20 + renderer.fontHeight * 2 + 2;
        context.drawTextWithShadow(renderer, label, -width / 2, y, 0xFF2E006B);
        context.getMatrices().pop();
    }
}
