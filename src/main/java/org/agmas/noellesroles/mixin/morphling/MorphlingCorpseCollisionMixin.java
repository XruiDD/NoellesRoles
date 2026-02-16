package org.agmas.noellesroles.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 变形者处于尸体模式时，移除硬碰撞箱，
 * 使其行为与真正的尸体实体 (PlayerBodyEntity) 一致。
 *
 * 使用 @WrapMethod 包装 collidesWith，在 wathe 的 @WrapMethod 外层执行，
 * 从而在 wathe 强制返回 true 之前拦截尸体模式玩家的碰撞。
 */
@Mixin(Entity.class)
public class MorphlingCorpseCollisionMixin {

    @WrapMethod(method = "collidesWith")
    private boolean noellesroles$disableCorpseCollision(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;

        // 如果自身是变形者尸体模式，不与任何实体碰撞
        if (self instanceof PlayerEntity selfPlayer) {
            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(selfPlayer);
            if (comp.corpseMode) {
                return false;
            }
        }

        // 如果对方是变形者尸体模式，也不碰撞
        if (other instanceof PlayerEntity otherPlayer) {
            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(otherPlayer);
            if (comp.corpseMode) {
                return false;
            }
        }

        // 非尸体模式，交给下层（wathe 的硬碰撞逻辑 → 原始方法）
        return original.call(other);
    }
}
