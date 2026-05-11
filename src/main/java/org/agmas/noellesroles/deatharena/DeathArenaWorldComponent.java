package org.agmas.noellesroles.deatharena;

import dev.doctor4t.wathe.index.WatheEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeathArenaWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<DeathArenaWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "death_arena_world"),
            DeathArenaWorldComponent.class
    );

    private final World world;
    private boolean active = false;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final List<SpawnLocation> spawnLocations = new ArrayList<>();
    private Identifier mapId = DeathArenaStateHelper.getConfiguredMapId();

    public DeathArenaWorldComponent(World world) {
        this.world = world;
    }

    public void reset() {
        cleanupArenaResetEntities();
        this.active = false;
        this.participants.clear();
        this.spawnLocations.clear();
        this.mapId = DeathArenaStateHelper.getConfiguredMapId();
        sync();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        sync();
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public void addParticipant(UUID uuid) {
        participants.add(uuid);
        sync();
    }

    public void removeParticipant(UUID uuid) {
        participants.remove(uuid);
        sync();
    }

    public boolean hasParticipants() {
        return !participants.isEmpty();
    }

    public boolean isParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public Identifier getMapId() {
        return mapId;
    }

    public void setMapId(Identifier mapId) {
        this.mapId = mapId;
        sync();
    }

    public void setSpawnLocations(List<SpawnLocation> locations) {
        spawnLocations.clear();
        spawnLocations.addAll(locations);
        sync();
    }

    public List<SpawnLocation> getSpawnLocations() {
        return spawnLocations;
    }

    public boolean hasSpawnLocations() {
        return !spawnLocations.isEmpty();
    }

    public void sync() {
        KEY.sync(world);
    }

    private void cleanupArenaResetEntities() {
        if (!(world instanceof ServerWorld serverWorld) || !DeathArenaStateHelper.isDeathArenaDimension(serverWorld)) {
            return;
        }

        cleanupArenaEntitiesByType(serverWorld, NoellesRolesEntities.ROLE_MINE_ENTITY_ENTITY_TYPE);
        cleanupArenaEntitiesByType(serverWorld, NoellesRolesEntities.POISON_GAS_BOMB_ENTITY);
        cleanupArenaEntitiesByType(serverWorld, NoellesRolesEntities.POISON_GAS_CLOUD_ENTITY);
        cleanupArenaEntitiesByType(serverWorld, NoellesRolesEntities.THROWING_AXE_ENTITY);
        cleanupArenaEntitiesByType(serverWorld, NoellesRolesEntities.HUNTER_TRAP_ENTITY);
        cleanupArenaEntitiesByType(serverWorld, WatheEntities.GRENADE);
        cleanupArenaEntitiesByType(serverWorld, WatheEntities.PLAYER_BODY);
        cleanupArenaEntitiesByType(serverWorld, WatheEntities.NOTE);
    }

    private static void cleanupArenaEntitiesByType(ServerWorld arenaWorld, EntityType<?> entityType) {
        for (Entity entity : arenaWorld.getEntitiesByType(entityType, entity -> true)) {
            entity.discard();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return false;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(active);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.active = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("active", active);
        tag.putString("mapId", mapId.toString());

        NbtList participantList = new NbtList();
        for (UUID uuid : participants) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("uuid", uuid);
            participantList.add(entry);
        }
        tag.put("participants", participantList);

        NbtList spawnList = new NbtList();
        for (SpawnLocation spawnLocation : spawnLocations) {
            NbtCompound entry = new NbtCompound();
            entry.putDouble("x", spawnLocation.pos().x);
            entry.putDouble("y", spawnLocation.pos().y);
            entry.putDouble("z", spawnLocation.pos().z);
            entry.putFloat("yaw", spawnLocation.yaw());
            entry.putFloat("pitch", spawnLocation.pitch());
            spawnList.add(entry);
        }
        tag.put("spawns", spawnList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.active = tag.getBoolean("active");
        this.mapId = Identifier.tryParse(tag.getString("mapId"));
        if (this.mapId == null) {
            this.mapId = DeathArenaStateHelper.getConfiguredMapId();
        }

        this.participants.clear();
        if (tag.contains("participants")) {
            NbtList participantList = tag.getList("participants", NbtCompound.COMPOUND_TYPE);
            for (int i = 0; i < participantList.size(); i++) {
                NbtCompound entry = participantList.getCompound(i);
                if (entry.containsUuid("uuid")) {
                    participants.add(entry.getUuid("uuid"));
                }
            }
        }

        this.spawnLocations.clear();
        if (tag.contains("spawns")) {
            NbtList spawnList = tag.getList("spawns", NbtCompound.COMPOUND_TYPE);
            for (int i = 0; i < spawnList.size(); i++) {
                NbtCompound entry = spawnList.getCompound(i);
                spawnLocations.add(new SpawnLocation(
                        new Vec3d(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z")),
                        entry.getFloat("yaw"),
                        entry.getFloat("pitch")
                ));
            }
        }
    }

    public record SpawnLocation(Vec3d pos, float yaw, float pitch) {
    }
}
