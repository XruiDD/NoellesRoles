package org.agmas.noellesroles.silencer;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
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

import java.util.UUID;

/**
 * 静语者玩家组件
 * 存储标记目标状态，服务端倒计时，自动同步到客户端
 */
public class SilencerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SilencerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "silencer"),
            SilencerPlayerComponent.class
    );

    public static final int MARK_DURATION_TICKS = GameConstants.getInTicks(0, 30);

    private final PlayerEntity player;
    private UUID markedTargetUuid = null;
    private String markedTargetName = null;
    private int markTicksRemaining = 0;

    public SilencerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.markedTargetUuid = null;
        this.markedTargetName = null;
        this.markTicksRemaining = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    /**
     * 标记目标
     */
    public void markTarget(UUID targetUuid, String targetName) {
        this.markedTargetUuid = targetUuid;
        this.markedTargetName = targetName;
        this.markTicksRemaining = MARK_DURATION_TICKS;
        this.sync();
    }

    /**
     * 清除标记
     */
    public void clearMark() {
        this.markedTargetUuid = null;
        this.markedTargetName = null;
        this.markTicksRemaining = 0;
        this.sync();
    }

    public boolean hasMarkedTarget() {
        return markedTargetUuid != null && markTicksRemaining > 0;
    }

    public UUID getMarkedTargetUuid() {
        return markedTargetUuid;
    }

    public String getMarkedTargetName() {
        return markedTargetName;
    }

    public int getMarkTicksRemaining() {
        return markTicksRemaining;
    }

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
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.markTicksRemaining = tag.contains("markTicksRemaining") ? tag.getInt("markTicksRemaining") : 0;
        this.markedTargetUuid = tag.containsUuid("markedTargetUuid") ? tag.getUuid("markedTargetUuid") : null;
        this.markedTargetName = tag.contains("markedTargetName") ? tag.getString("markedTargetName") : null;
    }
}
