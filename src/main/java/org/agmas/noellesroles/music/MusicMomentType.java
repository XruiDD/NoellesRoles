package org.agmas.noellesroles.music;

/**
 * BGM时刻类型枚举
 * 用于标识不同的世界BGM播放时刻
 */
public enum MusicMomentType {
    /**
     * 无BGM播放
     */
    NONE,

    /**
     * 黑警时刻
     */
    CORRUPT_COP_MOMENT;

    /**
     * 从字符串获取类型
     */
    public static MusicMomentType fromString(String str) {
        try {
            return valueOf(str);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
