package org.agmas.noellesroles.deatharena;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration;
import dev.doctor4t.wathe.config.datapack.MapRegistry;
import dev.doctor4t.wathe.config.datapack.MapRegistryEntry;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.ferryman.FerrymanPlayerComponent;
import org.agmas.noellesroles.looseend.LooseEndPlayerComponent;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownManagerAccessor;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class DeathArenaStateHelper {
    public static final Identifier DEFAULT_DEATH_ARENA_MAP_ID = Identifier.of("wathe", "yangguan_siwang");
    public static final Identifier DEFAULT_DEATH_ARENA_DIMENSION_ID = Identifier.of("wathe", "yangguan_siwang");
    private static final Random RANDOM = new Random();

    private DeathArenaStateHelper() {
    }

    public static Identifier getConfiguredMapId() {
        return parseIdentifierOrFallback(
                NoellesRolesConfig.HANDLER.instance().deathArenaMapId,
                DEFAULT_DEATH_ARENA_MAP_ID
        );
    }

    public static MapRegistryEntry getArenaMapEntry() {
        return MapRegistry.getInstance().getMap(getConfiguredMapId());
    }

    public static Identifier getArenaDimensionId() {
        Identifier configuredDimensionId = parseIdentifierOrFallback(
                NoellesRolesConfig.HANDLER.instance().deathArenaDimensionId,
                null
        );
        if (configuredDimensionId != null) {
            return configuredDimensionId;
        }
        MapRegistryEntry entry = getArenaMapEntry();
        if (entry != null) {
            return entry.dimensionId();
        }
        return DEFAULT_DEATH_ARENA_DIMENSION_ID;
    }

    public static ServerWorld getArenaWorld(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        MapRegistryEntry entry = getArenaMapEntry();
        if (entry == null) {
            return null;
        }
        RegistryKey<net.minecraft.world.World> worldKey = RegistryKey.of(RegistryKeys.WORLD, entry.dimensionId());
        return server.getWorld(worldKey);
    }

    public static boolean isDeathArenaParticipant(ServerPlayerEntity player) {
        return player != null && DeathArenaPlayerComponent.KEY.get(player).isInArena();
    }

    public static boolean isDeathArenaParticipant(UUID playerUuid, ServerWorld world) {
        return DeathArenaWorldComponent.KEY.get(world).isParticipant(playerUuid);
    }

    public static boolean isDeathArenaActive(ServerWorld world) {
        return world != null && DeathArenaWorldComponent.KEY.get(world).isActive();
    }

    public static boolean isDeathArenaDimension(ServerWorld world) {
        Identifier arenaDimensionId = getArenaDimensionId();
        return world != null
                && arenaDimensionId != null
                && world.getRegistryKey().getValue().equals(arenaDimensionId);
    }

    public static boolean isLooseEndsLikeWorld(ServerWorld world, GameWorldComponent gameWorld) {
        if (world == null || gameWorld == null) {
            return false;
        }
        return gameWorld.getGameMode() == WatheGameModes.LOOSE_ENDS
                || (isDeathArenaDimension(world) && isDeathArenaActive(getArenaOriginWorld(world.getServer())));
    }

    public static ServerWorld getArenaOriginWorld(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (DeathArenaWorldComponent.KEY.get(world).isActive()) {
                return world;
            }
        }
        return null;
    }

    public static ServerWorld resolveGameControlWorld(ServerWorld world) {
        if (world == null || !isDeathArenaDimension(world)) {
            return world;
        }

        ServerWorld originWorld = getArenaOriginWorld(world.getServer());
        return originWorld != null ? originWorld : world;
    }

    public static boolean isLooseEndsLikePlayer(ServerPlayerEntity player, GameWorldComponent gameWorld) {
        return gameWorld != null && player != null && (
                gameWorld.isRole(player, WatheRoles.LOOSE_END) || isDeathArenaParticipant(player)
        );
    }

    public static void applyLooseEndsOpeningState(ServerPlayerEntity player, GameWorldComponent gameWorld) {
        if (player == null || gameWorld == null) {
            return;
        }

        resetRespawnCooldowns(player);
        AbilityPlayerComponent.KEY.get(player).setCooldown(NoellesRolesConfig.HANDLER.instance().generalCooldownTicks);
        LooseEndPlayerComponent.KEY.get(player).reset();
        player.getInventory().clear();
        player.currentScreenHandler.sendContentUpdates();

        player.giveItemStack(ModItems.MASTER_KEY.getDefaultStack());
        player.giveItemStack(WatheItems.KNIFE.getDefaultStack());

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        shop.initializeShop(ShopUtils.getShopEntriesForPlayer(player));
        shop.balance = 0;
        shop.addToBalance(100);
        shop.sync();

        player.clearStatusEffects();
        player.extinguish();
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0F);
        int initialKnifeCooldown = GameConstants.getInTicks(0, 30);
        player.getItemCooldownManager().set(WatheItems.KNIFE, initialKnifeCooldown);
        AbilityPlayerComponent.KEY.get(player).markKnifeCooldownOverride(initialKnifeCooldown);
        player.getItemCooldownManager().remove(ModItems.MASTER_KEY);
        player.setAir(player.getMaxAir());
        player.setFrozenTicks(0);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
        player.setVelocity(Vec3d.ZERO);
        player.noClip = false;
        player.setInvisible(false);
        player.changeGameMode(GameMode.ADVENTURE);
        LooseEndPlayerComponent.KEY.get(player).startOpeningPhase();
    }

    private static void resetRespawnCooldowns(ServerPlayerEntity player) {
        AbilityPlayerComponent.KEY.get(player).reset();
        AssassinPlayerComponent.KEY.get(player).setCooldown(0);
        FerrymanPlayerComponent.KEY.get(player).clearReaction();
        TaotiePlayerComponent.KEY.get(player).setSwallowCooldown(0);
        clearAllItemCooldowns(player);
    }

    private static void clearAllItemCooldowns(ServerPlayerEntity player) {
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
        for (var item : new HashSet<>(accessor.getEntries().keySet())) {
            player.getItemCooldownManager().remove(item);
        }
    }

    public static Optional<List<DeathArenaWorldComponent.SpawnLocation>> loadConfiguredSpawnLocations() {
        MapRegistryEntry entry = getArenaMapEntry();
        MapEnhancementsConfiguration enhancements = entry == null ? null : entry.enhancements();
        if (enhancements == null) {
            return Optional.empty();
        }

        List<DeathArenaWorldComponent.SpawnLocation> locations = new ArrayList<>();
        for (int roomIndex = 0; roomIndex < enhancements.getRoomCount(); roomIndex++) {
            var room = enhancements.getRoomConfig(roomIndex);
            if (room.isEmpty()) {
                continue;
            }
            int spawnCount = room.get().spawnPoints().size();
            for (int spawnIndex = 0; spawnIndex < spawnCount; spawnIndex++) {
                var spawn = enhancements.getSpawnPointForPlayer(roomIndex, spawnIndex);
                spawn.ifPresent(spawnPoint -> locations.add(new DeathArenaWorldComponent.SpawnLocation(
                        new Vec3d(spawnPoint.x(), spawnPoint.y(), spawnPoint.z()),
                        spawnPoint.yaw(),
                        spawnPoint.pitch()
                )));
            }
        }
        return locations.isEmpty() ? Optional.empty() : Optional.of(locations);
    }

    public static DeathArenaWorldComponent.SpawnLocation pickRandomSpawn(DeathArenaWorldComponent component) {
        if (component == null || component.getSpawnLocations().isEmpty()) {
            return null;
        }
        return component.getSpawnLocations().get(RANDOM.nextInt(component.getSpawnLocations().size()));
    }

    private static Identifier parseIdentifierOrFallback(String raw, Identifier fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Identifier parsed = Identifier.tryParse(raw.trim());
        return parsed == null ? fallback : parsed;
    }
}
