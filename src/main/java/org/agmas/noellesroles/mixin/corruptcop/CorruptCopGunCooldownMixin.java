package org.agmas.noellesroles.mixin.corruptcop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunShootPayload.Receiver.class)
public abstract class CorruptCopGunCooldownMixin {

    /**
     * 黑警时刻期间枪冷却变为2秒
     */
    @Inject(method = "receive", at = @At("RETURN"), remap = false)
    private void modifyGunCooldownForCorruptCop(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player, Noellesroles.CORRUPT_COP)) {
            CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(player);
            int customCooldown = corruptCopComp.getGunCooldown();
            if (customCooldown > 0) {
                // 重置为黑警时刻的冷却时间
                player.getItemCooldownManager().set(WatheItems.REVOLVER, customCooldown);
            }
        }
    }
}
