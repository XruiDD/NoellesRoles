package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;
import org.jetbrains.annotations.Nullable;

/**
 * 特调利口酒 — 修饰剂。
 * 使同一杯酒中所有调剂的效果持续时间延长 100%（×2），
 * 包括基酒的负面效果和伏特加结束后的惩罚。
 * 自身不提供直接效果。
 */
public class SpecialLiqueurItem extends IngredientItem {
    public SpecialLiqueurItem(Settings settings) {
        super(settings, "special_liqueur");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, float durationMultiplier) {
        // 修饰剂，不提供直接效果；倍率由 getDurationMultiplier() 声明
    }

    @Override
    public float getDurationMultiplier() {
        return 2.0f;
    }

    @Override
    public @Nullable String getSuffixTranslationKey() {
        return "cocktail.noellesroles.liqueur_suffix";
    }

    @Override
    public boolean isModifier() {
        return true;
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xE6B422; // 金色
    }

    @Override
    public int getShopPrice() {
        return 125;
    }
}
