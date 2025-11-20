package org.agmas.noellesroles.mixin.jester;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerPsychoComponent;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.RoleHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class JesterJestMixin {

    @Inject(method = "killPlayer", at = @At("HEAD"), cancellable = true)
    private static void jesterJest(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, CallbackInfo ci) {
        if (killer != null) {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(victim.getWorld());
            if (RoleHelpers.instance.isPlayerOf(victim, Noellesroles.JESTER_ID) && !RoleHelpers.instance.isPlayerOf(killer, Noellesroles.JESTER_ID) && !gameWorldComponent.isKiller(killer)) {
                PlayerPsychoComponent component = (PlayerPsychoComponent)PlayerPsychoComponent.KEY.get(victim);
                if (component.getPsychoTicks() <= 0) {
                    component.startPsycho();
                    component.psychoTicks = GameConstants.getInTicks(1, 0);
                    ci.cancel();
                }
            }
        }
    }

}
