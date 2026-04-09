package org.agmas.noellesroles.mixin.hunter;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.hunter.HunterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public class HunterPlayerMoveMixin {
    @WrapMethod(method = "travel")
    private void noellesroles$blockTravel(Vec3d movementInput, Operation<Void> original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (HunterPlayerComponent.KEY.get(player).isTrapped()) {
            original.call(Vec3d.ZERO);
            return;
        }
        original.call(movementInput);
    }
}
