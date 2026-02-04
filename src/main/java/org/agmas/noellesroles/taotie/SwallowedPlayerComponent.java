package org.agmas.noellesroles.taotie;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * Component for players who have been swallowed by Taotie
 */
public class SwallowedPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<SwallowedPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "swallowed"), SwallowedPlayerComponent.class);

    private final PlayerEntity player;
    private boolean isSwallowed = false;
    private UUID swallowedBy = null;

    public static boolean isPlayerSwallowed(PlayerEntity player)
    {
        return KEY.get(player).isSwallowed;
    }

    public SwallowedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        // 重置摄像机到玩家自己
        if (this.isSwallowed && player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.setCameraEntity(serverPlayer);
        }
        this.isSwallowed = false;
        this.swallowedBy = null;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // Sync to all players so they know who is swallowed
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.isSwallowed);
        buf.writeBoolean(this.swallowedBy != null);
        if (this.swallowedBy != null) {
            buf.writeUuid(this.swallowedBy);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.isSwallowed = buf.readBoolean();
        if (buf.readBoolean()) {
            this.swallowedBy = buf.readUuid();
        } else {
            this.swallowedBy = null;
        }
    }

    /**
     * Mark this player as swallowed by the given Taotie
     * Immediately changes game mode to spectator and teleports to Taotie's position
     */
    public void setSwallowed(UUID taotieUuid) {
        this.isSwallowed = true;
        this.swallowedBy = taotieUuid;

        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Change to spectator mode
            serverPlayer.changeGameMode(GameMode.SPECTATOR);

            // Teleport to Taotie's position
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity taotie = serverWorld.getPlayerByUuid(taotieUuid);
                if (taotie != null) {
                    serverPlayer.teleport(serverWorld, taotie.getX(), taotie.getY(), taotie.getZ(),
                            serverPlayer.getYaw(), serverPlayer.getPitch());
                    // Set camera to follow Taotie
                    serverPlayer.setCameraEntity(taotie);
                }
            }
        }

        this.sync();
    }

    /**
     * Release this player from Taotie's stomach
     * Teleports to release position and changes to adventure mode
     */
    public void release(Vec3d position) {
        if (!isSwallowed) return;
        this.isSwallowed = false;
        this.swallowedBy = null;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Reset camera to self first
            serverPlayer.setCameraEntity(serverPlayer);

            // Change to adventure mode (force all alive players to adventure)
            serverPlayer.changeGameMode(GameMode.ADVENTURE);

            // Teleport to release position
            serverPlayer.teleport((ServerWorld) serverPlayer.getWorld(),
                    position.x, position.y, position.z,
                    serverPlayer.getYaw(), serverPlayer.getPitch());
        }
        this.sync();
    }


    public boolean isSwallowed() {
        return isSwallowed;
    }

    public UUID getSwallowedBy() {
        return swallowedBy;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("isSwallowed", this.isSwallowed);
        if (this.swallowedBy != null) {
            tag.putUuid("swallowedBy", this.swallowedBy);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.isSwallowed = tag.getBoolean("isSwallowed");
        if (tag.contains("swallowedBy")) {
            this.swallowedBy = tag.getUuid("swallowedBy");
        } else {
            this.swallowedBy = null;
        }
    }
}
