package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.packets.Packet;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.WalkieTalkieItem;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;

import java.util.HashMap;
import java.util.List;
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
                if (gameWorldComponent.isRole(p, Noellesroles.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES) && GameFunctions.isPlayerPlayingAndAlive(p)) {
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
    public <T extends Packet> void blockVoiceToSwallowedPlayers(SoundPacketEvent<T> event) {
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

        boolean shouldBlock = isShouldBlockVoiceToSwallowed(speaker, speakerSwallowed, recipientSwallowed);

        // Cancel the packet if it should be blocked
        if (shouldBlock) {
            event.cancel();
        }
    }

    private static boolean isShouldBlockVoiceToSwallowed(ServerPlayerEntity speaker, SwallowedPlayerComponent speakerSwallowed, SwallowedPlayerComponent recipientSwallowed) {
        boolean shouldBlock = false;

        // Case 1: Speaker is a real spectator (not swallowed) - block
        if (speaker.isSpectator() && !speakerSwallowed.isSwallowed()) {
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
        return shouldBlock;
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
                if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
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

    /**
     * 大嗓门广播：广播期间将语音发送给所有玩家
     * 被沉默时不生效；死亡后不生效；被吞噬时不生效
     */
    public void noisemakerBroadcastEvent(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        if (event.getSenderConnection().getPlayer() == null) return;
        if (event.getSenderConnection().getPlayer().getPlayer() == null) return;

        VoicechatServerApi api = event.getVoicechat();
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();

        // 被沉默时广播不生效
        SilencedPlayerComponent silencedComp = SilencedPlayerComponent.KEY.get(speaker);
        if (silencedComp.isSilenced()) return;

        // 被吞噬时广播不生效
        if (SwallowedPlayerComponent.isPlayerSwallowed(speaker)) return;

        // 死亡后广播不生效
        if (!GameFunctions.isPlayerPlayingAndAlive(speaker)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(speaker.getWorld());
        if (!gameWorldComponent.isRole(speaker, Noellesroles.NOISEMAKER)) return;

        NoisemakerPlayerComponent noisemakerComp = NoisemakerPlayerComponent.KEY.get(speaker);
        if (!noisemakerComp.isBroadcasting()) return;

        // 广播语音给所有玩家（在接收者位置播放，相当于直接在耳边听到）
        for (ServerPlayerEntity receiver : speaker.getServer().getPlayerManager().getPlayerList()) {
            if (receiver.equals(speaker)) continue;
            VoicechatConnection receiverCon = api.getConnectionOf(receiver.getUuid());
            if (receiverCon != null) {
                api.sendLocationalSoundPacketTo(receiverCon, event.getPacket().locationalSoundPacketBuilder()
                        .position(api.createPosition(receiver.getX(), receiver.getY(), receiver.getZ()))
                        .distance((float) api.getVoiceChatDistance())
                        .build());
            }
        }
    }

    /**
     * Block voice packets from silenced players (they cannot speak)
     * Also block voice packets TO silenced players (they cannot hear)
     */
    public void silencerVoiceEvent(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        if (event.getSenderConnection().getPlayer() == null) return;
        if (event.getSenderConnection().getPlayer().getPlayer() == null) return;

        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        if (speaker == null) return;

        // Block silenced players from speaking
        SilencedPlayerComponent speakerSilenced = SilencedPlayerComponent.KEY.get(speaker);
        if (speakerSilenced.isSilenced()) {
            event.cancel();
        }
    }
    public <T extends Packet> void blockVoiceToSilencedPlayers(SoundPacketEvent<T> event) {
        if (event.getReceiverConnection() != null
                && event.getReceiverConnection().getPlayer() != null
                && event.getReceiverConnection().getPlayer().getPlayer() != null) {
            ServerPlayerEntity recipient = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
            if (recipient != null) {
                SilencedPlayerComponent recipientSilenced = SilencedPlayerComponent.KEY.get(recipient);
                if (recipientSilenced.isSilenced()) {
                    event.cancel();
                }
            }
        }
    }
    /**
     * 饕餮胃部对讲机共享：
     * 当有人通过对讲机说话时，如果某个饕餮的胃里有人持有同频道的对讲机，
     * 则胃里所有人（包括饕餮自己）都能听到。
     * 已经通过 TrainVoicePlugin 正常路径收到的玩家会被跳过，避免重复收听。
     */
    public void walkieTalkieStomachShareEvent(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null) return;
        if (connection.getPlayer() == null || connection.getPlayer().getPlayer() == null) return;

        ServerPlayerEntity speaker = (ServerPlayerEntity) connection.getPlayer().getPlayer();
        if (speaker == null || speaker.isSpectator()) return;

        // 检查说话者是否手持对讲机（主手或副手）
        ItemStack mainHand = speaker.getMainHandStack();
        ItemStack offHand = speaker.getOffHandStack();
        ItemStack heldWalkieTalkie = null;
        if (mainHand.getItem() instanceof WalkieTalkieItem) {
            heldWalkieTalkie = mainHand;
        } else if (offHand.getItem() instanceof WalkieTalkieItem) {
            heldWalkieTalkie = offHand;
        }
        if (heldWalkieTalkie == null) return;

        WalkieTalkieComponent comp = heldWalkieTalkie.getOrDefault(
                WatheDataComponentTypes.WALKIE_TALKIE, WalkieTalkieComponent.DEFAULT);
        int senderChannel = comp.channel();
        byte[] encodedData = event.getPacket().getOpusEncodedData();

        // 遍历所有玩家，检查是否有饕餮的胃里存在同频对讲机
        for (ServerPlayerEntity potentialTaotie : speaker.getServer().getPlayerManager().getPlayerList()) {
            TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(potentialTaotie);
            List<UUID> stomachPlayers = taotieComp.getSwallowedPlayers();
            if (stomachPlayers.isEmpty()) continue;

            // 检查胃里是否有人持有同频道的对讲机
            boolean stomachHasChannel = false;
            for (UUID swallowedUuid : stomachPlayers) {
                PlayerEntity swallowed = potentialTaotie.getWorld().getPlayerByUuid(swallowedUuid);
                if (swallowed != null && TrainVoicePlugin.isReceivingChannel(swallowed, senderChannel)) {
                    stomachHasChannel = true;
                    break;
                }
            }
            if (!stomachHasChannel) continue;

            // 转发给饕餮本人（如果不是说话者、存活、且自己没有同频对讲机）
            if (potentialTaotie != speaker
                    && GameFunctions.isPlayerPlayingAndAlive(potentialTaotie)
                    && !TrainVoicePlugin.isReceivingChannel(potentialTaotie, senderChannel)) {
                VoicechatConnection taotieCon = api.getConnectionOf(potentialTaotie.getUuid());
                if (taotieCon != null) {
                    api.sendLocationalSoundPacketTo(taotieCon,
                            event.getPacket().locationalSoundPacketBuilder()
                                    .opusEncodedData(encodedData)
                                    .position(api.createPosition(
                                            potentialTaotie.getX(), potentialTaotie.getY(), potentialTaotie.getZ()))
                                    .distance(8f)
                                    .category(TrainVoicePlugin.WALKIE_TALKIE_CATEGORY)
                                    .build());
                }
            }

            // 转发给胃里没有同频对讲机的被吞玩家
            for (UUID swallowedUuid : stomachPlayers) {
                if (swallowedUuid.equals(speaker.getUuid())) continue;
                PlayerEntity swallowed = potentialTaotie.getWorld().getPlayerByUuid(swallowedUuid);
                if (swallowed == null) continue;
                if (TrainVoicePlugin.isReceivingChannel(swallowed, senderChannel)) continue; // 已通过正常路径收到

                VoicechatConnection swallowedCon = api.getConnectionOf(swallowedUuid);
                if (swallowedCon != null) {
                    api.sendLocationalSoundPacketTo(swallowedCon,
                            event.getPacket().locationalSoundPacketBuilder()
                                    .opusEncodedData(encodedData)
                                    .position(api.createPosition(
                                            swallowed.getX(), swallowed.getY(), swallowed.getZ()))
                                    .distance(8f)
                                    .category(TrainVoicePlugin.WALKIE_TALKIE_CATEGORY)
                                    .build());
                }
            }
        }
    }

    /**
     * 通灵者灵魂出窍时：阻止接收他人语音（听不到别人说话）
     */
    public <T extends Packet> void blockVoiceToProjectingSpiritualist(SoundPacketEvent<T> event) {
        if (event.getReceiverConnection() == null
                || event.getReceiverConnection().getPlayer() == null
                || event.getReceiverConnection().getPlayer().getPlayer() == null) return;

        ServerPlayerEntity recipient = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
        GameWorldComponent gameComp = GameWorldComponent.KEY.get(recipient.getWorld());
        if (gameComp.isRole(recipient, Noellesroles.SPIRIT_WALKER)) {
            SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(recipient);
            if (spiritComp.isProjecting()) {
                event.cancel();
            }
        }
    }

    /**
     * 通灵者灵魂出窍时：阻止发送语音（别人也听不到通灵者说话）
     */
    public void blockSpiritVoice(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null
                || event.getSenderConnection().getPlayer() == null
                || event.getSenderConnection().getPlayer().getPlayer() == null) return;

        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        GameWorldComponent gameComp = GameWorldComponent.KEY.get(speaker.getWorld());
        if (gameComp.isRole(speaker, Noellesroles.SPIRIT_WALKER)) {
            SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(speaker);
            if (spiritComp.isProjecting()) {
                event.cancel();
            }
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::paranoidEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::taotieVoiceEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::silencerVoiceEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::noisemakerBroadcastEvent);
        registration.registerEvent(MicrophonePacketEvent.class, this::walkieTalkieStomachShareEvent);
        registration.registerEvent(RemoveGroupEvent.class, this::onGroupRemoved);

        registration.registerEvent(EntitySoundPacketEvent.class, this::blockVoiceToSwallowedPlayers);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockVoiceToSwallowedPlayers);

        registration.registerEvent(EntitySoundPacketEvent.class, this::blockVoiceToSilencedPlayers);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockVoiceToSilencedPlayers);

        registration.registerEvent(EntitySoundPacketEvent.class, this::blockVoiceToProjectingSpiritualist);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockVoiceToProjectingSpiritualist);
        registration.registerEvent(MicrophonePacketEvent.class, this::blockSpiritVoice);

        VoicechatPlugin.super.registerEvents(registration);
    }
}
