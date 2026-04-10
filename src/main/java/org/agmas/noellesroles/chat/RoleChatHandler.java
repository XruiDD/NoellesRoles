package org.agmas.noellesroles.chat;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.packet.RoleBroadcastS2CPacket;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;

import java.util.UUID;

/**
 * 职业聊天处理器（服务端）
 *
 * 处理大嗓门、饕餮、静语者的聊天消息拦截与广播逻辑
 *
 * 优先级（从高到低）：
 * 1. 静语者 - 被静语的玩家消息被拦截，不广播
 * 2. 饕餮肚子 - 肚子里的人消息只广播给饕餮和肚子里的人
 * 3. 大嗓门广播 - 技能期间消息拦截并广播给所有人
 */
public class RoleChatHandler {

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            ServerWorld world = sender.getServerWorld();
            GameWorldComponent gwc = GameWorldComponent.KEY.get(world);

            // 仅在游戏运行中处理
            if (!gwc.isRunning()) return true;

            // 仅处理有角色的玩家
            if (gwc.getRole(sender.getUuid()) == null) return true;

            String chatMessage = message.getContent().getString();

            // 优先级1：被静语检查（最高优先级）
            if (SilencedPlayerComponent.isPlayerSilenced(sender)) {
                return false;
            }

            // 优先级2：饕餮肚子检查
            SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(sender);
            if (swallowed.isSwallowed()) {
                // 在饕餮肚子里 - 广播给肚子里的人
                broadcastToStomachGroup(sender, chatMessage, world, gwc, swallowed);
                return true; // 不拦截原版聊天（游戏中聊天框本身已被隐藏）
            }

            // 检查发送者是否是饕餮且有被吞的玩家
            if (gwc.isRole(sender, Noellesroles.TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(sender);
                if (!taotieComp.getSwallowedPlayers().isEmpty()) {
                    broadcastToStomachGroup(sender, chatMessage, world, gwc, swallowed);
                    return true; // 不拦截原版聊天
                }
            }

            // 优先级3：大嗓门广播（仅在不在肚子里时生效）
            if (gwc.isRole(sender, Noellesroles.NOISEMAKER)) {
                NoisemakerPlayerComponent noisemakerComp = NoisemakerPlayerComponent.KEY.get(sender);
                if (noisemakerComp.isBroadcasting()) {
                    broadcastToAll(sender, chatMessage, world, gwc);
                    return false; // 拦截原版聊天
                }
            }

            return true; // 正常处理
        });
    }

    /**
     * 大嗓门广播：发送给同一世界中所有玩家（排除被静语的玩家）
     * 格式：[角色名]:消息内容
     */
    private static void broadcastToAll(ServerPlayerEntity sender, String chatMessage,
                                        ServerWorld world, GameWorldComponent gwc) {
        String roleName = getLocalizedRoleName(sender, gwc);
        String formatted = "[" + roleName + "]:" + chatMessage;

        for (ServerPlayerEntity receiver : world.getPlayers()) {
            // 不发送给被静语的玩家
            if (SilencedPlayerComponent.isPlayerSilenced(receiver)) continue;

            ServerPlayNetworking.send(receiver, new RoleBroadcastS2CPacket(formatted));
        }
    }

    /**
     * 饕餮肚子广播：仅发送给饕餮和肚子里的玩家（排除发送者自身，避免重复显示）
     * 格式：[饕餮角色名]玩家名:消息内容
     */
    private static void broadcastToStomachGroup(ServerPlayerEntity sender, String chatMessage,
                                                 ServerWorld world, GameWorldComponent gwc,
                                                 SwallowedPlayerComponent senderSwallowed) {
        // 找到饕餮的UUID
        UUID taotieUuid;
        if (senderSwallowed.isSwallowed()) {
            taotieUuid = senderSwallowed.getSwallowedBy();
        } else if (gwc.isRole(sender, Noellesroles.TAOTIE)) {
            taotieUuid = sender.getUuid();
        } else {
            return;
        }

        // 获取饕餮玩家
        PlayerEntity taotieEntity = world.getPlayerByUuid(taotieUuid);
        if (!(taotieEntity instanceof ServerPlayerEntity taotiePlayer)) return;

        TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotiePlayer);

        // 获取饕餮的角色名
        String taotieRoleName = getLocalizedRoleName(taotiePlayer, gwc);
        // 格式：[饕餮角色名]发送者玩家名:消息内容
        String formatted = "[" + taotieRoleName + "]" + sender.getName().getString() + ":" + chatMessage;

        UUID senderUuid = sender.getUuid();

        // 发送给饕餮（如果不是发送者自身，且没被静语）
        if (!taotieUuid.equals(senderUuid) && !SilencedPlayerComponent.isPlayerSilenced(taotiePlayer)) {
            ServerPlayNetworking.send(taotiePlayer, new RoleBroadcastS2CPacket(formatted));
        }

        // 发送给所有被吞噬的玩家（排除发送者自身，且排除被静语的）
        for (UUID uuid : taotieComp.getSwallowedPlayers()) {
            if (uuid.equals(senderUuid)) continue;
            PlayerEntity swallowedEntity = world.getPlayerByUuid(uuid);
            if (swallowedEntity instanceof ServerPlayerEntity swallowedPlayer) {
                if (!SilencedPlayerComponent.isPlayerSilenced(swallowedPlayer)) {
                    ServerPlayNetworking.send(swallowedPlayer, new RoleBroadcastS2CPacket(formatted));
                }
            }
        }
    }

    /**
     * 获取玩家角色的本地化名称
     */
    private static String getLocalizedRoleName(ServerPlayerEntity player, GameWorldComponent gwc) {
        var role = gwc.getRole(player.getUuid());
        if (role != null) {
            return Text.translatable("announcement.role." + role.identifier().getPath()).getString();
        }
        return "???";
    }
}
