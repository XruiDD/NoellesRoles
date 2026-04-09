package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;

public class LiqueurItem extends IngredientItem {
    public LiqueurItem(Settings settings) {
        super(settings, "liqueur");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, EffectContext context) {
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xB565D9;
    }

    @Override
    public int getShopPrice() {
        return 125;
    }
}
