package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record DetectiveInvestigateC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final CustomPayload.Id<DetectiveInvestigateC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "detective_investigate"));
    public static final PacketCodec<RegistryByteBuf, DetectiveInvestigateC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, DetectiveInvestigateC2SPacket::targetPlayer,
            DetectiveInvestigateC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
