package org.agmas.noellesroles.client;

import com.google.common.collect.Maps;
import dev.doctor4t.wathe.api.event.*;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.client.gui.JesterTimeRenderer;
import org.agmas.noellesroles.client.screen.AssassinScreen;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.agmas.noellesroles.packet.CorruptCopMomentS2CPacket;
import org.agmas.noellesroles.packet.ReporterMarkC2SPacket;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.client.corruptcop.CorruptCopMomentMusicManager;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NoellesrolesClient implements ClientModInitializer {
    public static int insanityTime = 0;
    public static KeyBinding abilityBind;
    public static PlayerBodyEntity targetBody;
    public static PlayerEntity pathogenNearestTarget;
    public static double pathogenNearestTargetDistance;
    public static String pathogenTargetDirection;
    public static String pathogenTargetVertical; // 垂直位置提示
    public static PlayerEntity crosshairTarget;
    public static double crosshairTargetDistance;

    public static Map<UUID, UUID> SHUFFLED_PLAYER_ENTRIES_CACHE = Maps.newHashMap();


    @Override
    public void onInitializeClient() {
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Noellesroles.MOD_ID + ".ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.wathe.keybinds"));

        // 注册解毒剂冷却模型谓词
        ModelPredicateProviderRegistry.register(ModItems.ANTIDOTE, Identifier.of(Noellesroles.MOD_ID, "cooldown"),
                (stack, world, entity, seed) -> {
                    if (!(entity instanceof PlayerEntity player)) return 0.0f;
                    return player.getItemCooldownManager().isCoolingDown(stack.getItem()) ? 1.0f : 0.0f;
                });

        // 注册黑警时刻BGM到AmbienceUtil
        CorruptCopMomentMusicManager.register();

        // 注册黑警时刻S2C数据包接收器
        ClientPlayNetworking.registerGlobalReceiver(CorruptCopMomentS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.start()) {
                    CorruptCopMomentMusicManager.startMoment(payload.soundIndex());

                    // 显示黑警时刻标题 (5秒 = 100 ticks)
                    MinecraftClient client = context.client();
                    if (client.inGameHud != null) {
                        client.inGameHud.setTitle(Text.translatable("title.noellesroles.corrupt_cop_moment"));
                        client.inGameHud.setSubtitle(Text.translatable("subtitle.noellesroles.corrupt_cop_moment"));
                        client.inGameHud.setTitleTicks(10, 100, 10); // fadeIn, stay, fadeOut
                    }
                } else {
                    CorruptCopMomentMusicManager.stopMoment();
                }
            });
        });

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

            if (!(entity instanceof PlayerEntity player) || player.isSpectator() || player.isInvisible()) return null;

            if (MinecraftClient.getInstance().player == null) return null;

            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(
                MinecraftClient.getInstance().player.getWorld()
            );

            if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) return null;

            PlayerEntity localPlayer = MinecraftClient.getInstance().player;

            if (gameWorldComponent.isRole(localPlayer, Noellesroles.CORRUPT_COP)) {
                var comp = CorruptCopPlayerComponent.KEY.get(localPlayer);
                if (comp.canSeePlayersThroughWalls()){
                    return GetInstinctHighlight.HighlightResult.always(Noellesroles.CORRUPT_COP.color());
                }
            }

            if (gameWorldComponent.isRole(localPlayer, Noellesroles.JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(localPlayer);
                if (jesterComponent.inPsychoMode && player.getUuid().equals(jesterComponent.targetKiller))
                {
                    return GetInstinctHighlight.HighlightResult.always(Noellesroles.JESTER.color());
                }
            }

            // BARTENDER: 看到喝酒者发绿光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.BARTENDER)) {
                if (localPlayer.canSee(player)) {
                    BartenderPlayerComponent comp = BartenderPlayerComponent.KEY.get(player);
                    if (comp.glowTicks > 0) return GetInstinctHighlight.HighlightResult.always(Color.GREEN.getRGB());
                }
            }

            // PROFESSOR: 看到有铁人buff的人发蓝光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.PROFESSOR)) {
                if (localPlayer.canSee(player)) {
                    IronManPlayerComponent comp = IronManPlayerComponent.KEY.get(player);
                    if (comp.hasBuff()) {
                        return GetInstinctHighlight.HighlightResult.always(Color.BLUE.getRGB());
                    }
                }
            }

            // TOXICOLOGIST: 看到中毒者发红光（需要视线）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.TOXICOLOGIST)) {
                if (localPlayer.canSee(player)) {
                    PlayerPoisonComponent comp = PlayerPoisonComponent.KEY.get(player);
                    if (comp.poisonTicks > 0) return  GetInstinctHighlight.HighlightResult.always(Color.RED.getRGB());
                }
            }

            // PATHOGEN: 只有已感染的玩家显示绿色高亮（不再透视未感染玩家）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.PATHOGEN)) {
                InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                if (infected.isInfected() && localPlayer.canSee(player)) {
                    // Already infected - green
                    return GetInstinctHighlight.HighlightResult.always(Noellesroles.PATHOGEN.color());
                }
            }

            // REPORTER: 被标记的目标始终高亮显示（透视效果）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.REPORTER)) {
                ReporterPlayerComponent reporterComp = ReporterPlayerComponent.KEY.get(localPlayer);
                if (reporterComp.isMarkedTarget(player.getUuid())) {
                    // 被标记的目标 - 使用记者颜色透视
                    return GetInstinctHighlight.HighlightResult.always(Noellesroles.REPORTER.color());
                }
            }

            // SERIAL_KILLER: 当前目标始终高亮显示（透视效果）
            if (gameWorldComponent.isRole(localPlayer, Noellesroles.SERIAL_KILLER)) {
                SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(localPlayer);
                if (serialKillerComp.isCurrentTarget(player.getUuid())) {
                    // 当前目标 - 使用连环杀手颜色透视
                    return GetInstinctHighlight.HighlightResult.always(Noellesroles.SERIAL_KILLER.color());
                }
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
                    pathogenNearestTargetDistance = Double.MAX_VALUE;
                    pathogenTargetDirection = "";
                    pathogenTargetVertical = "";
                    PlayerEntity localPlayer = MinecraftClient.getInstance().player;

                    // 找到最近的未感染玩家（不限距离，用于指南针指向）
                    for (PlayerEntity player : localPlayer.getWorld().getPlayers()) {
                        if (player.equals(localPlayer)) continue;
                        if (player.isSpectator() || player.isCreative()) continue;
                        // 检查玩家是否有角色（在游戏中）
                        if (!gameWorldComponent.hasAnyRole(player)) continue;

                        InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                        if (infected.isInfected()) continue;

                        double distance = localPlayer.squaredDistanceTo(player);
                        if (distance < pathogenNearestTargetDistance) {
                            pathogenNearestTargetDistance = distance;
                            pathogenNearestTarget = player;
                        }
                    }

                    // 计算方向
                    if (pathogenNearestTarget != null) {
                        pathogenNearestTargetDistance = Math.sqrt(pathogenNearestTargetDistance);

                        // 计算从玩家指向目标的方向
                        double dx = pathogenNearestTarget.getX() - localPlayer.getX();
                        double dy = pathogenNearestTarget.getY() - localPlayer.getY();
                        double dz = pathogenNearestTarget.getZ() - localPlayer.getZ();

                        // 计算目标相对于玩家视角的角度
                        double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
                        double playerYaw = localPlayer.getYaw() % 360;
                        if (playerYaw < 0) playerYaw += 360;
                        if (targetAngle < 0) targetAngle += 360;

                        // 计算相对角度（目标相对于玩家面朝方向）
                        double relativeAngle = targetAngle - playerYaw;
                        if (relativeAngle < -180) relativeAngle += 360;
                        if (relativeAngle > 180) relativeAngle -= 360;

                        // 根据相对角度确定方向箭头
                        if (relativeAngle >= -22.5 && relativeAngle < 22.5) {
                            pathogenTargetDirection = "↑"; // 前方
                        } else if (relativeAngle >= 22.5 && relativeAngle < 67.5) {
                            pathogenTargetDirection = "↗"; // 右前方
                        } else if (relativeAngle >= 67.5 && relativeAngle < 112.5) {
                            pathogenTargetDirection = "→"; // 右方
                        } else if (relativeAngle >= 112.5 && relativeAngle < 157.5) {
                            pathogenTargetDirection = "↘"; // 右后方
                        } else if (relativeAngle >= 157.5 || relativeAngle < -157.5) {
                            pathogenTargetDirection = "↓"; // 后方
                        } else if (relativeAngle >= -157.5 && relativeAngle < -112.5) {
                            pathogenTargetDirection = "↙"; // 左后方
                        } else if (relativeAngle >= -112.5 && relativeAngle < -67.5) {
                            pathogenTargetDirection = "←"; // 左方
                        } else {
                            pathogenTargetDirection = "↖"; // 左前方
                        }

                        // 计算垂直位置提示（高度差超过2格才提示）
                        if (dy > 2) {
                            pathogenTargetVertical = "↑"; // 上方
                        } else if (dy < -2) {
                            pathogenTargetVertical = "↓"; // 下方
                        } else {
                            pathogenTargetVertical = ""; // 同一高度
                        }
                    }
                } else {
                    pathogenNearestTarget = null;
                    pathogenNearestTargetDistance = 0;
                    pathogenTargetDirection = "";
                    pathogenTargetVertical = "";
                }

                crosshairTarget = null;
                crosshairTargetDistance = 0;
                PlayerEntity localPlayer = MinecraftClient.getInstance().player;
                double maxDistance = 10.0; // 最大检测距离
                var eyePos = localPlayer.getEyePos();
                var hitResult = ProjectileUtil.getCollision(
                        localPlayer,
                        entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                        maxDistance
                );

                if (hitResult instanceof EntityHitResult entityHitResult&& entityHitResult.getEntity() instanceof PlayerEntity targetPlayer) {
                    crosshairTarget = targetPlayer;
                    crosshairTargetDistance = eyePos.distanceTo(entityHitResult.getPos());
                } else if (hitResult instanceof BlockHitResult blockHitResult){
                    Optional<PlayerEntity> sleepingPlayer = findSleepingPlayerOnBed(localPlayer.getWorld(), blockHitResult);
                    if (sleepingPlayer.isPresent() && sleepingPlayer.get() != localPlayer) {
                        crosshairTarget = sleepingPlayer.get();
                        crosshairTargetDistance = eyePos.distanceTo(blockHitResult.getPos());
                    }
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

                    // 记者角色按G发送标记数据包
                    if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.REPORTER)) {
                        if (crosshairTarget != null && crosshairTargetDistance <= 3.0) {
                            ClientPlayNetworking.send(new ReporterMarkC2SPacket(crosshairTarget.getUuid()));
                        }
                        return;
                    }

                    ClientPlayNetworking.send(new AbilityC2SPacket());
                });
            }

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                JesterTimeRenderer.tick();
            }
        });
    }

    /**
     * 检测床方块上是否有睡觉的玩家
     * @return 睡觉的玩家，如果没有则返回 Optional.empty()
     */
    public static Optional<PlayerEntity> findSleepingPlayerOnBed(World world, BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState state = world.getBlockState(blockPos);

        if (!(state.getBlock() instanceof BedBlock)) {
            return Optional.empty();
        }

        BedPart part = state.get(BedBlock.PART);
        Direction facing = state.get(BedBlock.FACING);
        BlockPos headPos = (part == BedPart.HEAD) ? blockPos : blockPos.offset(facing);

        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isSleeping()) {
                continue;
            }
            Optional<BlockPos> sleepingPosOpt = player.getSleepingPosition();
            if (sleepingPosOpt.isEmpty()) {
                continue;
            }
            BlockPos sleepingPos = sleepingPosOpt.get();
            if (sleepingPos.equals(headPos) || sleepingPos.equals(blockPos)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }
}
