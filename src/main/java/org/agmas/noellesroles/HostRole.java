package org.agmas.noellesroles;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class HostRole extends ModdedRole {
    public HostRole(Identifier id, int color, boolean winsWithKillers, boolean isKiller, int maxCount, int packet_id) {
        super(id, color, winsWithKillers, isKiller, maxCount, packet_id);
    }

    @Override
    public void onGameStarted(PlayerEntity player) {
        player.giveItemStack(ModItems.MASTER_KEY.getDefaultStack());

        super.onGameStarted(player);
    }
}
