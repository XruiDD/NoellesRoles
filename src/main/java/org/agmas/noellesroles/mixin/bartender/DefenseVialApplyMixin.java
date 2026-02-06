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

import java.util.List;

// Allow Fine Drink to be placed on food platters as visible items
@Mixin(FoodPlatterBlock.class)
public abstract class DefenseVialApplyMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void fineDrinkPlatter(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient) return;
        if (player.isCreative()) return;

        BlockEntity platter = world.getBlockEntity(pos);
        if (platter instanceof BeveragePlateBlockEntity blockEntity) {
            // Allow placing Fine Drink on the platter as a visible item
            if (player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.FINE_DRINK)) {
                blockEntity.addItem(player.getStackInHand(Hand.MAIN_HAND).copy());
                player.getStackInHand(Hand.MAIN_HAND).decrement(1);
                player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                // 记录放置上等佳酿到餐盘
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "place");
                    GameRecordManager.putBlockPos(extra, "pos", pos);
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.FINE_DRINK), null, extra);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Priority retrieval of Fine Drink when player has empty hand
            if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
                List<ItemStack> platterItems = blockEntity.getStoredItems();
                if (platterItems.isEmpty()) return;

                // Find Fine Drink in the platter (priority retrieval, forced)
                for (int i = 0; i < platterItems.size(); i++) {
                    if (platterItems.get(i).isOf(ModItems.FINE_DRINK)) {
                        // Remove Fine Drink from platter and give to player
                        ItemStack fineDrink = platterItems.remove(i).copy();
                        fineDrink.setCount(1);
                        fineDrink.set(DataComponentTypes.MAX_STACK_SIZE, 1);

                        // 记录从餐盘拿取上等佳酿（原模组的 recordPlatterTake 被绕过）
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(ModItems.FINE_DRINK), pos);
                        }

                        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                        player.setStackInHand(Hand.MAIN_HAND, fineDrink);
                        blockEntity.markDirty();
                        world.updateListeners(pos, state, state, 3);
                        cir.setReturnValue(ActionResult.SUCCESS);
                        return;
                    }
                }
            }
        }
    }
}
