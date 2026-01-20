package org.agmas.noellesroles;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent BOMB_BEEP = registerSound("item.bomb.beep");
    public static final SoundEvent BOMB_EXPLODE = registerSound("item.bomb.explode");
    public static final SoundEvent JESTER_LAUGH = registerSound("ambient.jester_laugh");
    // 黑警时刻相关音效
    public static final SoundEvent CORRUPT_COP_MOMENT_1 = registerSound("music.corrupt_cop_moment_1");
    public static final SoundEvent CORRUPT_COP_MOMENT_2 = registerSound("music.corrupt_cop_moment_2");
    public static final SoundEvent CORRUPT_COP_EXECUTION = registerSound("ambient.corrupt_cop_execution");

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of(Noellesroles.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
        // 静态初始化时自动注册
    }
}
