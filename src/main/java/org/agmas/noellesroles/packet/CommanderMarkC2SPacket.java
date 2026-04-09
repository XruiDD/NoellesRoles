package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record CommanderMarkC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final CustomPayload.Id<CommanderMarkC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(Noellesroles.MOD_ID, "commander_mark"));
    public static final PacketCodec<RegistryByteBuf, CommanderMarkC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            CommanderMarkC2SPacket::targetPlayer,
            CommanderMarkC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
