package org.agmas.noellesroles.client.mixin.bartender;

import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 亢奋状态 Mood 图标替换。
 * 当玩家拥有 STIMULATION 效果时，替换心情图标为 mood_stimulation。
 * 同时覆盖平民和杀手阵营的图标获取方法。
 */
@Mixin(MoodRenderer.class)
public abstract class StimulationMoodMixin {

    @Unique
    private static final Identifier MOOD_STIMULATION = Identifier.of(Noellesroles.MOD_ID, "hud/mood_stimulation");

    @Inject(method = "getCivilianMoodIcon", at = @At("HEAD"), cancellable = true)
    private static void overrideCivilianIcon(float mood, CallbackInfoReturnable<Identifier> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.hasStatusEffect(ModEffects.STIMULATION)) {
            cir.setReturnValue(MOOD_STIMULATION);
        }
    }

    @Inject(method = "getKillerMoodIcon", at = @At("HEAD"), cancellable = true)
    private static void overrideKillerIcon(CallbackInfoReturnable<Identifier> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.hasStatusEffect(ModEffects.STIMULATION)) {
            cir.setReturnValue(MOOD_STIMULATION);
        }
    }
}
