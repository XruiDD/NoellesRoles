package org.agmas.noellesroles;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class JesterRole extends ModdedRole {
    public JesterRole(Identifier id, int color, boolean winsWithKillers, boolean isKiller, int maxCount, int packet_id) {
        super(id, color, winsWithKillers, isKiller, maxCount, packet_id);
    }

    @Override
    public void onGameStarted(PlayerEntity player) {
        player.giveItemStack(ModItems.FAKE_KNIFE.getDefaultStack());
        player.giveItemStack(ModItems.FAKE_REVOLVER.getDefaultStack());
        player.giveItemStack(TMMItems.LOCKPICK.getDefaultStack());

        super.onGameStarted(player);
    }
}
