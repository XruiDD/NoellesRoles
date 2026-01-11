package org.agmas.noellesroles.mixin.toxicologist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to sync poison state to Toxicologist players,
 * allowing them to see poisoned players through walls.
 */
@Mixin(PlayerPoisonComponent.class)
public class ToxicologistPoisonSyncMixin {

    @Inject(method = "shouldSyncWith", at = @At("HEAD"), cancellable = true)
    private void syncWithToxicologist(ServerPlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(player, Noellesroles.TOXICOLOGIST)) {
            cir.setReturnValue(true);
        }
    }
}
