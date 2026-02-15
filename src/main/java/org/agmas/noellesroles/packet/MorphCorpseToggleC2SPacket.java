package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record MorphCorpseToggleC2SPacket() implements CustomPayload {
    public static final Identifier MORPH_CORPSE_TOGGLE_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "morph_corpse_toggle");
    public static final Id<MorphCorpseToggleC2SPacket> ID = new Id<>(MORPH_CORPSE_TOGGLE_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, MorphCorpseToggleC2SPacket> CODEC;

    public MorphCorpseToggleC2SPacket() {
    }

    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static MorphCorpseToggleC2SPacket read(PacketByteBuf buf) {
        return new MorphCorpseToggleC2SPacket();
    }

    static {
        CODEC = PacketCodec.of(MorphCorpseToggleC2SPacket::write, MorphCorpseToggleC2SPacket::read);
    }
}
