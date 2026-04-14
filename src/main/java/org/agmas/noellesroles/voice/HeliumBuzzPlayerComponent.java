package org.agmas.noellesroles.voice;

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

/**
 * 氦气嗨组件 — 派对狂的嗓音变调状态。
 * <p>
 * 跟 vanilla StatusEffect 不同：vanilla 只把效果同步给受影响玩家本人，
 * 别的监听者客户端收不到，导致变调判断失效。此组件 AutoSync 到所有追踪者。
 * <p>
 * 持有此组件的玩家说话时，接收端客户端的 {@code HeliumBuzzVoicechatPlugin}
 * 会在 SVC PCM 上做变调。
 */
public class HeliumBuzzPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<HeliumBuzzPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "helium_buzz"),
            HeliumBuzzPlayerComponent.class
    );

    private final PlayerEntity player;
    private int ticksRemaining = 0;
    private byte amplifier = 0;

    public HeliumBuzzPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * 服务端调用：施加效果。ticks &lt;= 0 表示清除。
     */
    public void apply(int ticks, int amplifier) {
        this.ticksRemaining = Math.max(0, ticks);
        this.amplifier = (byte) Math.max(0, Math.min(Byte.MAX_VALUE, amplifier));
        KEY.sync(this.player);
    }

    public void clear() {
        if (this.ticksRemaining == 0 && this.amplifier == 0) return;
        this.ticksRemaining = 0;
        this.amplifier = 0;
        KEY.sync(this.player);
    }

    public boolean isActive() {
        return ticksRemaining > 0;
    }

    public int getAmplifier() {
        return amplifier & 0xFF;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public static HeliumBuzzPlayerComponent get(PlayerEntity player) {
        return KEY.get(player);
    }

    @Override
    public void serverTick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining == 0) {
                this.amplifier = 0;
                KEY.sync(this.player);
            }
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(ticksRemaining);
        buf.writeByte(amplifier);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.ticksRemaining = buf.readVarInt();
        this.amplifier = buf.readByte();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("ticks", ticksRemaining);
        tag.putByte("amp", amplifier);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.ticksRemaining = tag.contains("ticks") ? tag.getInt("ticks") : 0;
        this.amplifier = tag.contains("amp") ? tag.getByte("amp") : 0;
    }
}
