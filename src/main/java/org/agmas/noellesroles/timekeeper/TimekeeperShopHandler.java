package org.agmas.noellesroles.timekeeper;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;

/**
 * 计时员商店处理器
 * - 添加特殊物品：减少30秒游戏时间（100金币）
 */
public class TimekeeperShopHandler {

    // 30秒 = 30 * 20 = 600 ticks
    private static final int TIME_REDUCTION_TICKS = 600;
    private static final int PRICE = 100;

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.TIMEKEEPER)) {
                // 创建减少时间的商店物品
                ItemStack reduceTimeItem = new ItemStack(Items.CLOCK);
                reduceTimeItem.set(DataComponentTypes.CUSTOM_NAME,
                        Text.translatable("item.noellesroles.timekeeper_reduce_time"));

                context.addEntry(0, new ShopEntry(reduceTimeItem, PRICE, ShopEntry.Type.TOOL) {
                    @Override
                    public boolean onBuy(PlayerEntity buyPlayer) {
                        // 减少游戏时间
                        GameTimeComponent timeComponent = GameTimeComponent.KEY.get(buyPlayer.getWorld());
                        timeComponent.addTime(-TIME_REDUCTION_TICKS);
                        return true;
                    }
                });
            }
        });
    }
}
