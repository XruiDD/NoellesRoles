package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;

public class SpecialSpiceItem extends IngredientItem {
    public SpecialSpiceItem(Settings settings) {
        super(settings, "special_spice");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, EffectContext context) {
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xC96F1A;
    }

    @Override
    public int getShopPrice() {
        return 25;
    }
}
