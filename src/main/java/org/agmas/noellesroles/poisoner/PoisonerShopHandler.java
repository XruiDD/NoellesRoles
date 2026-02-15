package org.agmas.noellesroles.poisoner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 毒师商店处理器
 * - 移除刀、枪、疯魔模式、手雷
 * - 保留毒药瓶、蝎子
 * - 添加毒针（100金币）和毒气弹（300金币）
 */
public class PoisonerShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.POISONER)) {
                context.getEntries().removeIf(entry ->
                    entry.stack().isOf(WatheItems.KNIFE) ||
                    entry.stack().isOf(WatheItems.REVOLVER) ||
                    entry.stack().isOf(WatheItems.PSYCHO_MODE) ||
                    entry.stack().isOf(WatheItems.GRENADE) ||
                    entry.stack().isOf(WatheItems.POISON_VIAL) ||
                    entry.stack().isOf(WatheItems.SCORPION)
                );
                context.addEntry(0, new ShopEntry(WatheItems.SCORPION.getDefaultStack(), 50, ShopEntry.Type.POISON));
                context.addEntry(0, new ShopEntry(WatheItems.POISON_VIAL.getDefaultStack(), 50, ShopEntry.Type.POISON));
                context.addEntry(0, new ShopEntry(ModItems.CATALYST.getDefaultStack(), 100, ShopEntry.Type.WEAPON));
                context.addEntry(0, new ShopEntry(ModItems.POISON_GAS_BOMB.getDefaultStack(), 300, ShopEntry.Type.WEAPON));
                context.addEntry(0, new ShopEntry(ModItems.POISON_NEEDLE.getDefaultStack(), 100, ShopEntry.Type.WEAPON));
            }
        });
    }
}
