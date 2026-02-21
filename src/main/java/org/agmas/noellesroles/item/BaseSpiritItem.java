package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.professor.IronManPlayerComponent;

import org.agmas.noellesroles.mixin.bartender.ItemCooldownEntryAccessor;
import org.agmas.noellesroles.mixin.bartender.ItemCooldownManagerAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseSpiritItem extends Item {
    public static final int MAX_INGREDIENTS = 3;
    // debuff 持续时间 3秒 = 60 ticks
    private static final int DEBUFF_DURATION = 3 * 20;

    public BaseSpiritItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32; // 与原版饮品一致
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            List<String> ingredients = getIngredients(stack);

            // 基酒 debuff: 3s 缓慢II + 失明
            boolean hasIce = ingredients.contains("ice_cube");

            if (!hasIce) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, DEBUFF_DURATION, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, DEBUFF_DURATION, 0, false, false, true));
            }

            // 基酒基础恢复 20% san
            PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
            float currentMood = moodComponent.getMood();
            moodComponent.setMood(Math.min(1.0f, currentMood + 0.2f));

            if (hasIce) {
                // 冰块消除 debuff，调制品效果立即生效
                applyIngredientEffectsStatic(player, ingredients);
            } else if (!ingredients.isEmpty()) {
                // 有调制品但无冰块：通过 BartenderPlayerComponent 延迟生效
                BartenderPlayerComponent comp = BartenderPlayerComponent.KEY.get(player);
                comp.schedulePendingEffects(ingredients, DEBUFF_DURATION);
            }

            stack.decrement(1);
        }
        return stack;
    }

    /**
     * 立即应用调制品效果（公开静态方法，供 BartenderPlayerComponent 延迟调用）
     */
    public static void applyIngredientEffectsStatic(ServerPlayerEntity player, List<String> ingredients) {
        for (String ingredient : ingredients) {
            switch (ingredient) {
                case "rum" -> {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6 * 20, 0, false, false, true));
                    PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
                    moodComponent.setMood(Math.min(1.0f, moodComponent.getMood() + 0.2f));
                }
                case "gin" -> {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6 * 20, 0, false, false, true));
                    PlayerMoodComponent moodComponent2 = PlayerMoodComponent.KEY.get(player);
                    moodComponent2.setMood(Math.min(1.0f, moodComponent2.getMood() + 0.2f));
                }
                case "vodka" -> {
                    player.addStatusEffect(new StatusEffectInstance(ModEffects.EUPHORIA, 15 * 20, 0, false, false, true));
                    // 立即减少所有物品 20% 冷却
                    reduceAllCooldowns(player);
                    PlayerMoodComponent moodComponent3 = PlayerMoodComponent.KEY.get(player);
                    moodComponent3.setMood(Math.min(1.0f, moodComponent3.getMood() + 0.2f));
                }
                case "tequila" -> {
                    player.addStatusEffect(new StatusEffectInstance(ModEffects.NO_COLLISION, 12 * 20, 0, false, false, true));
                    PlayerMoodComponent moodComponent4 = PlayerMoodComponent.KEY.get(player);
                    moodComponent4.setMood(Math.min(1.0f, moodComponent4.getMood() + 0.2f));
                }
                case "whiskey" -> {
                    // 限时护盾：使用铁人药剂机制，12s 后自动移除
                    applyWhiskeyShield(player, 12 * 20);
                    PlayerMoodComponent moodComponent5 = PlayerMoodComponent.KEY.get(player);
                    moodComponent5.setMood(Math.min(1.0f, moodComponent5.getMood() + 0.2f));
                }
                case "ice_cube" -> {
                    // 冰块本身不提供额外效果
                }
            }
        }
    }

    /**
     * 给予威士忌护盾（使用铁人药剂机制，限时）
     */
    private static void applyWhiskeyShield(ServerPlayerEntity player, int durationTicks) {
        IronManPlayerComponent ironMan = IronManPlayerComponent.KEY.get(player);
        if (!ironMan.hasBuff()) {
            ironMan.applyBuff();
        }
        // 通过 BartenderPlayerComponent 计时，到期自动移除
        BartenderPlayerComponent comp = BartenderPlayerComponent.KEY.get(player);
        comp.startWhiskeyShield(durationTicks);
    }

    /**
     * 伏特加效果：立即减少玩家背包内所有物品 20% 的冷却时间（按最大冷却时间计算）
     */
    @SuppressWarnings("unchecked")
    private static void reduceAllCooldowns(ServerPlayerEntity player) {
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
        int currentTick = accessor.getTick();
        Map<Item, ?> entries = accessor.getEntries();

        for (Map.Entry<Item, ?> entry : new ArrayList<>(entries.entrySet())) {
            ItemCooldownEntryAccessor entryAccessor = (ItemCooldownEntryAccessor) entry.getValue();
            int startTick = entryAccessor.getStartTick();
            int endTick = entryAccessor.getEndTick();

            if (endTick <= currentTick) continue; // 已过期

            int maxDuration = endTick - startTick;
            int reduction = (int) (maxDuration * 0.2);
            int newEndTick = endTick - reduction;

            if (newEndTick <= currentTick) {
                // 冷却已完全消除
                player.getItemCooldownManager().remove(entry.getKey());
            } else {
                // 设置新的冷却时间
                int newRemaining = newEndTick - currentTick;
                player.getItemCooldownManager().set(entry.getKey(), newRemaining);
            }
        }
    }

    /**
     * 获取基酒中的调制品列表
     */
    public static List<String> getIngredients(ItemStack stack) {
        List<String> ingredients = new ArrayList<>();
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("ingredients")) {
                NbtList list = nbt.getList("ingredients", NbtString.STRING_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    ingredients.add(list.getString(i));
                }
            }
        }
        return ingredients;
    }

    /**
     * 向基酒中添加调制品
     * @return true 如果添加成功
     */
    public static boolean addIngredient(ItemStack baseSpiritStack, String ingredientId) {
        List<String> current = getIngredients(baseSpiritStack);
        if (current.size() >= MAX_INGREDIENTS) {
            return false;
        }
        // 每种调制品只能添加一次
        if (current.contains(ingredientId)) {
            return false;
        }
        current.add(ingredientId);

        NbtCompound nbt;
        NbtComponent customData = baseSpiritStack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            nbt = customData.copyNbt();
        } else {
            nbt = new NbtCompound();
        }

        NbtList list = new NbtList();
        for (String id : current) {
            list.add(NbtString.of(id));
        }
        nbt.put("ingredients", list);
        baseSpiritStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return true;
    }

    /**
     * 获取调制品对应的显示颜色
     */
    private static Formatting getIngredientColor(String ingredientId) {
        return switch (ingredientId) {
            case "rum" -> Formatting.GOLD;
            case "gin" -> Formatting.GREEN;
            case "vodka" -> Formatting.AQUA;
            case "tequila" -> Formatting.YELLOW;
            case "whiskey" -> Formatting.RED;
            case "ice_cube" -> Formatting.WHITE;
            default -> Formatting.GRAY;
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        List<String> ingredients = getIngredients(stack);
        if (!ingredients.isEmpty()) {
            tooltip.add(Text.translatable("item.noellesroles.base_spirit.ingredients").formatted(Formatting.GRAY));
            for (String ingredient : ingredients) {
                String translationKey = "item.noellesroles." + ingredient;
                tooltip.add(Text.literal("  ").append(Text.translatable(translationKey).formatted(getIngredientColor(ingredient))));
            }
        }
        int remaining = MAX_INGREDIENTS - ingredients.size();
        if (remaining > 0) {
            tooltip.add(Text.translatable("item.noellesroles.base_spirit.slots_remaining", remaining).formatted(Formatting.DARK_GRAY));
        }
    }
}
