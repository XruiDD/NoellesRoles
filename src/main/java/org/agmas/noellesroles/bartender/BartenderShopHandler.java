package org.agmas.noellesroles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

public class BartenderShopHandler {
    public static void register() {
        // Replace bartender's shop with Fine Drink at fixed 50 gold price
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BARTENDER)) {
                context.clearEntries();
                context.addEntry(new ShopEntry(ModItems.FINE_DRINK.getDefaultStack(), 75, ShopEntry.Type.POISON));
            }
        });
    }
}
