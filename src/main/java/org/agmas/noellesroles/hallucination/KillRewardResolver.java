package org.agmas.noellesroles.hallucination;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;

public final class KillRewardResolver {
    private static final int BASE_KILL_MONEY = 100;
    private static final int BASE_CIVILIAN_KILL_TIME_SECONDS = 15;
    private static final int BASE_DUMMY_KILL_TIME_SECONDS = 30;

    private KillRewardResolver() {
    }

    public static KillRewardResult resolve(KillRewardContext context) {
        if (context == null || context.killer() == null) {
            return KillRewardResult.NONE;
        }

        PlayerEntity killer = context.killer();
        PlayerEntity victim = context.victim();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(killer.getWorld());

        if (context.hallucinationDummy()) {
            return resolveHallucinationDummyReward(context, gameWorld);
        }

        int money = 0;
        int timeDeltaSeconds = 0;

        if (victim != null && gameWorld.isRole(killer, WatheRoles.LOOSE_END)) {
            money += BASE_KILL_MONEY;
        }

        if (victim != null && gameWorld.isInnocent(killer) && !suppressesCivilianKillTime(context, gameWorld)) {
            timeDeltaSeconds += BASE_CIVILIAN_KILL_TIME_SECONDS;
        }

        if (victim != null && gameWorld.isRole(killer, Noellesroles.SERIAL_KILLER)) {
            SerialKillerPlayerComponent serial = SerialKillerPlayerComponent.KEY.get(killer);
            if (serial.isCurrentTarget(victim.getUuid())) {
                money += SerialKillerPlayerComponent.getBonusMoney();
            }
        }

        if (gameWorld.isRole(killer, Noellesroles.BOMBER) && context.deathReason() == Noellesroles.DEATH_REASON_BOMB) {
            money += 50;
        }

        if (gameWorld.isRole(killer, WatheRoles.LOOSE_END) && context.bombOwnerUuid() != null) {
            money += 25;
        } else if (context.bombOwnerUuid() != null) {
            money += 100;
        }

        if (gameWorld.isRole(killer, Noellesroles.COMMANDER)) {
            money -= 25;
        }

        if (context.deathReason() == dev.doctor4t.wathe.game.GameConstants.DeathReasons.POISON
                && context.poisonerUuid() != null) {
            PlayerEntity poisoner = killer.getWorld().getPlayerByUuid(context.poisonerUuid());
            if (poisoner != null && gameWorld.isRole(poisoner, WatheRoles.LOOSE_END)) {
                money += 25;
            }
        }

        return new KillRewardResult(money, timeDeltaSeconds);
    }

    private static KillRewardResult resolveHallucinationDummyReward(KillRewardContext context, GameWorldComponent gameWorld) {
        PlayerEntity killer = context.killer();
        if (killer == null) {
            return KillRewardResult.NONE;
        }

        int money = 0;
        if (gameWorld.canUseKillerFeatures(killer)) {
            money = BASE_KILL_MONEY;
            if (gameWorld.isRole(killer, Noellesroles.COMMANDER)) {
                money = 75;
            }
            if (context.deathReason() == Noellesroles.DEATH_REASON_BOMB && gameWorld.isRole(killer, Noellesroles.BOMBER)) {
                money = 150;
            }
        }

        int timeDeltaSeconds = 0;
        if (gameWorld.canUseKillerFeatures(killer) && !suppressesDummyKillTime(context)) {
            timeDeltaSeconds = BASE_DUMMY_KILL_TIME_SECONDS;
        }

        return new KillRewardResult(money, timeDeltaSeconds);
    }

    private static boolean suppressesCivilianKillTime(KillRewardContext context, GameWorldComponent gameWorld) {
        PlayerEntity killer = context.killer();
        if (killer == null) {
            return false;
        }
        return gameWorld.canUseKillerFeatures(killer)
                && context.deathReason() == dev.doctor4t.wathe.game.GameConstants.DeathReasons.GUN
                && killer.getMainHandStack().isOf(dev.doctor4t.wathe.index.WatheItems.REVOLVER);
    }

    private static boolean suppressesDummyKillTime(KillRewardContext context) {
        PlayerEntity killer = context.killer();
        if (killer == null || context.deathReason() != dev.doctor4t.wathe.game.GameConstants.DeathReasons.GUN) {
            return false;
        }

        if (killer.getMainHandStack().isOf(dev.doctor4t.wathe.index.WatheItems.REVOLVER)) {
            return true;
        }
        return false;
    }
}
