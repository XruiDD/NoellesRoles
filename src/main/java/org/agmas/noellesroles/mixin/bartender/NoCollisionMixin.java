package org.agmas.noellesroles.mixin.bartender;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.agmas.noellesroles.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拥有无碰撞效果的玩家可以穿过其他玩家
 */
@Mixin(Entity.class)
public abstract class NoCollisionMixin {

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void noCollisionEffect(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            cir.setReturnValue(false);
        }
    }
}
