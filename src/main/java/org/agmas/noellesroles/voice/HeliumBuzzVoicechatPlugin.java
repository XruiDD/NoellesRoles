package org.agmas.noellesroles.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 接收端客户端变调插件入口（位于公共 sourceSet）：
 * 必须在 SERVER 上也能被类加载——SVC 的 voicechat entrypoint 不会按 fabric.mod.json
 * 的 per-entry environment 字段过滤，所有插件类都会被尝试加载。
 * 因此这里仅在 CLIENT 环境注册事件，server 上 registerEvents 是 no-op。
 * 实际逻辑通过反射延迟到首次 CLIENT 调用时解析，避免 server 类路径上不存在
 * MinecraftClient 时触发链接错误。
 */
public class HeliumBuzzVoicechatPlugin implements VoicechatPlugin {

    private static final String CLIENT_RECEIVER =
            "org.agmas.noellesroles.client.voice.HeliumBuzzClientReceiver";

    @Override
    public String getPluginId() {
        return "noellesroles_helium_buzz";
    }

    @Override
    public void registerEvents(EventRegistration r) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
        try {
            Class<?> cls = Class.forName(CLIENT_RECEIVER);
            cls.getMethod("register", EventRegistration.class).invoke(null, r);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to wire HeliumBuzz client receiver", e);
        }
    }
}
