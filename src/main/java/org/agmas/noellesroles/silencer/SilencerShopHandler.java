package org.agmas.noellesroles.silencer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.Noellesroles;

/**
 * 静语者商店处理器
 * - 替换疯魔模式的购买回调，使用 startPsycho(false) 避免触发 psycho drone 音乐
 */
public class SilencerShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.SILENCER)) {
                for (int i = 0; i < context.getEntries().size(); i++) {
                    if (context.getEntries().get(i).id().equals("psycho_mode")) {
                        context.setEntry(i, new ShopEntry.Builder("psycho_mode", WatheItems.PSYCHO_MODE.getDefaultStack(), 350, ShopEntry.Type.WEAPON)
                                .cooldown(GameConstants.getInTicks(5, 0))
                                .onBuy(p -> PlayerPsychoComponent.KEY.get(p).startPsycho(false))
                                .build());
                        break;
                    }
                }
            }
        });
    }
}
