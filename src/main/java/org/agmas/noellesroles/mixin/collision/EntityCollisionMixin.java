package org.agmas.noellesroles.mixin.collision;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 通用碰撞禁用 Mixin（Entity 层）。
 * 处理以下情况的 collidesWith：
 * - 龙舌兰无碰撞效果（NO_COLLISION 药水）
 * - 变形者尸体模式（corpseMode）
 */
@Mixin(Entity.class)
public class EntityCollisionMixin {

    @WrapMethod(method = "collidesWith")
    private boolean noellesroles$disableCollision(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;

        if (shouldDisableCollision(self) || shouldDisableCollision(other)) {
            return false;
        }

        return original.call(other);
    }

    private static boolean shouldDisableCollision(Entity entity) {
        if (entity instanceof LivingEntity living && living.hasStatusEffect(ModEffects.NO_COLLISION)) {
            return true;
        }
        if (entity instanceof PlayerEntity player) {
            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(player);
            return comp.corpseMode;
        }
        return false;
    }
}
