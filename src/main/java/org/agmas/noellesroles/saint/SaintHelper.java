package org.agmas.noellesroles.saint;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;

public final class SaintHelper {
    private SaintHelper() {}

    public static boolean isProtectedFrom(PlayerEntity victim, PlayerEntity killer, GameWorldComponent gameWorldComponent) {
        if (killer == null || !gameWorldComponent.isRole(victim, Noellesroles.SAINT)) {
            return false;
        }
        Role killerRole = gameWorldComponent.getRole(killer);
        return killerRole != null && killerRole.isInnocent();
    }

    public static boolean shouldTrackKarma(PlayerEntity player, GameWorldComponent gameWorldComponent) {
        if (player == null || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        Role role = gameWorldComponent.getRole(player);
        return role != null && !role.isInnocent();
    }

    public static void tryTriggerKarmaLock(ServerPlayerEntity player, GameWorldComponent gameWorldComponent) {
        SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(player);
        if (!saintComponent.hasKarma() || saintComponent.isKarmaLocked()) {
            return;
        }
        saintComponent.triggerKarmaLock(gameWorldComponent.isRole(player, Noellesroles.BOMBER));
        GameRecordManager.event("saint_karma")
                .actor(player)
                .put("action", "triggered")
                .put("duration", Integer.toString(saintComponent.getKarmaLockTicks()))
                .record();
        player.sendMessage(Text.translatable("tip.saint.karma_triggered", Math.max(1, saintComponent.getKarmaLockTicks() / 20)), true);
    }

    /**
     * Handles saint-related logic in KillPlayer.BEFORE.
     * Returns a KillResult if the kill should be cancelled, null otherwise.
     */
    @Nullable
    public static KillPlayer.KillResult handleBeforeKill(PlayerEntity victim, PlayerEntity killer, Identifier deathReason, GameWorldComponent gameWorldComponent) {
        if (isProtectedFrom(victim, killer, gameWorldComponent)) {
            if (victim instanceof ServerPlayerEntity serverVictim) {
                var event = GameRecordManager.event("death_blocked")
                        .actor(serverVictim)
                        .put("block_reason", "saint_innocent_immunity")
                        .put("death_reason", deathReason.toString());
                if (killer instanceof ServerPlayerEntity serverKiller) {
                    event.target(serverKiller);
                }
                event.record();
            }
            return KillPlayer.KillResult.cancel();
        }

        if (killer instanceof ServerPlayerEntity serverKiller) {
            SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(serverKiller);
            if (saintComponent.isKarmaLocked()) {
                serverKiller.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, saintComponent.getKarmaLockTicks() / 20)), true);
                return KillPlayer.KillResult.cancel();
            }
        }

        return null;
    }

    /**
     * Handles saint-related logic in KillPlayer.AFTER (karma tracking on kill).
     */
    public static void handleAfterKill(PlayerEntity victim, PlayerEntity killer, GameWorldComponent gameComponent) {
        if (victim instanceof ServerPlayerEntity serverVictim
                && killer instanceof ServerPlayerEntity serverKiller
                && gameComponent.isRole(victim, Noellesroles.SAINT)
                && shouldTrackKarma(serverKiller, gameComponent)) {
            SaintPlayerComponent.KEY.get(serverKiller).markKarma();
            serverKiller.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    RegistryEntry.of(ModSounds.SAINT_BELL),
                    SoundCategory.PLAYERS,
                    serverKiller.getX(),
                    serverKiller.getY(),
                    serverKiller.getZ(),
                    5.0F,
                    1.0F,
                    serverKiller.getWorld().random.nextLong()
            ));
            serverKiller.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    RegistryEntry.of(ModSounds.SAINT_BELL),
                    SoundCategory.PLAYERS,
                    serverKiller.getX(),
                    serverKiller.getY(),
                    serverKiller.getZ(),
                    5.0F,
                    0.97F,
                    serverKiller.getWorld().random.nextLong()
            ));
            serverKiller.sendMessage(Text.translatable("tip.saint.karmic_debt"), true);
            GameRecordManager.event("saint_karma")
                    .actor(serverVictim)
                    .target(serverKiller)
                    .put("action", "marked")
                    .record();
        }

        if (killer instanceof ServerPlayerEntity serverKiller && shouldTrackKarma(serverKiller, gameComponent)) {
            tryTriggerKarmaLock(serverKiller, gameComponent);
        }
    }
}
