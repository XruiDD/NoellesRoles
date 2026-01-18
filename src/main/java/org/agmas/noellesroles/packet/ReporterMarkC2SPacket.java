package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record ReporterMarkC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final CustomPayload.Id<ReporterMarkC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "reporter_mark"));
    public static final PacketCodec<RegistryByteBuf, ReporterMarkC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ReporterMarkC2SPacket::targetPlayer,
            ReporterMarkC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
