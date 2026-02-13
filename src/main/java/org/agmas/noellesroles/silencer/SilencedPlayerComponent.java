package org.agmas.noellesroles.silencer;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * Component for players who have been silenced by a Silencer.
 * Tracks silence duration and the silencer who applied it.
 * Server-side only - no client sync needed.
 */
public class SilencedPlayerComponent implements Component, ServerTickingComponent {
    public static final ComponentKey<SilencedPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "silenced"),
            SilencedPlayerComponent.class
    );

    // 60 seconds silence duration
    public static final int SILENCE_DURATION_TICKS = GameConstants.getInTicks(1, 0);

    // Extra SAN drain while silenced: equivalent to 2 additional active tasks
    // Over 60s silence, this drains ~0.5 (50% of the mood bar)
    public static final float SILENCE_EXTRA_MOOD_DRAIN = 2f * GameConstants.MOOD_DRAIN;

    private final PlayerEntity player;
    private int silenceTicks = 0;
    private UUID silencedBy = null;

    public SilencedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Apply silence effect to this player
     * @param silencerUuid The UUID of the Silencer who applied the effect
     */
    public void applySilence(UUID silencerUuid) {
        this.silenceTicks = SILENCE_DURATION_TICKS;
        this.silencedBy = silencerUuid;
    }

    /**
     * Check if this player is currently silenced
     */
    public boolean isSilenced() {
        return silenceTicks > 0;
    }

    /**
     * Get remaining silence ticks
     */
    public int getSilenceTicks() {
        return silenceTicks;
    }

    /**
     * Get the UUID of the Silencer who applied the silence
     */
    public UUID getSilencedBy() {
        return silencedBy;
    }

    /**
     * Reset the silence state
     */
    public void reset() {
        this.silenceTicks = 0;
        this.silencedBy = null;
    }

    /**
     * Check if a player is silenced (static helper method)
     */
    public static boolean isPlayerSilenced(PlayerEntity player) {
        if (player == null) return false;
        SilencedPlayerComponent comp = KEY.get(player);
        return comp.isSilenced();
    }

    @Override
    public void serverTick() {
        if (this.silenceTicks > 0) {
            this.silenceTicks--;

            // Accelerated SAN drain for silenced players
            if (this.player instanceof ServerPlayerEntity) {
                PlayerMoodComponent moodComp = PlayerMoodComponent.KEY.get(this.player);
                moodComp.setMood(moodComp.getMood() - SILENCE_EXTRA_MOOD_DRAIN);
            }

            // Clear silencer reference when silence ends
            if (this.silenceTicks <= 0) {
                this.silencedBy = null;
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("silenceTicks", this.silenceTicks);
        if (this.silencedBy != null) {
            tag.putUuid("silencedBy", this.silencedBy);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.silenceTicks = tag.contains("silenceTicks") ? tag.getInt("silenceTicks") : 0;
        this.silencedBy = tag.containsUuid("silencedBy") ? tag.getUuid("silencedBy") : null;
    }
}
