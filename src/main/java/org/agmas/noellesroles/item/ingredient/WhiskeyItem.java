package org.agmas.noellesroles.item.ingredient;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.effect.WhiskeyShieldEffect;
import org.agmas.noellesroles.item.IngredientItem;

public class WhiskeyItem extends IngredientItem {
    public WhiskeyItem(Settings settings) {
        super(settings, "whiskey");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, EffectContext context) {
        WhiskeyShieldEffect.addShieldLayer(player, context.scaleDuration(20 * 20));
        addMoodBonus(player, 0.2f, context);
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xBF8040; // 橙（橙灰）
    }

    @Override
    public int getShopPrice() {
        return 175;
    }
}
