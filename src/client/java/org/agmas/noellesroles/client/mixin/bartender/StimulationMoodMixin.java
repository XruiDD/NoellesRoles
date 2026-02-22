package org.agmas.noellesroles.client.mixin.bartender;

import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 亢奋状态 Mood 图标替换 Mixin
 * 当好人（innocent）玩家拥有亢奋（STIMULATION）状态效果时，
 * 将左上角 mood 图标替换为 mood_stimulation，并显示黄色时间条。
 */
@Mixin(MoodRenderer.class)
public abstract class StimulationMoodMixin {

    @Shadow
    public static float moodOffset;

    @Unique
    private static final Identifier MOOD_STIMULATION = Identifier.of(Noellesroles.MOD_ID, "hud/mood_stimulation");

    @Unique
    private static final int STIMULATION_MAX_TICKS = 15 * 20; // 300 ticks = 15 seconds

    @Unique
    private static final int STIMULATION_BAR_COLOR = 0xFFFF00; // bright yellow

    @Inject(method = "renderCivilian", at = @At("HEAD"), cancellable = true)
    private static void renderStimulationMood(TextRenderer renderer, DrawContext context, float previousMood, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        StatusEffectInstance stimulation = player.getStatusEffect(ModEffects.STIMULATION);
        if (stimulation == null) return;

        // Push matrix and apply moodOffset (consistent with normal renderCivilian)
        context.getMatrices().push();
        context.getMatrices().translate(0, 3.0f * moodOffset, 0);

        // Draw mood_stimulation icon at the same position as normal mood icon
        context.drawGuiTexture(MOOD_STIMULATION, 5, 6, 14, 17);

        // Draw yellow time bar (same position pattern as psycho mode bar)
        int remainingTicks = stimulation.getDuration();
        float duration = Math.min(1.0f, (float) remainingTicks / STIMULATION_MAX_TICKS);

        context.getMatrices().push();
        context.getMatrices().translate(26, 8 + renderer.fontHeight, 0);
        context.getMatrices().scale(150 * duration, 1, 1);
        int alpha = (int) (0.9f * 255) << 24;
        context.fill(0, 0, 1, 1, STIMULATION_BAR_COLOR | alpha);
        context.getMatrices().pop();

        context.getMatrices().pop();

        ci.cancel();
    }
}
