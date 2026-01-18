package org.agmas.noellesroles.reporter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

/**
 * 记者玩家组件
 * 存储被记者标记的目标，记者可以透视被标记的玩家
 */
public class ReporterPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<ReporterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "reporter"), ReporterPlayerComponent.class);

    private final PlayerEntity player;

    // 被标记的目标玩家 UUID
    private UUID markedTarget;

    public ReporterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.markedTarget = null;
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
     * 标记目标玩家
     * @param targetUuid 目标玩家的 UUID
     */
    public void setMarkedTarget(UUID targetUuid) {
        this.markedTarget = targetUuid;
        this.sync();
    }

    /**
     * 获取被标记的目标
     * @return 目标玩家的 UUID，如果没有标记则返回 null
     */
    public UUID getMarkedTarget() {
        return this.markedTarget;
    }

    /**
     * 检查是否有被标记的目标
     */
    public boolean hasMarkedTarget() {
        return this.markedTarget != null;
    }

    /**
     * 检查指定玩家是否是被标记的目标
     * @param playerUuid 要检查的玩家 UUID
     */
    public boolean isMarkedTarget(UUID playerUuid) {
        return this.markedTarget != null && this.markedTarget.equals(playerUuid);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.markedTarget != null) {
            tag.putUuid("markedTarget", this.markedTarget);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("markedTarget")) {
            this.markedTarget = tag.getUuid("markedTarget");
        } else {
            this.markedTarget = null;
        }
    }
}
