package org.agmas.noellesroles.client.mixin.spiritualist;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 通灵者灵魂出窍时，隐藏所有其他玩家的名牌
 *
 * 用 SpiritCameraHandler.isActive() 而非 isProjecting()：与皮肤覆盖保持同一标志，
 * 避免相机/灰度滤镜尚未关闭的渲染帧短暂泄露真实名牌。
 */
@Mixin(RoleNameRenderer.class)
public abstract class SpiritNameMixin {

    @WrapOperation(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text spiritualist$hideName(PlayerEntity instance, Operation<Text> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && instance != client.player && SpiritCameraHandler.isActive()) {
            return Text.literal("");
        }
        return original.call(instance);
    }
}
