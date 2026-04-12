package org.agmas.noellesroles.spiritualist;

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

/**
 * 通灵者玩家组件
 * 存储灵魂出窍状态和本体坐标，自动同步到客户端
 */
public class SpiritPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<SpiritPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "spirit"),
            SpiritPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean projecting = false;
    private double bodyX, bodyY, bodyZ;

    public SpiritPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.projecting = false;
        this.bodyX = 0;
        this.bodyY = 0;
        this.bodyZ = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public boolean isProjecting() {
        return projecting;
    }

    public void startProjecting() {
        this.projecting = true;
        this.bodyX = player.getX();
        this.bodyY = player.getY();
        this.bodyZ = player.getZ();
        this.sync();
    }

    public void stopProjecting() {
        this.projecting = false;
        this.sync();
    }

    public double getBodyX() {
        return bodyX;
    }

    public double getBodyY() {
        return bodyY;
    }

    public double getBodyZ() {
        return bodyZ;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.projecting);
        buf.writeDouble(this.bodyX);
        buf.writeDouble(this.bodyY);
        buf.writeDouble(this.bodyZ);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.projecting = buf.readBoolean();
        this.bodyX = buf.readDouble();
        this.bodyY = buf.readDouble();
        this.bodyZ = buf.readDouble();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("projecting", this.projecting);
        tag.putDouble("bodyX", this.bodyX);
        tag.putDouble("bodyY", this.bodyY);
        tag.putDouble("bodyZ", this.bodyZ);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.projecting = tag.contains("projecting") && tag.getBoolean("projecting");
        this.bodyX = tag.contains("bodyX") ? tag.getDouble("bodyX") : 0;
        this.bodyY = tag.contains("bodyY") ? tag.getDouble("bodyY") : 0;
        this.bodyZ = tag.contains("bodyZ") ? tag.getDouble("bodyZ") : 0;
    }
}
