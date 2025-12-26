package org.agmas.noellesroles.recaller;

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
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class RecallerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<RecallerPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "recaller"), RecallerPlayerComponent.class);
    private final PlayerEntity player;
    public boolean placed = false;
    public double x = 0;
    public double y = 0;
    public double z = 0;

    public void reset() {
        this.placed = false;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.sync();
    }

    public RecallerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void clientTick() {
    }

    public void serverTick() {
        this.sync();
    }

    public void setPosition() {
        x = player.getX();
        y = player.getY();
        z = player.getZ();
        placed = true;
        this.sync();
    }


    public void teleport() {
        player.refreshPositionAfterTeleport(x,y,z);
        placed = false;
        this.sync();
    }


    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putDouble("x", this.x);
        tag.putDouble("y", this.y);
        tag.putDouble("z", this.z);
        tag.putBoolean("placed", this.placed);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.x = tag.contains("x") ? tag.getDouble("x") : 0;
        this.y = tag.contains("y") ? tag.getDouble("y") : 0;
        this.z = tag.contains("z") ? tag.getDouble("z") : 0;
        this.placed = tag.contains("placed") && tag.getBoolean("placed");
    }
}
