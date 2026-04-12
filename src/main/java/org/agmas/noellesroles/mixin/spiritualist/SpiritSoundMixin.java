package org.agmas.noellesroles.mixin.spiritualist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通灵者灵魂出窍时，服务端不给该玩家发送声音包
 */
@Mixin(ServerCommonNetworkHandler.class)
public class SpiritSoundMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockSoundPackets(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof PlaySoundS2CPacket) && !(packet instanceof PlaySoundFromEntityS2CPacket)) return;

        if ((Object) this instanceof ServerPlayNetworkHandler handler) {
            ServerPlayerEntity player = handler.player;
            GameWorldComponent gameComp = GameWorldComponent.KEY.get(player.getWorld());
            if (gameComp.isRole(player, Noellesroles.SPIRIT_WALKER)) {
                SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(player);
                if (spiritComp.isProjecting()) {
                    ci.cancel();
                }
            }
        }
    }
}
