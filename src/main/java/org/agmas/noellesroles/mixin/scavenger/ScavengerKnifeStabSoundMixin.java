package org.agmas.noellesroles.mixin.scavenger;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.util.SwallowedInteractionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KnifeStabPayload.Receiver.class)
public abstract class ScavengerKnifeStabSoundMixin {
    private static final int LOOSE_END_KNIFE_COOLDOWN_TICKS = 20 * 45;

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, remap = false)
    private void noellesroles$blockKnifeStabOnSwallowedPlayer(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity attacker = context.player();
        if (SwallowedInteractionHelper.blocksActor(attacker)) {
            ci.cancel();
            return;
        }

        Entity target = attacker.getServerWorld().getEntityById(payload.target());
        if (target instanceof ServerPlayerEntity targetPlayer && !GameFunctions.isPlayerPlayingAndAlive(targetPlayer)) {
            ci.cancel();
            return;
        }

        if (SwallowedInteractionHelper.blocksTargetForViewer(attacker, target)) {
            ci.cancel();
        }
    }

    /**
     * 清道夫角色刺杀时不播放刺杀音效
     */
    @Redirect(method = "receive", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"), remap = false)
    private void scavengerSilentKnifeStab(ServerPlayerEntity target, SoundEvent sound, float volume, float pitch, KnifeStabPayload payload, ServerPlayNetworking.Context context) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
        if (gameWorldComponent.isRole(context.player(), Noellesroles.SCAVENGER)) {
            return;
        }
        target.playSound(sound, volume, pitch);
    }

    @Inject(method = "receive", at = @At("RETURN"), remap = false)
    private void noellesroles$applyLooseEndKnifeCooldown(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity attacker = context.player();
        Entity target = attacker.getServerWorld().getEntityById(payload.target());
        if (!(target instanceof PlayerEntity)
                || SwallowedInteractionHelper.blocksActor(attacker)
                || SwallowedInteractionHelper.blocksTargetForViewer(attacker, target)) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(attacker.getWorld());
        if (gameWorldComponent.isRole(attacker, WatheRoles.LOOSE_END)) {
            attacker.getItemCooldownManager().set(dev.doctor4t.wathe.index.WatheItems.KNIFE, LOOSE_END_KNIFE_COOLDOWN_TICKS);
        }
    }
}
