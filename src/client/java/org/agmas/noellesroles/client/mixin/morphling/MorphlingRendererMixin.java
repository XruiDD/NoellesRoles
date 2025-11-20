package org.agmas.noellesroles.client.mixin.morphling;

import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerEntityRenderer.class)
public abstract class MorphlingRendererMixin {


    @Shadow public abstract Identifier getTexture(AbstractClientPlayerEntity abstractClientPlayerEntity);

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    void b(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfoReturnable<Identifier> cir) {
        if ((MorphlingPlayerComponent.KEY.get(abstractClientPlayerEntity)).getMorphTicks() > 0) {
            if (abstractClientPlayerEntity.getEntityWorld().getPlayerByUuid((MorphlingPlayerComponent.KEY.get(abstractClientPlayerEntity)).disguise) != null) {
                cir.setReturnValue(getTexture((AbstractClientPlayerEntity) abstractClientPlayerEntity.getEntityWorld().getPlayerByUuid((MorphlingPlayerComponent.KEY.get(abstractClientPlayerEntity)).disguise)));
                cir.cancel();
            } else {
                Log.info(LogCategory.GENERAL, "Morphling disguise is null!!!");
            }
            if (MorphlingPlayerComponent.KEY.get(abstractClientPlayerEntity).disguise.equals(MinecraftClient.getInstance().player.getUuid())) {
                cir.setReturnValue(getTexture(MinecraftClient.getInstance().player));
                cir.cancel();
            }
        }
    }
    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    SkinTextures b(AbstractClientPlayerEntity instance) {
        if ((MorphlingPlayerComponent.KEY.get(instance)).getMorphTicks() > 0) {
            if (instance.getEntityWorld().getPlayerByUuid((MorphlingPlayerComponent.KEY.get(instance)).disguise) != null) {

                 return ((AbstractClientPlayerEntity) instance.getEntityWorld().getPlayerByUuid((MorphlingPlayerComponent.KEY.get(instance)).disguise)).getSkinTextures();
            } else {
                Log.info(LogCategory.GENERAL, "Morphling disguise is null!!!");
            }
        }
        return instance.getSkinTextures();
    }

}
