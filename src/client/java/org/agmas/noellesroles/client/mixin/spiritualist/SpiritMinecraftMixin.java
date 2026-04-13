package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵魂出窍时：
 * 1. 禁止攻击、选取、持续挖掘方块
 * 2. 阻止快捷栏切换
 * 3. 断线时兜底关闭灵魂出窍
 */
@Mixin(MinecraftClient.class)
public class SpiritMinecraftMixin {

    // 禁止攻击（左键单击）
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(false);
        }
    }

    // 禁止物品选取（中键）
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockItemPick(CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            ci.cancel();
        }
    }

    // 禁止持续挖掘方块（长按左键）
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void spiritualist$blockBreaking(CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            ci.cancel();
        }
    }

    // 照搬 Freecam MinecraftMixin：灵魂出窍时阻止快捷栏切换
    // ordinal 2 = hotbarKeys[i].wasPressed()，cancel 后跳过 hotbar 及之后所有按键处理
    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 2), cancellable = true)
    private void spiritualist$blockHotbar(CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            ci.cancel();
        }
    }

    // 断线时兜底关闭灵魂出窍
    @Inject(method = "disconnect()V", at = @At("HEAD"))
    private void spiritualist$disableOnDisconnect(CallbackInfo ci) {
        if (SpiritCameraHandler.isActive()) {
            SpiritCameraHandler.disable();
        }
    }
}
