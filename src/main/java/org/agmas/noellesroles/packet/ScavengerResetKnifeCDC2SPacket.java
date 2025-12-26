package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 清道夫重置刀冷却数据包（客户端->服务端）
 * 清道夫可以花费100金币重置刀的冷却时间
 */
public record ScavengerResetKnifeCDC2SPacket() implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "scavenger_reset_knife_cd");
    public static final Id<ScavengerResetKnifeCDC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, ScavengerResetKnifeCDC2SPacket> CODEC;

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(net.minecraft.network.PacketByteBuf buf) {
        // 空数据包，无需写入任何数据
    }

    public static ScavengerResetKnifeCDC2SPacket read(net.minecraft.network.PacketByteBuf buf) {
        return new ScavengerResetKnifeCDC2SPacket();
    }

    static {
        CODEC = PacketCodec.of(ScavengerResetKnifeCDC2SPacket::write, ScavengerResetKnifeCDC2SPacket::read);
    }
}
