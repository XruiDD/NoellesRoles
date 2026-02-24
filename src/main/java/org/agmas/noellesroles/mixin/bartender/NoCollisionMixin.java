package org.agmas.noellesroles.mixin.bartender;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
 * 使用 @WrapMethod 在 Wathe 的 @WrapMethod 外层拦截碰撞检测。
 */
@Mixin(Entity.class)
public abstract class NoCollisionMixin {

    /**
     * Wathe 使用 @WrapMethod 包裹 collidesWith，在游戏进行中强制玩家间碰撞返回 true。
     * 我们也使用 @WrapMethod，在 Wathe 外层执行，在 Wathe 强制返回 true 之前拦截。
     */
    @WrapMethod(method = "collidesWith")
    private boolean noellesroles$noCollisionCollidesWith(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            return false;
        }
        if (other instanceof LivingEntity otherLiving && otherLiving.hasStatusEffect(ModEffects.NO_COLLISION)) {
            return false;
        }
        return original.call(other);
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
