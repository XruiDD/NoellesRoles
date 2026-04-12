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
    private final String ingredientId;

    private static final Map<String, IngredientItem> REGISTRY = new HashMap<>();

    protected IngredientItem(Settings settings, String ingredientId) {
        super(settings);
        this.ingredientId = ingredientId;
    }

    // --- 子类必须实现 ---

    /**
     * 应用调剂效果（带持续时间倍率）。
     * 倍率由同杯中所有调剂的 getDurationMultiplier() 相乘决定。
     */
    public abstract void applyEffect(ServerPlayerEntity player, float durationMultiplier);

    /** 无倍率版本，委托到 applyEffect(player, 1.0f) */
    public void applyEffect(ServerPlayerEntity player) {
        applyEffect(player, 1.0f);
    }

    /** 调剂在 tooltip 中的显示颜色（RGB） */
    public abstract int getDisplayColorRgb();

    /** 商店价格 */
    public abstract int getShopPrice();

    // --- 可选 override ---

    /** 是否消除基酒 debuff（默认 false，冰块 override 为 true） */
    public boolean removesDebuff() {
        return false;
    }

    /** 是否为修饰剂（不参与鸡尾酒命名，如冰块、特调利口酒、特调香料） */
    public boolean isModifier() {
        return false;
    }

    /** 持续时间倍率贡献（默认 1.0，特调利口酒 override 为 2.0） */
    public float getDurationMultiplier() {
        return 1.0f;
    }

    /** 鸡尾酒名称后缀翻译 key（默认 null 无后缀，修饰剂按需 override） */
    public @Nullable String getSuffixTranslationKey() {
        return null;
    }

    // --- 通用方法 ---

    public final String getIngredientId() {
        return ingredientId;
    }

    /** 通用心情恢复 helper，子类调用 */
    protected void addMoodBonus(ServerPlayerEntity player, float amount) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        mood.setMood(Math.min(1.0f, mood.getMood() + amount));
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
