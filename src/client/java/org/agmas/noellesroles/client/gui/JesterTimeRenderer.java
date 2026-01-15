package org.agmas.noellesroles.client.gui;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 小丑疯魔模式倒计时渲染器
 * 显示粉红色的倒计时，当小丑处于疯魔模式时
 */
public class JesterTimeRenderer {
    public static TimeRenderer.TimeNumberRenderer view = new TimeRenderer.TimeNumberRenderer();
    public static float offsetDelta = 0f;

    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, @NotNull DrawContext context, float delta) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

        // 只有小丑角色才显示
        if (!gameWorldComponent.isRole(player, Noellesroles.JESTER)) {
            return;
        }

        JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);

        // 只有在疯魔模式中才显示
        if (!jesterComponent.inPsychoMode) {
            return;
        }

        int time = jesterComponent.psychoModeTicks;

        // 更新动画效果
        if (Math.abs(view.getTarget() - time) > 10) {
            offsetDelta = time > view.getTarget() ? .6f : -.6f;
        }

        // 当时间少于10秒时，添加紧急效果
        if (time < 200) { // 10秒 = 200 ticks
            offsetDelta = -0.9f;
        } else {
            offsetDelta = MathHelper.lerp(delta / 16, offsetDelta, 0f);
        }
        Wathe.LOGGER.info("time: {}",time);
        view.setTarget(time);

        // 小丑粉红色 RGB(248, 200, 220) = 0.973, 0.784, 0.863
        // 根据时间剩余添加红色警告效果
        float r = 0.973f;
        float g = 0.784f;
        float b = 0.863f;

        // 时间紧急时闪烁效果
        if (time < 200) { // 最后10秒
            float pulse = (float) Math.sin(time / 3.0) * 0.3f + 0.7f;
            r = 1.0f;
            g = 0.2f * pulse;
            b = 0.4f * pulse;
        } else if (time < 600) { // 最后30秒
            float pulse = (float) Math.sin(time / 5.0) * 0.2f + 0.8f;
            r = 0.973f * pulse + 0.2f;
            g = 0.784f * pulse;
            b = 0.863f * pulse;
        }

        int colour = MathHelper.packRgb(r, g, b) | 0xFF000000;

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2f, 6, 0);
        view.render(renderer, context, 0, 0, colour, delta);
        context.getMatrices().pop();
    }

    public static void tick() {
        view.update();
    }
}
