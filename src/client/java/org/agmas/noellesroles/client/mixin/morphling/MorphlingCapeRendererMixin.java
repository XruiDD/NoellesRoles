package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(CapeFeatureRenderer.class)
public class MorphlingCapeRendererMixin {

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
                return WatheClient.PLAYER_ENTRIES_CACHE.get(shuffledUuid).getSkinTextures();
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
            }
        }

        return original.call(instance);
    }
}
