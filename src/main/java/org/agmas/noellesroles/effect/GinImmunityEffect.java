package org.agmas.noellesroles.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 杜松子酒免疫关灯效果 - 拥有此效果时免疫熄灯系统的失明
 */
public class GinImmunityEffect extends StatusEffect {
    public GinImmunityEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x00FF00); // 绿色
    }
}
