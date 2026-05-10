package org.agmas.noellesroles.deatharena;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.game.MapResetTask;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.agmas.noellesroles.entity.HunterTrapEntity;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndsRadarWorldComponent;
import org.agmas.noellesroles.mixin.wathe.GameWorldComponentAccessor;
import org.agmas.noellesroles.util.SpectatorStateHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeathArenaServerController {
    private static final int RESPAWN_DELAY_TICKS = 2;
    private static final Map<RegistryKey<World>, MapResetTask> MAP_RESET_TASKS = new ConcurrentHashMap<>();

    private DeathArenaServerController() {
    }

    public static boolean handleToggle(ServerPlayerEntity player) {
        if (player == null || !(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        DeathArenaPlayerComponent arenaPlayer = DeathArenaPlayerComponent.KEY.get(player);
        if (arenaPlayer.isInArena()) {
            leaveArena(player, false);
            return true;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRunning()
                || gameWorld.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE
                || !SpectatorStateHelper.isInGameRealSpectator(player, gameWorld)) {
            player.sendMessage(Text.translatable("message.noellesroles.death_arena_unavailable"), true);
            return false;
        }

        ServerWorld arenaDimension = DeathArenaStateHelper.getArenaWorld(world.getServer());
        if (arenaDimension == null) {
            player.sendMessage(Text.translatable("message.noellesroles.death_arena_map_missing"), true);
            return false;
        }
        if (isArenaMapResetting(arenaDimension)) {
            player.sendMessage(Text.translatable("message.noellesroles.death_arena_unavailable"), true);
            return false;
        }

        DeathArenaWorldComponent arenaWorld = DeathArenaWorldComponent.KEY.get(world);
        boolean startingArena = !arenaWorld.isActive();
        if (!arenaWorld.hasSpawnLocations()) {
            var loadedSpawns = DeathArenaStateHelper.loadConfiguredSpawnLocations();
            if (loadedSpawns.isEmpty()) {
                player.sendMessage(Text.translatable("message.noellesroles.death_arena_map_missing"), true);
                return false;
            }
            arenaWorld.setMapId(DeathArenaStateHelper.getConfiguredMapId());
            arenaWorld.setSpawnLocations(loadedSpawns.get());
        }

        arenaWorld.setActive(true);
        sanitizeArenaDimension(arenaDimension, startingArena);
        arenaWorld.addParticipant(player.getUuid());
        arenaPlayer.enter(
                world.getRegistryKey().getValue(),
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                player.interactionManager.getGameMode()
        );

        prepareArenaPlayer(player);
        DeathArenaStateHelper.applyLooseEndsOpeningState(player, gameWorld);
        restoreArenaAliveState(arenaDimension, player);
        registerArenaParticipantRole(arenaDimension, player);
        teleportToArenaSpawn(player, arenaDimension, arenaWorld);
        return true;
    }

    public static void handleDeathAfter(ServerPlayerEntity victim) {
        if (victim == null) {
            return;
        }
        if (!DeathArenaStateHelper.isDeathArenaParticipant(victim)) {
            return;
        }

        DeathArenaPlayerComponent arenaPlayer = DeathArenaPlayerComponent.KEY.get(victim);
        arenaPlayer.scheduleRespawnAt(victim.getServer().getTicks() + RESPAWN_DELAY_TICKS);
    }

    public static void tick(ServerWorld world) {
        if (world == null) {
            return;
        }

        tickMapResetTask(world);
        processPendingRespawns(world.getServer());

        DeathArenaWorldComponent arenaWorld = DeathArenaWorldComponent.KEY.get(world);
        if (!arenaWorld.isActive()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRunning() || gameWorld.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            forceShutdown(world, true);
            return;
        }

        if (!arenaWorld.hasParticipants()) {
            arenaWorld.setActive(false);
            resetArenaDimension(world.getServer());
        }
    }

    public static void forceShutdown(ServerWorld world, boolean autoExit) {
        if (world == null) {
            return;
        }
        DeathArenaWorldComponent arenaWorld = DeathArenaWorldComponent.KEY.get(world);
        Set<UUID> participants = new HashSet<>(arenaWorld.getParticipants());
        for (UUID uuid : participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                leaveArena(player, autoExit);
            } else {
                arenaWorld.removeParticipant(uuid);
            }
        }

        cleanupArenaArtifacts(world.getServer());
        arenaWorld.reset();
        resetArenaDimension(world.getServer());
    }

    public static void forceShutdownFromAnyContext(ServerWorld world, boolean autoExit) {
        if (world == null) {
            return;
        }

        ServerWorld originWorld = DeathArenaStateHelper.getArenaOriginWorld(world.getServer());
        if (originWorld != null) {
            forceShutdown(originWorld, autoExit);
            return;
        }

        MinecraftServer server = world.getServer();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (DeathArenaStateHelper.isDeathArenaParticipant(player)) {
                leaveArena(player, autoExit);
            }
        }

        cleanupArenaArtifacts(server);
        for (ServerWorld serverWorld : server.getWorlds()) {
            DeathArenaWorldComponent.KEY.get(serverWorld).reset();
        }
        resetArenaDimension(server);
    }

    public static void resetForNewRound(MinecraftServer server) {
        if (server == null) {
            return;
        }

        cleanupArenaArtifacts(server);
        for (ServerWorld world : server.getWorlds()) {
            DeathArenaWorldComponent.KEY.get(world).reset();
        }

        ServerWorld arenaWorld = DeathArenaStateHelper.getArenaWorld(server);
        if (arenaWorld == null) {
            return;
        }

        MAP_RESET_TASKS.remove(arenaWorld.getRegistryKey());
        resetArenaDimension(server);
        MAP_RESET_TASKS.put(
                arenaWorld.getRegistryKey(),
                new MapResetTask(arenaWorld, () -> resetArenaDimension(server))
        );
    }

    public static void rememberArenaBody(ServerWorld world, UUID bodyUuid) {
        if (world == null || bodyUuid == null) {
            return;
        }
        DeathArenaWorldComponent.KEY.get(world).addArenaBody(bodyUuid);
    }

    public static void cleanupArenaBodies(ServerWorld world) {
        if (world == null) {
            return;
        }
        DeathArenaWorldComponent arenaWorld = DeathArenaWorldComponent.KEY.get(world);
        for (UUID bodyUuid : new HashSet<>(arenaWorld.getArenaBodies())) {
            for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                Entity entity = serverWorld.getEntity(bodyUuid);
                if (entity != null) {
                    entity.discard();
                    break;
                }
            }
        }
        arenaWorld.getArenaBodies().clear();
        arenaWorld.sync();
    }

    public static void cleanupArenaArtifacts(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerWorld world : server.getWorlds()) {
            cleanupArenaBodies(world);
            cleanupHunterTraps(world);
        }
    }

    public static void cleanupHunterTraps(ServerWorld world) {
        if (world == null) {
            return;
        }
        for (HunterTrapEntity trap : world.getEntitiesByType(net.minecraft.util.TypeFilter.equals(HunterTrapEntity.class), entity -> true)) {
            trap.discard();
        }
    }

    public static void leaveArena(ServerPlayerEntity player, boolean autoExit) {
        if (player == null || !(player.getWorld() instanceof ServerWorld currentWorld)) {
            return;
        }

        DeathArenaPlayerComponent arenaPlayer = DeathArenaPlayerComponent.KEY.get(player);
        if (!arenaPlayer.isInArena()) {
            return;
        }

        MinecraftServer server = currentWorld.getServer();
        Identifier returnWorldId = arenaPlayer.getReturnWorldId();
        ServerWorld returnWorld = server.getWorld(World.OVERWORLD);
        if (returnWorldId != null) {
            ServerWorld configuredWorld = server.getWorld(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, returnWorldId));
            if (configuredWorld != null) {
                returnWorld = configuredWorld;
            }
        }
        if (returnWorld == null) {
            returnWorld = currentWorld;
        }

        DeathArenaWorldComponent originArenaWorld = DeathArenaWorldComponent.KEY.get(returnWorld);
        originArenaWorld.removeParticipant(player.getUuid());
        if (!originArenaWorld.hasParticipants()) {
            originArenaWorld.setActive(false);
            resetArenaDimension(server);
        }

        arenaPlayer.leave(autoExit);
        LooseEndPlayerComponent.KEY.get(player).stopOpeningPhase();
        player.changeGameMode(GameMode.SPECTATOR);
        player.setCameraEntity(player);
        player.setInvisible(false);
        player.noClip = false;
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;

        Vec3d returnPos = arenaPlayer.getReturnPos();
        if (returnPos.equals(Vec3d.ZERO)) {
            MapVariablesWorldComponent mapVariables = MapVariablesWorldComponent.KEY.get(returnWorld);
            var spectatorSpawn = mapVariables.getSpectatorSpawnPos();
            if (spectatorSpawn != null) {
                returnPos = spectatorSpawn.pos;
            }
        }

        if (player.getWorld() != returnWorld) {
            player.teleport(returnWorld, returnPos.x, returnPos.y, returnPos.z, arenaPlayer.getReturnYaw(), arenaPlayer.getReturnPitch());
        } else {
            player.requestTeleport(returnPos.x, returnPos.y, returnPos.z);
            player.setYaw(arenaPlayer.getReturnYaw());
            player.setPitch(arenaPlayer.getReturnPitch());
        }
    }

    private static void processPendingRespawns(MinecraftServer server) {
        long currentTick = server.getTicks();
        for (ServerPlayerEntity player : new java.util.ArrayList<>(server.getPlayerManager().getPlayerList())) {
            DeathArenaPlayerComponent arenaPlayer = DeathArenaPlayerComponent.KEY.get(player);
            if (!arenaPlayer.isInArena() || !arenaPlayer.isPendingRespawn()) {
                continue;
            }
            if (arenaPlayer.getRespawnAtTick() > currentTick) {
                continue;
            }
            respawnPlayer(player);
        }
    }

    private static void respawnPlayer(ServerPlayerEntity player) {
        if (!DeathArenaStateHelper.isDeathArenaParticipant(player)) {
            return;
        }

        ServerPlayerEntity activePlayer = player;
        DeathArenaPlayerComponent arenaPlayer = DeathArenaPlayerComponent.KEY.get(activePlayer);
        MinecraftServer server = activePlayer.getServer();
        if (server == null) {
            return;
        }

        ServerWorld originWorld = server.getWorld(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, arenaPlayer.getReturnWorldId()));
        ServerWorld arenaWorld = DeathArenaStateHelper.getArenaWorld(server);
        if (originWorld == null || arenaWorld == null) {
            leaveArena(activePlayer, true);
            return;
        }

        prepareArenaPlayer(activePlayer);
        DeathArenaStateHelper.applyLooseEndsOpeningState(activePlayer, GameWorldComponent.KEY.get(originWorld));
        restoreArenaAliveState(arenaWorld, activePlayer);
        registerArenaParticipantRole(arenaWorld, activePlayer);
        teleportToArenaSpawn(activePlayer, arenaWorld, DeathArenaWorldComponent.KEY.get(originWorld));
        arenaPlayer.setPendingRespawn(false);
    }

    private static void teleportToArenaSpawn(ServerPlayerEntity player, ServerWorld targetWorld, DeathArenaWorldComponent arenaWorld) {
        DeathArenaWorldComponent.SpawnLocation spawn = DeathArenaStateHelper.pickRandomSpawn(arenaWorld);
        if (spawn == null) {
            return;
        }
        player.teleport(targetWorld, spawn.pos().x, spawn.pos().y, spawn.pos().z, spawn.yaw(), spawn.pitch());
    }

    private static void prepareArenaPlayer(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
        player.setCameraEntity(player);
        player.setInvisible(false);
        player.noClip = false;
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        player.setHealth(player.getMaxHealth());
    }

    private static void registerArenaParticipantRole(ServerWorld arenaWorld, ServerPlayerEntity player) {
        GameWorldComponent arenaGame = GameWorldComponent.KEY.get(arenaWorld);
        arenaGame.addRole(player, dev.doctor4t.wathe.api.WatheRoles.LOOSE_END);
        arenaGame.sync();
    }

    private static void restoreArenaAliveState(ServerWorld arenaWorld, ServerPlayerEntity player) {
        if (arenaWorld == null || player == null) {
            return;
        }

        GameWorldComponent arenaGame = GameWorldComponent.KEY.get(arenaWorld);
        GameWorldComponentAccessor accessor = (GameWorldComponentAccessor) (Object) arenaGame;
        if (accessor.noellesroles$getDeadPlayers().remove(player.getUuid())) {
            arenaGame.sync();
        }
    }

    private static void sanitizeArenaDimension(ServerWorld arenaWorld, boolean fullReset) {
        GameWorldComponent arenaGame = GameWorldComponent.KEY.get(arenaWorld);
        if (fullReset) {
            arenaGame.clearRoleMap();
            arenaGame.clearRooms();
        }
        arenaGame.setGameStatus(GameWorldComponent.GameStatus.ACTIVE);
        arenaGame.setGameMode(WatheGameModes.LOOSE_ENDS);
        arenaGame.sync();

        if (fullReset) {
            LooseEndsRadarWorldComponent.KEY.get(arenaWorld).reset();
            LooseEndsRadarWorldComponent.KEY.sync(arenaWorld);
        }
    }

    private static void resetArenaDimension(MinecraftServer server) {
        ServerWorld arenaWorld = DeathArenaStateHelper.getArenaWorld(server);
        if (arenaWorld == null) {
            return;
        }

        GameWorldComponent arenaGame = GameWorldComponent.KEY.get(arenaWorld);
        arenaGame.clearRoleMap();
        arenaGame.clearRooms();
        arenaGame.setGameStatus(GameWorldComponent.GameStatus.INACTIVE);
        arenaGame.setGameMode(WatheGameModes.MURDER);
        arenaGame.sync();

        LooseEndsRadarWorldComponent.KEY.get(arenaWorld).reset();
        LooseEndsRadarWorldComponent.KEY.sync(arenaWorld);
    }

    private static void tickMapResetTask(ServerWorld world) {
        MapResetTask task = MAP_RESET_TASKS.get(world.getRegistryKey());
        if (task == null) {
            return;
        }
        if (task.tick()) {
            MAP_RESET_TASKS.remove(world.getRegistryKey());
        }
    }

    private static boolean isArenaMapResetting(ServerWorld world) {
        MapResetTask task = MAP_RESET_TASKS.get(world.getRegistryKey());
        return task != null && !task.isFinished();
    }
}
