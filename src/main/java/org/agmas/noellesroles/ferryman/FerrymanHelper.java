package org.agmas.noellesroles.ferryman;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.util.TypeFilter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FerrymanHelper {
    public static final int COUNTER_STUN_AMPLIFIER = 6;
    public static final int CORPSE_RANGE = 5;

    private FerrymanHelper() {}

    public static boolean isReactionDeathReason(Identifier deathReason) {
        return deathReason != GameConstants.DeathReasons.FELL_OUT_OF_TRAIN
                && deathReason != GameConstants.DeathReasons.ESCAPED;
    }

    public static boolean canSpawn(dev.doctor4t.wathe.api.RoleSelectionContext ctx) {
        Role vulture = WatheRoles.getRole(Noellesroles.VULTURE_ID);
        return vulture == null || !ctx.isRoleAssigned(vulture);
    }

    public static PlayerBodyEntity findTargetBody(ServerPlayerEntity player, FerrymanPlayerComponent ferrymanComponent) {
        int decomposedAge = GameConstants.TIME_TO_DECOMPOSITION + GameConstants.DECOMPOSING_TIME;
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F).normalize();
        PlayerBodyEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PlayerBodyEntity body : player.getWorld().getEntitiesByType(
                TypeFilter.equals(PlayerBodyEntity.class),
                player.getBoundingBox().expand(CORPSE_RANGE),
                corpse -> corpse.getPlayerUuid() != null
                        && !ferrymanComponent.hasFerriedBody(corpse.getUuid())
                        && corpse.age < decomposedAge)) {
            Vec3d toBody = body.getPos().add(0.0, 0.6, 0.0).subtract(eyePos);
            double distance = toBody.length();
            if (distance > CORPSE_RANGE || distance <= 0.001D) continue;

            double alignment = look.dotProduct(toBody.normalize());
            if (alignment < 0.78D) continue;
            if (!player.canSee(body)) continue;

            double score = alignment - distance * 0.02D;
            if (score > bestScore) {
                bestScore = score;
                best = body;
            }
        }

        return best;
    }

    /**
     * Handles ferryman reaction logic in KillPlayer.BEFORE.
     * Returns a KillResult if the kill should be cancelled, null otherwise.
     */
    /**
     * Handles ferryman pending death resolution in the tick event.
     */
    public static void handleTick(ServerWorld world, GameWorldComponent gc) {
        for (UUID uuid : gc.getAllWithRole(Noellesroles.FERRYMAN)) {
            PlayerEntity ferrymanEntity = world.getPlayerByUuid(uuid);
            if (!(ferrymanEntity instanceof ServerPlayerEntity ferryman) || !GameFunctions.isPlayerPlayingAndAlive(ferryman)) continue;

            FerrymanPlayerComponent ferrymanComponent = FerrymanPlayerComponent.KEY.get(ferryman);
            if (ferrymanComponent.isReactionActive()) continue;

            Identifier pendingDeathReason = ferrymanComponent.getPendingDeathReason();
            if (pendingDeathReason == null) continue;

            ServerPlayerEntity attacker = null;
            UUID attackerUuid = ferrymanComponent.getPendingAttackerUuid();
            if (attackerUuid != null) {
                PlayerEntity attackerEntity = world.getPlayerByUuid(attackerUuid);
                if (attackerEntity instanceof ServerPlayerEntity serverAttacker) {
                    attacker = serverAttacker;
                }
            }

            ferrymanComponent.clearReaction();
            AbilityPlayerComponent.KEY.get(ferryman).setCooldown(1);
            GameFunctions.killPlayer(ferryman, true, attacker, pendingDeathReason);
        }
    }

    @Nullable
    public static KillPlayer.KillResult handleBeforeKill(PlayerEntity victim, PlayerEntity killer, Identifier deathReason, GameWorldComponent gameWorldComponent) {
        if (!(victim instanceof ServerPlayerEntity serverVictim)) return null;
        if (!gameWorldComponent.isRole(serverVictim, Noellesroles.FERRYMAN)) return null;
        if (!GameFunctions.isPlayerPlayingAndAlive(serverVictim)) return null;
        if (SwallowedPlayerComponent.isPlayerSwallowed(serverVictim)) return null;
        if (!isReactionDeathReason(deathReason)) return null;

        FerrymanPlayerComponent ferrymanComponent = FerrymanPlayerComponent.KEY.get(serverVictim);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(serverVictim);
        if (ferrymanComponent.isReactionActive() || abilityComponent.getCooldown() > 0) return null;

        UUID attackerUuid = killer != null ? killer.getUuid() : null;
        if (ferrymanComponent.beginReaction(attackerUuid, deathReason)) {
            serverVictim.getWorld().sendEntityStatus(serverVictim, EntityStatuses.ADD_PORTAL_PARTICLES);
            return KillPlayer.KillResult.cancel();
        }

        return null;
    }
}
