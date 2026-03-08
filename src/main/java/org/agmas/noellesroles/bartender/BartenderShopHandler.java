package org.agmas.noellesroles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.IngredientItem;

public class BartenderShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BARTENDER)) {
                context.clearEntries();
                context.addEntry(new ShopEntry(ModItems.FINE_DRINK.getDefaultStack(), 75, ShopEntry.Type.POISON));
                context.addEntry(new ShopEntry(ModItems.BASE_SPIRIT.getDefaultStack(), 50, ShopEntry.Type.POISON));
                addIngredientEntry(context, ModItems.RUM);
                addIngredientEntry(context, ModItems.GIN);
                addIngredientEntry(context, ModItems.VODKA);
                addIngredientEntry(context, ModItems.TEQUILA);
                addIngredientEntry(context, ModItems.WHISKEY);
                addIngredientEntry(context, ModItems.ICE_CUBE);
            }
        });
    }

    private static void addIngredientEntry(BuildShopEntries.ShopContext context, Item item) {
        if (item instanceof IngredientItem ingredientItem) {
            context.addEntry(new ShopEntry(
                    item.getDefaultStack(),
                    ingredientItem.getShopPrice(),
                    ShopEntry.Type.POISON
            ));
        }
    }
}
