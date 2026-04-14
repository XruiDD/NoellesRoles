package org.agmas.noellesroles.partyanimal;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 派对狂玩家组件 — 仅存储派对狂本人已标记的目标与倒计时。
 * 变声效果通过 HeliumBuzzPlayerComponent 施加。
 */
public class PartyAnimalPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PartyAnimalPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "party_animal"),
            PartyAnimalPlayerComponent.class
    );

    public static final int MARK_DURATION_TICKS = GameConstants.getInTicks(1, 0); // 60 秒

    private final PlayerEntity player;
    private UUID markedTargetUuid = null;
    private String markedTargetName = null;
    private int markTicksRemaining = 0;
    // 每个目标本局已奖励过的最高变声等级（1..3）。服务端使用，不同步到客户端。
    private final Map<UUID, Integer> rewardedMaxLevel = new HashMap<>();
    // 上一次释放的目标；标记另一个目标前禁止再次标记此人（防止连续标记同一人）
    private UUID lastReleasedTargetUuid = null;

    public UUID getLastReleasedTargetUuid() { return lastReleasedTargetUuid; }
    public void setLastReleasedTargetUuid(UUID uuid) { this.lastReleasedTargetUuid = uuid; }

    /** 返回目标之前被奖励过的最高等级（0 表示没有）。*/
    public int getRewardedLevel(UUID targetUuid) {
        return rewardedMaxLevel.getOrDefault(targetUuid, 0);
    }

    /** 记录：目标被奖励过的最高等级提升到 level。*/
    public void recordRewarded(UUID targetUuid, int level) {
        Integer cur = rewardedMaxLevel.get(targetUuid);
        if (cur == null || cur < level) {
            rewardedMaxLevel.put(targetUuid, level);
        }
    }

    public void resetRewards() {
        rewardedMaxLevel.clear();
    }

    public PartyAnimalPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.markedTargetUuid = null;
        this.markedTargetName = null;
        this.markTicksRemaining = 0;
        this.rewardedMaxLevel.clear();
        this.lastReleasedTargetUuid = null;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    public void markTarget(UUID targetUuid, String targetName) {
        this.markedTargetUuid = targetUuid;
        this.markedTargetName = targetName;
        this.markTicksRemaining = MARK_DURATION_TICKS;
        this.sync();
    }

    public void clearMark() {
        this.markedTargetUuid = null;
        this.markedTargetName = null;
        this.markTicksRemaining = 0;
        this.sync();
    }

    public boolean hasMarkedTarget() {
        return markedTargetUuid != null && markTicksRemaining > 0;
    }

    public UUID getMarkedTargetUuid() { return markedTargetUuid; }
    public String getMarkedTargetName() { return markedTargetName; }
    public int getMarkTicksRemaining() { return markTicksRemaining; }

    @Override
    public void serverTick() {
        if (this.markTicksRemaining > 0) {
            this.markTicksRemaining--;
            if (this.markTicksRemaining <= 0) {
                this.markedTargetUuid = null;
                this.markedTargetName = null;
            }
            if (this.markTicksRemaining % 20 == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.markTicksRemaining);
        buf.writeBoolean(this.markedTargetUuid != null);
        if (this.markedTargetUuid != null) {
            buf.writeUuid(this.markedTargetUuid);
            buf.writeString(this.markedTargetName != null ? this.markedTargetName : "");
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.markTicksRemaining = buf.readInt();
        boolean hasTarget = buf.readBoolean();
        if (hasTarget) {
            this.markedTargetUuid = buf.readUuid();
            this.markedTargetName = buf.readString();
        } else {
            this.markedTargetUuid = null;
            this.markedTargetName = null;
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("markTicksRemaining", this.markTicksRemaining);
        if (this.markedTargetUuid != null) {
            tag.putUuid("markedTargetUuid", this.markedTargetUuid);
        }
        if (this.markedTargetName != null) {
            tag.putString("markedTargetName", this.markedTargetName);
        }
        NbtCompound sub = new NbtCompound();
        for (Map.Entry<UUID, Integer> e : this.rewardedMaxLevel.entrySet()) {
            sub.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("rewardedLevels", sub);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.markTicksRemaining = tag.contains("markTicksRemaining") ? tag.getInt("markTicksRemaining") : 0;
        this.markedTargetUuid = tag.containsUuid("markedTargetUuid") ? tag.getUuid("markedTargetUuid") : null;
        this.markedTargetName = tag.contains("markedTargetName") ? tag.getString("markedTargetName") : null;
        this.rewardedMaxLevel.clear();
        if (tag.contains("rewardedLevels", NbtElement.COMPOUND_TYPE)) {
            NbtCompound sub = tag.getCompound("rewardedLevels");
            for (String key : sub.getKeys()) {
                try {
                    this.rewardedMaxLevel.put(UUID.fromString(key), sub.getInt(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
