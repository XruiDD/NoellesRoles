package org.agmas.noellesroles.bomber;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 炸弹客商店处理器
 * - 移除刀、枪、疯魔模式、毒药和蝎子
 * - 添加定时炸弹（150金币）在第一位
 * - 修改手雷价格为300金币
 * - 保留其他工具类物品
 */
public class BomberShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BOMBER)) {
                context.getEntries().removeIf(entry ->
                    entry.stack().isOf(WatheItems.KNIFE) ||
                    entry.stack().isOf(WatheItems.REVOLVER) ||
                    entry.stack().isOf(WatheItems.POISON_VIAL) ||
                    entry.stack().isOf(WatheItems.SCORPION) ||
                    entry.stack().isOf(WatheItems.PSYCHO_MODE) ||
                    entry.stack().isOf(WatheItems.GRENADE)
                );
                context.addEntry(0, new ShopEntry(WatheItems.GRENADE.getDefaultStack(), 300, ShopEntry.Type.WEAPON));
                context.addEntry(0, new ShopEntry(ModItems.TIMED_BOMB.getDefaultStack(), 100, ShopEntry.Type.WEAPON));
            }
        });
    }
}
