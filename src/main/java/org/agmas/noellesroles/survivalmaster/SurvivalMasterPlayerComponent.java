package org.agmas.noellesroles.survivalmaster;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.music.MusicMomentType;
import org.agmas.noellesroles.music.WorldMusicComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 生存大师玩家组件
 * 管理生存时刻状态和倒计时
 */
public class SurvivalMasterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SurvivalMasterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "survival_master"), SurvivalMasterPlayerComponent.class);
    private static final Identifier EVENT_MOMENT_START = Identifier.of(Noellesroles.MOD_ID, "survival_moment_start");
    private static final Identifier EVENT_MOMENT_END = Identifier.of(Noellesroles.MOD_ID, "survival_moment_end");

    public static final int SURVIVAL_MOMENT_DURATION = GameConstants.getInTicks(2, 0); // 120 seconds

    private final PlayerEntity player;

    // 生存时刻是否激活
    private boolean survivalMomentActive = false;
    // 生存时刻倒计时（ticks）
    private int survivalMomentTicks = 0;
    // 开局杀手数量
    private int killerCountAtStart = 0;

    public SurvivalMasterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.survivalMomentActive = false;
        this.survivalMomentTicks = 0;
        this.killerCountAtStart = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.survivalMomentActive);
        buf.writeInt(this.survivalMomentTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.survivalMomentActive = buf.readBoolean();
        this.survivalMomentTicks = buf.readInt();
    }

    /**
     * 在游戏开始时初始化
     * @param killerCount 开局杀手数量
     */
    public void initializeForGame(int killerCount) {
        this.killerCountAtStart = killerCount;
        this.survivalMomentActive = false;
        this.survivalMomentTicks = 0;
        this.sync();
    }

    /**
     * 检查是否应该触发生存时刻
     * 条件：
     * 1. 所有阻止胜利的中立（黑警、饕餮）都死亡
     * 2. 存活玩家数 == min(killerCountAtStart + 1, 4)
     * 3. 生存大师存活
     */
    public void checkAndTriggerMoment(ServerWorld serverWorld) {
        if (survivalMomentActive) return;
        if (killerCountAtStart <= 0) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);

        // 检查阻止胜利的中立是否都已死亡
        // 黑警
        for (UUID uuid : gameComponent.getAllWithRole(Noellesroles.CORRUPT_COP)) {
            PlayerEntity corruptCop = serverWorld.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerPlayingAndAlive(corruptCop)) {
                return; // 黑警还活着，不触发
            }
        }
        // 饕餮
        for (UUID uuid : gameComponent.getAllWithRole(Noellesroles.TAOTIE)) {
            PlayerEntity taotie = serverWorld.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                return; // 饕餮还活着，不触发
            }
        }

        // 计算触发人数阈值: min(killerCountAtStart + 1, 4)
        int threshold = Math.min(killerCountAtStart + 1, 4);

        // 统计存活玩家数（不包括被饕餮吞噬的）
        int aliveCount = 0;
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            if (GameFunctions.isPlayerPlayingAndAlive(p) && !p.isSpectator()) {
                aliveCount++;
            }
        }

        if (aliveCount <= threshold) {
            triggerSurvivalMoment(serverWorld);
        }
    }

    /**
     * 触发生存时刻
     */
    private void triggerSurvivalMoment(ServerWorld serverWorld) {
        this.survivalMomentActive = true;
        this.survivalMomentTicks = SURVIVAL_MOMENT_DURATION;

        if (player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            extra.putInt("killer_count", killerCountAtStart);
            extra.putInt("duration_ticks", SURVIVAL_MOMENT_DURATION);
            GameRecordManager.recordGlobalEvent(serverWorld, EVENT_MOMENT_START, serverPlayer, extra);
        }

        // 向所有玩家广播标题
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            p.networkHandler.sendPacket(new TitleS2CPacket(
                Text.translatable("title.noellesroles.survival_moment")
                    .formatted(Formatting.DARK_GREEN, Formatting.BOLD)
            ));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.translatable("subtitle.noellesroles.survival_moment")
                    .formatted(Formatting.GREEN)
            ));
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 100, 10));
            p.sendMessage(Text.translatable("title.noellesroles.survival_moment")
                    .formatted(Formatting.DARK_GREEN, Formatting.BOLD), false);
        }

        broadcastCountdown();
        this.sync();
    }

    /**
     * 结束生存时刻（生存大师被杀时调用）
     */
    public void endSurvivalMoment() {
        if (!survivalMomentActive) return;

        this.survivalMomentActive = false;
        this.survivalMomentTicks = 0;

        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverWorld, EVENT_MOMENT_END, serverPlayer, null);
        }

        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            p.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
            p.sendMessage(Text.translatable("tip.noellesroles.survival_moment_ended")
                    .formatted(Formatting.GRAY), true);
        }

        this.sync();
    }

    /**
     * 广播倒计时
     */
    private void broadcastCountdown() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        int secondsLeft = survivalMomentTicks / 20;

        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
            if (gameComponent.isRole(p, Noellesroles.SURVIVAL_MASTER)) {
                // 生存大师看到自己的倒计时
                p.sendMessage(Text.translatable("tip.survival_master.moment_active", secondsLeft), true);
            } else {
                // 其他玩家看到杀手视角的倒计时
                p.sendMessage(Text.translatable("tip.noellesroles.survival_moment_countdown", secondsLeft), true);
            }
        }
    }

    @Override
    public void serverTick() {
        if (survivalMomentActive && survivalMomentTicks > 0) {
            survivalMomentTicks--;

            if (survivalMomentTicks % 20 == 0) {
                broadcastCountdown();
                this.sync();
            }
            // 倒计时结束在 CheckWinCondition 中检查
        }
    }

    public boolean isSurvivalMomentActive() {
        return survivalMomentActive;
    }

    public int getSurvivalMomentTicks() {
        return survivalMomentTicks;
    }

    /**
     * 生存时刻是否已完成（计时归零）
     */
    public boolean hasSurvivalMomentCompleted() {
        return survivalMomentActive && survivalMomentTicks <= 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("survivalMomentActive", this.survivalMomentActive);
        tag.putInt("survivalMomentTicks", this.survivalMomentTicks);
        tag.putInt("killerCountAtStart", this.killerCountAtStart);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.survivalMomentActive = tag.getBoolean("survivalMomentActive");
        this.survivalMomentTicks = tag.getInt("survivalMomentTicks");
        this.killerCountAtStart = tag.getInt("killerCountAtStart");
    }
}
