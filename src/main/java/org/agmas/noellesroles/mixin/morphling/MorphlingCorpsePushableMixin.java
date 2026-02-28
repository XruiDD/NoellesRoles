package org.agmas.noellesroles.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 变形者处于尸体模式时，双向禁止推动。
 *
 * isPushable → false：尸体不会被别人推动。
 * pushAway → no-op：尸体不会主动推动靠近的玩家。
 */
@Mixin(LivingEntity.class)
public class MorphlingCorpsePushableMixin {

    @WrapMethod(method = "isPushable")
    private boolean noellesroles$disableCorpsePush(Operation<Boolean> original) {
        if ((Object) this instanceof PlayerEntity player) {
            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(player);
            if (comp.corpseMode) {
                return false;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "pushAway")
    private void noellesroles$disableCorpsePushAway(Entity entity, Operation<Void> original) {
        if ((Object) this instanceof PlayerEntity player) {
            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(player);
            if (comp.corpseMode) {
                return;
            }
        }
        original.call(entity);
    }
}
