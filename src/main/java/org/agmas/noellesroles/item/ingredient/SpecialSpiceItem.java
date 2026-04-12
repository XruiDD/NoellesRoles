package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;
import org.jetbrains.annotations.Nullable;

/**
 * 特调香料 — 修饰剂。
 * 添加后额外恢复 60% SAN 值（理智/情绪）。
 * 不参与鸡尾酒命名。
 */
public class SpecialSpiceItem extends IngredientItem {
    public SpecialSpiceItem(Settings settings) {
        super(settings, "special_spice");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, float durationMultiplier) {
        addMoodBonus(player, 0.6f);
    }

    @Override
    public @Nullable String getSuffixTranslationKey() {
        return "cocktail.noellesroles.spice_suffix";
    }

    @Override
    public boolean isModifier() {
        return true;
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xD2691E; // 肉桂棕
    }

    @Override
    public int getShopPrice() {
        return 25;
    }
}
