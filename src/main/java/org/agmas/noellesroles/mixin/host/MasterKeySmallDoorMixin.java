package org.agmas.noellesroles.mixin.host;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmallDoorBlock.class)
public abstract class MasterKeySmallDoorMixin {

    // 在方法开始时检查冷却，如果中立角色的KEY处于冷却状态，直接返回FAIL（不发送消息）
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void checkNeutralKeyCooldown(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack handStack = player.getMainHandStack();
        if (handStack.isOf(WatheItems.KEY)) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            boolean isNeutralRole = gameWorld.isRole(player, Noellesroles.VULTURE)
                    || gameWorld.isRole(player, Noellesroles.CORRUPT_COP)
                    || gameWorld.isRole(player, Noellesroles.PATHOGEN);

            if (isNeutralRole) {
                // 获取门的实体和钥匙名称
                BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
                if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity) {
                    // 如果门已经打开，允许关门（不检查冷却）
                    if (entity.isOpen()) {
                        return;
                    }

                    String keyName = entity.getKeyName();

                    // 如果门不需要钥匙（公共区域门），不需要冷却检查
                    if (keyName.isEmpty()) {
                        return;
                    }

                    // 检查是否是自己的钥匙
                    LoreComponent lore = handStack.get(DataComponentTypes.LORE);
                    if (lore != null && !lore.lines().isEmpty()) {
                        boolean isOwnKey = lore.lines().getFirst().getString().equals(keyName);

                        // 如果不是自己的钥匙且处于冷却状态，直接返回FAIL
                        if (!isOwnKey && player.getItemCooldownManager().isCoolingDown(WatheItems.KEY)) {
                            cir.setReturnValue(ActionResult.FAIL);
                        }
                    }
                }
            }
        }
    }

    // ordinal=0: 拦截LOCKPICK检查，让中立角色的KEY（非自己房间）可以像LOCKPICK一样开门
    @WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z", ordinal = 0))
    private boolean neutralKeyAsLockpick(ItemStack instance, Item item, Operation<Boolean> original, BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // 先调用原始方法检查是否是真的LOCKPICK
        if (original.call(instance, item)) {
            return true;
        }

        // 检查是否是中立角色拿着KEY
        if (instance.isOf(WatheItems.KEY)) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            boolean isNeutralRole = gameWorld.isRole(player, Noellesroles.VULTURE)
                    || gameWorld.isRole(player, Noellesroles.CORRUPT_COP)
                    || gameWorld.isRole(player, Noellesroles.PATHOGEN);

            if (isNeutralRole) {
                // 获取门的实体和钥匙名称
                BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
                if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity) {
                    String keyName = entity.getKeyName();

                    // 如果门不需要钥匙（公共区域门），不需要特殊处理，返回false让原逻辑处理
                    if (keyName.isEmpty()) {
                        return false;
                    }

                    // 检查是否是自己的钥匙
                    LoreComponent lore = instance.get(DataComponentTypes.LORE);
                    if (lore != null && !lore.lines().isEmpty()) {
                        boolean isOwnKey = lore.lines().getFirst().getString().equals(keyName);

                        // 如果是自己的钥匙，返回false，让原来的KEY逻辑处理（无冷却）
                        if (isOwnKey) {
                            return false;
                        }

                        // 如果不是自己的钥匙，作为万能钥匙使用（冷却已在@Inject中检查）
                        // 设置10秒冷却（200 ticks），返回true表示可以开门
                        player.getItemCooldownManager().set(WatheItems.KEY, 200);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // ordinal=2: 拦截KEY检查，让MASTER_KEY也能通过KEY的检查
    @WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z", ordinal = 2))
    private boolean masterKeyAsKey(ItemStack instance, Item item, Operation<Boolean> original) {
        return original.call(instance, item) || instance.isOf(ModItems.MASTER_KEY);
    }
}
