package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.spiritualist.SpiritCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阻止 SpiritCamera 实体被渲染（包括阴影）
 */
@Mixin(EntityRenderDispatcher.class)
public class SpiritRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void spiritualist$hideSpiritCamera(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof SpiritCamera) {
            cir.setReturnValue(false);
        }
    }
}
