package org.agmas.noellesroles;

import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class AwesomeBinglusRole extends ModdedRole {
    public AwesomeBinglusRole(Identifier id, int color, boolean winsWithKillers, boolean isKiller, int maxCount, int packet_id) {
        super(id, color, winsWithKillers, isKiller, maxCount, packet_id);
    }

    @Override
    public void onGameStarted(PlayerEntity player) {
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());
        player.giveItemStack(TMMItems.NOTE.getDefaultStack());

        super.onGameStarted(player);
    }
}
