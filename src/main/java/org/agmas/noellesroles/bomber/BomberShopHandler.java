package org.agmas.noellesroles.bomber;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.event.BuildShopEntries;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import dev.doctor4t.trainmurdermystery.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 爆破手商店处理器
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
                    entry.stack().isOf(TMMItems.KNIFE) ||
                    entry.stack().isOf(TMMItems.REVOLVER) ||
                    entry.stack().isOf(TMMItems.POISON_VIAL) ||
                    entry.stack().isOf(TMMItems.SCORPION) ||
                    entry.stack().isOf(TMMItems.PSYCHO_MODE) ||
                    entry.stack().isOf(TMMItems.GRENADE)
                );
                context.addEntry(0, new ShopEntry(TMMItems.GRENADE.getDefaultStack(), 300, ShopEntry.Type.WEAPON));
                context.addEntry(0, new ShopEntry(ModItems.TIMED_BOMB.getDefaultStack(), 150, ShopEntry.Type.WEAPON));
            }
        });
    }
}
