package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntityRenderer.class)
public abstract class MorphlingRendererMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    void b(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfoReturnable<Identifier> cir) {
        if (WatheClient.moodComponent != null) {
            if ((ConfigWorldComponent.KEY.get(abstractClientPlayerEntity.getWorld())).insaneSeesMorphs
                    && WatheClient.moodComponent.isLowerThanDepressed()
                    && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(abstractClientPlayerEntity.getUuid())) {

                UUID targetUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(abstractClientPlayerEntity.getUuid());
                if (targetUuid != null) {
                    PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
                    if (entry != null) {
                        cir.setReturnValue(entry.getSkinTextures().texture());
                        cir.cancel();
                        return;
                    }
                }
            }
        }
        var morphComponent = MorphlingPlayerComponent.KEY.get(abstractClientPlayerEntity);
        if (morphComponent.getMorphTicks() > 0) {
            UUID disguiseUuid = morphComponent.disguise;
            if (disguiseUuid == null) return;

            // 首先检查是否伪装成本地玩家自己
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && disguiseUuid.equals(client.player.getUuid())) {
                cir.setReturnValue(client.player.getSkinTextures().texture());
                cir.cancel();
                return;
            }

            // 尝试从世界中获取目标玩家实体
            AbstractClientPlayerEntity disguiseEntity = (AbstractClientPlayerEntity) abstractClientPlayerEntity.getEntityWorld().getPlayerByUuid(disguiseUuid);
            if (disguiseEntity != null) {
                cir.setReturnValue(disguiseEntity.getSkinTextures().texture());
                cir.cancel();
                return;
            }

            // 目标不在世界中（已死亡为旁观者），回退到缓存获取贴图
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
            if (cachedEntry != null) {
                cir.setReturnValue(cachedEntry.getSkinTextures().texture());
                cir.cancel();
                return;
            }
        }
    }

    @WrapOperation(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    SkinTextures b(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        if (WatheClient.moodComponent != null) {
            if ((ConfigWorldComponent.KEY.get(instance.getWorld())).insaneSeesMorphs
                    && WatheClient.moodComponent.isLowerThanDepressed()
                    && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(instance.getUuid())) {

                UUID targetUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(instance.getUuid());
                if (targetUuid != null) {
                    // 【修复 NPE】：同上，检查 entry 是否为 null
                    PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
                    if (entry != null) {
                        return entry.getSkinTextures();
                    }
                }
            }
        }
        var morphComponent = MorphlingPlayerComponent.KEY.get(instance);
        if (morphComponent.getMorphTicks() > 0) {
            UUID disguiseUuid = morphComponent.disguise;
            AbstractClientPlayerEntity disguiseEntity = (AbstractClientPlayerEntity) instance.getEntityWorld().getPlayerByUuid(disguiseUuid);

            if (disguiseEntity != null) {
                return disguiseEntity.getSkinTextures();
            } else {
                // 目标不在世界中（已死亡），回退到缓存
                PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
                if (cachedEntry != null) {
                    return cachedEntry.getSkinTextures();
                }
            }
        }
        return original.call(instance);
    }
}