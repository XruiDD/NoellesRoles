package org.agmas.noellesroles.voodoo;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class VoodooPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<VoodooPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "voodoo"), VoodooPlayerComponent.class);
    private final PlayerEntity player;
    public UUID target;
    /** 被巫毒诅咒后距离死亡的剩余 tick；-1 表示未被诅咒。 */
    public int pendingDeathTicks = -1;

    public void reset() {
        this.target = player.getUuid();
        this.pendingDeathTicks = -1;
        this.sync();
    }

    public VoodooPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void setTarget(UUID target) {
        this.target = target;
        this.sync();
    }

    /**
     * 巫毒师死亡时调用：让本玩家（绑定目标）在 {@code ticks} 之后死于巫毒诅咒，
     * 期间在动作栏向其显示提示。
     */
    public void startPendingDeath(int ticks) {
        this.pendingDeathTicks = ticks;
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable("hud.voodoo.cursed"), true);
        }
    }

    @Override
    public void serverTick() {
        if (this.pendingDeathTicks <= -1) return;

        // 目标已死亡 / 离开对局 / 对局结束：取消巫毒倒计时
        if (!(this.player instanceof ServerPlayerEntity serverPlayer) || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            this.pendingDeathTicks = -1;
            return;
        }

        this.pendingDeathTicks--;

        if (this.pendingDeathTicks <= 0) {
            this.pendingDeathTicks = -1;
            // 巫毒诅咒可穿透疯魔盾 / 铁人 / 威士忌护盾（见各自的排除逻辑与 ShouldPiercePsychoArmour 事件），
            // 但仍能被小丑禁锢等其它保护拦下，因此使用普通致死（force=false）
            GameFunctions.killPlayer(serverPlayer, true, null, Noellesroles.VOODOO_ID);
            return;
        }

        // 每秒刷新一次动作栏提示，防止文字淡出
        if (this.pendingDeathTicks % 20 == 0) {
            serverPlayer.sendMessage(Text.translatable("hud.voodoo.cursed"), true);
        }
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putUuid("target", player.getUuid());
        tag.putInt("pendingDeathTicks", this.pendingDeathTicks);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.target = tag.contains("target") ? tag.getUuid("target") : player.getUuid();
        this.pendingDeathTicks = tag.contains("pendingDeathTicks") ? tag.getInt("pendingDeathTicks") : -1;
    }
}
