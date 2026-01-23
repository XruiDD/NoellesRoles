package org.agmas.noellesroles.mixin.noisemaker;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class NoisemakerKillMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;Z)V", at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/entity/PlayerBodyEntity;setHeadYaw(F)V"))
    private static void noisemakerKill(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier identifier,boolean force, CallbackInfo ci, @Local PlayerBodyEntity body) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());
        if (gameWorldComponent.isRole(victim, Noellesroles.NOISEMAKER)) {
           body.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20*60, 0));
           if (victim.getWorld() instanceof ServerWorld serverWorld) {
               RegistryEntry<SoundEvent> soundEntry = RegistryEntry.of(SoundEvents.ENTITY_ALLAY_DEATH);
               var seed = serverWorld.random.nextLong();
               for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                   if(gameWorldComponent.isInnocent(player) || player.isSpectator()){
                       player.networkHandler.sendPacket(new PlaySoundS2CPacket(soundEntry, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(),1.0f, 1.0f,seed));
                       player.sendMessage(Text.translatable("noellesroles.noisemaker.death_scream"), true);
                   }
               }
           }
        }
    }

}
