package org.agmas.noellesroles.bandit;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 强盗商店处理器
 * - 删除手榴弹、疯魔模式、毒药、蝎子
 * - 手枪价格改为200
 * - 手枪后面插入投掷斧（200金币）
 */
public class BanditShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BANDIT)) {
                // 删除手榴弹、疯魔模式、毒药、蝎子
                context.getEntries().removeIf(entry -> {
                    String id = entry.id();
                    return id.equals("grenade") || id.equals("psycho_mode") ||
                           id.equals("poison_vial") || id.equals("scorpion");
                });

                // 手枪价格改为150，飞斧插入手枪后面
                for (int i = 0; i < context.getEntries().size(); i++) {
                    if (context.getEntries().get(i).id().equals("revolver")) {
                        context.setEntry(i, new ShopEntry.Builder("revolver", WatheItems.REVOLVER.getDefaultStack(), 150, ShopEntry.Type.WEAPON).build());
                        context.addEntry(i + 1, new ShopEntry(ModItems.THROWING_AXE.getDefaultStack(), 200, ShopEntry.Type.WEAPON));
                        break;
                    }
                }
            }
        });
    }
}
