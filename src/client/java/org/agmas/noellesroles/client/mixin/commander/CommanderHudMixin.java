package org.agmas.noellesroles.client.mixin.commander;

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
import org.agmas.noellesroles.commander.CommanderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(InGameHud.class)
public abstract class CommanderHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderCommanderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = HudRenderHelper.getActivePlayer();
        if (player == null) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.COMMANDER)) return;

        CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(player);
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(player);

        int drawY = context.getScaledWindowHeight();
        List<String> markedNames = commanderComp.getThreatTargetNames();
        if (!markedNames.isEmpty()) {
            Text line2 = Text.translatable("tip.commander.marked", String.join("、", markedNames));
            drawY = HudRenderHelper.drawBottomRight(context, getTextRenderer(), line2, drawY, 0xCAA1FF);
        }

        Text line1;
        if (abilityComp.getCooldown() > 0) {
            line1 = Text.translatable("tip.noellesroles.cooldown", abilityComp.getCooldown() / 20);
        } else if (commanderComp.canMarkMore()) {
            line1 = Text.translatable("tip.commander.ready",
                    NoellesrolesClient.abilityBind.getBoundKeyLocalizedText(),
                    commanderComp.getRemainingMarks());
        } else {
            line1 = Text.translatable("tip.commander.no_marks");
        }

        HudRenderHelper.drawBottomRight(context, getTextRenderer(), line1, drawY, 0xCAA1FF);
    }
}
