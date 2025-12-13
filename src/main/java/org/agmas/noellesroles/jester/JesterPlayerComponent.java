package org.agmas.noellesroles.jester;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class JesterPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<JesterPlayerComponent> KEY =
        ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "jester"), JesterPlayerComponent.class);

    private final PlayerEntity player;
    public boolean won = false;

    public JesterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.won = false;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("won", this.won);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.won = tag.getBoolean("won");
    }
}
