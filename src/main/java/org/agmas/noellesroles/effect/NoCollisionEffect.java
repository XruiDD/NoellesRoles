package org.agmas.noellesroles.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 无碰撞体积效果 - 拥有此效果的玩家可以穿过其他玩家
 */
public class NoCollisionEffect extends StatusEffect {
    public NoCollisionEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFFFFFF); // 白色
    }
}
