package org.agmas.noellesroles.commander;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.util.RoleUtils;

import java.util.UUID;

public final class CommanderHelper {
    private CommanderHelper() {}

    public static boolean isNotificationRecipient(GameWorldComponent gameWorldComponent, PlayerEntity player) {
        Role role = gameWorldComponent.getRole(player);
        return role == Noellesroles.UNDERCOVER || RoleUtils.isActualKillerRole(role);
    }

    public static boolean isPayoutRecipient(GameWorldComponent gameWorldComponent, PlayerEntity player) {
        return RoleUtils.isActualKillerRole(gameWorldComponent.getRole(player));
    }

    public static void notifyIdentity(ServerWorld world, ServerPlayerEntity commander) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
            if (!RoleUtils.isActualKillerRole(gameWorldComponent.getRole(player))) continue;
            if (player.getUuid().equals(commander.getUuid())) continue;
            player.sendMessage(Text.translatable("tip.commander.identity_known", commander.getName()), true);
        }
    }

    public static void tryBroadcastIdentity(ServerWorld world) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        for (UUID uuid : gameWorldComponent.getAllWithRole(Noellesroles.COMMANDER)) {
            PlayerEntity commanderEntity = world.getPlayerByUuid(uuid);
            if (!(commanderEntity instanceof ServerPlayerEntity commander) || !GameFunctions.isPlayerPlayingAndAlive(commander)) continue;
            CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(commander);
            if (commanderComp.isIntroBroadcasted()) continue;
            notifyIdentity(world, commander);
            commanderComp.setIntroBroadcasted(true);
        }
    }

    public static void broadcastDeath(ServerWorld world, ServerPlayerEntity commander) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
            if (!isNotificationRecipient(gameWorldComponent, player)) continue;
            player.sendMessage(Text.translatable("tip.commander.dead", commander.getName()), true);
        }
    }

    /**
     * Handles commander-related logic in KillPlayer.AFTER (death broadcast + reward payout).
     */
    public static void handleAfterKill(PlayerEntity victim, PlayerEntity killer, GameWorldComponent gameComponent) {
        if (victim instanceof ServerPlayerEntity serverVictim && gameComponent.isRole(victim, Noellesroles.COMMANDER) && victim.getWorld() instanceof ServerWorld serverWorld) {
            broadcastDeath(serverWorld, serverVictim);
        }

        if (killer != null && victim.getWorld() instanceof ServerWorld rewardWorld) {
            for (UUID commanderUuid : gameComponent.getAllWithRole(Noellesroles.COMMANDER)) {
                PlayerEntity commanderEntity = rewardWorld.getPlayerByUuid(commanderUuid);
                if (!(commanderEntity instanceof ServerPlayerEntity commander)) continue;
                CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(commander);
                if (!commanderComp.isThreatTarget(victim.getUuid())) continue;

                if (!killer.getUuid().equals(commander.getUuid()) && RoleUtils.isActualKillerRole(gameComponent.getRole(killer))) {
                    for (ServerPlayerEntity player : rewardWorld.getPlayers()) {
                        if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                        if (!isPayoutRecipient(gameComponent, player)) continue;
                        PlayerShopComponent.KEY.get(player).addToBalance(50);
                        player.sendMessage(Text.translatable("tip.commander.reward", victim.getName()), true);
                    }
                }

                commanderComp.removeThreatTarget(victim.getUuid());
            }
        }
    }

    public static void checkLastKillerDeath(ServerWorld world) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        for (UUID uuid : gameWorldComponent.getAllWithRole(Noellesroles.COMMANDER)) {
            PlayerEntity commanderEntity = world.getPlayerByUuid(uuid);
            if (!(commanderEntity instanceof ServerPlayerEntity commander) || !GameFunctions.isPlayerPlayingAndAlive(commander)) continue;

            boolean hasOtherLivingKillers = false;
            for (UUID killerUuid : gameWorldComponent.getAllKillerTeamPlayers()) {
                if (killerUuid.equals(commander.getUuid())) continue;
                PlayerEntity killer = world.getPlayerByUuid(killerUuid);
                if (killer == null || !GameFunctions.isPlayerPlayingAndAlive(killer)) continue;
                if (!RoleUtils.isActualKillerRole(gameWorldComponent.getRole(killer))) continue;
                hasOtherLivingKillers = true;
                break;
            }

            if (!hasOtherLivingKillers) {
                GameFunctions.killPlayer(commander, true, null, Noellesroles.DEATH_REASON_COMMANDER_SUICIDE);
            }
        }
    }
}
