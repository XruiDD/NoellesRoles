package org.agmas.noellesroles.client.mixin;

import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.client.gui.RoleAnnouncementText;
import dev.doctor4t.trainmurdermystery.client.gui.RoundTextRenderer;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.RoleHelpers;
import org.agmas.noellesroles.client.NoellesrolesClient;
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

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void b(CallbackInfoReturnable<Boolean> cir) {
        if (NoellesrolesClient.clientModdedRole != null) {
            if (instinctKeybind.isPressed() && NoellesrolesClient.clientModdedRole.id.equals(Noellesroles.JESTER_ID)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void b(Entity target, CallbackInfoReturnable<Integer> cir) {
        if (target instanceof PlayerEntity) {
            if (NoellesrolesClient.clientModdedRole != null) {
                if (NoellesrolesClient.clientModdedRole.id.equals(Noellesroles.JESTER_ID) && TMMClient.isPlayerAliveAndInSurvival() && TMMClient.isInstinctEnabled()) {
                    cir.setReturnValue(Color.PINK.getRGB());
                }
            }
            if (GameFunctions.isPlayerSpectatingOrCreative(MinecraftClient.getInstance().player) && TMMClient.isInstinctEnabled()) {
                if (RoleHelpers.instance.isOfAnyModdedRole((PlayerEntity) target)) {
                    cir.setReturnValue(RoleHelpers.instance.getRoleOfPlayer((PlayerEntity) target).color);
                    cir.cancel();
                }
            }
        }
    }
}
