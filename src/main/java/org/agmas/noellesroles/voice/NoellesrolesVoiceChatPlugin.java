package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.RemoveGroupEvent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoellesrolesVoiceChatPlugin implements VoicechatPlugin {
    // Static reference to the server API for use by other classes
    private static VoicechatServerApi serverApi = null;

    // Map of Taotie UUID to their stomach group
    private static final Map<UUID, Group> taotieStomachGroups = new HashMap<>();

    public static VoicechatServerApi getServerApi() {
        return serverApi;
    }

    /**
     * Get or create a stomach group for a Taotie player
     * @param taotie The Taotie player
     * @return The stomach group for this Taotie
     */
    public static Group getOrCreateStomachGroup(ServerPlayerEntity taotie) {
        if (serverApi == null) return null;

        UUID taotieUuid = taotie.getUuid();

        // Return existing group if valid
        Group existing = taotieStomachGroups.get(taotieUuid);
        if (existing != null) {
            return existing;
        }

        // Create new group with player name (max 16 chars, perfect for Minecraft usernames)
        String groupName = "tts-" + taotie.getName().getString();
        Group newGroup = serverApi.groupBuilder()
                .setPersistent(false)
                .setName(groupName)
                .setType(Group.Type.NORMAL)
                .setHidden(true)
                .build();

        taotieStomachGroups.put(taotieUuid, newGroup);
        return newGroup;
    }

    /**
     * Add a swallowed player to a Taotie's stomach group
     * @param taotie The Taotie player
     * @param swallowedPlayer The player being swallowed
     */
    public static void addToStomachGroup(ServerPlayerEntity taotie, ServerPlayerEntity swallowedPlayer) {
        if (serverApi == null) return;

        Group stomachGroup = getOrCreateStomachGroup(taotie);
        if (stomachGroup == null) return;

        VoicechatConnection connection = serverApi.getConnectionOf(swallowedPlayer.getUuid());
        if (connection != null) {
            connection.setGroup(stomachGroup);
        }
    }

    /**
     * Remove a player from voice chat groups (set to null)
     * @param playerUuid The UUID of the player to remove
     */
    public static void removeFromVoiceChat(UUID playerUuid) {
        if (serverApi == null) return;

        VoicechatConnection connection = serverApi.getConnectionOf(playerUuid);
        if (connection != null) {
            connection.setGroup(null);
        }
    }

    /**
     * Clear a Taotie's stomach group (called when all players are released)
     * @param taotieUuid The UUID of the Taotie
     */
    public static void clearStomachGroup(UUID taotieUuid) {
        taotieStomachGroups.remove(taotieUuid);
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
     * Block voice packets to swallowed players from:
     * 1. Real spectators (not swallowed)
     * 2. Players swallowed by other Taotie
     *
     * Swallowed players should only hear:
     * - Players swallowed by the same Taotie
     * - The Taotie who swallowed them
     */
    public void blockVoiceToSwallowedPlayers(MicrophonePacketEvent event) {
        // Get recipient (the player who would receive this sound packet)
        if (event.getReceiverConnection() == null || event.getSenderConnection() == null){
            return;
        }
        ServerPlayerEntity recipient = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
        SwallowedPlayerComponent recipientSwallowed = SwallowedPlayerComponent.KEY.get(recipient);

        // Only process if recipient is swallowed
        if (!recipientSwallowed.isSwallowed()) {
            return;
        }

        // Get speaker
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        SwallowedPlayerComponent speakerSwallowed = SwallowedPlayerComponent.KEY.get(speaker);

        boolean shouldBlock = false;

        // Case 1: Speaker is a real spectator (not swallowed) - block
        if (speaker.interactionManager.getGameMode().equals(GameMode.SPECTATOR) && !speakerSwallowed.isSwallowed()) {
            shouldBlock = true;
        }

        // Case 2: Speaker is swallowed by a different Taotie - block
        if (speakerSwallowed.isSwallowed()) {
            UUID recipientTaotie = recipientSwallowed.getSwallowedBy();
            UUID speakerTaotie = speakerSwallowed.getSwallowedBy();
            if (recipientTaotie != null && speakerTaotie != null && !recipientTaotie.equals(speakerTaotie)) {
                shouldBlock = true;
            }
        }

        // Cancel the packet if it should be blocked
        if (shouldBlock) {
            event.cancel();
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

    /**
     * Handle VoiceChat group removal events
     * When a Taotie stomach group is auto-deleted (no members), clear the reference
     */
    public void onGroupRemoved(RemoveGroupEvent event) {
        Group removedGroup = event.getGroup();
        if (removedGroup == null) return;

        // Remove from our map if it exists
        taotieStomachGroups.entrySet().removeIf(entry -> entry.getValue().equals(removedGroup));
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::paranoidEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::taotieVoiceEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::blockVoiceToSwallowedPlayers);
        registration.registerEvent(RemoveGroupEvent.class, this::onGroupRemoved);
        VoicechatPlugin.super.registerEvents(registration);
    }
}
