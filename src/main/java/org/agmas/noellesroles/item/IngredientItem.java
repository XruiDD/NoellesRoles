package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调制品物品抽象基类。
 * 每种调剂继承此类并实现自己的效果逻辑。
 * 通过静态注册表支持 string ID 查找（用于 NBT 反序列化）。
 */
public abstract class IngredientItem extends Item {
    public record EffectContext(int durationMultiplier, boolean suppressMoodBonus) {
        public static final EffectContext DEFAULT = new EffectContext(1, false);

        public int scaleDuration(int ticks) {
            return ticks * Math.max(1, this.durationMultiplier);
        }
    }

    private final String ingredientId;

    private static final Map<String, IngredientItem> REGISTRY = new HashMap<>();

    protected IngredientItem(Settings settings, String ingredientId) {
        super(settings);
        this.ingredientId = ingredientId;
    }

    // --- 子类必须实现 ---

    /** 应用调剂效果到玩家（药水效果 + 心情恢复等） */
    public abstract void applyEffect(ServerPlayerEntity player, EffectContext context);

    /** 调剂在 tooltip 中的显示颜色（RGB） */
    public abstract int getDisplayColorRgb();

    /** 商店价格 */
    public abstract int getShopPrice();

    // --- 可选 override ---

    /** 是否消除基酒 debuff（默认 false，冰块 override 为 true） */
    public boolean removesDebuff() {
        return false;
    }

    // --- 通用方法 ---

    public final String getIngredientId() {
        return ingredientId;
    }

    public final void applyEffect(ServerPlayerEntity player) {
        this.applyEffect(player, EffectContext.DEFAULT);
    }

    /** 通用心情恢复 helper，子类调用 */
    protected void addMoodBonus(ServerPlayerEntity player, float amount) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        mood.setMood(Math.min(1.0f, mood.getMood() + amount));
    }

    protected void addMoodBonus(ServerPlayerEntity player, float amount, EffectContext context) {
        if (!context.suppressMoodBonus()) {
            addMoodBonus(player, amount);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.ingredient.tooltip").formatted(Formatting.GRAY));
        String effectKey = "item.noellesroles." + ingredientId + ".effect";
        tooltip.add(Text.translatable(effectKey).formatted(Formatting.DARK_PURPLE));
    }

    // --- 静态注册表 ---

    public static void register(IngredientItem item) {
        REGISTRY.put(item.getIngredientId(), item);
    }

    public static @Nullable IngredientItem fromId(String id) {
        return REGISTRY.get(id);
    }
}
