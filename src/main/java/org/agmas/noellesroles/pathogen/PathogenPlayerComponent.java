package org.agmas.noellesroles.pathogen;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class PathogenPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<PathogenPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "pathogen"), PathogenPlayerComponent.class);
    private final PlayerEntity player;
    private int baseCooldownTicks = GameConstants.getInTicks(0, 15); // default 15 seconds

    public PathogenPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.baseCooldownTicks = GameConstants.getInTicks(0, 15);
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public int getBaseCooldownTicks() {
        return baseCooldownTicks;
    }

    /**
     * Calculate and set base cooldown based on initial player count
     * 6-11 players: 20s, 12-17 players: 15s, 18-24 players: 10s, 24+ players: 7s
     * @param playerCount the number of players at game start
     */
    public void setBaseCooldownByPlayerCount(int playerCount) {
        int cooldownSeconds;
        if (playerCount > 24) {
            cooldownSeconds = 7;
        } else if (playerCount >= 18) {
            cooldownSeconds = 10;
        } else if (playerCount >= 12) {
            cooldownSeconds = 15;
        } else {
            cooldownSeconds = 20;
        }
        this.baseCooldownTicks = GameConstants.getInTicks(0, cooldownSeconds);
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("baseCooldownTicks", this.baseCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.baseCooldownTicks = tag.contains("baseCooldownTicks") ? tag.getInt("baseCooldownTicks") : GameConstants.getInTicks(0, 15);
    }
}
