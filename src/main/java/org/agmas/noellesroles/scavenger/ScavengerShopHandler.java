package org.agmas.noellesroles.scavenger;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;

/**
 * 清道夫商店处理器
 * - 移除所有攻击性武器（枪、手雷、疯魔模式、毒药、蝎子）
 * - 只保留刀
 * - 添加特殊物品：重置刀CD（100金币）
 */
public class ScavengerShopHandler {
    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.SCAVENGER)) {
                // 移除除了刀以外的所有攻击性武器
                context.getEntries().removeIf(entry ->
                    entry.stack().isOf(WatheItems.REVOLVER) ||       // 移除枪
                    entry.stack().isOf(WatheItems.POISON_VIAL) ||    // 移除毒药
                    entry.stack().isOf(WatheItems.SCORPION) ||       // 移除蝎子
                    entry.stack().isOf(WatheItems.PSYCHO_MODE) ||    // 移除疯魔模式
                    entry.stack().isOf(WatheItems.GRENADE)           // 移除手雷
                );
                ItemStack resetCDItem = new ItemStack(Items.CLOCK);
                resetCDItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                        Text.translatable("item.noellesroles.scavenger_reset_knife_cd"));
                context.addEntry(1, new ShopEntry(resetCDItem, 125, ShopEntry.Type.WEAPON) {
                    @Override
                    public boolean onBuy(PlayerEntity buyPlayer) {
                        buyPlayer.getItemCooldownManager().set(WatheItems.KNIFE, 0);
                        return true;
                    }
                });
            }
        });
    }
}
