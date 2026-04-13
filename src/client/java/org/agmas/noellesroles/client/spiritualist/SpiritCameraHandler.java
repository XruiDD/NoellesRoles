package org.agmas.noellesroles.client.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.Perspective;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.jetbrains.annotations.Nullable;

/**
 * 通灵者灵魂出窍管理器
 * 完整复刻 Freecam 主控的 onEnable / onDisable / preTick 流程
 */
public class SpiritCameraHandler {

    static final MinecraftClient MC = MinecraftClient.getInstance();
    private static SpiritCamera spiritCamera;
    private static boolean active = false;
    private static Perspective rememberedPerspective = null;

    public static boolean isActive() {
        return active;
    }

    @Nullable
    public static SpiritCamera getSpiritCamera() {
        return spiritCamera;
    }

    /**
     * 照搬 Freecam.onEnable + onEnableFreecam
     */
    public static void enable() {
        if (active || MC.player == null || MC.world == null) return;

        // --- onEnable ---
        MC.chunkCullingEnabled = false;
        MC.gameRenderer.setRenderHand(false);

        rememberedPerspective = MC.options.getPerspective();
        if (MC.gameRenderer.getCamera().isThirdPerson()) {
            MC.options.setPerspective(Perspective.FIRST_PERSON);
        }

        // --- onEnableFreecam ---
        spiritCamera = new SpiritCamera(-421);
        spiritCamera.applyPosition(MC.player);

        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(MC.player);
        spiritCamera.setBodyPosition(spiritComp.getBodyX(), spiritComp.getBodyY(), spiritComp.getBodyZ());

        spiritCamera.spawn();
        MC.setCameraEntity(spiritCamera);

        active = true;
    }

    /**
     * 照搬 Freecam.onDisable + onDisabled
     */
    public static void disable() {
        if (!active) return;

        // --- onDisable ---
        MC.chunkCullingEnabled = true;
        MC.gameRenderer.setRenderHand(true);
        MC.setCameraEntity(MC.player);

        if (spiritCamera != null) {
            spiritCamera.despawn();
            spiritCamera.input = new Input();
            spiritCamera = null;
        }

        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }

        active = false;

        // --- onDisabled ---
        if (rememberedPerspective != null) {
            MC.options.setPerspective(rememberedPerspective);
            rememberedPerspective = null;
        }
    }

    /**
     * 每 tick 调用，照搬 Freecam.preTick：
     * 1. 检测服务端同步的状态变更，自动开启/关闭
     * 2. 冻结真实玩家输入（保留 sneaking）
     * 3. 控制手臂渲染
     */
    public static void tick() {
        if (MC.player == null) return;

        SpiritPlayerComponent spiritComp = SpiritPlayerComponent.KEY.get(MC.player);

        if (spiritComp.isProjecting() && !active) {
            enable();
        } else if (!spiritComp.isProjecting() && active) {
            disable();
        }

        if (active) {
            // 照搬 Freecam.preTick：冻结真实玩家输入
            if (MC.player != null && MC.player.input instanceof KeyboardInput) {
                Input input = new Input();
                input.sneaking = MC.player.input.sneaking;
                MC.player.input = input;
            }

            // 照搬 Freecam.preTick：手臂渲染
            MC.gameRenderer.setRenderHand(false);
        }
    }
}
