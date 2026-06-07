package org.agmas.noellesroles.client.jester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 客户端缓存当前是否有小丑处于"小丑时刻"，及其 UUID。
 * 每客户端 tick 刷新一次，供渲染 mixin / 高亮短路使用，避免渲染热路径重复扫描。
 */
public final class JesterMomentClient {
    private static UUID activeJesterUuid = null;

    private JesterMomentClient() {}

    public static void clientTick(MinecraftClient client) {
        activeJesterUuid = null;
        if (client.world == null) return;
        GameWorldComponent gwc = GameWorldComponent.KEY.get(client.world);
        for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
            if (gwc.isRole(p, Noellesroles.JESTER) && JesterPlayerComponent.KEY.get(p).inPsychoMode) {
                activeJesterUuid = p.getUuid();
                return;
            }
        }
    }

    public static boolean isActive() {
        return activeJesterUuid != null;
    }

    /**
     * 覆盖层（皮肤/球棒/握姿）是否应作用于【本地视角】。
     * 仅当：小丑时刻进行中 + 本地玩家存活在局中 + 本地玩家不是小丑本人。
     * 小丑本人、已死亡/旁观者看到的是真实世界（不受覆盖层影响）。
     */
    public static boolean isActiveForLocalViewer() {
        if (activeJesterUuid == null) return false;
        var self = MinecraftClient.getInstance().player;
        if (self == null) return false;
        if (!WatheClient.isPlayerPlayingAndAlive()) return false;
        return !self.getUuid().equals(activeJesterUuid);
    }

    public static @Nullable AbstractClientPlayerEntity getActiveJester(World world) {
        if (activeJesterUuid == null || world == null) return null;
        PlayerEntity p = world.getPlayerByUuid(activeJesterUuid);
        return (p instanceof AbstractClientPlayerEntity acp) ? acp : null;
    }
}
