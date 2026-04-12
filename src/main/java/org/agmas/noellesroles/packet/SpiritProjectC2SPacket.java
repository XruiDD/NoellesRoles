package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record SpiritProjectC2SPacket() implements CustomPayload {
    public static final Identifier SPIRIT_PROJECT_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "spirit_project");
    public static final Id<SpiritProjectC2SPacket> ID = new Id<>(SPIRIT_PROJECT_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SpiritProjectC2SPacket> CODEC;

    public SpiritProjectC2SPacket() {
    }

    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static SpiritProjectC2SPacket read(PacketByteBuf buf) {
        return new SpiritProjectC2SPacket();
    }

    static {
        CODEC = PacketCodec.of(SpiritProjectC2SPacket::write, SpiritProjectC2SPacket::read);
    }
}
