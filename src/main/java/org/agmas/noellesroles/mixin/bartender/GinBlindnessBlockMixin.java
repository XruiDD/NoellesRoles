package org.agmas.noellesroles.mixin.bartender;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 杜松子酒夜视期间拦截失明效果的添加
 * 防止熄灯系统在夜视期间重新施加失明
 */
@Mixin(LivingEntity.class)
public class GinBlindnessBlockMixin {

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void blockBlindnessDuringGinNightVision(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (effect.getEffectType() != StatusEffects.BLINDNESS) return;

        BartenderPlayerComponent comp = BartenderPlayerComponent.KEY.get(player);
        if (comp.isGinNightVisionActive()) {
            cir.setReturnValue(false);
        }
    }
}
