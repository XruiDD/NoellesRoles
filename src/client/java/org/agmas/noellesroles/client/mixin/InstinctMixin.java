package org.agmas.noellesroles.client.mixin;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerPoisonComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.client.gui.RoundTextRenderer;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.executioner.ExecutionerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(TMMClient.class)
public abstract class InstinctMixin {


    @Shadow public static KeyBinding instinctKeybind;

    // Helper method to check if there's a clear line of sight between player and target
    private static boolean hasLineOfSight(PlayerEntity viewer, PlayerEntity target) {
        Vec3d viewerEyes = viewer.getEyePos();
        Vec3d targetEyes = target.getEyePos();

        RaycastContext context = new RaycastContext(
            viewerEyes,
            targetEyes,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            viewer
        );

        HitResult result = viewer.getWorld().raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void b(CallbackInfoReturnable<Boolean> cir) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void b(Entity target, CallbackInfoReturnable<Integer> cir) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        if (target instanceof PlayerEntity) {
            if (!((PlayerEntity)target).isSpectator()) {
                // Check line of sight for BARTENDER
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.BARTENDER)) {
                    if (hasLineOfSight(MinecraftClient.getInstance().player, (PlayerEntity) target)) {
                        BartenderPlayerComponent bartenderPlayerComponent = BartenderPlayerComponent.KEY.get((PlayerEntity) target);
                        if (bartenderPlayerComponent.glowTicks > 0) {
                            cir.setReturnValue(Color.GREEN.getRGB());
                            return;
                        }
                        if (bartenderPlayerComponent.armor > 0) {
                            cir.setReturnValue(Color.BLUE.getRGB());
                            cir.cancel();
                            return;
                        }
                    }
                }

                // Check line of sight for TOXICOLOGIST
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.TOXICOLOGIST)) {
                    if (hasLineOfSight(MinecraftClient.getInstance().player, (PlayerEntity) target)) {
                        PlayerPoisonComponent playerPoisonComponent = PlayerPoisonComponent.KEY.get((PlayerEntity) target);
                        if (playerPoisonComponent.poisonTicks > 0) {
                            cir.setReturnValue(Color.RED.getRGB());
                            cir.cancel();
                            return;
                        }
                    }
                }
            }
        }
        if (target instanceof PlayerEntity) {
            if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.EXECUTIONER)) {
                ExecutionerPlayerComponent executionerPlayerComponent = (ExecutionerPlayerComponent) ExecutionerPlayerComponent.KEY.get((PlayerEntity) MinecraftClient.getInstance().player);
                if (executionerPlayerComponent.target.equals(target.getUuid())) {
                    cir.setReturnValue(Color.YELLOW.getRGB());
                    cir.cancel();
                }
            }
            if (!((PlayerEntity)target).isSpectator() && TMMClient.isInstinctEnabled()) {
                if (gameWorldComponent.isRole((PlayerEntity) target, Noellesroles.VULTURE) && TMMClient.isKiller() && TMMClient.isPlayerAliveAndInSurvival()) {
                    cir.setReturnValue(Noellesroles.VULTURE.color());
                    cir.cancel();
                }
            }
            if (!((PlayerEntity)target).isSpectator() && TMMClient.isInstinctEnabled()) {
                if (gameWorldComponent.isRole((PlayerEntity) target, Noellesroles.EXECUTIONER) && TMMClient.isKiller() && TMMClient.isPlayerAliveAndInSurvival()) {
                    cir.setReturnValue(Noellesroles.EXECUTIONER.color());
                    cir.cancel();
                }
            }
            if (!((PlayerEntity)target).isSpectator() && TMMClient.isInstinctEnabled()) {
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.EXECUTIONER) && TMMClient.isPlayerAliveAndInSurvival()) {
                    cir.setReturnValue(Noellesroles.EXECUTIONER.color());
                    cir.cancel();
                }
            }
        }
    }
}
