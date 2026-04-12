package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通灵者灵魂出窍时，禁止所有交互（攻击、使用物品、交互方块/实体）
 */
@Mixin(ClientPlayerInteractionManager.class)
public class SpiritInteractionMixin {

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockInteractEntityAt(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
