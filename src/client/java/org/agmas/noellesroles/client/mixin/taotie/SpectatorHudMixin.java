package org.agmas.noellesroles.client.mixin.taotie;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.SpectatorHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents in-game players (playing and alive) from opening the vanilla spectator
 * teleport menu. Players swallowed by Taotie are forced into spectator mode but are
 * still "playing and alive", so without this they could teleport around the map via
 * the spectator menu. Genuine spectators (eliminated / dead) keep the menu.
 *
 * <p>The menu has two open paths and both must be blocked: {@code selectSlot} (hotbar
 * keys 1-9) and {@code useSelectedCommand} (middle mouse button) — both create a new
 * {@code SpectatorMenu} when none is open.
 */
@Mixin(SpectatorHud.class)
public class SpectatorHudMixin {

    @Unique
    private boolean noellesroles$shouldBlockSpectatorMenu() {
        return GameFunctions.isPlayerPlayingAndAlive(MinecraftClient.getInstance().player);
    }

    @Inject(method = "selectSlot", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventMenuViaHotbarKey(int slot, CallbackInfo ci) {
        if (noellesroles$shouldBlockSpectatorMenu()) {
            ci.cancel();
        }
    }

    @Inject(method = "useSelectedCommand", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventMenuViaMiddleClick(CallbackInfo ci) {
        if (noellesroles$shouldBlockSpectatorMenu()) {
            ci.cancel();
        }
    }
}
