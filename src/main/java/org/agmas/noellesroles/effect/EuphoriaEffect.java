package org.agmas.noellesroles.effect;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;

public class EuphoriaEffect extends StatusEffect {
    public EuphoriaEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFF4500); // 橙红色
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true; // 每tick都执行
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayerEntity player)) return true;

        // 锁定体力为无限（绑定亢奋效果本身，无论来源）
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);

        // 物品冷却恢复速度 +20%（每5tick额外调用一次update，等效+20%恢复速度）
        if (player.age % 5 == 0) {
            player.getItemCooldownManager().update();
        }

        return true;
    }
}
