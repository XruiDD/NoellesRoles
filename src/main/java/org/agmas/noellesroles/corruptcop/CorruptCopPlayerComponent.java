package org.agmas.noellesroles.corruptcop;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * Component for tracking Corrupt Cop  player state.
 * The Corrupt Cop is a neutral role that:
 * - Has a revolver like the Vigilante
 * - Can kill anyone without punishment (gun doesn't drop)
 * - Wins by being the last player standing
 * - Blocks other factions from winning while alive
 */
public class CorruptCopPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<CorruptCopPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "corrupt_cop"), CorruptCopPlayerComponent.class);

    private final PlayerEntity player;
    private int killCount = 0;

    public CorruptCopPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.killCount = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public int getKillCount() {
        return killCount;
    }

    public void incrementKillCount() {
        this.killCount++;
        this.sync();
    }

    @Override
    public void clientTick() {
    }

    @Override
    public void serverTick() {
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("killCount", this.killCount);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.killCount = tag.contains("killCount") ? tag.getInt("killCount") : 0;
    }
}
