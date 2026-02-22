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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// Allow Fine Drink and Base Spirit to be placed on food platters/drink trays
// Retrieval priority: Base Spirit > Fine Drink > others
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
                handStack.decrement(1);
                player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "place");
                    GameRecordManager.putBlockPos(extra, "pos", pos);
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(handStack.getItem()), null, extra);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // === 拿取：优先级 基酒 > 上等佳酿 > 其他（交给原版逻辑） ===
            if (handStack.isEmpty()) {
                List<ItemStack> platterItems = blockEntity.getStoredItems();
                if (platterItems.isEmpty()) return;

                // 1) 最高优先：基酒（多个则随机选一杯）
                List<Integer> baseSpiritIndices = new ArrayList<>();
                for (int i = 0; i < platterItems.size(); i++) {
                    if (platterItems.get(i).isOf(ModItems.BASE_SPIRIT)) {
                        baseSpiritIndices.add(i);
                    }
                }
                if (!baseSpiritIndices.isEmpty()) {
                    int chosen = baseSpiritIndices.get(world.random.nextInt(baseSpiritIndices.size()));
                    ItemStack taken = platterItems.remove(chosen).copy();
                    taken.setCount(1);
                    taken.set(DataComponentTypes.MAX_STACK_SIZE, 1);
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(ModItems.BASE_SPIRIT), pos, null);
                    }
                    player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                    player.setStackInHand(Hand.MAIN_HAND, taken);
                    blockEntity.markDirty();
                    world.updateListeners(pos, state, state, 3);
                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }

                // 2) 次优先：上等佳酿
                for (int i = 0; i < platterItems.size(); i++) {
                    if (platterItems.get(i).isOf(ModItems.FINE_DRINK)) {
                        ItemStack fineDrink = platterItems.remove(i).copy();
                        fineDrink.setCount(1);
                        fineDrink.set(DataComponentTypes.MAX_STACK_SIZE, 1);
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(ModItems.FINE_DRINK), pos, null);
                        }
                        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                        player.setStackInHand(Hand.MAIN_HAND, fineDrink);
                        blockEntity.markDirty();
                        world.updateListeners(pos, state, state, 3);
                        cir.setReturnValue(ActionResult.SUCCESS);
                        return;
                    }
                }

                // 3) 没有基酒也没有上等佳酿，不拦截，交给原版 onUse 逻辑
            }
        }
    }
}
