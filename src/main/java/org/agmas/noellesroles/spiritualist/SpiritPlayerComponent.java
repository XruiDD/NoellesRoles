package org.agmas.noellesroles.spiritualist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 灵界行者玩家组件
 * 存储灵魂出窍状态和本体坐标，自动同步到客户端
 * 服务端 tick 自动检测异常状态并强制取消出窍
 */
public class SpiritPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SpiritPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "spirit"),
            SpiritPlayerComponent.class
    );

    /** 本体被移动超过此距离（格）视为被传送，强制取消出窍 */
    private static final double TELEPORT_THRESHOLD_SQ = 2.0; // 1格

    private final PlayerEntity player;
    private boolean projecting = false;
    private double bodyX, bodyY, bodyZ;

    public SpiritPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.projecting = false;
        this.bodyX = 0;
        this.bodyY = 0;
        this.bodyZ = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public boolean isProjecting() {
        return projecting;
    }

    public void startProjecting() {
        this.projecting = true;
        this.bodyX = player.getX();
        this.bodyY = player.getY();
        this.bodyZ = player.getZ();
        this.sync();
    }

    public void stopProjecting() {
        this.projecting = false;
        this.sync();
    }

    /**
     * 强制取消灵魂出窍并设冷却，记录回放事件
     * @param reason 取消原因，用于回放文本（"killed", "teleported", "swallowed", "game_end"）
     */
    public void cancelProjection(String reason) {
        if (!this.projecting) return;
        this.projecting = false;
        AbilityPlayerComponent abilityComp = AbilityPlayerComponent.KEY.get(this.player);
        abilityComp.setCooldown(GameConstants.getInTicks(1, 0));

        if (player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            extra.putString("action", "forced_return");
            extra.putString("reason", reason);
            GameRecordManager.recordSkillUse(serverPlayer, Noellesroles.SPIRIT_WALKER_ID, null, extra);
        }

        this.sync();
    }

    /** 无原因的取消（向后兼容） */
    public void cancelProjection() {
        cancelProjection("unknown");
    }

    public double getBodyX() { return bodyX; }
    public double getBodyY() { return bodyY; }
    public double getBodyZ() { return bodyZ; }

    /**
     * 服务端每 tick 检测：灵魂出窍中本体是否发生异常
     * - 本体被传送（位置偏移超过阈值）
     * - 本体被饕餮吞噬
     * - 本体死亡/不再存活
     * - 游戏不再运行
     * - 角色不再是灵界行者
     */
    @Override
    public void serverTick() {
        if (!projecting) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // 玩家不再存活
        if (!GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            cancelProjection("killed");
            return;
        }

        // 游戏不在运行或角色变了
        GameWorldComponent gameComp = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameComp.isRunning() || !gameComp.isRole(player, Noellesroles.SPIRIT_WALKER)) {
            cancelProjection("game_end");
            return;
        }

        // 被饕餮吞噬
        if (SwallowedPlayerComponent.isPlayerSwallowed(serverPlayer)) {
            cancelProjection("swallowed");
            return;
        }

        // 本体被传送（位置偏移超过阈值）
        double dx = player.getX() - bodyX;
        double dy = player.getY() - bodyY;
        double dz = player.getZ() - bodyZ;
        if (dx * dx + dy * dy + dz * dz > TELEPORT_THRESHOLD_SQ) {
            cancelProjection("teleported");
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.projecting);
        buf.writeDouble(this.bodyX);
        buf.writeDouble(this.bodyY);
        buf.writeDouble(this.bodyZ);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.projecting = buf.readBoolean();
        this.bodyX = buf.readDouble();
        this.bodyY = buf.readDouble();
        this.bodyZ = buf.readDouble();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("projecting", this.projecting);
        tag.putDouble("bodyX", this.bodyX);
        tag.putDouble("bodyY", this.bodyY);
        tag.putDouble("bodyZ", this.bodyZ);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.projecting = tag.contains("projecting") && tag.getBoolean("projecting");
        this.bodyX = tag.contains("bodyX") ? tag.getDouble("bodyX") : 0;
        this.bodyY = tag.contains("bodyY") ? tag.getDouble("bodyY") : 0;
        this.bodyZ = tag.contains("bodyZ") ? tag.getDouble("bodyZ") : 0;
    }
}
