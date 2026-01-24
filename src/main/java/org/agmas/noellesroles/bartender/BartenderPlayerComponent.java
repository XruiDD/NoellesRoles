package org.agmas.noellesroles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
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
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class BartenderPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BartenderPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "bartender"), BartenderPlayerComponent.class);

    private final PlayerEntity player;
    public int glowTicks = 0;

    public void reset() {
        this.glowTicks = 0;
        this.sync();
    }

    public BartenderPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void serverTick() {
        if (this.glowTicks > 0) {
            --this.glowTicks;
            if (glowTicks == 0) {
                sync();
            }
        }
    }

    public void startGlow() {
        setGlowTicks(GameConstants.getInTicks(0, 40));
        this.sync();
    }

    public void setGlowTicks(int ticks) {
        this.glowTicks = ticks;
        this.sync();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.BARTENDER);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.glowTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.glowTicks = buf.readBoolean() ? 1 : 0;
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("glowTicks", this.glowTicks);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.glowTicks = tag.contains("glowTicks") ? tag.getInt("glowTicks") : 0;
    }
}
