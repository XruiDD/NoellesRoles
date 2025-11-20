package org.agmas.noellesroles.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.client.gui.RoundTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RoundTextRenderer.class)
public abstract class RoleTextMixin {

    // this is so scuffed

    @ModifyArg(method = "renderHud", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 0), index =  1)
    private static Text welcome(Text text, @Local(argsOnly = true)ClientPlayerEntity player) {
        if (isMurder(player) && NoellesrolesClient.clientModdedRole != null) {
            return Text.translatable("announcement.welcome", Text.translatable("announcement.title."+NoellesrolesClient.clientModdedRole.translationKey).withColor(NoellesrolesClient.clientModdedRole.color));
        }
        return text;
    }
    @ModifyArg(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 1), index =  1)
    private static Text premise(Text text, @Local(argsOnly = true)ClientPlayerEntity player) {
        if (isMurder(player) && NoellesrolesClient.clientModdedRole != null) {
            return Text.translatable("announcement.premise." + NoellesrolesClient.clientModdedRole.translationKey);
        }
        return text;
    }
    @ModifyArg(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 2), index =  1)
    private static Text goal(Text text, @Local(argsOnly = true)ClientPlayerEntity player) {
        if (isMurder(player) && NoellesrolesClient.clientModdedRole != null) {
            return Text.translatable("announcement.goal." + NoellesrolesClient.clientModdedRole.translationKey);
        }
        return text;
    }


    @ModifyArg(method = "renderHud", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 0), index = 2)
    private static int welcomeW(int width, @Local(argsOnly = true)ClientPlayerEntity player, @Local(argsOnly = true)TextRenderer renderer) {
        if (isMurder(player) && NoellesrolesClient.clientModdedRole != null) {
            return -renderer.getWidth(Text.translatable("announcement.welcome", Text.translatable("announcement.title."+NoellesrolesClient.clientModdedRole.translationKey)))/2;
        }
        return width;
    }
    @ModifyArg(method = "renderHud", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 1), index = 2)
    private static int premiseW(int width, @Local(argsOnly = true)ClientPlayerEntity player, @Local(argsOnly = true)TextRenderer renderer) {
        if (isMurder(player) && NoellesrolesClient.clientModdedRole != null) {
            return -renderer.getWidth(Text.translatable("announcement.premise." + NoellesrolesClient.clientModdedRole.translationKey))/2;
        }
        return width;
    }

    @ModifyArg(method = "renderHud", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 2), index = 2)
    private static int goalW(int width, @Local(argsOnly = true)ClientPlayerEntity player, @Local(argsOnly = true)TextRenderer renderer) {
        if (isMurder(player) && NoellesrolesClient.clientModdedRole != null) {
            return -renderer.getWidth(Text.translatable("announcement.goal." + NoellesrolesClient.clientModdedRole.translationKey))/2;
        }
        return width;
    }

    @Unique
    private static boolean isMurder(PlayerEntity player) {
        return ((GameWorldComponent)GameWorldComponent.KEY.get(player.getWorld())).getGameMode() != GameWorldComponent.GameMode.LOOSE_ENDS;
    }
}
