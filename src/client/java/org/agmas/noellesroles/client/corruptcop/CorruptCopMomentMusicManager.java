package org.agmas.noellesroles.client.corruptcop;

import dev.doctor4t.ratatouille.client.util.ambience.AmbienceUtil;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;


/**
 * 黑警时刻BGM管理器
 * 使用 AmbienceUtil 库管理BGM播放
 * 同时管理透视循环计时
 */
public class CorruptCopMomentMusicManager {
    private static boolean corruptCopMomentActive = false;
    private static int soundIndex = 0;

    /**
     * 注册黑警时刻BGM到AmbienceUtil
     * 在客户端初始化时调用
     */
    public static void register() {

        AmbienceUtil.registerBackgroundAmbience(new BackgroundAmbience(
                ModSounds.CORRUPT_COP_MOMENT_1,
                player -> corruptCopMomentActive && soundIndex == 1,
                40
        ));

        AmbienceUtil.registerBackgroundAmbience(new BackgroundAmbience(
                ModSounds.CORRUPT_COP_MOMENT_2,
                player -> corruptCopMomentActive && soundIndex == 2,
                40
        ));

    }

    /**
     * 设置黑警时刻状态（由网络包调用）
     */
    public static void setMomentActive(boolean active) {
        corruptCopMomentActive = active;
        if (MinecraftClient.getInstance().player != null) {
            CorruptCopPlayerComponent.KEY.get(MinecraftClient.getInstance().player).resetVisionCycleTimer();
        }
    }

    /**
     * 开始黑警时刻
     */
    public static void startMoment(int soundIndex) {
        CorruptCopMomentMusicManager.soundIndex = soundIndex;
        setMomentActive(true);
    }

    /**
     * 停止黑警时刻
     */
    public static void stopMoment() {
        setMomentActive(false);
    }

    /**
     * 检查黑警时刻是否激活
     */
    public static boolean isActive() {
        return corruptCopMomentActive;
    }
}
