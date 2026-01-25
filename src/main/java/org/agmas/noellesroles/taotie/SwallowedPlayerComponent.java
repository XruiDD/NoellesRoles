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
public class SwallowedPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
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
     */
    public void setSwallowed(UUID taotieUuid, GameMode originalMode) {
        this.isSwallowed = true;
        this.swallowedBy = taotieUuid;
        this.sync();
    }

    /**
     * Release this player from Taotie's stomach
     */
    public void release(Vec3d position) {
        if (!isSwallowed) return;

        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Reset camera to self first
            serverPlayer.setCameraEntity(serverPlayer);

            serverPlayer.changeGameMode(GameMode.ADVENTURE);

            // Teleport to release position
            serverPlayer.teleport((ServerWorld) serverPlayer.getWorld(),
                    position.x, position.y, position.z,
                    serverPlayer.getYaw(), serverPlayer.getPitch());
        }

        this.isSwallowed = false;
        this.swallowedBy = null;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (!isSwallowed || swallowedBy == null) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // Force spectator mode every tick
        if (serverPlayer.interactionManager.getGameMode() != GameMode.SPECTATOR) {
            serverPlayer.changeGameMode(GameMode.SPECTATOR);
        }

        // Find the Taotie player
        PlayerEntity taotie = serverWorld.getPlayerByUuid(swallowedBy);
        if (taotie != null && GameFunctions.isPlayerAliveAndSurvival(taotie)) {
            // Force camera to follow Taotie every tick (prevent player from escaping)
            if (serverPlayer.getCameraEntity() != taotie) {
                serverPlayer.setCameraEntity(taotie);
            }

            // Teleport swallowed player to Taotie's position (prevent them from moving away)
            if (serverPlayer.squaredDistanceTo(taotie) > 1.0) {
                serverPlayer.teleport(serverWorld, taotie.getX(), taotie.getY(), taotie.getZ(),
                        serverPlayer.getYaw(), serverPlayer.getPitch());
            }
        }
        // If Taotie is dead, TaotiePlayerComponent.releaseAllPlayers will handle release
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
