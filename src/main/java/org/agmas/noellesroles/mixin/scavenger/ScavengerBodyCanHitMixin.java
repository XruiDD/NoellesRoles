package org.agmas.noellesroles.mixin.scavenger;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import org.agmas.noellesroles.scavenger.HiddenBodiesWorldComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 清道夫隐藏尸体准星穿透Mixin
 * canHit()在LivingEntity中被override，所以必须直接target PlayerBodyEntity。
 * 当尸体被清道夫隐藏时返回false，使准星射线穿透隐藏尸体，
 * 玩家可以正常操作后面的方块。
 */
@Mixin(PlayerBodyEntity.class)
public class ScavengerBodyCanHitMixin {

    @WrapMethod(method = "canHit")
    private boolean noellesroles$disableScavengerBodyTargeting(Operation<Boolean> original) {
        PlayerBodyEntity self = (PlayerBodyEntity) (Object) this;
        HiddenBodiesWorldComponent hiddenBodies = HiddenBodiesWorldComponent.KEY.get(self.getWorld());
        if (hiddenBodies.isHidden(self.getPlayerUuid())) {
            return false;
        }
        return original.call();
    }
}
