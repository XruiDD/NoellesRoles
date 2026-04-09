package org.agmas.noellesroles.mixin.silencer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 拦截 PlayerPsychoComponent.stopPsycho() 无参版本中对 stopPsycho(boolean) 的调用。
 * 当玩家是静语者时，传入 false 避免递减 psychosActive 计数器
 * （因为静语者的 startPsycho 也传了 false，不递增计数器）。
 */
@Mixin(PlayerPsychoComponent.class)
public class SilencerPsychoStopMixin {

    @Shadow @Final private PlayerEntity player;

    @Redirect(
            method = "stopPsycho()V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerPsychoComponent;stopPsycho(Z)V")
    )
    private void noellesroles$silencerStopPsycho(PlayerPsychoComponent instance, boolean trackActive) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (gameWorld.isRole(this.player, Noellesroles.SILENCER)) {
            instance.stopPsycho(false);
        } else {
            instance.stopPsycho(true);
        }
    }
}
