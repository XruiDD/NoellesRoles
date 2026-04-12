package org.agmas.noellesroles.item.ingredient;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.item.IngredientItem;

public class GinItem extends IngredientItem {
    public GinItem(Settings settings) {
        super(settings, "gin");
    }

    private static final int BASE_DURATION = 10 * 20;

    @Override
    public void applyEffect(ServerPlayerEntity player, float durationMultiplier) {
        int duration = (int)(BASE_DURATION * durationMultiplier);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(ModEffects.GIN_IMMUNITY, duration, 0, false, false, true));
        addMoodBonus(player, 0.2f);
    }

    @Override
    public int getDisplayColorRgb() {
        return 0x1A237E; // 蓝（深蓝）
    }

    @Override
    public int getShopPrice() {
        return 100;
    }
}
