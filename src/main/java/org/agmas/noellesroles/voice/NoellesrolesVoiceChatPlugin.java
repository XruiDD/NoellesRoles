package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.UUID;

public class NoellesrolesVoiceChatPlugin implements VoicechatPlugin {
    // Static reference to the server API for use by other classes
    private static VoicechatServerApi serverApi = null;

    public static VoicechatServerApi getServerApi() {
        return serverApi;
    }

    @Override
    public String getPluginId() {
        return Noellesroles.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApiInstance) {
            serverApi = serverApiInstance;
        }
        VoicechatPlugin.super.initialize(api);
    }

    public void paranoidEvent(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity spectator = ((ServerPlayerEntity)event.getSenderConnection().getPlayer().getPlayer());
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(spectator.getWorld());
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(spectator);
        if (spectator.interactionManager.getGameMode().equals(GameMode.SPECTATOR) && !swallowedComp.isSwallowed()) {
            spectator.getWorld().getPlayers().forEach((p) -> {
                if (gameWorldComponent.isRole(p, Noellesroles.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES) && GameFunctions.isPlayerAliveAndSurvival(p)) {
                    if (spectator.distanceTo(p) <= api.getVoiceChatDistance()) {
                        VoicechatConnection con = api.getConnectionOf(p.getUuid());
                        api.sendLocationalSoundPacketTo(con, event.getPacket().locationalSoundPacketBuilder()
                                        .position(api.createPosition(p.getX(), p.getY(), p.getZ()))
                                        .distance((float)api.getVoiceChatDistance())
                                        .build());
                    }
                }
            });
        }
    }

    /**
     * Taotie voice system:
     * - Swallowed players are in an ISOLATED Group, can talk to each other, outsiders can't hear
     * - Forward swallowed player voice to Taotie
     * - Forward Taotie voice to swallowed players
     */
    public void taotieVoiceEvent(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        //Swallowed player speaks -> forward to Taotie
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(speaker);
        if (swallowedComp.isSwallowed()) {
            UUID taotieUuid = swallowedComp.getSwallowedBy();
            if (taotieUuid != null) {
                PlayerEntity taotie = speaker.getWorld().getPlayerByUuid(taotieUuid);
                if (taotie != null && GameFunctions.isPlayerAliveAndSurvival(taotie)) {
                    VoicechatConnection taotieCon = api.getConnectionOf(taotieUuid);
                    if (taotieCon != null) {
                        // Forward voice to Taotie at Taotie's position
                        api.sendLocationalSoundPacketTo(taotieCon, event.getPacket()
                                .locationalSoundPacketBuilder()
                                .position(api.createPosition(taotie.getX(), taotie.getY(), taotie.getZ()))
                                .distance((float) api.getVoiceChatDistance())
                                .build());
                    }
                }
            }
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::paranoidEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::taotieVoiceEvent);
        VoicechatPlugin.super.registerEvents(registration);
    }
}
