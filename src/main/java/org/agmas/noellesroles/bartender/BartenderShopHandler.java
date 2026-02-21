package org.agmas.noellesroles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

public class BartenderShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BARTENDER)) {
                context.clearEntries();
                context.addEntry(new ShopEntry(ModItems.FINE_DRINK.getDefaultStack(), 75, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.BASE_SPIRIT.getDefaultStack(), 50, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.RUM.getDefaultStack(), 100, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.GIN.getDefaultStack(), 100, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.VODKA.getDefaultStack(), 275, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.TEQUILA.getDefaultStack(), 175, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.WHISKEY.getDefaultStack(), 75, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.ICE_CUBE.getDefaultStack(), 25, ShopEntry.Type.POISON));
            }
        });
    }
}
