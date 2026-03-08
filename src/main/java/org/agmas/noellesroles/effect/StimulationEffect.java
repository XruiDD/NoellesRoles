package org.agmas.noellesroles.effect;

import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.index.WatheAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;

/**
 * 亢奋效果（伏特加）。
 * - 亢奋期间：无限体力（属性修饰符）+ 新设置的物品冷却缩短为 80%（由 StimulationCooldownMixin 实现）
 * - 亢奋结束：体力归零 + 疲劳 + 3 秒缓慢 II
 */
public class StimulationEffect extends StatusEffect {

    public static final Identifier VODKA_STAMINA_MODIFIER_ID =
            Identifier.of(Noellesroles.MOD_ID, "vodka_stimulation_sprint");

    public StimulationEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFFFF00);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayerEntity player)) return true;

        // 最后一 tick：清理修饰符 + 施加惩罚
        StatusEffectInstance instance = player.getStatusEffect(ModEffects.STIMULATION);
        if (instance != null && instance.getDuration() <= 1) {
            removeStimulation(player);
            return true;
        }

        return true;
    }

    /**
     * 给玩家添加体力修饰符（无限体力）。
     * 由 VodkaItem 在生效时调用。
     */
    public static void applyStaminaModifier(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (attr != null && !attr.hasModifier(VODKA_STAMINA_MODIFIER_ID)) {
            attr.addTemporaryModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                    VODKA_STAMINA_MODIFIER_ID, 499.0,
                    net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            // 填满体力 + 清除疲劳 + 同步到客户端
            PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
            stamina.setSprintingTicks((int) attr.getValue());
            stamina.setMaxSprintTime((int) attr.getValue());
            stamina.setExhausted(false);
            stamina.sync();
        }
    }

    /**
     * 移除体力修饰符 + 施加惩罚。
     */
    private static void removeStimulation(ServerPlayerEntity player) {
        // 移除体力修饰符
        EntityAttributeInstance attr = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (attr != null) {
            attr.removeModifier(VODKA_STAMINA_MODIFIER_ID);
        }
        // 体力归零 + 疲劳
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setSprintingTicks(0);
        stamina.setExhausted(true);
        stamina.sync();
        // 3 秒缓慢 II
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, 3 * 20, 1, false, false, true));
    }
}
