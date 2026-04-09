package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record CriminalReasonerReasonC2SPacket(UUID victimPlayer, UUID suspectPlayer) implements CustomPayload {
    public static final CustomPayload.Id<CriminalReasonerReasonC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "criminal_reasoner_reason"));
    public static final PacketCodec<RegistryByteBuf, CriminalReasonerReasonC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, CriminalReasonerReasonC2SPacket::victimPlayer,
            Uuids.PACKET_CODEC, CriminalReasonerReasonC2SPacket::suspectPlayer,
            CriminalReasonerReasonC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
