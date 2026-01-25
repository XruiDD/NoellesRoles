package org.agmas.noellesroles.client.mixin.taotie;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.SpectatorHud;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents swallowed players from opening spectator menu
 */
@Mixin(SpectatorHud.class)
public class SpectatorHudMixin {

    @Inject(method = "selectSlot", at = @At("HEAD"), cancellable = true)
    private void preventSwallowedPlayerMenu(int slot, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null) {
            SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
            if (swallowedComp.isSwallowed()) {
                // Cancel opening spectator menu for swallowed players
                ci.cancel();
            }
        }
    }
}
