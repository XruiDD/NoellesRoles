package org.agmas.noellesroles.mixin.bartender;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.agmas.noellesroles.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拥有无碰撞效果的玩家可以穿过其他玩家。
 * 注入 collidesWith（Wathe 使用的碰撞方法）、isCollidable、isPushable 和 pushAwayFrom，
 * 全面覆盖碰撞检测。
 */
@Mixin(Entity.class)
public abstract class NoCollisionMixin {

    /**
     * collidesWith 是 Wathe 实际使用的碰撞判定方法。
     * 如果自己或对方有 NO_COLLISION 效果，返回 false 以穿过。
     */
    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    private void noCollisionCollidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            cir.setReturnValue(false);
            return;
        }
        if (other instanceof LivingEntity otherLiving && otherLiving.hasStatusEffect(ModEffects.NO_COLLISION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void noCollisionIsCollidable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void noCollisionIsPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void noCollisionPushAwayFrom(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            ci.cancel();
            return;
        }
        if (entity instanceof LivingEntity other && other.hasStatusEffect(ModEffects.NO_COLLISION)) {
            ci.cancel();
        }
    }
}
