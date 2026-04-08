package org.agmas.noellesroles.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class FractureEffect extends StatusEffect {
    public FractureEffect() {
        super(StatusEffectCategory.HARMFUL, 0x8F6B52);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return false;
    }

}
