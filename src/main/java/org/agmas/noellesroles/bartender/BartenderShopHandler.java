package org.agmas.noellesroles.bartender;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.event.BuildShopEntries;
import dev.doctor4t.trainmurdermystery.event.ShopPurchase;
import dev.doctor4t.trainmurdermystery.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

public class BartenderShopHandler {
    public static void register() {
        // 替换酒保的商店内容
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BARTENDER)) {
                context.clearEntries();
                BartenderPlayerComponent bartender = BartenderPlayerComponent.KEY.get(player);
                int price = bartender.getCurrentPrice();
                context.addEntry(new ShopEntry(ModItems.DEFENSE_VIAL.getDefaultStack(), price, ShopEntry.Type.POISON));
            }
        });

        // 购买后增加价格（仅服务端）
        ShopPurchase.AFTER.register((player, entry, index, actualPrice) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BARTENDER)) {
                BartenderPlayerComponent bartender = BartenderPlayerComponent.KEY.get(player);
                bartender.increasePrice();
            }
        });
    }
}
