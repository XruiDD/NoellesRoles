package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record SingleplayerTestC2SPacket(String roleId, String gameModeId, String mapEffectId, int startMinutes) implements CustomPayload {
    public static final Identifier SINGLEPLAYER_TEST_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "singleplayer_test");
    public static final Id<SingleplayerTestC2SPacket> ID = new Id<>(SINGLEPLAYER_TEST_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SingleplayerTestC2SPacket> CODEC = PacketCodec.of(SingleplayerTestC2SPacket::write, SingleplayerTestC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(this.roleId);
        buf.writeString(this.gameModeId);
        buf.writeString(this.mapEffectId);
        buf.writeVarInt(this.startMinutes);
    }

    public static SingleplayerTestC2SPacket read(PacketByteBuf buf) {
        return new SingleplayerTestC2SPacket(
            buf.readString(),
            buf.readString(),
            buf.readString(),
            buf.readVarInt()
        );
    }
}
