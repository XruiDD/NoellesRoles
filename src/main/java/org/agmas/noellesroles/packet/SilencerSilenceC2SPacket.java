package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * Client to Server packet for Silencer's silence ability
 */
public record SilencerSilenceC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "silencer_silence");
    public static final Id<SilencerSilenceC2SPacket> ID = new Id<>(PACKET_ID);

    public static final PacketCodec<RegistryByteBuf, SilencerSilenceC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), SilencerSilenceC2SPacket::targetPlayer,
            SilencerSilenceC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
