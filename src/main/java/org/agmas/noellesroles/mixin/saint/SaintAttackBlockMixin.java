package org.agmas.noellesroles.mixin.saint;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class SaintAttackBlockMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKarmaAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient) {
            return;
        }
        if (!SaintPlayerComponent.KEY.get(player).isKarmaLocked()) {
            return;
        }
        player.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, SaintPlayerComponent.KEY.get(player).getKarmaLockTicks() / 20)).formatted(Formatting.RED), true);
        ci.cancel();
    }
}
