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
    public void applyEffect(ServerPlayerEntity player, float durationMultiplier) {
        int duration = (int)(7 * 20 * durationMultiplier);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1, false, false, true));
        addMoodBonus(player, 0.2f);
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
