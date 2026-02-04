package org.agmas.noellesroles.mixin.taotie;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 被吞玩家的风雪检测视为无遮挡。
 */
@Mixin(Wathe.class)
public class SwallowedPlayerSkyVisibilityMixin {

    @Inject(method = "isSkyVisibleAdjacent", at = @At("HEAD"), cancellable = true)
    private static void forceSkyInvisibleWhenSwallowed(Entity player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof PlayerEntity playerEntity) {
            SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(playerEntity);
            if (swallowed.isSwallowed()) {
                cir.setReturnValue(false);
            }
        }
    }
}
