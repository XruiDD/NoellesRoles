package org.agmas.noellesroles.corruptcop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.CorruptCopMomentS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;

import java.util.UUID;

/**
 * 黑警玩家组件
 * 管理黑警时刻状态
 */
public class CorruptCopPlayerComponent implements Component, ClientTickingComponent {
    public static final ComponentKey<CorruptCopPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "corrupt_cop"), CorruptCopPlayerComponent.class);

    private final PlayerEntity player;

    // 黑警时刻是否激活
    private boolean corruptCopMomentActive = false;
    // 触发黑警时刻的人数阈值
    private int triggerThreshold = 0;

    // 透视循环计时器（客户端）
    // 周期：20秒不能透视 + 10秒能透视 = 30秒循环
    private int visionCycleTimer = 0;
    private static final int VISION_OFF_DURATION = 20 * 20; // 20秒 = 400 ticks
    private static final int VISION_ON_DURATION = 10 * 20;  // 10秒 = 200 ticks
    private static final int VISION_CYCLE_TOTAL = VISION_OFF_DURATION + VISION_ON_DURATION; // 600 ticks

    private static final int MOMENT_GUN_COOLDOWN = GameConstants.getInTicks(0, 2); // 2秒

    public CorruptCopPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        endCorruptCopMoment();
        this.corruptCopMomentActive = false;
        this.triggerThreshold = 0;
        this.visionCycleTimer = 0;
    }

    /**
     * 在游戏开始时初始化
     * @param totalPlayers 总玩家数
     */
    public void initializeForGame(int totalPlayers) {
        // n = 玩家数 / 5，最低触发条件是10人局（n=2）
        this.triggerThreshold = totalPlayers / 5;
        this.corruptCopMomentActive = false;
    }

    /**
     * 检查是否应该触发黑警时刻
     *
     * @param currentAliveCount 当前存活人数
     */
    public void checkAndTriggerMoment(int currentAliveCount) {
        if (corruptCopMomentActive) {
            return;
        }

        if (currentAliveCount <= triggerThreshold && triggerThreshold >= 2) {
            triggerCorruptCopMoment();
        }
    }

    /**
     * 触发黑警时刻
     */
    private void triggerCorruptCopMoment() {
        this.corruptCopMomentActive = true;

        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // 给黑警撬棍
        boolean hasCrowbar = false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(WatheItems.CROWBAR)) {
                hasCrowbar = true;
                break;
            }
        }
        if (!hasCrowbar) {
            player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
        }

        // 向黑警发送提示
        player.sendMessage(Text.translatable("tip.corrupt_cop.moment_triggered")
                .formatted(Formatting.DARK_RED, Formatting.BOLD), false);

        // 向所有玩家广播黑警时刻开始
        for (var player : player.getWorld().getPlayers()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // 发送BGM播放包
                ServerPlayNetworking.send(serverPlayer, new CorruptCopMomentS2CPacket(true));
            }
        }
    }

    /**
     * 结束黑警时刻
     */
    public void endCorruptCopMoment() {
        if (!corruptCopMomentActive) return;

        this.corruptCopMomentActive = false;

        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // 向所有玩家发送停止BGM的包
        for (var player : player.getWorld().getPlayers()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new CorruptCopMomentS2CPacket(false));
            }
        }
    }

    /**
     * 黑警击杀时调用
     */
    public void onKill() {
        if (!corruptCopMomentActive) return;
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        // 播放处决音效（全场）
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
        for (UUID playerUuid : gameComponent.getAllPlayers()) {
            PlayerEntity p = serverWorld.getPlayerByUuid(playerUuid);
            if (p instanceof ServerPlayerEntity serverPlayer) {
                serverWorld.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        ModSounds.CORRUPT_COP_EXECUTION, SoundCategory.MASTER, 1.0F, 1.0F);
            }
        }
    }

    /**
     * 获取黑警时刻的枪冷却时间
     */
    public int getGunCooldown() {
        if (corruptCopMomentActive) {
            return MOMENT_GUN_COOLDOWN;
        }
        // 返回-1表示使用默认冷却
        return -1;
    }

    public boolean isCorruptCopMomentActive() {
        return corruptCopMomentActive;
    }

    /**
     * 检查黑警是否处于透视阶段
     * 周期：20秒不能透视 -> 10秒能透视 -> 循环
     */
    public boolean canSeePlayersThroughWalls() {
        if (!corruptCopMomentActive) return false;
        // 在 VISION_OFF_DURATION 之后的 VISION_ON_DURATION 期间可以透视
        return visionCycleTimer >= VISION_OFF_DURATION;
    }

    public void resetVisionCycleTimer() {
        visionCycleTimer = 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("corruptCopMomentActive", this.corruptCopMomentActive);
        tag.putInt("triggerThreshold", this.triggerThreshold);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.corruptCopMomentActive = tag.getBoolean("corruptCopMomentActive");
        this.triggerThreshold = tag.getInt("triggerThreshold");
    }

    @Override
    public void clientTick() {
        if (corruptCopMomentActive) {
            visionCycleTimer++;
            if (visionCycleTimer >= VISION_CYCLE_TOTAL) {
                visionCycleTimer = 0; // 循环
            }
        }
    }
}
