package org.agmas.noellesroles.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/** 派对狂施放/标记包（客户端 -> 服务端）。 */
public record PartyAnimalBuzzC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "party_animal_buzz");
    public static final Id<PartyAnimalBuzzC2SPacket> ID = new Id<>(PACKET_ID);

    public static final PacketCodec<RegistryByteBuf, PartyAnimalBuzzC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), PartyAnimalBuzzC2SPacket::targetPlayer,
            PartyAnimalBuzzC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
