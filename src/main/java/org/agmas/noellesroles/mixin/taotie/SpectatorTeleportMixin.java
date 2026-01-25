package org.agmas.noellesroles.mixin.taotie;

import net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents swallowed players from using spectator teleport
 */
@Mixin(ServerPlayNetworkHandler.class)
public class SpectatorTeleportMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onSpectatorTeleport", at = @At("HEAD"), cancellable = true)
    private void preventSwallowedPlayerTeleport(SpectatorTeleportC2SPacket packet, CallbackInfo ci) {
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(this.player);
        if (swallowedComp.isSwallowed()) {
            // Cancel the teleport for swallowed players
            ci.cancel();
        }
    }
}
