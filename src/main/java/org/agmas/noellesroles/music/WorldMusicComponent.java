package org.agmas.noellesroles.music;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 世界BGM组件
 * 使用CCA同步世界级别的BGM播放状态
 * 支持多种不同的BGM时刻类型
 */
public class WorldMusicComponent implements AutoSyncedComponent {
    public static final ComponentKey<WorldMusicComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "world_music"),
            WorldMusicComponent.class
    );

    private final World world;
    private MusicMomentType currentMoment = MusicMomentType.NONE;
    private int musicIndex = 0; // 用于存储音乐索引或其他参数

    public WorldMusicComponent(World world) {
        this.world = world;
    }

    /**
     * 开始播放BGM
     * @param type BGM类型
     * @param index 音乐索引（如果有多首可选）
     */
    public void startMusic(MusicMomentType type, int index) {
        if (this.currentMoment != type || this.musicIndex != index) {
            this.currentMoment = type;
            this.musicIndex = index;
            sync();
        }
    }

    /**
     * 停止当前BGM
     */
    public void stopMusic() {
        if (this.currentMoment != MusicMomentType.NONE) {
            this.currentMoment = MusicMomentType.NONE;
            this.musicIndex = 0;
            sync();
        }
    }

    /**
     * 获取当前BGM类型
     */
    public MusicMomentType getCurrentMoment() {
        return currentMoment;
    }

    /**
     * 获取音乐索引
     */
    public int getMusicIndex() {
        return musicIndex;
    }

    private void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeString(currentMoment.name());
        buf.writeInt(musicIndex);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.currentMoment = MusicMomentType.fromString(buf.readString());
        this.musicIndex = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putString("currentMoment", currentMoment.name());
        tag.putInt("musicIndex", musicIndex);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("currentMoment")) {
            this.currentMoment = MusicMomentType.fromString(tag.getString("currentMoment"));
        }
        if (tag.contains("musicIndex")) {
            this.musicIndex = tag.getInt("musicIndex");
        }
    }
}
