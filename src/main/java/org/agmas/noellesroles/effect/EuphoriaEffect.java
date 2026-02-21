package org.agmas.noellesroles.effect;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;

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

        // 锁定体力为无限（设为 MAX_VALUE 触发 Wathe 的 isInfiniteStamina()，跳过耐力消耗）
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);

        // 物品冷却恢复速度 +20%（每5tick额外调用一次update，等效+20%恢复速度）
        if (player.age % 5 == 0) {
            player.getItemCooldownManager().update();
        }

        // 检查效果是否即将结束（剩余1tick）
        StatusEffectInstance instance = player.getStatusEffect(ModEffects.EUPHORIA);
        if (instance != null && instance.getDuration() <= 1) {
            onEuphoriaEnd(player);
        }

        return true;
    }

    private void onEuphoriaEnd(ServerPlayerEntity player) {
        // 清空体力，设为0
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setSprintingTicks(0);
        stamina.setExhausted(true);

        // 额外恢复 20% san
        PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
        moodComponent.setMood(Math.min(1.0f, moodComponent.getMood() + 0.2f));
    }
}
