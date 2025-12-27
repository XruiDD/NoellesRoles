package org.agmas.noellesroles.client;

import com.google.common.collect.Maps;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.api.event.CanSeeBodyRole;
import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.client.screen.AssassinScreen;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NoellesrolesClient implements ClientModInitializer {
    public static int insanityTime = 0;
    public static KeyBinding abilityBind;
    public static PlayerBodyEntity targetBody;
    public static PlayerEntity pathogenNearestTarget;

    public static Map<UUID, UUID> SHUFFLED_PLAYER_ENTRIES_CACHE = Maps.newHashMap();


    @Override
    public void onInitializeClient() {
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Noellesroles.MOD_ID + ".ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.wathe.keybinds"));

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
                    return !moodComponent.isLowerThanMid() || !WatheClient.isPlayerAliveAndInSurvival();
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

            if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) return null;

            PlayerEntity localPlayer = MinecraftClient.getInstance().player;

            // BARTENDER: 看到喝酒者发绿光，有护甲者发蓝光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.BARTENDER)) {
                if (localPlayer.canSee(player)) {
                    BartenderPlayerComponent comp = BartenderPlayerComponent.KEY.get(player);
                    if (comp.glowTicks > 0) return GetInstinctHighlight.HighlightResult.always(Color.GREEN.getRGB());
                    if (comp.armor > 0) return  GetInstinctHighlight.HighlightResult.always(Color.BLUE.getRGB());
                }
            }

            // TOXICOLOGIST: 看到中毒者发红光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.TOXICOLOGIST)) {
                if (localPlayer.canSee(player)) {
                    PlayerPoisonComponent comp = PlayerPoisonComponent.KEY.get(player);
                    if (comp.poisonTicks > 0) return  GetInstinctHighlight.HighlightResult.always(Color.RED.getRGB());
                }
            }

            // PATHOGEN: 已感染绿色，最近未感染目标显示按键提示（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.PATHOGEN)) {
                InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                if (infected.isInfected()) {
                    // Already infected - green
                    return GetInstinctHighlight.HighlightResult.always(Noellesroles.PATHOGEN.color());
                }
                return GetInstinctHighlight.HighlightResult.withKeybind(Color.BLUE.getRGB());
            }
            return null;
        });
        // 注册 GetInstinctHighlight 监听器：秃鹫的本能高亮逻辑
        GetInstinctHighlight.EVENT.register(entity -> {
            if (!(entity instanceof PlayerBodyEntity)) return null;
            if (MinecraftClient.getInstance().player == null) return null;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(
                    MinecraftClient.getInstance().player.getWorld()
            );
            PlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.VULTURE))
                return GetInstinctHighlight.HighlightResult.withKeybind(Noellesroles.VULTURE.color());
            return null;
        });

        // 注册 GetInstinctHighlight 监听器：卧底角色高亮逻辑
        // 让杀手误认为卧底是同伙（按Alt时显示红色）
        GetInstinctHighlight.EVENT.register(entity -> {
            if (!(entity instanceof PlayerEntity player) || player.isSpectator()) return null;
            if (MinecraftClient.getInstance().player == null) return null;

            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(
                    MinecraftClient.getInstance().player.getWorld()
            );
            PlayerEntity localPlayer = MinecraftClient.getInstance().player;

            // 只有当查看者是杀手时才生效
            if (!gameWorldComponent.canUseKillerFeatures(localPlayer)) return null;
            if (!GameFunctions.isPlayerAliveAndSurvival(localPlayer)) return null;

            // 如果目标是卧底，让杀手误以为是同伙（显示红色）
            if (gameWorldComponent.isRole(player, Noellesroles.UNDERCOVER)) {
                // 使用与杀手同伙相同的红色（需要按Alt键）
                return GetInstinctHighlight.HighlightResult.withKeybind(MathHelper.hsvToRgb(0F, 1.0F, 0.6F), GetInstinctHighlight.HighlightResult.PRIORITY_HIGH);
            }

            return null;
        });

        // 注册 ShouldShowCohort 监听器：卧底角色cohort提示逻辑
        // 让杀手看向卧底时显示"cohort"（同伙）提示
        ShouldShowCohort.EVENT.register((viewer, target) -> {
            if (viewer == null || target == null) return null;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.getWorld());

            // 只有当查看者是杀手时才生效
            if (!gameWorldComponent.canUseKillerFeatures(viewer)) return null;
            if (!GameFunctions.isPlayerAliveAndSurvival(viewer)) return null;

            // 如果目标是卧底，显示cohort提示
            if (gameWorldComponent.isRole(target, Noellesroles.UNDERCOVER)) {
                return ShouldShowCohort.CohortResult.show();
            }

            return null; // 不处理，使用默认逻辑
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            insanityTime++;
            if (insanityTime >= 20*6) {
                insanityTime = 0;
                List<UUID> keys = new ArrayList<UUID>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
                List<UUID> originalkeys = new ArrayList<UUID>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
                Collections.shuffle(keys);
                int i = 0;
                for (UUID o : originalkeys) {
                    SHUFFLED_PLAYER_ENTRIES_CACHE.put(o, keys.get(i));
                    i++;
                }
            }

            // 更新病原体最近目标
            if (MinecraftClient.getInstance().player != null) {
                GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.PATHOGEN)) {
                    pathogenNearestTarget = null;
                    double nearestDistance = 9.0; // 3^2 = 9
                    PlayerEntity localPlayer = MinecraftClient.getInstance().player;

                    for (PlayerEntity player : localPlayer.getWorld().getPlayers()) {
                        if (player.equals(localPlayer)) continue;
                        if (player.isSpectator() || player.isCreative()) continue;
                        // 检查玩家是否有角色（在游戏中）
                        if (!gameWorldComponent.hasAnyRole(player)) continue;

                        InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                        if (infected.isInfected()) continue;

                        double distance = localPlayer.squaredDistanceTo(player);
                        if (distance < nearestDistance) {
                            // 检查视线（不能隔墙感染）
                            if (localPlayer.canSee(player)) {
                                nearestDistance = distance;
                                pathogenNearestTarget = player;
                            }
                        }
                    }
                } else {
                    pathogenNearestTarget = null;
                }
            }

            if (abilityBind.wasPressed()) {
                client.execute(() -> {
                    if (MinecraftClient.getInstance().player == null) return;
                    GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());

                    // 刺客角色按G打开刺客界面
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.ASSASSIN)) {
                        if (GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) {
                            AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
                            // 检查是否可以使用技能（不在冷却中且有剩余次数）
                            if (assassinComp.canGuess()) {
                                MinecraftClient.getInstance().setScreen(new AssassinScreen((net.minecraft.client.network.ClientPlayerEntity) MinecraftClient.getInstance().player));
                            }
                            // 如果不能使用，不打开界面，HUD 会显示相应的提示信息
                        }
                        return;
                    }

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
