package org.agmas.noellesroles.client;

import com.google.common.collect.Maps;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerMoodComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerPoisonComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import dev.doctor4t.trainmurdermystery.event.CanSeeBodyRole;
import dev.doctor4t.trainmurdermystery.event.CanSeeMoney;
import dev.doctor4t.trainmurdermystery.event.GetInstinctHighlight;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NoellesrolesClient implements ClientModInitializer {


    public static int insanityTime = 0;
    public static KeyBinding abilityBind;
    public static PlayerEntity target;
    public static PlayerBodyEntity targetBody;

    public static Map<UUID, UUID> SHUFFLED_PLAYER_ENTRIES_CACHE = Maps.newHashMap();

    // 检查两个玩家之间是否有视线
    private static boolean hasLineOfSight(PlayerEntity viewer, PlayerEntity target) {
        Vec3d viewerEyes = viewer.getEyePos();
        Vec3d targetEyes = target.getEyePos();
        RaycastContext context = new RaycastContext(
            viewerEyes, targetEyes,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            viewer
        );
        HitResult result = viewer.getWorld().raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }


    @Override
    public void onInitializeClient() {
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Noellesroles.MOD_ID + ".ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.trainmurdermystery.keybinds"));

        CanSeeMoney.EVENT.register(player -> {
            if (player.isSpectator()) return null;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(
                    player.getWorld()
            );
            if(gameWorldComponent.isRole(player, Noellesroles.RECALLER) && !gameWorldComponent.isPlayerDead(player.getUuid())){
                return CanSeeMoney.Result.ALLOW;
            }
            return null;
        });

        // 注册 CanSeeBodyRole 监听器：验尸官可以看到尸体的角色（需要理智值检查）
        CanSeeBodyRole.EVENT.register(player -> {
            if (player instanceof PlayerEntity playerEntity && playerEntity.getWorld() != null) {
                GameWorldComponent component = GameWorldComponent.KEY.get(playerEntity.getWorld());
                if (component.isRole(playerEntity, Noellesroles.CORONER)) {
                    // 验尸官需要 50% 以上的理智值才能查看尸体信息
                    PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(playerEntity);
                    return !moodComponent.isLowerThanMid() || !TMMClient.isPlayerAliveAndInSurvival();
                }
            }
            return false;
        });

        // 注册 GetInstinctHighlight 监听器：各角色的本能高亮逻辑
        GetInstinctHighlight.EVENT.register(entity -> {
            if (!(entity instanceof PlayerEntity player) || player.isSpectator()) return null;
            if (MinecraftClient.getInstance().player == null) return null;

            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(
                MinecraftClient.getInstance().player.getWorld()
            );
            PlayerEntity localPlayer = MinecraftClient.getInstance().player;

            // BARTENDER: 看到喝酒者发绿光，有护甲者发蓝光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.BARTENDER)) {
                if (hasLineOfSight(localPlayer, player)) {
                    BartenderPlayerComponent comp = BartenderPlayerComponent.KEY.get(player);
                    if (comp.glowTicks > 0) return GetInstinctHighlight.HighlightResult.always(Color.GREEN.getRGB());
                    if (comp.armor > 0) return  GetInstinctHighlight.HighlightResult.always(Color.BLUE.getRGB());
                }
            }

            // TOXICOLOGIST: 看到中毒者发红光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.TOXICOLOGIST)) {
                if (hasLineOfSight(localPlayer, player)) {
                    PlayerPoisonComponent comp = PlayerPoisonComponent.KEY.get(player);
                    if (comp.poisonTicks > 0) return  GetInstinctHighlight.HighlightResult.always(Color.RED.getRGB());
                }
            }
            return null;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            insanityTime++;
            if (insanityTime >= 20*6) {
                insanityTime = 0;
                List<UUID> keys = new ArrayList<UUID>(TMMClient.PLAYER_ENTRIES_CACHE.keySet());
                List<UUID> originalkeys = new ArrayList<UUID>(TMMClient.PLAYER_ENTRIES_CACHE.keySet());
                Collections.shuffle(keys);
                int i = 0;
                for (UUID o : originalkeys) {
                    SHUFFLED_PLAYER_ENTRIES_CACHE.put(o, keys.get(i));
                    i++;
                }
            }
            if (abilityBind.wasPressed()) {
                client.execute(() -> {
                    if (MinecraftClient.getInstance().player == null) return;
                    GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.VULTURE)) {
                        if (targetBody == null) return;
                        ClientPlayNetworking.send(new VultureEatC2SPacket(targetBody.getUuid()));
                        return;
                    }
                    ClientPlayNetworking.send(new AbilityC2SPacket());
                });
            }
        });
    }
}
