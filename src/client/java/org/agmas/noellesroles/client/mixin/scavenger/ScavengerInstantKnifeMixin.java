package org.agmas.noellesroles.client.mixin.scavenger;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.item.KnifeItem;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 清道夫角色不需要蓄力，直接刀人
 */
@Mixin(KnifeItem.class)
public abstract class ScavengerInstantKnifeMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void scavengerInstantKnifeUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!world.isClient) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(user.getWorld());
        if (!gameWorldComponent.isRole(user, Noellesroles.SCAVENGER)) return;

        ItemStack itemStack = user.getStackInHand(hand);

        // 直接执行刀人逻辑
        HitResult collision = KnifeItem.getKnifeTarget(user);
        if (collision instanceof EntityHitResult entityHitResult) {
            ClientPlayNetworking.send(new KnifeStabPayload(entityHitResult.getEntity().getId()));
        } else if (collision instanceof BlockHitResult blockHitResult) {
            Optional<PlayerEntity> sleepingPlayer = RevolverItem.findSleepingPlayerOnBed(world, blockHitResult);
            sleepingPlayer.ifPresent(target -> ClientPlayNetworking.send(new KnifeStabPayload(target.getId())));
        }

        // 取消原有逻辑，不进入蓄力状态
        cir.setReturnValue(TypedActionResult.success(itemStack));
    }
}
