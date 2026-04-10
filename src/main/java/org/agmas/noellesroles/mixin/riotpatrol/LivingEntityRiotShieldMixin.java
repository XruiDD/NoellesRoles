package org.agmas.noellesroles.mixin.riotpatrol;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityRiotShieldMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockFrontalMeleeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof PlayerEntity attacker)) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        if (!(target instanceof PlayerEntity player)) {
            return;
        }

        if (!this.noellesroles$isBlockableMeleeSource(source, attacker)) {
            return;
        }

        RiotPatrolPlayerComponent component = RiotPatrolPlayerComponent.KEY.get(player);
        if (!component.blocksAttacker(attacker)) {
            return;
        }

        if (!player.getWorld().isClient) {
            component.playShieldBlockEffects();
        }
        cir.setReturnValue(false);
    }

    private boolean noellesroles$isBlockableMeleeSource(DamageSource source, PlayerEntity attacker) {
        if (source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        return source.getSource() == attacker;
    }
}
