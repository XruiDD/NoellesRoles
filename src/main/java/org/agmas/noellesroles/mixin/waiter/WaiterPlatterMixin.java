package org.agmas.noellesroles.mixin.waiter;

import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 服务员可以从餐盘/饮品托盘上拿取两倍数量的食物/饮品
 * 普通玩家只能携带1份同类物品，服务员可以携带2份
 */
@Mixin(FoodPlatterBlock.class)
public abstract class WaiterPlatterMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void waiterDoublePickup(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                     BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(player, Noellesroles.WAITER)) return;

        // 只处理空手拾取的情况，其他操作（创造模式放置、下毒等）由原始逻辑处理
        if (!player.getStackInHand(Hand.MAIN_HAND).isEmpty()) return;

        if (!(world.getBlockEntity(pos) instanceof BeveragePlateBlockEntity blockEntity)) return;

        List<ItemStack> platter = blockEntity.getStoredItems();
        if (platter.isEmpty()) return; // 空餐盘交给原始逻辑

        // 检查玩家是否已经拥有餐盘上的同类物品
        Set<net.minecraft.item.Item> platterItemTypes = new HashSet<>();
        for (ItemStack platterItem : platter) {
            platterItemTypes.add(platterItem.getItem());
        }

        boolean hasMatchingItem = false;
        int matchCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack invItem = player.getInventory().getStack(i);
            if (!invItem.isEmpty() && platterItemTypes.contains(invItem.getItem())) {
                hasMatchingItem = true;
                matchCount++;
            }
        }

        // 如果没有同类物品，让原始逻辑处理（给第1份）
        if (!hasMatchingItem) return;

        // 已经有2份或以上，不能再拿
        if (matchCount >= 2) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        // 已有1份，服务员可以拿第2份
        ItemStack randomItem = platter.get(world.getRandom().nextInt(platter.size())).copy();
        randomItem.setCount(1);
        randomItem.set(DataComponentTypes.MAX_STACK_SIZE, 1);
        String poisoner = blockEntity.getPoisoner();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(randomItem.getItem()), pos, poisoner);
        }
        if (poisoner != null) {
            randomItem.set(WatheDataComponentTypes.POISONER, poisoner);
            blockEntity.setPoisoner(null);
        }
        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
        player.setStackInHand(Hand.MAIN_HAND, randomItem);

        cir.setReturnValue(ActionResult.PASS);
    }
}
