package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;

public class IceCubeItem extends IngredientItem {
    public IceCubeItem(Settings settings) {
        super(settings, "ice_cube");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player) {
        // 冰块本身不提供效果
    }

    @Override
    public boolean removesDebuff() {
        return true;
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xE0E0E0; // 白
    }

    @Override
    public int getShopPrice() {
        return 25;
    }
}
