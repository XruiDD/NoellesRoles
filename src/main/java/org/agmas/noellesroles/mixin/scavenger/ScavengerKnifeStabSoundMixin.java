package org.agmas.noellesroles.mixin.scavenger;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KnifeStabPayload.Receiver.class)
public abstract class ScavengerKnifeStabSoundMixin {

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
}
