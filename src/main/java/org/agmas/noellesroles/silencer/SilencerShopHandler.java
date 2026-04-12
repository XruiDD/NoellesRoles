package org.agmas.noellesroles.silencer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.Noellesroles;

import java.util.List;

import static dev.doctor4t.wathe.game.GameConstants.getInTicks;

/**
 * 静语者商店处理器
 * - 替换疯魔模式：使用时不触发全服BGM（静默疯魔）
 */
public class SilencerShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.SILENCER)) {
                // 找到原版疯魔模式的位置
                List<ShopEntry> entries = context.getEntries();
                int psychoIndex = -1;
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).stack().isOf(WatheItems.PSYCHO_MODE)) {
                        psychoIndex = i;
                        break;
                    }
                }
                if (psychoIndex < 0) psychoIndex = 0;

                // 原位替换为静默疯魔模式（startPsycho(false) 不追踪计数器，不触发BGM）
                entries.remove(psychoIndex);
                context.addEntry(psychoIndex, new ShopEntry.Builder("psycho_mode", WatheItems.PSYCHO_MODE.getDefaultStack(), 350, ShopEntry.Type.WEAPON)
                    .cooldown(getInTicks(5, 0))
                    .onBuy(p -> PlayerPsychoComponent.KEY.get(p).startPsycho(false))
                    .build());
            }
        });
    }
}
