package org.agmas.noellesroles.client.mixin.neutral;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 中立阵营 Mood 图标替换。
 * 当玩家是中立角色时，替换心情图标为对应的中立阵营旗帜。
 */
@Mixin(MoodRenderer.class)
public abstract class NeutralMoodMixin {

    @Unique
    private static final Identifier MOOD_JESTER = Identifier.of(Noellesroles.MOD_ID, "hud/mood_jester");
    @Unique
    private static final Identifier MOOD_PATHOGEN = Identifier.of(Noellesroles.MOD_ID, "hud/mood_pathogen");
    @Unique
    private static final Identifier MOOD_VULTURE = Identifier.of(Noellesroles.MOD_ID, "hud/mood_vulture");
    @Unique
    private static final Identifier MOOD_TAOTIE = Identifier.of(Noellesroles.MOD_ID, "hud/mood_taotie");
    @Unique
    private static final Identifier MOOD_CRIMINAL_REASONER = Identifier.of(Noellesroles.MOD_ID, "hud/mood_criminal_reasoner");
    @Unique
    private static final Identifier MOOD_CORRUPT_COP = Identifier.of(Noellesroles.MOD_ID, "hud/mood_corruptedcop");
    @Unique
    private static final Identifier MOOD_CORRUPT_COP_MOMENT = Identifier.of(Noellesroles.MOD_ID, "hud/mood_corrupted_moment");

    @Unique
    private static @Nullable Identifier getNeutralMoodIdentifier(ClientPlayerEntity player) {
        if (player.hasStatusEffect(ModEffects.STIMULATION)) return null;

        GameWorldComponent gwc = GameWorldComponent.KEY.get(player.getWorld());

        if (gwc.isRole(player, Noellesroles.JESTER)) return MOOD_JESTER;
        if (gwc.isRole(player, Noellesroles.PATHOGEN)) return MOOD_PATHOGEN;
        if (gwc.isRole(player, Noellesroles.VULTURE)) return MOOD_VULTURE;
        if (gwc.isRole(player, Noellesroles.TAOTIE)) return MOOD_TAOTIE;
        if (gwc.isRole(player, Noellesroles.CRIMINAL_REASONER)) return MOOD_CRIMINAL_REASONER;
        if (gwc.isRole(player, Noellesroles.CORRUPT_COP)) {
            CorruptCopPlayerComponent comp = CorruptCopPlayerComponent.KEY.get(player);
            return comp.isCorruptCopMomentActive() ? MOOD_CORRUPT_COP_MOMENT : MOOD_CORRUPT_COP;
        }

        return null;
    }

    @Inject(method = "getCivilianMoodIcon", at = @At("HEAD"), cancellable = true)
    private static void overrideCivilianForNeutral(float mood, CallbackInfoReturnable<Identifier> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        Identifier id = getNeutralMoodIdentifier(player);
        if (id != null) cir.setReturnValue(id);
    }

    @Inject(method = "getKillerMoodIcon", at = @At("HEAD"), cancellable = true)
    private static void overrideKillerForNeutral(CallbackInfoReturnable<Identifier> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        Identifier id = getNeutralMoodIdentifier(player);
        if (id != null) cir.setReturnValue(id);
    }
}
