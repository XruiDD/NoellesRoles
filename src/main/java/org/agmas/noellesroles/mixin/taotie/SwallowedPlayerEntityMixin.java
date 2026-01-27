package org.agmas.noellesroles.mixin.taotie;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让被吞的玩家看不到其他观察者玩家
 * (包括真正的旁观者和被其他饕餮吞的玩家)
 */
@Mixin(Entity.class)
public class SwallowedPlayerEntityMixin {

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void makeSpectatorsInvisibleToSwallowedPlayers(PlayerEntity viewer, CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = (Entity) (Object) this;

        // 检查查看者是否是被吞的玩家
        SwallowedPlayerComponent viewerSwallowed = SwallowedPlayerComponent.KEY.get(viewer);
        if (!viewerSwallowed.isSwallowed()) {
            return; // 查看者不是被吞的玩家，不做处理
        }

        // 如果当前实体是观察者玩家
        if (thisEntity instanceof PlayerEntity targetPlayer && targetPlayer.isSpectator()) {
            cir.setReturnValue(true);
        }
    }
}
