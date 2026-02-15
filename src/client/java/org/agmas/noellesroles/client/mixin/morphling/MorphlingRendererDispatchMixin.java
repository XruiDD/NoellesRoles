package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unchecked")
@Mixin(EntityRenderDispatcher.class)
public class MorphlingRendererDispatchMixin {
    @Shadow
    private Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity>> modelRenderers;

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void noellesroles$morphlingModelSwap(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;

        SkinTextures.Model targetModel = null;

        // 优先处理疯狂模式
        if (WatheClient.moodComponent != null) {
            ConfigWorldComponent config = ConfigWorldComponent.KEY.get(player.getWorld());
            if (config.insaneSeesMorphs
                    && WatheClient.moodComponent.isLowerThanDepressed()
                    && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(player.getUuid())) {
                UUID shuffledUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(player.getUuid());
                if (shuffledUuid != null) {
                    PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(shuffledUuid);
                    if (entry != null) {
                        targetModel = entry.getSkinTextures().model();
                    }
                }
            }
        }

        // 处理变形者伪装
        if (targetModel == null) {
            MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(player);
            if (morphComp.getMorphTicks() > 0 && morphComp.disguise != null) {
                // 优先从在线玩家获取
                PlayerEntity disguisePlayer = player.getWorld().getPlayerByUuid(morphComp.disguise);
                if (disguisePlayer instanceof AbstractClientPlayerEntity disguiseClient) {
                    targetModel = disguiseClient.getSkinTextures().model();
                } else {
                    // 回退到缓存（目标可能已死亡）
                    PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(morphComp.disguise);
                    if (cachedEntry != null) {
                        targetModel = cachedEntry.getSkinTextures().model();
                    }
                }
            }
        }

        if (targetModel != null) {
            EntityRenderer<? extends PlayerEntity> renderer = this.modelRenderers.get(targetModel);
            if (renderer != null) {
                cir.setReturnValue((EntityRenderer<? super T>) renderer);
            }
        }
    }
}
