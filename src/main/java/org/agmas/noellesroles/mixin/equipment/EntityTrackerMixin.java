package org.agmas.noellesroles.mixin.equipment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.agmas.noellesroles.util.HiddenEquipmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * 服务端装备广播隐藏 Mixin
 * <p>
 * 拦截装备数据包广播，为每个接收者单独处理隐藏逻辑。
 */
@Mixin(ServerChunkLoadingManager.EntityTracker.class)
public class EntityTrackerMixin {

    @Shadow
    @Final
    Entity entity;

    @Shadow
    @Final
    private Set<PlayerAssociatedNetworkHandler> listeners;

    @Inject(method = "sendToOtherNearbyPlayers", at = @At("HEAD"), cancellable = true)
    private void hideEquipmentBroadcast(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof EntityEquipmentUpdateS2CPacket equipmentPacket)) return;
        if (!(entity instanceof PlayerEntity holder)) return;

        ci.cancel();

        for (PlayerAssociatedNetworkHandler handler : listeners) {
            ServerPlayerEntity observer = handler.getPlayer();
            EntityEquipmentUpdateS2CPacket filtered = HiddenEquipmentHelper.filterPacket(equipmentPacket, holder, observer);
            handler.sendPacket(filtered != null ? filtered : packet);
        }
    }
}
