package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * Client to Server packet for Taotie swallow action
 */
public record TaotieSwallowC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final Identifier TAOTIE_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "taotie_swallow");
    public static final Id<TaotieSwallowC2SPacket> ID = new Id<>(TAOTIE_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, TaotieSwallowC2SPacket> CODEC;

    public TaotieSwallowC2SPacket(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetPlayer);
    }

    public static TaotieSwallowC2SPacket read(PacketByteBuf buf) {
        return new TaotieSwallowC2SPacket(buf.readUuid());
    }

    @Override
    public UUID targetPlayer() {
        return this.targetPlayer;
    }

    static {
        CODEC = PacketCodec.of(TaotieSwallowC2SPacket::write, TaotieSwallowC2SPacket::read);
    }
}
