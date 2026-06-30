package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * 影子小丑「影誓」结盟请求：按 G 瞄准搭档发起/同意结盟。
 */
public record ShadowAllyRequestC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final Identifier SHADOW_ALLY_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "shadow_ally_request");
    public static final Id<ShadowAllyRequestC2SPacket> ID = new Id<>(SHADOW_ALLY_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ShadowAllyRequestC2SPacket> CODEC;

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetPlayer);
    }

    public static ShadowAllyRequestC2SPacket read(PacketByteBuf buf) {
        return new ShadowAllyRequestC2SPacket(buf.readUuid());
    }

    static {
        CODEC = PacketCodec.of(ShadowAllyRequestC2SPacket::write, ShadowAllyRequestC2SPacket::read);
    }
}
