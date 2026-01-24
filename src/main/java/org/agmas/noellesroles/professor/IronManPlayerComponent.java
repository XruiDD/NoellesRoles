package org.agmas.noellesroles.professor;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class IronManPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<IronManPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "iron_man"),
            IronManPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean hasBuff = false;

    public IronManPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void applyBuff() {
        this.hasBuff = true;
        this.sync();
    }

    public void removeBuff() {
        this.hasBuff = false;
        this.sync();
    }

    public boolean hasBuff() {
        return this.hasBuff;
    }

    public void reset() {
        this.hasBuff = false;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // Only sync to professor players (for blue instinct)
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(recipient.getWorld());
        return gameWorld.isRole(recipient, Noellesroles.PROFESSOR);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.hasBuff);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hasBuff = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("hasBuff", this.hasBuff);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hasBuff = tag.contains("hasBuff") && tag.getBoolean("hasBuff");
    }
}
