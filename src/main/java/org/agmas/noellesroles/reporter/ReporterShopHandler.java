package org.agmas.noellesroles.reporter;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.Noellesroles;

public class ReporterShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.REPORTER)) {
                // 便条: 10金币, 4个一组, 无限制
                context.addEntry(new ShopEntry.Builder("note", new ItemStack(WatheItems.NOTE, 4), 10, ShopEntry.Type.TOOL)
                    .build());
            }
        });
    }
}
