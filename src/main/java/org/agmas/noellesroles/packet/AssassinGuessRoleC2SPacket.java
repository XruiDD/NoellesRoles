package org.agmas.noellesroles.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record AssassinGuessRoleC2SPacket(UUID targetPlayer, Identifier guessedRole) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "assassin_guess_role");
    public static final Id<AssassinGuessRoleC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, AssassinGuessRoleC2SPacket> CODEC;

    public AssassinGuessRoleC2SPacket(UUID targetPlayer, Identifier guessedRole) {
        this.targetPlayer = targetPlayer;
        this.guessedRole = guessedRole;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetPlayer);
        buf.writeIdentifier(this.guessedRole);
    }

    public static AssassinGuessRoleC2SPacket read(PacketByteBuf buf) {
        return new AssassinGuessRoleC2SPacket(buf.readUuid(), buf.readIdentifier());
    }

    public UUID targetPlayer() {
        return this.targetPlayer;
    }

    public Identifier guessedRole() {
        return this.guessedRole;
    }

    static {
        CODEC = PacketCodec.of(AssassinGuessRoleC2SPacket::write, AssassinGuessRoleC2SPacket::read);
    }
}
