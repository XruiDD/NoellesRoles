package org.agmas.noellesroles.mixin.riotpatrol;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.RiotShieldItem;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {
    private static final double SHOVE_RANGE = 1.5;
    private static final double SHOVE_RANGE_SQ = SHOVE_RANGE * SHOVE_RANGE;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$riotShieldKnockback(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getMainHandStack().isOf(ModItems.RIOT_SHIELD)) {
            return;
        }
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.RIOT_PATROL)) {
            return;
        }
        RiotPatrolPlayerComponent component = RiotPatrolPlayerComponent.KEY.get(player);
        if (player.getItemCooldownManager().isCoolingDown(ModItems.RIOT_SHIELD)) {
            ci.cancel();
            return;
        }
        if (!(target instanceof PlayerEntity livingTarget) || player.squaredDistanceTo(livingTarget) > SHOVE_RANGE_SQ) {
            ci.cancel();
            return;
        }

        if (!player.getWorld().isClient) {
            component.lowerShield(false);
            player.clearActiveItem();
            player.getItemCooldownManager().set(ModItems.RIOT_SHIELD, RiotShieldItem.SHOVE_COOLDOWN_TICKS);

            livingTarget.takeKnockback(0.45, player.getX() - livingTarget.getX(), player.getZ() - livingTarget.getZ());
            livingTarget.velocityModified = true;
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, player.getSoundCategory(), 0.8F, 1.15F);
        }

        ci.cancel();
    }
}
