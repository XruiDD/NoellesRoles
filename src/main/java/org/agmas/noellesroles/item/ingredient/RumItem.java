package org.agmas.noellesroles.item.ingredient;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.item.IngredientItem;

public class RumItem extends IngredientItem {
    public RumItem(Settings settings) {
        super(settings, "rum");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, EffectContext context) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, context.scaleDuration(7 * 20), 1, false, false, true));
        addMoodBonus(player, 0.2f, context);
    }

    @Override
    public int getDisplayColorRgb() {
        return 0xB0BEC5; // 白（偏蓝灰）
    }

    @Override
    public int getShopPrice() {
        return 100;
    }
}
