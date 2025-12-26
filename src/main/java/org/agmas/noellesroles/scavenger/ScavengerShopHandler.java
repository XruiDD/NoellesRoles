package org.agmas.noellesroles.scavenger;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.event.BuildShopEntries;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import dev.doctor4t.trainmurdermystery.util.ShopEntry;
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
                    entry.stack().isOf(TMMItems.REVOLVER) ||       // 移除枪
                    entry.stack().isOf(TMMItems.POISON_VIAL) ||    // 移除毒药
                    entry.stack().isOf(TMMItems.SCORPION) ||       // 移除蝎子
                    entry.stack().isOf(TMMItems.PSYCHO_MODE) ||    // 移除疯魔模式
                    entry.stack().isOf(TMMItems.GRENADE)           // 移除手雷
                );

                // 添加特殊物品：重置刀CD（使用时钟物品作为图标）
                ItemStack resetCDItem = new ItemStack(Items.CLOCK);
                resetCDItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                    Text.translatable("item.noellesroles.scavenger_reset_knife_cd"));
                resetCDItem.set(net.minecraft.component.DataComponentTypes.LORE,
                    java.util.List.of(Text.translatable("item.noellesroles.scavenger_reset_knife_cd.tooltip")));

                // 添加到商店（放在第一位，100金币）
                context.addEntry(0, new ShopEntry(resetCDItem, 100, ShopEntry.Type.TOOL));
            }
        });
    }
}
