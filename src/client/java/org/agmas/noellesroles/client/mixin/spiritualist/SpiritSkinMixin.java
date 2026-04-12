package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通灵者灵魂出窍时，覆盖所有其他玩家的 getSkinTextures()
 * 返回 Steve 皮肤，同时 capeTexture/elytraTexture 为 null（隐藏披风和鞘翅）
 * 这是皮肤和披风的统一入口，不需要单独 Mixin PlayerEntityRenderer 和 CapeFeatureRenderer
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class SpiritSkinMixin {

    private static final SkinTextures STEVE_SKIN_TEXTURES = new SkinTextures(
            net.minecraft.util.Identifier.ofVanilla("textures/entity/player/wide/steve.png"),
            null, null, null,
            SkinTextures.Model.WIDE,
            true
    );

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void spiritualist$overrideSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 只对其他玩家生效，不覆盖自己
        if ((Object) this == client.player) return;

        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(client.player);
        if (spiritComp.isProjecting()) {
            cir.setReturnValue(STEVE_SKIN_TEXTURES);
        }
    }
}
