package org.agmas.noellesroles.client.music;

import dev.doctor4t.ratatouille.client.util.ambience.AmbienceUtil;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.music.MusicMomentType;
import org.agmas.noellesroles.music.WorldMusicComponent;

/**
 * 世界BGM管理器
 * 统一管理所有世界级别的BGM播放
 * 通过CCA组件获取BGM状态，使用AmbienceUtil播放BGM
 */
public class WorldMusicManager {
    private static MusicMomentType lastMoment = MusicMomentType.NONE;
    private static int lastMusicIndex = 0;

    /**
     * 注册所有世界BGM到AmbienceUtil
     * 在客户端初始化时调用
     */
    public static void register() {
        // 注册黑警时刻BGM 1
        AmbienceUtil.registerBackgroundAmbience(new BackgroundAmbience(
                ModSounds.CORRUPT_COP_MOMENT_1,
                player -> isPlaying(MusicMomentType.CORRUPT_COP_MOMENT, 1),
                40
        ));

        // 注册黑警时刻BGM 2
        AmbienceUtil.registerBackgroundAmbience(new BackgroundAmbience(
                ModSounds.CORRUPT_COP_MOMENT_2,
                player -> isPlaying(MusicMomentType.CORRUPT_COP_MOMENT, 2),
                40
        ));

        // 注册小丑时刻BGM
        AmbienceUtil.registerBackgroundAmbience(new BackgroundAmbience(
                ModSounds.JESTER_MOMENT,
                player -> isPlaying(MusicMomentType.JESTER_MOMENT, 1),
                40
        ));
    }

    /**
     * 检查指定BGM是否正在播放
     */
    private static boolean isPlaying(MusicMomentType type, int index) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return false;
        }

        WorldMusicComponent component = WorldMusicComponent.KEY.get(client.world);
        return component.getCurrentMoment() == type && component.getMusicIndex() == index;
    }

    /**
     * 客户端tick更新
     * 检测BGM状态变化并触发相应事件
     */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        WorldMusicComponent component = WorldMusicComponent.KEY.get(client.world);
        MusicMomentType currentMoment = component.getCurrentMoment();
        int currentIndex = component.getMusicIndex();

        // 检测BGM状态变化
        if (currentMoment != lastMoment || currentIndex != lastMusicIndex) {
            onMusicChanged(lastMoment, currentMoment, currentIndex);
            lastMoment = currentMoment;
            lastMusicIndex = currentIndex;
        }
    }

    /**
     * BGM状态变化回调
     */
    private static void onMusicChanged(MusicMomentType oldMoment, MusicMomentType newMoment, int musicIndex) {
        MinecraftClient client = MinecraftClient.getInstance();

        // 处理黑警时刻特殊逻辑
        if (newMoment == MusicMomentType.CORRUPT_COP_MOMENT) {
            // 重置透视循环计时
            if (client.player != null) {
                CorruptCopPlayerComponent.KEY.get(client.player).resetVisionCycleTimer();
            }

            // 显示黑警时刻标题 (5秒 = 100 ticks)
            if (client.inGameHud != null) {
                client.inGameHud.setTitle(Text.translatable("title.noellesroles.corrupt_cop_moment"));
                client.inGameHud.setSubtitle(Text.translatable("subtitle.noellesroles.corrupt_cop_moment"));
                client.inGameHud.setTitleTicks(10, 100, 10); // fadeIn, stay, fadeOut
            }
        }

        // 未来可以在这里添加其他BGM类型的特殊处理
    }

    /**
     * 获取当前BGM状态（用于调试）
     */
    public static String getCurrentMusicInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return "No world";
        }

        WorldMusicComponent component = WorldMusicComponent.KEY.get(client.world);
        return String.format("Moment: %s, Index: %d",
                component.getCurrentMoment(),
                component.getMusicIndex());
    }
}
