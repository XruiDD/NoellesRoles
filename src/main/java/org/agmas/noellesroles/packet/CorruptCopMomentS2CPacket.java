package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * 黑警时刻S2C数据包
 * 用于通知客户端播放/停止黑警时刻BGM
 */
public record CorruptCopMomentS2CPacket(boolean start,int soundIndex) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "corrupt_cop_moment");
    public static final CustomPayload.Id<CorruptCopMomentS2CPacket> ID = new CustomPayload.Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, CorruptCopMomentS2CPacket> CODEC;

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.start);
        buf.writeInt(this.soundIndex);
    }

    public static CorruptCopMomentS2CPacket read(PacketByteBuf buf) {
        return new CorruptCopMomentS2CPacket(buf.readBoolean(), buf.readInt());
    }

    @Override
    public boolean start() {
        return this.start;
    }

    static {
        CODEC = PacketCodec.of(CorruptCopMomentS2CPacket::write, CorruptCopMomentS2CPacket::read);
    }
}
