package org.agmas.noellesroles.mixin.equipment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.util.HiddenEquipmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * 服务端初始装备同步隐藏 Mixin
 */
@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {

    @Shadow
    @Final
    private Entity entity;

    @WrapOperation(
            method = "sendPackets",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V")
    )
    private void hideEquipment(
            Consumer<Packet<ClientPlayPacketListener>> sender,
            Object packetObj,
            Operation<Void> original,
            ServerPlayerEntity player
    ) {
        if (packetObj instanceof EntityEquipmentUpdateS2CPacket packet
                && entity instanceof PlayerEntity holder
                && player != entity) {
            EntityEquipmentUpdateS2CPacket filtered = HiddenEquipmentHelper.filterPacket(packet, holder, player);
            if (filtered != null) {
                original.call(sender, filtered);
                return;
            }
        }
        original.call(sender, packetObj);
    }
}
