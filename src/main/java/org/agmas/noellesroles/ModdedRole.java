package org.agmas.noellesroles;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModdedRole {

    public final Identifier id;
    public int packet_id;
    public final int color;
    public final boolean winsWithKillers;
    public final boolean isKiller;
    public final String translationKey;
    public final String networkKey;
    public final int maxCount;

    public List<UUID> playersInRole = new ArrayList<>();

    public ModdedRole(Identifier id, int color, boolean winsWithKillers, boolean isKiller, int maxCount, int packet_id) {
        this.color = color;
        this.winsWithKillers = winsWithKillers;
        this.isKiller = isKiller;
        this.id = id;
        this.translationKey = id.getNamespace() +"." + id.getPath();
        this.networkKey = "moddedrole_" + id.getNamespace() + id.getPath();
        this.maxCount = maxCount;
        this.packet_id = packet_id;
        Log.info(LogCategory.GENERAL, "Added " + translationKey + " with packet ID" + packet_id);
    }

    public void onGameStarted(PlayerEntity player) {}

    public List<UUID> getPlayers() {
        return this.playersInRole;
    }

    public void addPlayer(PlayerEntity killer) {
        this.addPlayer(killer.getUuid());
    }

    public void addPlayer(UUID killer) {
        this.playersInRole.add(killer);
    }

    public void setPlayers(List<UUID> killers) {
        this.playersInRole = killers;
    }
    public boolean isInRole(@NotNull PlayerEntity player) {
        return this.playersInRole.contains(player.getUuid());
    }

    public void resetPlayerList() {
        this.playersInRole =new ArrayList();
    }
}
