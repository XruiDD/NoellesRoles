package org.agmas.noellesroles.item.ingredient;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.effect.StimulationEffect;
import org.agmas.noellesroles.item.IngredientItem;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownEntryAccessor;
import org.agmas.noellesroles.mixin.accessor.ItemCooldownManagerAccessor;

import java.util.ArrayList;
import java.util.Map;

public class VodkaItem extends IngredientItem {
    public VodkaItem(Settings settings) {
        super(settings, "vodka");
    }

    @Override
    public void applyEffect(ServerPlayerEntity player) {
        // 15 秒亢奋效果
        player.addStatusEffect(new StatusEffectInstance(
                ModEffects.STIMULATION, 15 * 20, 0, false, false, true));
        // 无限体力（属性修饰符）
        StimulationEffect.applyStaminaModifier(player);
        // 生效瞬间减少所有冷却 20%
        reduceAllCooldowns(player);
        // 心情恢复
        addMoodBonus(player, 0.2f);
    }

    /**
     * 生效瞬间减少所有物品冷却 20%。
     * 直接访问 ItemCooldownManager 的 public 字段，无需 Accessor mixin。
     */
    private static void reduceAllCooldowns(ServerPlayerEntity player) {
        ItemCooldownManager cdm = player.getItemCooldownManager();
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) cdm;
        Map<Item, ?> entries = accessor.getEntries();
        int tick = accessor.getTick();
        for (var entry : new ArrayList<>(entries.entrySet())) {
            ItemCooldownEntryAccessor entryAccessor = (ItemCooldownEntryAccessor) entry.getValue();
            int remaining = entryAccessor.getEndTick() - tick;
            if (remaining <= 0) continue;
            int newRemaining = Math.max(1, (int) (remaining * 0.8f));
            cdm.set(entry.getKey(), newRemaining);
        }
    }

    @Override
    public int getDisplayColorRgb() {
        return 0x00ACC1;
    }

    @Override
    public int getShopPrice() {
        return 175;
    }
}
