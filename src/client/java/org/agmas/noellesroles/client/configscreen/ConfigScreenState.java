package org.agmas.noellesroles.client.configscreen;

import dev.doctor4t.wathe.WatheConfig;
import org.agmas.noellesroles.config.NoellesRolesConfig;

public class ConfigScreenState {
    private final NoellesRolesConfig noellesRolesConfig;
    private WatheConfig.InstinctModeConfig instinctMode;
    private int chatHistoryLimit;
    private boolean showMatchPlayerCount;

    public static ConfigScreenState defaults() {
        return new ConfigScreenState(
                new NoellesRolesConfig(),
                WatheConfig.InstinctModeConfig.HOLD,
                500,
                true
        );
    }

    private ConfigScreenState(
            NoellesRolesConfig noellesRolesConfig,
            WatheConfig.InstinctModeConfig instinctMode,
            int chatHistoryLimit,
            boolean showMatchPlayerCount
    ) {
        this.noellesRolesConfig = noellesRolesConfig;
        this.instinctMode = instinctMode;
        this.chatHistoryLimit = chatHistoryLimit;
        this.showMatchPlayerCount = showMatchPlayerCount;
    }

    public static ConfigScreenState capture() {
        return new ConfigScreenState(
                NoellesRolesConfig.HANDLER.instance().copy(),
                WatheConfig.instinctMode,
                WatheConfig.chatHistoryLimit,
                WatheConfig.showMatchPlayerCount
        );
    }

    public ConfigScreenState copy() {
        return new ConfigScreenState(
                noellesRolesConfig.copy(),
                instinctMode,
                chatHistoryLimit,
                showMatchPlayerCount
        );
    }

    public void apply(boolean includeRestrictedSettings) {
        if (!includeRestrictedSettings) {
            NoellesRolesConfig savedConfig = NoellesRolesConfig.HANDLER.instance();
            noellesRolesConfig.insanePlayersSeeMorphs = savedConfig.insanePlayersSeeMorphs;
            noellesRolesConfig.generalCooldownTicks = savedConfig.generalCooldownTicks;
            noellesRolesConfig.voodooNonKillerDeaths = savedConfig.voodooNonKillerDeaths;
            noellesRolesConfig.showFogRadiusHud = savedConfig.showFogRadiusHud;
            noellesRolesConfig.showHallucinationHud = savedConfig.showHallucinationHud;
            noellesRolesConfig.lockSoundPhysicsRemasteredConfig = savedConfig.lockSoundPhysicsRemasteredConfig;
            noellesRolesConfig.soundPhysicsRemasteredLockedValues = savedConfig.soundPhysicsRemasteredLockedValues;
            noellesRolesConfig.lockTalkBubblesConfig = savedConfig.lockTalkBubblesConfig;
            noellesRolesConfig.talkBubblesLockedValues = savedConfig.talkBubblesLockedValues;
        }

        NoellesRolesConfig.HANDLER.instance().copyFrom(noellesRolesConfig);
        NoellesRolesConfig.HANDLER.save();

        WatheConfig.instinctMode = instinctMode;
        WatheConfig.chatHistoryLimit = chatHistoryLimit;
        WatheConfig.showMatchPlayerCount = showMatchPlayerCount;
        writeWatheConfig();
    }

    private static void writeWatheConfig() {
        try {
            Class<?> midnightConfigClass = Class.forName("eu.midnightdust.lib.config.MidnightConfig");
            midnightConfigClass.getMethod("write", String.class).invoke(null, "wathe");
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to write Wathe config", exception);
        }
    }

    public NoellesRolesConfig noellesRolesConfig() {
        return noellesRolesConfig;
    }

    public WatheConfig.InstinctModeConfig instinctMode() {
        return instinctMode;
    }

    public void instinctMode(WatheConfig.InstinctModeConfig instinctMode) {
        this.instinctMode = instinctMode;
    }

    public int chatHistoryLimit() {
        return chatHistoryLimit;
    }

    public void chatHistoryLimit(int chatHistoryLimit) {
        this.chatHistoryLimit = Math.max(WatheConfig.MIN_CHAT_HISTORY_LIMIT, Math.min(WatheConfig.MAX_CHAT_HISTORY_LIMIT, chatHistoryLimit));
    }

    public boolean showMatchPlayerCount() {
        return showMatchPlayerCount;
    }

    public void showMatchPlayerCount(boolean showMatchPlayerCount) {
        this.showMatchPlayerCount = showMatchPlayerCount;
    }
}
