package org.agmas.noellesroles.vulture;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;

public final class VultureHelper {
    private VultureHelper() {}

    public static boolean canSpawn(dev.doctor4t.wathe.api.RoleSelectionContext ctx) {
        Role ferryman = WatheRoles.getRole(Noellesroles.FERRYMAN_ID);
        return ferryman == null || !ctx.isRoleAssigned(ferryman);
    }
}
