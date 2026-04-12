package org.agmas.noellesroles.client.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.jetbrains.annotations.Nullable;

/**
 * 通灵者灵魂出窍管理器
 * 负责创建/销毁 SpiritCamera、切换相机、冻结真实玩家输入、静音、隐藏手臂
 */
public class SpiritCameraHandler {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static SpiritCamera spiritCamera;
    private static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    @Nullable
    public static SpiritCamera getSpiritCamera() {
        return spiritCamera;
    }

    public static void enable() {
        if (active || MC.player == null || MC.world == null) return;

        spiritCamera = new SpiritCamera(-421);
        spiritCamera.applyPosition(MC.player);

        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(MC.player);
        spiritCamera.setBodyPosition(spiritComp.getBodyX(), spiritComp.getBodyY(), spiritComp.getBodyZ());

        spiritCamera.spawn();
        MC.setCameraEntity(spiritCamera);

        // 隐藏手臂
        MC.gameRenderer.setRenderHand(false);

        active = true;
    }

    public static void disable() {
        if (!active) return;

        MC.setCameraEntity(MC.player);

        // 恢复手臂
        MC.gameRenderer.setRenderHand(true);

        // 恢复真实玩家输入
        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }

        if (spiritCamera != null) {
            spiritCamera.despawn();
            spiritCamera.input = new Input();
            spiritCamera = null;
        }

        active = false;
    }

    /**
     * 每 tick 调用：检测状态变更，冻结真实玩家输入
     */
    public static void tick() {
        if (MC.player == null) return;

        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(MC.player);

        if (spiritComp.isProjecting() && !active) {
            enable();
        } else if (!spiritComp.isProjecting() && active) {
            disable();
        }

        // 冻结真实玩家输入
        if (active && MC.player.input instanceof KeyboardInput) {
            Input frozenInput = new Input();
            frozenInput.sneaking = MC.player.input.sneaking;
            MC.player.input = frozenInput;
        }
    }
}
