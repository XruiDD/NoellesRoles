package org.agmas.noellesroles.mixin.spiritualist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵界行者受到攻击时，服务端强制退出灵魂出窍
 */
@Mixin(LivingEntity.class)
public class SpiritDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void spiritualist$exitOnDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount <= 0) return;
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        GameWorldComponent gameComp = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameComp.isRole(player, Noellesroles.SPIRIT_WALKER)) return;

        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(player);
        if (spiritComp.isProjecting()) {
            spiritComp.cancelProjection("damaged");
        }
    }
}
