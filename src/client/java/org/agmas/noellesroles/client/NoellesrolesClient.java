package org.agmas.noellesroles.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.agmas.noellesroles.ModdedRole;
import org.agmas.noellesroles.Noellesroles;
import org.lwjgl.glfw.GLFW;

public class NoellesrolesClient implements ClientModInitializer {

    private static KeyBinding abilityBind;

    public static ModdedRole clientModdedRole = null;
    @Override
    public void onInitializeClient() {
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Noellesroles.MOD_ID + ".ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.trainmurdermystery.keybinds"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {});
    }
}
