package org.agmas.noellesroles.serialkiller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 连环杀手玩家组件
 * 管理透视目标和击杀奖励
 */
public class SerialKillerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SerialKillerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "serial_killer"), SerialKillerPlayerComponent.class);

    private final PlayerEntity player;

    // 当前透视目标的 UUID
    private UUID currentTarget;

    // 额外金钱奖励
    private static final int BONUS_MONEY = 100;

    // 每隔多少 tick 检查一次目标有效性
    private static final int CHECK_INTERVAL = 20;
    private int checkTimer = 0;

    public SerialKillerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.currentTarget = null;
        this.checkTimer = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void serverTick() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);
        if (!gameWorldComponent.isRole(player, Noellesroles.SERIAL_KILLER)) return;

        if (++checkTimer < CHECK_INTERVAL) return;
        checkTimer = 0;

        if (currentTarget != null) {
            // 检查当前目标是否仍然有效
            if (isTargetValid(currentTarget, gameWorldComponent, serverWorld)) return;

            // 目标失效，寻找新目标
            reassignTarget(gameWorldComponent, serverWorld);
        } else {
            // 没有目标，尝试寻找一个
            assignTarget(gameWorldComponent, serverWorld);
        }
    }

    /**
     * 检查目标是否仍然有效（存活、未被吞噬、非杀手阵营）
     */
    private boolean isTargetValid(UUID targetUuid, GameWorldComponent gameWorldComponent, ServerWorld serverWorld) {
        if (targetUuid.equals(player.getUuid())) return false;

        PlayerEntity targetPlayer = serverWorld.getPlayerByUuid(targetUuid);
        if (targetPlayer == null) return false;
        if (!GameFunctions.isPlayerPlayingAndAlive(targetPlayer) || SwallowedPlayerComponent.isPlayerSwallowed(targetPlayer)) return false;

        var role = gameWorldComponent.getRole(targetPlayer);
        if (role == null || role == Noellesroles.UNDERCOVER) return false;
        if (role.canUseKiller()) return false;

        return true;
    }

    /**
     * 尝试分配一个新目标（无前一个目标时）
     */
    private void assignTarget(GameWorldComponent gameWorldComponent, ServerWorld serverWorld) {
        List<UUID> eligibleTargets = getEligibleTargets(gameWorldComponent, serverWorld);
        if (eligibleTargets.isEmpty()) return;

        Random random = new Random();
        this.currentTarget = eligibleTargets.get(random.nextInt(eligibleTargets.size()));
        this.sync();

        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayerEntity targetPlayer = serverWorld.getServer().getPlayerManager().getPlayer(this.currentTarget);
            NbtCompound extra = new NbtCompound();
            extra.putString("action", "assign");
            GameRecordManager.recordSkillUse(serverPlayer, Noellesroles.SERIAL_KILLER_ID, targetPlayer, extra);
        }
    }

    /**
     * 目标失效后重新分配新目标
     */
    private void reassignTarget(GameWorldComponent gameWorldComponent, ServerWorld serverWorld) {
        UUID previousTarget = this.currentTarget;
        List<UUID> eligibleTargets = getEligibleTargets(gameWorldComponent, serverWorld);

        if (!eligibleTargets.isEmpty()) {
            Random random = new Random();
            this.currentTarget = eligibleTargets.get(random.nextInt(eligibleTargets.size()));
            this.sync();

            if (player instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayerEntity newTargetPlayer = serverWorld.getServer().getPlayerManager().getPlayer(this.currentTarget);
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "reassign");
                if (previousTarget != null) {
                    extra.putUuid("previous_target", previousTarget);
                }
                GameRecordManager.recordSkillUse(serverPlayer, Noellesroles.SERIAL_KILLER_ID, newTargetPlayer, extra);
            }
        } else {
            this.currentTarget = null;
            this.sync();
        }
    }

    /**
     * 在游戏开始时随机选择一个非杀手阵营的玩家作为目标
     * @param gameWorldComponent 游戏世界组件
     */
    public void initializeTarget(GameWorldComponent gameWorldComponent) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        assignTarget(gameWorldComponent, serverWorld);
    }

    /**
     * 当目标死亡时自动更换目标
     * @param gameWorldComponent 游戏世界组件
     */
    public void onTargetDeath(GameWorldComponent gameWorldComponent) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        reassignTarget(gameWorldComponent, serverWorld);
    }

    /**
     * 获取可以成为目标的玩家列表（非杀手阵营的存活玩家）
     */
    private List<UUID> getEligibleTargets(GameWorldComponent gameWorldComponent, ServerWorld serverWorld) {
        List<UUID> eligibleTargets = new ArrayList<>();

        for (UUID playerUuid : gameWorldComponent.getAllPlayers()) {
            if (playerUuid.equals(player.getUuid())) continue; // 不能选择自己
            if (playerUuid.equals(currentTarget)) continue; // 排除当前（已失效的）目标

            PlayerEntity targetPlayer = serverWorld.getPlayerByUuid(playerUuid);
            if (targetPlayer == null) continue;
            if (!GameFunctions.isPlayerPlayingAndAlive(targetPlayer) || SwallowedPlayerComponent.isPlayerSwallowed(targetPlayer)) continue;


            // 检查是否为非杀手阵营
            var role = gameWorldComponent.getRole(targetPlayer);
            if (role == null || role == Noellesroles.UNDERCOVER) continue;

            // 排除杀手阵营角色
            if (role.canUseKiller()) continue;

            eligibleTargets.add(playerUuid);
        }

        return eligibleTargets;
    }

    /**
     * 获取当前透视目标
     * @return 目标玩家的 UUID，如果没有目标则返回 null
     */
    public UUID getCurrentTarget() {
        return this.currentTarget;
    }

    /**
     * 检查是否有透视目标
     */
    public boolean hasTarget() {
        return this.currentTarget != null;
    }

    /**
     * 检查指定玩家是否是当前透视目标
     * @param playerUuid 要检查的玩家 UUID
     */
    public boolean isCurrentTarget(UUID playerUuid) {
        return this.currentTarget != null && this.currentTarget.equals(playerUuid);
    }

    /**
     * 获取额外金钱奖励金额
     */
    public static int getBonusMoney() {
        return BONUS_MONEY;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.currentTarget != null) {
            tag.putUuid("currentTarget", this.currentTarget);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("currentTarget")) {
            this.currentTarget = tag.getUuid("currentTarget");
        } else {
            this.currentTarget = null;
        }
    }
}
