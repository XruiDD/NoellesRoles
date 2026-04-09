package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

public final class RoleUtils {
    private RoleUtils() {}

    public static boolean isActualKillerRole(Role role) {
        return role == WatheRoles.KILLER
                || role == Noellesroles.SWAPPER
                || role == Noellesroles.PHANTOM
                || role == Noellesroles.MORPHLING
                || role == Noellesroles.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES
                || role == Noellesroles.BOMBER
                || role == Noellesroles.ASSASSIN
                || role == Noellesroles.SCAVENGER
                || role == Noellesroles.SERIAL_KILLER
                || role == Noellesroles.SILENCER
                || role == Noellesroles.POISONER
                || role == Noellesroles.BANDIT
                || role == Noellesroles.HUNTER
                || role == Noellesroles.COMMANDER;
    }

    public static int countAliveAndNotSwallowed(ServerWorld serverWorld) {
        int count = 0;
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(p) || p.isSpectator()) continue;
            SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(p);
            if (!swallowed.isSwallowed()) {
                count++;
            }
        }
        return count;
    }
}
