package org.agmas.noellesroles.waiter;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;

import java.util.List;

public class WaiterShopHandler {
    // 所有可以随机购买到的食物和饮品
    private static final List<Item> RANDOM_FOOD_AND_DRINKS = List.of(
            // wathe 饮品
            WatheItems.OLD_FASHIONED,
            WatheItems.MOJITO,
            WatheItems.MARTINI,
            WatheItems.COSMOPOLITAN,
            WatheItems.CHAMPAGNE,
            // 食物
            Items.BREAD,
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.BAKED_POTATO,
            Items.APPLE
    );

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.WAITER)) {
                context.clearEntries();

                // 商店展示图标：用香槟物品但自定义名称
                ItemStack displayStack = WatheItems.CHAMPAGNE.getDefaultStack();
                displayStack.set(DataComponentTypes.CUSTOM_NAME,
                        Text.translatable("item.noellesroles.waiter_service"));

                // 随机食物/饮品 - 100金币
                context.addEntry(new ShopEntry.Builder(
                        "waiter_random_food_or_drink",
                        displayStack,
                        100,
                        ShopEntry.Type.TOOL
                ).onBuy(buyer -> {
                    Item randomItem = RANDOM_FOOD_AND_DRINKS.get(
                            buyer.getWorld().getRandom().nextInt(RANDOM_FOOD_AND_DRINKS.size()));
                    ItemStack stack = new ItemStack(randomItem);
                    return ShopEntry.insertStackInFreeSlot(buyer, stack);
                }).build());
            }
        });
    }
}
