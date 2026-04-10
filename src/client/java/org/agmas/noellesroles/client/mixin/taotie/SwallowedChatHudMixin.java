package org.agmas.noellesroles.client.mixin.taotie;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 饕餮肚子里的玩家虽然是旁观模式，但不应看到聊天历史
 * wathe 主模组的 ChatHudMixin 只屏蔽了 isPlayerAliveAndInSurvival() 的玩家
 * 被吞噬玩家是旁观模式，不会被 wathe 的检查拦截
 * 此 mixin 补充检查：如果游戏运行中且玩家被吞噬 → 不渲染聊天历史
 */
@Mixin(value = ChatHud.class, priority = 1200)
public class SwallowedChatHudMixin {
    @WrapMethod(method = "render")
    public void noellesroles$hideSwallowedChatHistory(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, Operation<Void> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            GameWorldComponent gwc = GameWorldComponent.KEY.get(client.player.getWorld());
            if (gwc.isRunning() && SwallowedPlayerComponent.isPlayerSwallowed(client.player)) {
                return; // 不渲染聊天历史
            }
        }
        original.call(context, currentTick, mouseX, mouseY, focused);
    }
}
