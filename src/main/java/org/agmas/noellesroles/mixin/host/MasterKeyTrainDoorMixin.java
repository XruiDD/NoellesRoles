package org.agmas.noellesroles.mixin.host;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block.TrainDoorBlock;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.BlockState;
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

@Mixin(TrainDoorBlock.class)
public abstract class MasterKeyTrainDoorMixin {

    // 在方法开始时检查冷却，如果中立角色的KEY处于冷却状态，直接返回FAIL（不发送消息）
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void checkNeutralKeyCooldown(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack handStack = player.getMainHandStack();
        if (handStack.isOf(WatheItems.KEY)) {
            // 获取门实体，检查门是否已打开（允许关门，不检查冷却）
            BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity) {
                if (entity.isOpen()) {
                    return;
                }
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            boolean isNeutralRole = gameWorld.isRole(player, Noellesroles.VULTURE)
                    || gameWorld.isRole(player, Noellesroles.PATHOGEN);

            // 如果是中立角色且处于冷却状态，直接返回FAIL
            if (isNeutralRole && player.getItemCooldownManager().isCoolingDown(WatheItems.KEY)) {
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

    // 拦截 LOCKPICK 检查，让 MASTER_KEY 和中立角色的 KEY 也能通过
    @WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private boolean masterKeyOrNeutralKey(ItemStack instance, Item item, Operation<Boolean> original, BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // 如果原本就是 LOCKPICK 或者是 MASTER_KEY，允许通过
        if (original.call(instance, item) || instance.isOf(ModItems.MASTER_KEY)) {
            return true;
        }

        // 检查是否是中立角色拿着 KEY
        if (instance.isOf(WatheItems.KEY)) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            boolean isNeutralRole = gameWorld.isRole(player, Noellesroles.VULTURE)
                    || gameWorld.isRole(player, Noellesroles.PATHOGEN);

            if (isNeutralRole) {
                // 冷却已在@Inject中检查，这里直接设置冷却并返回true
                player.getItemCooldownManager().set(WatheItems.KEY, 200);
                return true;
            }
        }

        return false;
    }

}
