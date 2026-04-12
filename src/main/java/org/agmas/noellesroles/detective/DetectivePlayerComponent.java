package org.agmas.noellesroles.detective;

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
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 侦探玩家组件
 * 存储查验结果的高亮状态（目标UUID、颜色、剩余时间），同步到客户端用于渲染高亮。
 */
public class DetectivePlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DetectivePlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "detective"), DetectivePlayerComponent.class);

    private final PlayerEntity player;

    private UUID highlightTargetUuid;
    private int highlightColor;
    private int highlightRemainingTicks;

    public DetectivePlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void setHighlight(UUID targetUuid, int color, int durationTicks) {
        this.highlightTargetUuid = targetUuid;
        this.highlightColor = color;
        this.highlightRemainingTicks = durationTicks;
        this.sync();
    }

    public void clearHighlight() {
        this.highlightTargetUuid = null;
        this.highlightColor = 0;
        this.highlightRemainingTicks = 0;
        this.sync();
    }

    public void reset() {
        this.highlightTargetUuid = null;
        this.highlightColor = 0;
        this.highlightRemainingTicks = 0;
        this.sync();
    }

    public UUID getHighlightTargetUuid() {
        return highlightTargetUuid;
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    public int getHighlightRemainingTicks() {
        return highlightRemainingTicks;
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
        if (highlightRemainingTicks > 0) {
            highlightRemainingTicks--;
            if (highlightRemainingTicks == 0) {
                highlightTargetUuid = null;
                highlightColor = 0;
            }
            if (highlightRemainingTicks % 20 == 0 || highlightRemainingTicks == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (highlightTargetUuid != null) {
            tag.putUuid("highlightTarget", highlightTargetUuid);
            tag.putInt("highlightColor", highlightColor);
            tag.putInt("highlightTicks", highlightRemainingTicks);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.containsUuid("highlightTarget")) {
            highlightTargetUuid = tag.getUuid("highlightTarget");
            highlightColor = tag.getInt("highlightColor");
            highlightRemainingTicks = tag.getInt("highlightTicks");
        } else {
            highlightTargetUuid = null;
            highlightColor = 0;
            highlightRemainingTicks = 0;
        }
    }
}
