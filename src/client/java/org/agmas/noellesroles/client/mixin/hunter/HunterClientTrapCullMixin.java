package org.agmas.noellesroles.client.mixin.hunter;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.entity.HunterTrapEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class HunterClientTrapCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hideTrapForUnauthorized(double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof HunterTrapEntity trap)) {
            return;
        }
        if (!(net.minecraft.client.MinecraftClient.getInstance().player instanceof ClientPlayerEntity player)) {
            return;
        }
        if (!trap.canBeSeenBy(player)) {
            cir.setReturnValue(false);
        }
    }
}
