package org.agmas.noellesroles.mixin.taotie;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止被吞玩家使用旁观功能
 * - 阻止潜行退出观战
 * - 阻止攻击切换目标
 */
@Mixin(ServerPlayerEntity.class)
public abstract class SwallowedPlayerSpectatorMixin {

    @Shadow
    public abstract Entity getCameraEntity();

    @Shadow
    public abstract boolean isSpectator();

    /**
     * 阻止被吞玩家通过潜行退出观战模式
     */
    @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
    private void preventSwallowedPlayerDismount(CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);

        // 如果是被吞的玩家且正在观战
        if (swallowedComp.isSwallowed() && player.isSpectator() && this.getCameraEntity() != player) {
            cir.setReturnValue(false); // 阻止退出观战
        }
    }

    /**
     * 阻止被吞玩家通过攻击切换观战目标
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void preventSwallowedPlayerAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);

        // 如果是被吞的玩家且正在观战
        if (swallowedComp.isSwallowed() && player.isSpectator() && this.getCameraEntity() != player) {
            ci.cancel(); // 取消攻击操作，防止切换观战目标
        }
    }
}
