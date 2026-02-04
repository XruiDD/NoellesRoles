package org.agmas.noellesroles.mixin.taotie;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止被吞玩家使用旁观功能
 * - 阻止setCameraEntity，防止被吞玩家退出观战
 * - 阻止攻击切换目标
 */
@Mixin(ServerPlayerEntity.class)
public abstract class SwallowedPlayerSpectatorMixin {

    @Shadow
    public abstract Entity getCameraEntity();

    @Inject(method = "setCameraEntity", at = @At("HEAD"), cancellable = true)
    private void preventSwallowedPlayerSetCamera(Entity entity, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
        if (swallowedComp.isSwallowed() && swallowedComp.getSwallowedBy() != entity.getUuid()) {
            ci.cancel();
        }
    }
}
