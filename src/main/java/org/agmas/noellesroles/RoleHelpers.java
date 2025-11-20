package org.agmas.noellesroles;

import dev.doctor4t.trainmurdermystery.cca.ScoreboardRoleSelectorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

public class RoleHelpers {

    public static RoleHelpers instance;
    private static HashMap<Identifier, ModdedRole> moddedRoles = new HashMap<>();
    private static HashMap<Integer, Identifier> registerOrder = new HashMap<>();
    public static void registerNewRole(Identifier id, int color, boolean winsWithKillers, boolean isKiller, int maxCount) {
        int packet_id = 100 + moddedRoles.size() + 1;

        moddedRoles.put(id, new ModdedRole(id,color,winsWithKillers,isKiller, maxCount, packet_id));
        registerOrder.put(packet_id, id);
    }

    public ModdedRole getRoleFromAnnouncerID(int packet_id) {
        return moddedRoles.get(registerOrder.get(packet_id));
    }
    public static void registerNewTypedRole(ModdedRole role) {
        int packet_id = 100 + moddedRoles.size() + 1;
        role.packet_id = packet_id;

        moddedRoles.put(role.id,role);
        registerOrder.put(packet_id, role.id);
    }
    public static void registerNewRole(Identifier id, int color, boolean winsWithKillers, boolean isKiller) {
        registerNewRole(id,color,winsWithKillers,isKiller,0);
    }
    public Collection<ModdedRole> getModdedRoles() {
        return moddedRoles.values();
    }

    public boolean isPlayerOf(PlayerEntity name, Identifier role) {
        return moddedRoles.get(role).isInRole(name);
    }
    public ModdedRole getRoleOfPlayer(PlayerEntity name) {
        for (ModdedRole role : getModdedRoles()) {
            if (role.isInRole(name)) return role;
        }
        return null;
    }
    public boolean isOfAnyModdedRole(PlayerEntity name) {
        for (ModdedRole role : getModdedRoles()) {
            if (role.isInRole(name)) return true;
        }
        return false;
    }
}
