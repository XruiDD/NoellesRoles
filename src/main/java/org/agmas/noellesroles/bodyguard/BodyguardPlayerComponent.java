package org.agmas.noellesroles.bodyguard;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 保镖玩家组件
 * 同步连环杀手的目标，保护目标免受近距离击杀和吞噬
 */
public class BodyguardPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BodyguardPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "bodyguard"), BodyguardPlayerComponent.class);

    private final PlayerEntity player;

    // 与连环杀手同步的当前目标
    private UUID currentTarget;

    public BodyguardPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.currentTarget = null;
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
        if (!gameWorldComponent.isRole(player, Noellesroles.BODYGUARD)) return;

        // 从第一个存活的连环杀手同步目标
        UUID newTarget = null;
        for (UUID uuid : gameWorldComponent.getAllWithRole(Noellesroles.SERIAL_KILLER)) {
            PlayerEntity serialKiller = serverWorld.getPlayerByUuid(uuid);
            if (serialKiller != null && GameFunctions.isPlayerPlayingAndAlive(serialKiller)) {
                SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(serialKiller);
                newTarget = serialKillerComp.getCurrentTarget();
                break;
            }
        }

        if (!java.util.Objects.equals(newTarget, currentTarget)) {
            this.currentTarget = newTarget;
            this.sync();
        }
    }

    /**
     * 检查指定玩家是否是当前保护目标
     */
    public boolean isCurrentTarget(UUID playerUuid) {
        return this.currentTarget != null && this.currentTarget.equals(playerUuid);
    }

    /**
     * 获取当前保护目标
     */
    public UUID getCurrentTarget() {
        return this.currentTarget;
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
