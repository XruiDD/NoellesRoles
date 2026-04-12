package org.agmas.noellesroles.item.ingredient;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.item.IngredientItem;

public class TequilaItem extends IngredientItem {
    public TequilaItem(Settings settings) {
        super(settings, "tequila");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player, float durationMultiplier) {
        int duration = (int)(7 * 20 * durationMultiplier);
        player.addStatusEffect(new StatusEffectInstance(ModEffects.NO_COLLISION, duration, 0, false, false, true));
        addMoodBonus(player, 0.2f);
    }

    @Override
    public int getDisplayColorRgb() {
        return 0x4CAF50; // 绿
    }

    @Override
    public int getShopPrice() {
        return 50;
    }
}
