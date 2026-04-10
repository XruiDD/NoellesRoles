package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 职业广播消息 S2C 数据包
 * 服务端发送给客户端，用于在屏幕上方显示职业相关的文字消息
 * 复用 WalkieTalkieBroadcastRenderer 在屏幕上方渲染
 */
public record RoleBroadcastS2CPacket(String message) implements CustomPayload {
    public static final Id<RoleBroadcastS2CPacket> ID =
            new Id<>(Identifier.of(Noellesroles.MOD_ID, "role_broadcast"));

    public static final PacketCodec<RegistryByteBuf, RoleBroadcastS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, RoleBroadcastS2CPacket::message,
            RoleBroadcastS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
