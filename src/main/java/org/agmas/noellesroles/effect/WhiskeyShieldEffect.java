package org.agmas.noellesroles.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;

/**
 * 威士忌护盾效果 - 每个等级代表一层护盾。
 * amplifier 0 = 1 层，amplifier 1 = 2 层，以此类推。
 * 受到致命伤害或被吞噬时消耗一层。
 */
public class WhiskeyShieldEffect extends StatusEffect {
    public WhiskeyShieldEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xD2691E); // 棕色
    }

    /**
     * 消耗一层护盾。
     * @return true 如果成功消耗（玩家有护盾效果）
     */
    public static boolean consumeShield(ServerPlayerEntity player) {
        StatusEffectInstance shield = player.getStatusEffect(ModEffects.WHISKEY_SHIELD);
        if (shield == null) return false;

        int amplifier = shield.getAmplifier();
        int duration = shield.getDuration();
        player.removeStatusEffect(ModEffects.WHISKEY_SHIELD);

        if (amplifier > 0) {
            // 还有剩余层数，重新应用（等级 -1）
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.WHISKEY_SHIELD, duration, amplifier - 1, false, false, true));
        }
        return true;
    }

    /**
     * 叠加一层护盾。如果已有效果则等级 +1，保留剩余持续时间不重置。
     */
    public static void addShieldLayer(ServerPlayerEntity player, int durationTicks) {
        StatusEffectInstance existing = player.getStatusEffect(ModEffects.WHISKEY_SHIELD);
        if (existing != null) {
            // 已有护盾：等级 +1，保留原剩余时间
            int newAmplifier = existing.getAmplifier() + 1;
            int remainingDuration = existing.getDuration();
            player.removeStatusEffect(ModEffects.WHISKEY_SHIELD);
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.WHISKEY_SHIELD, remainingDuration, newAmplifier, false, false, true));
        } else {
            // 首次获得护盾
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.WHISKEY_SHIELD, durationTicks, 0, false, false, true));
        }
    }
}
