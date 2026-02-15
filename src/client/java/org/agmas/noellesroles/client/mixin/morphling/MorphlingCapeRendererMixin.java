package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CapeFeatureRenderer.class)
public class MorphlingCapeRendererMixin {

    // 尸体模式下禁止渲染披风
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipCapeInCorpseMode(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
            int i, AbstractClientPlayerEntity player, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(player);
        if (comp.corpseMode) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures morphling_wrapCapeTexture(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {


        // 优先处理疯狂模式
        if (WatheClient.moodComponent != null) {
            ConfigWorldComponent config = ConfigWorldComponent.KEY.get(instance.getWorld());
            if (config.insaneSeesMorphs &&
                WatheClient.moodComponent.isLowerThanDepressed() &&
                NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(instance.getUuid())) {

                UUID shuffledUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(instance.getUuid());
                var entry = WatheClient.PLAYER_ENTRIES_CACHE.get(shuffledUuid);
                if (entry != null) {
                    return entry.getSkinTextures();
                }
            }
        }

        // 处理变形者伪装
        MorphlingPlayerComponent component = MorphlingPlayerComponent.KEY.get(instance);
        if (component.getMorphTicks() > 0) {
            UUID disguiseUuid = component.disguise;
            if (disguiseUuid != null) {
                PlayerEntity target = instance.getWorld().getPlayerByUuid(disguiseUuid);
                if (target instanceof AbstractClientPlayerEntity) {
                    return ((AbstractClientPlayerEntity) target).getSkinTextures();
                }
                // 目标不在世界中（已死亡），回退到缓存
                var cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
                if (cachedEntry != null) {
                    return cachedEntry.getSkinTextures();
                }
            }
        }

        return original.call(instance);
    }
}
