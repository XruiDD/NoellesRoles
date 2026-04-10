package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 静语状态同步 S2C 数据包
 * 服务端在玩家被静语/解除静语时发送给该客户端
 * 客户端根据此状态过滤屏幕上方的广播消息
 */
public record SilencedStateS2CPacket(boolean silenced) implements CustomPayload {
    public static final Id<SilencedStateS2CPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "silenced_state"));

    public static final PacketCodec<RegistryByteBuf, SilencedStateS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, SilencedStateS2CPacket::silenced,
            SilencedStateS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
