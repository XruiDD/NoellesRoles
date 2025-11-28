package org.agmas.noellesroles.client.mixin.seer;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerMoodComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.client.gui.RoleNameRenderer;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import org.agmas.harpymodloader.client.HarpymodloaderClient;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RoleNameRenderer.class)
public abstract class SeerHudMixin {

    @Shadow private static float nametagAlpha;

    @Shadow private static Text nametag;

    @Inject(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", ordinal = 0))
    private static void b(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (TMMClient.isPlayerSpectatingOrCreative()) return;
        if (nametagAlpha <= 0) {
            NoellesrolesClient.target = null;
        }
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.SEER)) {
            PlayerMoodComponent moodComponent = (PlayerMoodComponent) PlayerMoodComponent.KEY.get(MinecraftClient.getInstance().player);
            if (NoellesrolesClient.seer_revealedPlayers.contains(NoellesrolesClient.target)) {
                if (HarpymodloaderClient.hudRole.isInnocent()) {
                    Text name = Text.literal("Innocent");
                    context.drawTextWithShadow(renderer, name, -renderer.getWidth(name) / 2, 0, Colors.GREEN | (int) (nametagAlpha * 255.0F) << 24);
                } else {
                    Text name = Text.literal("Evil").formatted(Formatting.RED);
                    context.drawTextWithShadow(renderer, name, -renderer.getWidth(name) / 2, 0, Colors.RED | (int) (nametagAlpha * 255.0F) << 24);
                }
                return;
            }

            if (moodComponent.isLowerThanMid()) {
                Text name = Text.literal("50% sanity required to use ability");
                context.drawTextWithShadow(renderer, name, -renderer.getWidth(name) / 2, 0, Colors.YELLOW);
                return;
            }

            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
            Text line = Text.literal("Use ").append(NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()).append(Text.literal(" to reveal ").append(nametag).append("!"));

            if (abilityPlayerComponent.cooldown > 0) {
                line = Text.literal("Ability usable in " + abilityPlayerComponent.cooldown / 20 + "s");
            }

            context.drawTextWithShadow(renderer, line, -renderer.getWidth(line) / 2, 0, Noellesroles.SEER.color() | (int) (nametagAlpha * 255.0F) << 24);
        }
    }
    @Inject(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static void b(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci, @Local PlayerEntity target) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        NoellesrolesClient.target = target;

    }
}
