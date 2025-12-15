package org.agmas.noellesroles.pathogen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

public class InfectedPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<InfectedPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "infected"), InfectedPlayerComponent.class);
    private final PlayerEntity player;
    private boolean infected = false;
    private UUID infectedBy = null;

    public InfectedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.infected = false;
        this.infectedBy = null;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void setInfected(boolean infected, UUID pathogenUuid) {
        this.infected = infected;
        this.infectedBy = pathogenUuid;
        this.sync();
    }

    public boolean isInfected() {
        return infected;
    }

    public UUID getInfectedBy() {
        return infectedBy;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("infected", this.infected);
        if (this.infectedBy != null) {
            tag.putUuid("infectedBy", this.infectedBy);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.infected = tag.getBoolean("infected");
        if (tag.contains("infectedBy")) {
            this.infectedBy = tag.getUuid("infectedBy");
        } else {
            this.infectedBy = null;
        }
    }
}
