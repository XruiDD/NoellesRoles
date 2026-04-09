package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;

public class IceCubeItem extends IngredientItem {
    public IceCubeItem(Settings settings) {
        super(settings, "ice_cube");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, EffectContext context) {
    }

    @Override
    public boolean removesDebuff() {
        return true;
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xE0E0E0;
    }

    @Override
    public int getShopPrice() {
        return 25;
    }
}
