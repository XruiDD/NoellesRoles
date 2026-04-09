package org.agmas.noellesroles.client.mixin.ferryman;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.util.HudRenderHelper;
import org.agmas.noellesroles.ferryman.FerrymanPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class FerrymanHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    private static void noellesroles$renderSoulParticle(DrawContext context, int centerX, int centerY, int size, int alpha) {
        int outer = (alpha << 24) | 0x7BD6FF;
        int core = (Math.min(255, alpha + 40) << 24) | 0xE8FAFF;
        int flame = (Math.min(255, alpha + 15) << 24) | 0x4CCBFF;
        int half = size / 2;

        context.fill(centerX - half, centerY - half, centerX + half, centerY + half, outer);
        context.fill(centerX - Math.max(1, size / 6), centerY - size, centerX + Math.max(1, size / 6), centerY + half, flame);
        context.fill(centerX - Math.max(1, size / 3), centerY - Math.max(1, size / 3), centerX + Math.max(1, size / 3), centerY + Math.max(1, size / 3), core);
    }

    private static void noellesroles$renderReactionParticles(DrawContext context, long nowMillis) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int band = Math.max(34, width / 7);
        double time = nowMillis / 1000.0;

        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < 15; i++) {
                double laneOffset = 0.19 * i;
                double cycle = (time * 0.9 + laneOffset) % 1.0;
                double eased = 1.0 - Math.pow(1.0 - cycle, 2.0);
                double wobble = Math.sin(time * 4.0 + i * 1.7) * 0.08;
                double depth = 0.25 + 0.75 * ((Math.sin(i * 12.345 + side * 2.2) + 1.0) * 0.5);

                int x = side == 0
                        ? (int) (8 + band * (0.18 + depth * 0.72))
                        : (int) (width - 8 - band * (0.18 + depth * 0.72));
                x += (int) (wobble * band * (side == 0 ? 1 : -1));

                int y = (int) (height * (0.90 - eased * 0.78 + Math.cos(i * 3.1 + time * 2.3) * 0.015));
                int alpha = (int) (35 + 140 * Math.sin(cycle * Math.PI));
                int size = 3 + (int) Math.round(depth * 4.0);

                noellesroles$renderSoulParticle(context, x, y, size, alpha);
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderFerrymanHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.FERRYMAN)) return;

        FerrymanPlayerComponent ferryman = FerrymanPlayerComponent.KEY.get(player);
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);

        Text line;
        if (ferryman.isReactionActive()) {
            line = Text.translatable("tip.ferryman.reaction_ready", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        } else if (ability.getCooldown() > 0) {
            line = Text.translatable("tip.noellesroles.cooldown", ability.getCooldown() / 20);
        } else if (NoellesrolesClient.targetBody != null) {
            line = Text.translatable("tip.ferryman.ferry", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        } else {
            line = Text.translatable("tip.ferryman.progress", ferryman.getFerriedCount(), ferryman.getFerriedRequired(), ferryman.getBlessingStacks());
        }

        int drawY = context.getScaledWindowHeight();
        HudRenderHelper.drawBottomRight(context, getTextRenderer(), line, drawY, Noellesroles.FERRYMAN.color());

        if (ferryman.isReactionActive()) {
            noellesroles$renderReactionParticles(context, System.currentTimeMillis());
        }
    }
}
