package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家重生或切换维度时，客户端兜底关闭灵魂出窍
 */
@Mixin(ClientPlayNetworkHandler.class)
public class SpiritClientPacketMixin {

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void spiritualist$disableOnRespawn(CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            SpiritCameraHandler.disable();
        }
    }
}
