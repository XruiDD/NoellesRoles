package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
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
                applyIngredientEffects(player, ingredients);
            } else if (!ingredients.isEmpty()) {
                // 有调制品但无冰块：debuff 结束后生效（3秒后）
                scheduleIngredientEffects(player, ingredients, DEBUFF_DURATION);
            }

            stack.decrement(1);
        }
        return stack;
    }

    /**
     * 延迟应用调制品效果（debuff结束后）
     */
    private void scheduleIngredientEffects(ServerPlayerEntity player, List<String> ingredients, int delayTicks) {
        // 使用一个不可见的状态效果作为计时器，在效果结束时触发
        // 更简单的方式：给予调制品效果但延迟开始
        // Minecraft 不支持延迟效果，所以我们用更长的持续时间并在 debuff 时间后开始
        for (String ingredient : ingredients) {
            switch (ingredient) {
                case "rum" -> {
                    // 6s 速度I，延迟 debuff 时间后生效
                    // 总时长 = debuff + 效果时长，隐藏前 debuff 时间
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, delayTicks + 6 * 20, 0, false, false, true));
                    // 额外恢复 20% san
                    PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
                    moodComponent.setMood(Math.min(1.0f, moodComponent.getMood() + 0.2f));
                }
                case "gin" -> {
                    // 夜视效果：6s，每次2s间隔1.5s（需要特殊处理，这里先给6s夜视）
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, delayTicks + 6 * 20, 0, false, false, true));
                    // 额外恢复 20% san
                    PlayerMoodComponent moodComponent2 = PlayerMoodComponent.KEY.get(player);
                    moodComponent2.setMood(Math.min(1.0f, moodComponent2.getMood() + 0.2f));
                }
                case "vodka" -> {
                    // 15s 亢奋效果
                    player.addStatusEffect(new StatusEffectInstance(ModEffects.EUPHORIA, delayTicks + 15 * 20, 0, false, false, true));
                    // 立即减少所有物品 20% 冷却
                    reduceAllCooldowns(player);
                    // 额外恢复 20% san（亢奋效果结束时在EuphoriaEffect中额外恢复）
                    PlayerMoodComponent moodComponent3 = PlayerMoodComponent.KEY.get(player);
                    moodComponent3.setMood(Math.min(1.0f, moodComponent3.getMood() + 0.2f));
                }
                case "tequila" -> {
                    // 12s 无碰撞体积效果
                    player.addStatusEffect(new StatusEffectInstance(ModEffects.NO_COLLISION, delayTicks + 12 * 20, 0, false, false, true));
                    // 额外恢复 20% san
                    PlayerMoodComponent moodComponent4 = PlayerMoodComponent.KEY.get(player);
                    moodComponent4.setMood(Math.min(1.0f, moodComponent4.getMood() + 0.2f));
                }
                case "whiskey" -> {
                    // 12s 限时护盾（1层，参考 Psycho Mode 护盾）
                    applyWhiskeyShield(player, delayTicks + 12 * 20);
                    // 额外恢复 20% san
                    PlayerMoodComponent moodComponent5 = PlayerMoodComponent.KEY.get(player);
                    moodComponent5.setMood(Math.min(1.0f, moodComponent5.getMood() + 0.2f));
                }
                case "ice_cube" -> {
                    // 冰块在这个分支不会出现（已在上面处理）
                }
            }
        }
    }

    /**
     * 立即应用调制品效果（冰块消除debuff时）
     */
    private void applyIngredientEffects(ServerPlayerEntity player, List<String> ingredients) {
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
                    // 12s 限时护盾（1层，参考 Psycho Mode 护盾）
                    applyWhiskeyShield(player, 12 * 20);
                    // 额外恢复 20% san
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
     * 给予威士忌护盾（利用 Psycho Mode 的护盾机制）
     */
    private void applyWhiskeyShield(ServerPlayerEntity player, int durationTicks) {
        PlayerPsychoComponent psychoComp = PlayerPsychoComponent.KEY.get(player);
        // 如果已经在疯魔模式中，只增加护甲层数
        if (psychoComp.getPsychoTicks() > 0) {
            psychoComp.setArmour(psychoComp.getArmour() + 1);
        } else {
            // 设置护盾时间和层数，手动增加 psychosActive 计数
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            gameWorld.setPsychosActive(gameWorld.getPsychosActive() + 1);
            psychoComp.setPsychoTicks(durationTicks);
            psychoComp.setArmour(1);
        }
    }

    /**
     * 伏特加效果：立即减少玩家背包内所有物品 20% 的冷却时间（按最大冷却时间计算）
     */
    @SuppressWarnings("unchecked")
    private void reduceAllCooldowns(ServerPlayerEntity player) {
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

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        List<String> ingredients = getIngredients(stack);
        if (!ingredients.isEmpty()) {
            tooltip.add(Text.translatable("item.noellesroles.base_spirit.ingredients").formatted(Formatting.GRAY));
            for (String ingredient : ingredients) {
                String translationKey = "item.noellesroles." + ingredient;
                tooltip.add(Text.literal("  ").append(Text.translatable(translationKey).formatted(Formatting.AQUA)));
            }
        }
        int remaining = MAX_INGREDIENTS - ingredients.size();
        if (remaining > 0) {
            tooltip.add(Text.translatable("item.noellesroles.base_spirit.slots_remaining", remaining).formatted(Formatting.DARK_GRAY));
        }
    }
}
