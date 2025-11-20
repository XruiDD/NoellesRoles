package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record MorphC2SPacket(UUID player) implements CustomPayload {
    public static final Identifier HOLO_MODE_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "morph");
    public static final CustomPayload.Id<MorphC2SPacket> ID = new CustomPayload.Id<>(HOLO_MODE_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, MorphC2SPacket> CODEC;

    public MorphC2SPacket(UUID player) {
        this.player = player;
    }

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.player);
    }

    public static MorphC2SPacket read(PacketByteBuf buf) {
        return new MorphC2SPacket(buf.readUuid());
    }


    public UUID player() {
        return this.player;
    }


    static {
        CODEC = PacketCodec.of(MorphC2SPacket::write, MorphC2SPacket::read);
    }
}