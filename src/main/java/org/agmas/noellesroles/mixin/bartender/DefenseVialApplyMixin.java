package org.agmas.noellesroles.mixin.bartender;

import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 允许上等佳酿和基酒放置到餐盘 / 从餐盘优先拿取。
 * 拿取优先级：基酒 > 上等佳酿 > 其他（交给原版逻辑）
 */
@Mixin(FoodPlatterBlock.class)
public abstract class DefenseVialApplyMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void fineDrinkPlatter(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient) return;
        if (player.isCreative()) return;

        BlockEntity platter = world.getBlockEntity(pos);
        if (platter instanceof BeveragePlateBlockEntity blockEntity) {
            ItemStack handStack = player.getStackInHand(Hand.MAIN_HAND);

            // === 放置：上等佳酿或基酒 ===
            if (handStack.isOf(ModItems.FINE_DRINK) || handStack.isOf(ModItems.BASE_SPIRIT)) {
                blockEntity.addItem(handStack.copy());
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "place");
                    GameRecordManager.putBlockPos(extra, "pos", pos);
                    // 基酒额外记录调剂信息
                    if (handStack.isOf(ModItems.BASE_SPIRIT)) {
                        noellesroles$putIngredients(extra, handStack);
                    }
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(handStack.getItem()), null, extra);
                }
                handStack.decrement(1);
                player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // === 拿取：空手时优先拿取基酒 > 上等佳酿 ===
            if (handStack.isEmpty()) {
                List<ItemStack> platterItems = blockEntity.getStoredItems();
                if (platterItems.isEmpty()) return;

                // 优先拿取基酒
                if (noellesroles$tryTakeItem(platterItems, ModItems.BASE_SPIRIT, player, blockEntity, pos, state, world, cir)) return;
                // 其次拿取上等佳酿
                if (noellesroles$tryTakeItem(platterItems, ModItems.FINE_DRINK, player, blockEntity, pos, state, world, cir)) return;
            }
        }
    }

    /**
     * 尝试从餐盘中拿取指定物品，成功则设置 ActionResult 并返回 true
     */
    @Unique
    private static boolean noellesroles$tryTakeItem(
            List<ItemStack> platterItems, net.minecraft.item.Item targetItem,
            PlayerEntity player, BeveragePlateBlockEntity blockEntity,
            BlockPos pos, BlockState state, World world,
            CallbackInfoReturnable<ActionResult> cir) {
        for (int i = 0; i < platterItems.size(); i++) {
            if (platterItems.get(i).isOf(targetItem)) {
                ItemStack taken = platterItems.remove(i).copy();
                taken.setCount(1);
                taken.set(DataComponentTypes.MAX_STACK_SIZE, 1);

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = null;
                    if (targetItem == ModItems.BASE_SPIRIT) {
                        extra = new NbtCompound();
                        noellesroles$putIngredients(extra, taken);
                    }
                    GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(targetItem), pos, null, extra);
                }

                player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                player.setStackInHand(Hand.MAIN_HAND, taken);
                blockEntity.markDirty();
                world.updateListeners(pos, state, state, 3);
                cir.setReturnValue(ActionResult.SUCCESS);
                return true;
            }
        }
        return false;
    }

    /**
     * 将基酒的调剂信息写入 NbtCompound
     */
    @Unique
    private static void noellesroles$putIngredients(NbtCompound extra, ItemStack baseSpiritStack) {
        List<String> ingredients = BaseSpiritItem.getIngredients(baseSpiritStack);
        if (!ingredients.isEmpty()) {
            NbtList ingredientNbt = new NbtList();
            for (String id : ingredients) ingredientNbt.add(NbtString.of(id));
            extra.put("ingredients", ingredientNbt);
        }
    }
}
