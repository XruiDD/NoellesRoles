package org.agmas.noellesroles.client.mixin.riotpatrol;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class RiotShieldMouseLockMixin {

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$disableLookSensor(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (RiotPatrolPlayerComponent.KEY.get(client.player).isShieldActive()) {
            ci.cancel();
        }
    }
}
