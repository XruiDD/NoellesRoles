package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.Scheduler;
import org.agmas.noellesroles.bartender.CocktailRegistry;

import java.util.ArrayList;
import java.util.List;

public class BaseSpiritItem extends CocktailItem {
    public static final int MAX_INGREDIENTS = 3;
    private static final int DEBUFF_DURATION = 3 * 20;

    public BaseSpiritItem(Settings settings) {
        super(settings);
    }

    // ===== 调制逻辑（替代 IngredientMixingMixin）=====

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot,
            ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT) return false;
        if (!(otherStack.getItem() instanceof IngredientItem ingredientItem)) return false;

        // 记录调制前的 ingredients
        List<String> prevIngredients = getIngredients(stack);

        if (addIngredient(stack, ingredientItem.getIngredientId())) {
            if (!player.isCreative()) {
                otherStack.decrement(1);
                cursorStackReference.set(otherStack);
            }
            // 服务端记录调酒事件（ingredients NbtList 格式，与 drink/place/take 一致）
            if (!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                List<String> afterIngredients = getIngredients(stack);
                // 构建 prev_ingredients compound（包含 NbtList）
                NbtCompound prevData = new NbtCompound();
                if (!prevIngredients.isEmpty()) {
                    NbtList prevNbt = new NbtList();
                    for (String id : prevIngredients) prevNbt.add(NbtString.of(id));
                    prevData.put("ingredients", prevNbt);
                }
                // 构建 after ingredients compound
                NbtCompound afterData = new NbtCompound();
                NbtList afterNbt = new NbtList();
                for (String id : afterIngredients) afterNbt.add(NbtString.of(id));
                afterData.put("ingredients", afterNbt);

                GameRecordManager.event("ingredient_mixed")
                        .actor(serverPlayer)
                        .put("ingredient", ingredientItem.getIngredientId())
                        .putNbt("prev", prevData)
                        .putNbt("after", afterData)
                        .record();
            }
            return true;
        }
        return false;
    }

    // ===== 名称系统 =====

    @Override
    public Text getName(ItemStack stack) {
        List<String> ingredients = getIngredients(stack);
        MutableText cocktailName = CocktailRegistry.getCocktailName(ingredients);
        if (cocktailName != null) return cocktailName;
        return super.getName(stack);
    }

    // ===== 饮用逻辑 =====

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            PlayerMoodComponent.KEY.get(player).drinkCocktail();

            List<String> ingredients = getIngredients(stack);
            boolean hasIce = hasAnyRemovesDebuff(ingredients);

            // 记录饮用事件
            NbtCompound extra = null;
            if (!ingredients.isEmpty()) {
                extra = new NbtCompound();
                NbtList ingredientNbt = new NbtList();
                for (String id : ingredients) ingredientNbt.add(NbtString.of(id));
                extra.put("ingredients", ingredientNbt);
            }
            GameRecordManager.recordItemUse(player, Registries.ITEM.getId(this), null, extra);

            if (hasIce) {
                addBaseMoodBonus(player);
                applyIngredientEffectsStatic(player, ingredients);
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, DEBUFF_DURATION, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, DEBUFF_DURATION, 0, false, false, true));
                List<String> capturedIngredients = List.copyOf(ingredients);
                Scheduler.schedule(() -> applyDelayedEffects(player, capturedIngredients), DEBUFF_DURATION);
            }

            if (!player.isCreative()) {
                stack.decrement(1);
            }
        }
        return stack;
    }

    // ===== 效果方法 =====

    private static void addBaseMoodBonus(ServerPlayerEntity player) {
        PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
        moodComponent.setMood(Math.min(1.0f, moodComponent.getMood() + 0.2f));
    }

    public static void applyIngredientEffectsStatic(ServerPlayerEntity player, List<String> ingredients) {
        for (String id : ingredients) {
            IngredientItem item = IngredientItem.fromId(id);
            if (item != null) item.applyEffect(player);
        }
    }

    public static void applyDelayedEffects(ServerPlayerEntity player, List<String> ingredients) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;
        addBaseMoodBonus(player);
        applyIngredientEffectsStatic(player, ingredients);
    }

    private static boolean hasAnyRemovesDebuff(List<String> ingredients) {
        for (String id : ingredients) {
            IngredientItem item = IngredientItem.fromId(id);
            if (item != null && item.removesDebuff()) return true;
        }
        return false;
    }

    // ===== NBT 操作 =====

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

    public static boolean addIngredient(ItemStack baseSpiritStack, String ingredientId) {
        List<String> current = getIngredients(baseSpiritStack);
        if (current.size() >= MAX_INGREDIENTS) return false;
        if (current.contains(ingredientId)) return false;
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

    // ===== Tooltip =====

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        List<String> ingredients = getIngredients(stack);
        if (!ingredients.isEmpty()) {
            tooltip.add(Text.translatable("item.noellesroles.base_spirit.ingredients").formatted(Formatting.GRAY));
            for (String ingredient : ingredients) {
                String translationKey = "item.noellesroles." + ingredient;
                IngredientItem item = IngredientItem.fromId(ingredient);
                int color = item != null ? item.getDisplayColorRgb() : 0x999999;
                tooltip.add(Text.literal("  ").append(
                        Text.translatable(translationKey).setStyle(Style.EMPTY.withColor(color))));
            }
        }
        int remaining = MAX_INGREDIENTS - ingredients.size();
        if (remaining > 0) {
            tooltip.add(Text.translatable("item.noellesroles.base_spirit.slots_remaining", remaining).formatted(Formatting.DARK_GRAY));
        }
    }
}
