package org.agmas.noellesroles.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;

import java.util.List;

/**
 * 调制品物品 - 不可直接使用，只能添加到基酒中。
 * 玩家手持调制品右键点击背包中的基酒来添加。
 */
public class IngredientItem extends Item {
    private final String ingredientId;

    public IngredientItem(Settings settings, String ingredientId) {
        super(settings);
        this.ingredientId = ingredientId;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.ingredient.tooltip").formatted(Formatting.GRAY));
    }
}
