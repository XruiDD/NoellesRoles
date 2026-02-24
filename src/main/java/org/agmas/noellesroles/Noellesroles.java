package org.agmas.noellesroles;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleAppearanceCondition;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.*;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import dev.doctor4t.wathe.record.replay.ReplayEventFormatter;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderShopHandler;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.agmas.noellesroles.packet.MorphCorpseToggleC2SPacket;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.agmas.noellesroles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.pathogen.PathogenPlayerComponent;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.bomber.BomberShopHandler;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.scavenger.ScavengerPlayerComponent;
import org.agmas.noellesroles.scavenger.ScavengerShopHandler;
import org.agmas.noellesroles.timekeeper.TimekeeperShopHandler;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.packet.ReporterMarkC2SPacket;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.packet.TaotieSwallowC2SPacket;
import org.agmas.noellesroles.packet.SilencerSilenceC2SPacket;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.silencer.SilencerPlayerComponent;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.poisoner.PoisonerShopHandler;
import org.agmas.noellesroles.bandit.BanditShopHandler;
import org.agmas.noellesroles.survivalmaster.SurvivalMasterPlayerComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.*;
import java.util.List;


public class Noellesroles implements ModInitializer {

    public static String MOD_ID = "noellesroles";


    public static Identifier MORPHLING_ID = Identifier.of(MOD_ID, "morphling");
    public static Identifier CONDUCTOR_ID = Identifier.of(MOD_ID, "conductor");
    public static Identifier BARTENDER_ID = Identifier.of(MOD_ID, "bartender");
    public static Identifier NOISEMAKER_ID = Identifier.of(MOD_ID, "noisemaker");
    public static Identifier PHANTOM_ID = Identifier.of(MOD_ID, "phantom");
    public static Identifier AWESOME_BINGLUS_ID = Identifier.of(MOD_ID, "awesome_binglus");
    public static Identifier SWAPPER_ID = Identifier.of(MOD_ID, "swapper");
    public static Identifier VOODOO_ID = Identifier.of(MOD_ID, "voodoo");
    public static Identifier CORONER_ID = Identifier.of(MOD_ID, "coroner");
    public static Identifier RECALLER_ID = Identifier.of(MOD_ID, "recaller");
    public static Identifier VULTURE_ID = Identifier.of(MOD_ID, "vulture");
    public static Identifier THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID = Identifier.of(MOD_ID, "the_insane_damned_paranoid_killer");
    public static Identifier TIMEKEEPER_ID = Identifier.of(MOD_ID, "time_keeper");
    public static Identifier UNDERCOVER_ID = Identifier.of(MOD_ID, "undercover");
    public static Identifier TOXICOLOGIST_ID = Identifier.of(MOD_ID, "toxicologist");
    public static Identifier JESTER_ID = Identifier.of(MOD_ID, "jester");
    public static Identifier CORRUPT_COP_ID = Identifier.of(MOD_ID, "corrupt_cop");
    public static Identifier PATHOGEN_ID = Identifier.of(MOD_ID, "pathogen");
    public static Identifier BOMBER_ID = Identifier.of(MOD_ID, "bomber");
    public static Identifier ASSASSIN_ID = Identifier.of(MOD_ID, "assassin");
    public static Identifier SCAVENGER_ID = Identifier.of(MOD_ID, "scavenger");
    public static Identifier REPORTER_ID = Identifier.of(MOD_ID, "reporter");
    public static Identifier SERIAL_KILLER_ID = Identifier.of(MOD_ID, "serial_killer");
    public static Identifier PROFESSOR_ID = Identifier.of(MOD_ID, "professor");
    public static Identifier ATTENDANT_ID = Identifier.of(MOD_ID, "attendant");
    public static Identifier TAOTIE_ID = Identifier.of(MOD_ID, "taotie");
    public static Identifier SILENCER_ID = Identifier.of(MOD_ID, "silencer");
    public static Identifier BODYGUARD_ID = Identifier.of(MOD_ID, "bodyguard");
    public static Identifier POISONER_ID = Identifier.of(MOD_ID, "poisoner");
    public static Identifier BANDIT_ID = Identifier.of(MOD_ID, "bandit");
    public static Identifier SURVIVAL_MASTER_ID = Identifier.of(MOD_ID, "survival_master");

    // 炸弹死亡原因
    public static Identifier DEATH_REASON_BOMB = Identifier.of(MOD_ID, "bomb");
    // 刺客死亡原因
    public static Identifier DEATH_REASON_ASSASSINATED = Identifier.of(MOD_ID, "assassinated");  // 被刺客猜中身份
    public static Identifier DEATH_REASON_ASSASSIN_MISFIRE = Identifier.of(MOD_ID, "assassin_misfire");  // 刺客猜错自己死亡
    public static Identifier DEATH_REASON_JESTER_TIMEOUT = Identifier.of(MOD_ID, "jester_timeout");
    // 饕餮吞噬死亡原因（游戏结束时被消化）
    public static Identifier DEATH_REASON_DIGESTED = Identifier.of(MOD_ID, "digested");
    // 保镖牺牲死亡原因
    public static Identifier DEATH_REASON_BODYGUARD_SACRIFICE = Identifier.of(MOD_ID, "bodyguard_sacrifice");
    // 投掷斧死亡原因
    public static Identifier DEATH_REASON_THROWING_AXE = Identifier.of(MOD_ID, "throwing_axe");


    // 下毒来源
    public static Identifier POISON_SOURCE_NEEDLE = Identifier.of(MOD_ID, "needle");
    public static Identifier POISON_SOURCE_GAS_BOMB = Identifier.of(MOD_ID, "gas_bomb");

    public static Role SWAPPER = WatheRoles.registerRole(new Role(SWAPPER_ID, new Color(57, 4, 170).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    public static Role PHANTOM =WatheRoles.registerRole(new Role(PHANTOM_ID, new Color(80, 5, 5, 192).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    public static Role MORPHLING =WatheRoles.registerRole(new Role(MORPHLING_ID, new Color(170, 2, 61).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    public static Role THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES = WatheRoles.registerRole(new Role(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID, new Color(255, 0, 0, 192).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    // 炸弹客角色 - 杀手阵营，无法购买刀和枪，只能用炸弹
    public static Role BOMBER = WatheRoles.registerRole(new Role(BOMBER_ID, new Color(50, 50, 50).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 刺客角色 - 杀手阵营，可以猜测玩家身份
    public static Role ASSASSIN = WatheRoles.registerRole(new Role(ASSASSIN_ID, new Color(139, 0, 0).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 清道夫角色 - 杀手阵营，杀人后尸体对其他人不可见（秃鹫和中立除外），杀人奖励+50金币，只能买刀，可以花100金币重置刀CD
    public static Role SCAVENGER = WatheRoles.registerRole(new Role(SCAVENGER_ID, new Color(101, 67, 33).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 连环杀手角色 - 杀手阵营，开局随机选择一个非杀手阵营的人为透视目标，目标死后自动更换，杀掉目标后获得额外金钱奖励
    public static Role SERIAL_KILLER = WatheRoles.registerRole(new Role(SERIAL_KILLER_ID, new Color(102, 34, 34).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 静语者角色 - 杀手阵营，可以让目标无法使用voicechat说话，且无法听到他人说话，持续60秒，冷却45秒
    public static Role SILENCER = WatheRoles.registerRole(new Role(SILENCER_ID, new Color(80, 70, 110).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 毒师角色 - 杀手阵营，使用毒针和毒气弹
    public static Role POISONER = WatheRoles.registerRole(new Role(POISONER_ID, new Color(30, 80, 20).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 强盗角色 - 杀手阵营，使用投掷斧远程贯穿击杀
    public static Role BANDIT = WatheRoles.registerRole(new Role(BANDIT_ID, new Color(90, 100, 40).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));


    public static HashMap<Role, RoleAnnouncementTexts.RoleAnnouncementText> roleRoleAnnouncementTextHashMap = new HashMap<>();
    public static Role TIMEKEEPER = WatheRoles.registerRole(new Role(TIMEKEEPER_ID, new Color(0, 38, 255).getRGB(), true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), true));
    public static Role UNDERCOVER = WatheRoles.registerRole(new Role(UNDERCOVER_ID, new Color(192, 192, 192).getRGB(), true, false, Role.MoodType.NONE, GameConstants.getInTicks(0, 10), false, RoleAppearanceCondition.minKillers(2)));
    public static Role CONDUCTOR =WatheRoles.registerRole(new Role(CONDUCTOR_ID, new Color(255, 205, 84).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role AWESOME_BINGLUS = WatheRoles.registerRole(new Role(AWESOME_BINGLUS_ID, new Color(155, 255, 168).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role BARTENDER =WatheRoles.registerRole(new Role(BARTENDER_ID, new Color(217,241,240).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role NOISEMAKER =WatheRoles.registerRole(new Role(NOISEMAKER_ID, new Color(200, 255, 0).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role VOODOO =WatheRoles.registerRole(new Role(VOODOO_ID, new Color(128, 114, 253).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role CORONER =WatheRoles.registerRole(new Role(CORONER_ID, new Color(122, 122, 122).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role RECALLER = WatheRoles.registerRole(new Role(RECALLER_ID, new Color(158, 255, 255).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role TOXICOLOGIST = WatheRoles.registerRole(new Role(TOXICOLOGIST_ID, new Color(184, 41, 90).getRGB(), true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), false));
    // 记者角色 - 无辜者阵营，可以标记一个玩家并透视他
    public static Role REPORTER = WatheRoles.registerRole(new Role(REPORTER_ID, new Color(210, 180, 100).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 教授角色 - 无辜者阵营，开局自带铁人药剂，可以保护其他玩家
    public static Role PROFESSOR = WatheRoles.registerRole(new Role(PROFESSOR_ID, new Color(70, 130, 180).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 乘务员角色 - 无辜者阵营，开局获得一本书记录所有房间的乘客
    public static Role ATTENDANT = WatheRoles.registerRole(new Role(ATTENDANT_ID, new Color(100, 149, 237).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    // 保镖角色 - 无辜者阵营，保护连环杀手的目标，仅与连环杀手一起出现
    public static Role BODYGUARD = WatheRoles.registerRole(new Role(BODYGUARD_ID, new Color(70, 130, 250).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false, ctx -> ctx.isRoleAssigned(SERIAL_KILLER)));
    // 生存大师角色 - 无辜者阵营，无法被杀手本能察觉，触发生存时刻后杀手必须在120秒内找到并杀死他
    public static Role SURVIVAL_MASTER = WatheRoles.registerRole(new Role(SURVIVAL_MASTER_ID, new Color(50, 180, 160).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));


    // 小丑角色 - 中立阵营，被无辜者杀死时获胜
    public static Role JESTER = WatheRoles.registerRole(new Role(JESTER_ID, new Color(248, 200, 220).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role VULTURE =WatheRoles.registerRole(new Role(VULTURE_ID, new Color(181, 103, 0).getRGB(),false,false,Role.MoodType.FAKE,-1,false));
    // 黑警角色 - 中立阵营，杀光所有人获胜，阻止其他阵营获胜
    public static Role CORRUPT_COP = WatheRoles.registerRole(new Role(CORRUPT_COP_ID, new Color(25, 50, 100).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), true));
    // 病原体角色 - 中立阵营，感染所有存活玩家获胜
    public static Role PATHOGEN = WatheRoles.registerRole(new Role(PATHOGEN_ID, 0x7FFF00, false, false, Role.MoodType.FAKE, Integer.MAX_VALUE , false));
    // 饕餮角色 - 中立阵营，吞噬玩家获胜
    public static Role TAOTIE = WatheRoles.registerRole(new Role(TAOTIE_ID, new Color(139, 69, 19).getRGB(), false, false, Role.MoodType.FAKE, Integer.MAX_VALUE, false));

    public static final CustomPayload.Id<MorphC2SPacket> MORPH_PACKET = MorphC2SPacket.ID;
    public static final CustomPayload.Id<SwapperC2SPacket> SWAP_PACKET = SwapperC2SPacket.ID;
    public static final CustomPayload.Id<AbilityC2SPacket> ABILITY_PACKET = AbilityC2SPacket.ID;
    public static final CustomPayload.Id<VultureEatC2SPacket> VULTURE_PACKET = VultureEatC2SPacket.ID;
    public static final CustomPayload.Id<AssassinGuessRoleC2SPacket> ASSASSIN_GUESS_ROLE_PACKET = AssassinGuessRoleC2SPacket.ID;
    public static final CustomPayload.Id<ReporterMarkC2SPacket> REPORTER_MARK_PACKET = ReporterMarkC2SPacket.ID;
    public static final CustomPayload.Id<TaotieSwallowC2SPacket> TAOTIE_SWALLOW_PACKET = TaotieSwallowC2SPacket.ID;
    public static final CustomPayload.Id<SilencerSilenceC2SPacket> SILENCER_SILENCE_PACKET = SilencerSilenceC2SPacket.ID;
    public static final ArrayList<Role> VANNILA_ROLES = new ArrayList<>();
    public static final ArrayList<Identifier> VANNILA_ROLE_IDS = new ArrayList<>();
    // 中立万能钥匙可用角色集合
    private static final Set<Role> NEUTRAL_MASTER_KEY_ROLES = Set.of(VULTURE, PATHOGEN, TAOTIE);
    private static final int MOMENT_TRIGGER_MIN_THRESHOLD = 2;

    public static void checkAndTriggerMomentsForWorld(ServerWorld serverWorld) {
        if (serverWorld == null) return;
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);

        int corruptCopAliveCount = 0;
        int taotieAliveCount = 0;
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(p) || p.isSpectator()) continue;
            SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(p);
            if (!swallowed.isSwallowed()) {
                corruptCopAliveCount++;
                taotieAliveCount++;
            }
        }

        if (corruptCopAliveCount >= MOMENT_TRIGGER_MIN_THRESHOLD) {
            for (UUID uuid : gameComponent.getAllWithRole(CORRUPT_COP)) {
                PlayerEntity corruptCop = serverWorld.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(corruptCop) && GameFunctions.isPlayerAliveAndSurvival(corruptCop)) {
                    CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(corruptCop);
                    corruptCopComp.checkAndTriggerMoment(corruptCopAliveCount);
                }
            }
        }

        if (taotieAliveCount >= MOMENT_TRIGGER_MIN_THRESHOLD) {
            for (UUID uuid : gameComponent.getAllWithRole(TAOTIE)) {
                PlayerEntity taotie = serverWorld.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                    taotieComp.checkAndTriggerMoment(taotieAliveCount);
                }
            }
        }

        // 检查是否应该触发生存时刻
        for (UUID uuid : gameComponent.getAllWithRole(SURVIVAL_MASTER)) {
            PlayerEntity survivalMaster = serverWorld.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerPlayingAndAlive(survivalMaster)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(survivalMaster);
                survivalComp.checkAndTriggerMoment(serverWorld);
            }
        }
    }

    @Override
    public void onInitialize() {
        VANNILA_ROLES.add(WatheRoles.KILLER);
        VANNILA_ROLES.add(WatheRoles.VIGILANTE);
        VANNILA_ROLES.add(WatheRoles.CIVILIAN);
        VANNILA_ROLES.add(WatheRoles.LOOSE_END);
        VANNILA_ROLE_IDS.add(WatheRoles.LOOSE_END.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.VIGILANTE.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.CIVILIAN.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.KILLER.identifier());
        NoellesRolesConfig.HANDLER.load();
        ModItems.init();
        ModSounds.init();
        PayloadTypeRegistry.playC2S().register(MorphC2SPacket.ID, MorphC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MorphCorpseToggleC2SPacket.ID, MorphCorpseToggleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityC2SPacket.ID, AbilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapperC2SPacket.ID, SwapperC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VultureEatC2SPacket.ID, VultureEatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AssassinGuessRoleC2SPacket.ID, AssassinGuessRoleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ReporterMarkC2SPacket.ID, ReporterMarkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TaotieSwallowC2SPacket.ID, TaotieSwallowC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SilencerSilenceC2SPacket.ID, SilencerSilenceC2SPacket.CODEC);

        registerEvents();

        BartenderShopHandler.register();
        BomberShopHandler.register();
        ScavengerShopHandler.register();
        TimekeeperShopHandler.register();
        PoisonerShopHandler.register();
        BanditShopHandler.register();

        // 毒师手持毒针时允许攻击玩家
        AllowPlayerPunching.EVENT.register((attacker, victim) ->
                attacker.getMainHandStack().isOf(ModItems.POISON_NEEDLE)
        );

        // 注册 DLC 回放格式化器
        registerReplayFormatters();

        registerPackets();
        NoellesRolesEntities.init();

    }


    public void registerEvents() {

        ServerWorldEvents.LOAD.register((server, world) -> {
            GameWorldComponent.KEY.get(world).setRoleEnabled(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES, false);
            GameWorldComponent.KEY.get(world).setRoleEnabled(AWESOME_BINGLUS, false);
        });

        // 修复：断线重连后清理语音群组
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> {
                SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
                if (!swallowedComp.isSwallowed()) {
                    TrainVoicePlugin.addPlayer(player.getUuid());
                }
            });
        });

        // Master key should drop on death
        ShouldDropOnDeath.EVENT.register((stack, victim) -> stack.isOf(ModItems.MASTER_KEY));

        KillPlayer.BEFORE.register(((victim, killer, deathReason) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());

            // 黑警被杀时结束黑警时刻
            if (gameWorldComponent.isRole(victim, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(victim);
                if(corruptCopComp.isCorruptCopMomentActive() && deathReason == DEATH_REASON_ASSASSINATED){
                    // 记录黑警时刻免疫刺客
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                            .actor(serverVictim)
                            .put("block_reason", "corrupt_cop_moment")
                            .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            // 饕餮时刻时无法被刺客杀死
            if (gameWorldComponent.isRole(victim, TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(victim);
                if(taotieComp.isTaotieMomentActive() && deathReason == DEATH_REASON_ASSASSINATED){
                    // 记录饕餮时刻免疫刺客
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                            .actor(serverVictim)
                            .put("block_reason", "taotie_moment")
                            .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            // 生存时刻时生存大师免疫被刺客刺杀
            if (gameWorldComponent.isRole(victim, SURVIVAL_MASTER)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(victim);
                if (survivalComp.isSurvivalMomentActive() && deathReason == DEATH_REASON_ASSASSINATED) {
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                            .actor(serverVictim)
                            .put("block_reason", "survival_moment")
                            .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            if (gameWorldComponent.isRole(victim, JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                if (jesterComponent.inStasis && deathReason != GameConstants.DeathReasons.FELL_OUT_OF_TRAIN && deathReason != GameConstants.DeathReasons.ESCAPED) {
                    // 记录小丑禁锢免疫死亡
                    if (victim instanceof ServerPlayerEntity serverVictim) {
                        var event = GameRecordManager.event("death_blocked")
                            .actor(serverVictim)
                            .put("block_reason", "jester_stasis")
                            .put("death_reason", deathReason.toString());
                        if (killer instanceof ServerPlayerEntity serverKiller) {
                            event.target(serverKiller);
                        }
                        event.record();
                    }
                    return KillPlayer.KillResult.cancel();
                }
            }

            if (deathReason == GameConstants.DeathReasons.FELL_OUT_OF_TRAIN) return null;

            if (gameWorldComponent.isRole(victim, JESTER) &&
                deathReason == GameConstants.DeathReasons.GUN &&
                killer != null) {
                Role killerRole = gameWorldComponent.getRole(killer);
                if (killerRole != null && killerRole.isInnocent()) {
                    JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                    if(!jesterComponent.inPsychoMode){
                        jesterComponent.targetKiller = killer.getUuid();
                        jesterComponent.enterStasis(GameConstants.getInTicks(0, 5));
                        return KillPlayer.KillResult.cancel();
                    }
                }
            }

            // Iron Man buff protection (from Professor)
            IronManPlayerComponent ironManComp = IronManPlayerComponent.KEY.get(victim);
            if (ironManComp.hasBuff() && deathReason != GameConstants.DeathReasons.SHOT_INNOCENT && deathReason != DEATH_REASON_ASSASSINATED) {
                victim.getWorld().playSound(null, victim.getBlockPos(), WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5.0F, 1.0F);
                // 记录铁人药水保护生效
                if (victim instanceof ServerPlayerEntity serverVictim) {
                    var deathBlockedEvent = GameRecordManager.event("death_blocked")
                        .actor(serverVictim)
                        .put("block_reason", "iron_man_buff")
                        .put("death_reason", deathReason.toString());
                    if (killer instanceof ServerPlayerEntity serverKiller) {
                        deathBlockedEvent.target(serverKiller);
                    }
                    deathBlockedEvent.record();

                    var ironManEvent = GameRecordManager.event("iron_man_activated")
                        .actor(serverVictim)
                        .put("action", "block_damage");
                    if (killer instanceof ServerPlayerEntity serverKiller) {
                        ironManEvent.target(serverKiller);
                    }
                    ironManEvent.record();
                }
                ironManComp.removeBuff();
                return KillPlayer.KillResult.cancel();
            }

            // 保镖保护逻辑：如果受害者是保镖的保护目标且保镖在3格内，保镖牺牲自己保护目标
            if (deathReason == GameConstants.DeathReasons.KNIFE) {
                if (victim.getWorld() instanceof ServerWorld bodyguardWorld) {
                    for (UUID bodyguardUuid : gameWorldComponent.getAllWithRole(BODYGUARD)) {
                        PlayerEntity bodyguardPlayer = bodyguardWorld.getPlayerByUuid(bodyguardUuid);
                        if (bodyguardPlayer != null && GameFunctions.isPlayerPlayingAndAlive(bodyguardPlayer)) {
                            BodyguardPlayerComponent bodyguardComp = BodyguardPlayerComponent.KEY.get(bodyguardPlayer);
                            if (bodyguardComp.isCurrentTarget(victim.getUuid()) && bodyguardPlayer.squaredDistanceTo(victim) <= 9.0) {
                                // 记录保镖保护技能事件
                                GameRecordManager.recordSkillUse((ServerPlayerEntity) bodyguardPlayer, BODYGUARD_ID, victim, null);
                                GameFunctions.killPlayer((ServerPlayerEntity) bodyguardPlayer, true, killer, DEATH_REASON_BODYGUARD_SACRIFICE);
                                return KillPlayer.KillResult.cancel();
                            }
                        }
                    }
                }
            }

            // 被吞噬的玩家可以被杀死，但不生成尸体
            SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(victim);
            if (swallowedComp.isSwallowed()) {
                return KillPlayer.KillResult.allowWithoutBody();
            }
            return null;
        }));
        CanSeePoison.EVENT.register((player)->{
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorldComponent.isRole((PlayerEntity) player, Noellesroles.TOXICOLOGIST)) {
                return true;
            }
            return false;
        });
        RoleAssigned.EVENT.register((player, role)->{
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(player);
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
            abilityPlayerComponent.cooldown = NoellesRolesConfig.HANDLER.instance().generalCooldownTicks;
            if (role.equals(VULTURE)) {
                VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(player);
                vulturePlayerComponent.reset();
                vulturePlayerComponent.setBodiesRequired(gameWorldComponent.getAllPlayers().size() / 2);
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            }
            if (role.equals(CONDUCTOR)) {
                player.giveItemStack(ModItems.MASTER_KEY.getDefaultStack());
            }
            if (role.equals(AWESOME_BINGLUS)) {
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
                player.giveItemStack(WatheItems.NOTE.getDefaultStack());
            }
            if (role.equals(JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);
                jesterComponent.reset();
                int totalPlayers = gameWorldComponent.getAllPlayers().size();
                jesterComponent.psychoArmour = Math.max(1, totalPlayers / 5);
            }
            if (role.equals(CORRUPT_COP)) {
                player.giveItemStack(WatheItems.REVOLVER.getDefaultStack());
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
                // 初始化黑警时刻组件
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(player);
                corruptCopComp.initializeForGame(gameWorldComponent.getAllPlayers().size());
            }
            if (role.equals(PATHOGEN)) {
                PathogenPlayerComponent pathogenComp = PathogenPlayerComponent.KEY.get(player);
                pathogenComp.reset();
                // Set base cooldown based on player count (6-11: 20s, 12-17: 15s, 18-24: 10s, 24+: 7s)
                pathogenComp.setBaseCooldownByPlayerCount(gameWorldComponent.getAllPlayers().size());
                // Set initial cooldown to 10 seconds
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 10);
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            }
            if (role.equals(ASSASSIN)) {
                AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(player);
                assassinComp.reset();
                int totalPlayers = gameWorldComponent.getAllPlayers().size();
                assassinComp.setMaxGuesses(totalPlayers);  // (totalPlayers + 3) / 4
                // 刺客开局冷却30秒
                assassinComp.setCooldown(GameConstants.getInTicks(0, 60));
                // 刺客没有开局道具，只依靠猜测技能
            }
            if (role.equals(REPORTER)) {
                ReporterPlayerComponent reporterComp = ReporterPlayerComponent.KEY.get(player);
                reporterComp.reset();
                // 记者开局冷却30秒
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 30);
            }
            if (role.equals(TOXICOLOGIST)) {
                // 毒理学家开局获得解毒剂，2分钟冷却
                player.giveItemStack(ModItems.ANTIDOTE.getDefaultStack());
                player.getItemCooldownManager().set(ModItems.ANTIDOTE, org.agmas.noellesroles.item.AntidoteItem.INITIAL_COOLDOWN_TICKS);
            }
            if (role.equals(SERIAL_KILLER)) {
                SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(player);
                serialKillerComp.reset();
                // 初始化透视目标
                serialKillerComp.initializeTarget(gameWorldComponent);
            }
            if (role.equals(PROFESSOR)) {
                // Professor starts with 1 Iron Man Vial
                player.giveItemStack(ModItems.IRON_MAN_VIAL.getDefaultStack());
                player.getItemCooldownManager().set(ModItems.IRON_MAN_VIAL, 20 * 60 * 3);
            }
            if (role.equals(ATTENDANT)) {
                // 乘务员开局获得一本房间信息书
                ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

                // 构建书籍内容
                List<RawFilteredPair<Text>> pages = new ArrayList<>();
                HashMap<Integer, GameWorldComponent.RoomData> rooms = gameWorldComponent.getRooms();
                HashMap<UUID, com.mojang.authlib.GameProfile> profiles = gameWorldComponent.getGameProfiles();

                // 第一页：标题页
                StringBuilder titlePage = new StringBuilder();
                titlePage.append("§l§0乘客房间登记表§r\n\n");
                titlePage.append("§8本登记表记录了\n所有乘客的房间分配§r\n\n");
                titlePage.append("§8共 ").append(rooms.size()).append(" 个房间§r");
                pages.add(RawFilteredPair.of(Text.literal(titlePage.toString())));

                if (!rooms.isEmpty()) {
                    // 按房间索引排序
                    List<GameWorldComponent.RoomData> sortedRooms = new ArrayList<>(rooms.values());
                    sortedRooms.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

                    for (GameWorldComponent.RoomData room : sortedRooms) {
                        StringBuilder pageContent = new StringBuilder();
                        pageContent.append("§l§1【").append(room.getName()).append("】§r\n\n");

                        List<UUID> roomPlayers = room.getPlayers();
                        if (roomPlayers.isEmpty()) {
                            pageContent.append("§8无乘客§r");
                        } else {
                            for (UUID playerUuid : roomPlayers) {
                                com.mojang.authlib.GameProfile profile = profiles.get(playerUuid);
                                String playerName = profile != null ? profile.getName() : "未知";
                                pageContent.append("§0• ").append(playerName).append("§r\n");
                            }
                        }

                        pages.add(RawFilteredPair.of(Text.literal(pageContent.toString())));
                    }
                }

                // 设置书籍组件
                WrittenBookContentComponent bookContent = new WrittenBookContentComponent(
                    RawFilteredPair.of("乘客登记表"),
                    "乘务员",
                    0,
                    pages,
                    true
                );
                book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContent);

                player.giveItemStack(book);
            }
            if (role.equals(TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(player);
                taotieComp.initializeForGame(gameWorldComponent.getAllPlayers().size());
                taotieComp.setSwallowCooldown(GameConstants.getInTicks(1, 0));
                player.giveItemStack(ModItems.NEUTRAL_MASTER_KEY.getDefaultStack());
            }
            if(role.equals(BOMBER)) {
                player.getItemCooldownManager().set(ModItems.TIMED_BOMB, 20 * 45);
            }
            if (role.equals(SILENCER)) {
                // 静语者开局冷却45秒
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 45);
            }
            if (role.equals(UNDERCOVER)) {
                player.giveItemStack(WatheItems.WALKIE_TALKIE.getDefaultStack());
            }
            if (role.equals(BODYGUARD)) {
                BodyguardPlayerComponent bodyguardComp = BodyguardPlayerComponent.KEY.get(player);
                bodyguardComp.reset();
            }
            if (role.equals(SURVIVAL_MASTER)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(player);
                survivalComp.reset();
                survivalComp.initializeForGame(gameWorldComponent.getAllKillerTeamPlayers().size());
            }
            if (role.equals(POISONER)) {
                player.getItemCooldownManager().set(ModItems.POISON_NEEDLE, GameConstants.getInTicks(1, 0)); // 1分钟初始冷却
            }
        });
        ResetPlayer.EVENT.register(player -> {
            BartenderPlayerComponent.KEY.get(player).reset();
            MorphlingPlayerComponent.KEY.get(player).reset();
            VoodooPlayerComponent.KEY.get(player).reset();
            RecallerPlayerComponent.KEY.get(player).reset();
            VulturePlayerComponent.KEY.get(player).reset();
            JesterPlayerComponent.KEY.get(player).reset();
            InfectedPlayerComponent.KEY.get(player).reset();
            PathogenPlayerComponent.KEY.get(player).reset();
            BomberPlayerComponent.KEY.get(player).reset();
            AssassinPlayerComponent.KEY.get(player).reset();
            ScavengerPlayerComponent.KEY.get(player).reset();
            CorruptCopPlayerComponent.KEY.get(player).reset();
            ReporterPlayerComponent.KEY.get(player).reset();
            SerialKillerPlayerComponent.KEY.get(player).reset();
            IronManPlayerComponent.KEY.get(player).reset();
            TaotiePlayerComponent.KEY.get(player).reset();
            SwallowedPlayerComponent.KEY.get(player).reset();
            SilencedPlayerComponent.KEY.get(player).reset();
            SilencerPlayerComponent.KEY.get(player).reset();
            BodyguardPlayerComponent.KEY.get(player).reset();
            SurvivalMasterPlayerComponent.KEY.get(player).reset();
        });

        // Bartender and Recaller get +50 coins when completing tasks
        TaskComplete.EVENT.register((player, taskType) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
            Role role = gameWorldComponent.getRole(player);
            if (role != null && (role.equals(BARTENDER) || role.equals(RECALLER) || role.equals(TIMEKEEPER))) {
                PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(player);
                playerShopComponent.addToBalance(50);
            }
        });
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) -> {
            // 生存时刻完成 → 乘客胜利
            for (UUID uuid : gameComponent.getAllWithRole(SURVIVAL_MASTER)) {
                PlayerEntity survivalMaster = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(survivalMaster)) {
                    SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(survivalMaster);
                    if (survivalComp.hasSurvivalMomentCompleted()) {
                        return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.PASSENGERS);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(VULTURE)) {
                PlayerEntity vulture = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(vulture)) {
                    VulturePlayerComponent component = VulturePlayerComponent.KEY.get(vulture);
                    if (component.hasWon()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) vulture);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(PATHOGEN)) {
                PlayerEntity pathogen = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(pathogen)) {
                    boolean allInfected = true;
                    for (UUID playerUuid : gameComponent.getAllPlayers()) {
                        if (playerUuid.equals(uuid)) continue;
                        PlayerEntity player = world.getPlayerByUuid(playerUuid);
                        if (player == null) continue;
                        if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;
                        InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                        if (!infected.isInfected()) {
                            allInfected = false;
                            break;
                        }
                    }
                    if (allInfected && GameFunctions.isPlayerPlayingAndAlive(pathogen)) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) pathogen);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(jester)) {
                    JesterPlayerComponent component = JesterPlayerComponent.KEY.get(jester);
                    if (component.won) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) jester);
                    }
                    if (component.inPsychoMode && (currentStatus == GameFunctions.WinStatus.KILLERS
                            || currentStatus == GameFunctions.WinStatus.PASSENGERS)) {
                        return CheckWinCondition.WinResult.block();
                    }
                }
            }

            // Taotie win condition check (priority over corrupt cop)
            for (UUID uuid : gameComponent.getAllWithRole(TAOTIE)) {
                PlayerEntity taotie = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);

                    // Win condition 1: Swallowed all players
                    if (taotieComp.hasSwallowedEveryone()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) taotie);
                    }

                    // Win condition 2: Taotie Moment completed
                    if (taotieComp.hasTaotieMomentCompleted()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) taotie);
                    }

                    // Block other factions from winning while Taotie is alive
                    if (currentStatus == GameFunctions.WinStatus.KILLERS
                            || currentStatus == GameFunctions.WinStatus.PASSENGERS) {
                        return CheckWinCondition.WinResult.block();
                    }
                }
            }

            // Find living corrupt cop
            ServerPlayerEntity livingCorruptCop = null;
            for (UUID uuid : gameComponent.getAllWithRole(CORRUPT_COP)) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerPlayingAndAlive(player)) {
                    livingCorruptCop = player;
                    break;
                }
            }

            // If no corrupt cop alive, don't interfere
            if (livingCorruptCop == null) {
                return null;
            }

            // Count all living players (excluding spectators/creative)
            int aliveCount = 0;
            boolean corruptCopIsAlive = false;
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (GameFunctions.isPlayerPlayingAndAlive(player)) {
                    aliveCount++;
                    if (player.getUuid().equals(livingCorruptCop.getUuid())) {
                        corruptCopIsAlive = true;
                    }
                }
            }

            // If corrupt cop is the only one alive, they win
            if (aliveCount == 1 && corruptCopIsAlive) {
                return CheckWinCondition.WinResult.neutralWin(livingCorruptCop);
            }

            // Block killers and passengers from winning while corrupt cop is alive
            if (currentStatus == GameFunctions.WinStatus.KILLERS
                    || currentStatus == GameFunctions.WinStatus.PASSENGERS) {
                return CheckWinCondition.WinResult.block();
            }

            return null;
        });

        // Jester kill detection - when jester is killed by an innocent, mark as won
        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(victim.getWorld());

            // 记录被吞玩家肚内死亡标记
            SwallowedPlayerComponent victimSwallowedCheck = SwallowedPlayerComponent.KEY.get(victim);
            if (victimSwallowedCheck.isSwallowed() && victim instanceof ServerPlayerEntity serverVictim) {
                UUID taotieUuid = victimSwallowedCheck.getSwallowedBy();
                var event = GameRecordManager.event("death_in_stomach")
                    .actor(serverVictim)
                    .put("death_reason", deathReason.toString());
                if (taotieUuid != null) {
                    event.putUuid("taotie_uuid", taotieUuid);
                }
                if (killer != null) {
                    event.putUuid("killer_uuid", killer.getUuid());
                }
                event.record();
            }

            // 连环杀手处理：击杀目标奖励和目标更换
            if (victim.getWorld() instanceof ServerWorld serverWorld) {
                for (UUID uuid : gameComponent.getAllWithRole(SERIAL_KILLER)) {
                    PlayerEntity serialKiller = serverWorld.getPlayerByUuid(uuid);
                    if (serialKiller != null && GameFunctions.isPlayerPlayingAndAlive(serialKiller)) {
                        SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(serialKiller);

                        // 如果被杀者是连环杀手的目标
                        if (serialKillerComp.isCurrentTarget(victim.getUuid())) {
                            // 如果是连环杀手亲自击杀的，给予额外金钱奖励
                            if (killer != null && killer.getUuid().equals(serialKiller.getUuid())) {
                                PlayerShopComponent.KEY.get(killer).addToBalance(SerialKillerPlayerComponent.getBonusMoney());
                                // 计算减半的刀CD并标记（在serverTick中执行，因为KnifeStabPayload会在AFTER之后设置CD）
                                if (deathReason == GameConstants.DeathReasons.KNIFE) {
                                    int totalPlayers = serverWorld.getPlayers().size();
                                    int killerCount = gameComponent.getAllKillerTeamPlayers().size();
                                    int killerRatio = gameComponent.getKillerDividend();
                                    int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
                                    int baseCooldown = GameConstants.ITEM_COOLDOWNS.get(WatheItems.KNIFE);
                                    int cooldownReductionPerExcess = GameConstants.getInTicks(0, 5);
                                    int adjustedCooldown = Math.max(GameConstants.getInTicks(0, 10), baseCooldown - (excessPlayers * cooldownReductionPerExcess));
                                    serialKillerComp.markKnifeCdOverride(adjustedCooldown / 2);
                                }
                            }
                            // 目标死亡，自动更换新目标
                            serialKillerComp.onTargetDeath(gameComponent);
                        }
                    }
                }
            }

            BomberPlayerComponent bomberPlayerComponent = BomberPlayerComponent.KEY.get(victim);
            // 炸弹客击杀奖励
            if (killer != null && gameComponent.isRole(killer, BOMBER) && deathReason == DEATH_REASON_BOMB) {
                PlayerShopComponent.KEY.get(killer).addToBalance(50);
            }else if(bomberPlayerComponent.hasBomb()){
                PlayerEntity bomber = victim.getWorld().getPlayerByUuid(bomberPlayerComponent.getBomberUuid());
                if(GameFunctions.isPlayerPlayingAndAlive(bomber)){
                    if((deathReason == GameConstants.DeathReasons.FELL_OUT_OF_TRAIN || deathReason == GameConstants.DeathReasons.ESCAPED)){
                        PlayerShopComponent.KEY.get(bomber).addToBalance(150);
                    }else{
                        PlayerShopComponent.KEY.get(bomber).addToBalance(100);
                    }
                }
            }


            // 记录清道夫杀人
            if (killer != null && gameComponent.isRole(killer, SCAVENGER) && !gameComponent.isRole(victim, NOISEMAKER)) {
                ScavengerPlayerComponent scavengerComp = ScavengerPlayerComponent.KEY.get(killer);
                scavengerComp.addHiddenBody(victim.getUuid());
            }

            if (NoellesRolesConfig.HANDLER.instance().voodooNonKillerDeaths || killer != null) {
                if (gameComponent.isRole(victim, Noellesroles.VOODOO)) {
                    VoodooPlayerComponent voodooPlayerComponent = VoodooPlayerComponent.KEY.get(victim);
                    if (voodooPlayerComponent.target != null && (deathReason != DEATH_REASON_ASSASSINATED || !gameComponent.isRole(voodooPlayerComponent.target ,ASSASSIN))) {
                        ServerPlayerEntity voodooed = (ServerPlayerEntity) victim.getWorld().getPlayerByUuid(voodooPlayerComponent.target);
                        if (voodooed != null) {
                            if (GameFunctions.isPlayerPlayingAndAlive(voodooed) && voodooed != victim) {
                                // 记录 Voodoo 连锁死亡
                                if (victim instanceof ServerPlayerEntity serverVictim && voodooed instanceof ServerPlayerEntity serverVoodooed) {
                                    GameRecordManager.event("voodoo_chain_death")
                                        .actor(serverVictim)
                                        .target(serverVoodooed)
                                        .put("voodoo_death_reason", deathReason.toString())
                                        .record();
                                }
                                GameFunctions.killPlayer(voodooed, true, null, Identifier.of(Noellesroles.MOD_ID, "voodoo"));
                            }
                        }
                    }
                }
            }


            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = victim.getWorld().getPlayerByUuid(uuid);
                if (jester != null) {
                    JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(jester);
                    if (jesterComponent.targetKiller != null &&
                            victim.getUuid().equals(jesterComponent.targetKiller)) {
                        jesterComponent.won = true;
                        break;
                    }
                }
            }

            if (gameComponent.isRole(victim, JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                // 如果小丑在疯魔模式中被杀，游戏继续，不触发胜利
                if (jesterComponent.inPsychoMode) {
                    jesterComponent.reset();
                }
            }

            // 黑警击杀处理和黑警时刻检查
            if (killer != null && gameComponent.isRole(killer, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(killer);
                if (corruptCopComp.isCorruptCopMomentActive()) {
                    corruptCopComp.onKill();
                }
            }

            // 黑警被杀时结束黑警时刻
            if (gameComponent.isRole(victim, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(victim);
                corruptCopComp.endCorruptCopMoment();
            }

            // 生存大师被杀时结束生存时刻
            if (gameComponent.isRole(victim, SURVIVAL_MASTER)) {
                SurvivalMasterPlayerComponent survivalComp = SurvivalMasterPlayerComponent.KEY.get(victim);
                survivalComp.endSurvivalMoment();
            }

            // 饕餮被杀时释放所有被吞玩家并结束饕餮时刻
            if (gameComponent.isRole(victim, TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(victim);
                taotieComp.releaseAllPlayers(victim.getPos());
                taotieComp.endTaotieMoment();
            }

            // 被吞玩家死亡后的特殊处理（不生成尸体，播放打嗝音效）
            SwallowedPlayerComponent victimSwallowed = SwallowedPlayerComponent.KEY.get(victim);
            if (victimSwallowed.isSwallowed()) {
                UUID taotieUuid = victimSwallowed.getSwallowedBy();
                if (taotieUuid != null && victim.getWorld() instanceof ServerWorld serverWorld2) {
                    PlayerEntity taotie = serverWorld2.getPlayerByUuid(taotieUuid);
                    if (taotie != null) {
                        TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                        taotieComp.removeSwallowedPlayer((ServerPlayerEntity) victim);
                        // 播放打嗝音效
                        serverWorld2.playSound(null, taotie.getBlockPos(),
                            SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.5F, 0.8F);
                    }
                }
                victimSwallowed.reset();
            }

            // 检查是否应该触发黑警时刻
            if (victim.getWorld() instanceof ServerWorld serverWorld) {
                checkAndTriggerMomentsForWorld(serverWorld);
            }

            bomberPlayerComponent.reset();
        });

        ShouldPunishGunShooter.EVENT.register((shooter, victim) -> {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(shooter.getWorld());
            if (gameComponent.isRole(shooter, CORRUPT_COP)) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            return null;
        });

        DoorInteraction.EVENT.register((DoorInteraction.DoorInteractionContext context) -> {
            if (context.isBlasted() || context.isJammed()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            if (context.isOpen()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            PlayerEntity player = context.getPlayer();

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.getWorld());
            if (gameWorld.isRole(player, Noellesroles.JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);
                if (jesterComponent.inPsychoMode) {
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                }
            }

            ItemStack handItem = context.getHandItem();
            DoorInteraction.DoorType doorType = context.getDoorType();
            if (handItem.isOf(ModItems.MASTER_KEY)) {
                if (doorType == DoorInteraction.DoorType.TRAIN_DOOR || context.requiresKey()) {
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                }
            }
            if (handItem.isOf(ModItems.NEUTRAL_MASTER_KEY)) {
                if (player.getItemCooldownManager().isCoolingDown(ModItems.NEUTRAL_MASTER_KEY)) {
                    return DoorInteraction.DoorInteractionResult.DENY;
                }
                Role playerRole = gameWorld.getRole(player);
                if (NEUTRAL_MASTER_KEY_ROLES.contains(playerRole)){
                    player.getItemCooldownManager().set(ModItems.NEUTRAL_MASTER_KEY, 200);
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                } else if (gameWorld.isRole(player, Noellesroles.CORRUPT_COP) && doorType == DoorInteraction.DoorType.SMALL_DOOR){
                    player.getItemCooldownManager().set(ModItems.NEUTRAL_MASTER_KEY, 200);
                    return DoorInteraction.DoorInteractionResult.ALLOW;
                }
            }
            return DoorInteraction.DoorInteractionResult.PASS;
        });

        // 游戏结束时清理投掷斧实体
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                for (var entity : serverWorld.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.entity.ThrowingAxeEntity.class), e -> true)) {
                    entity.discard();
                }
            }
        });

        // 游戏胜利确定时，杀死所有被饕餮吞噬的玩家
        GameEvents.ON_WIN_DETERMINED.register((world, gameComponent, winStatus, neutralWinner) -> {
            for (UUID taotieUuid : gameComponent.getAllWithRole(TAOTIE)) {
                ServerPlayerEntity taotie = (ServerPlayerEntity) world.getPlayerByUuid(taotieUuid);
                if (taotie != null && GameFunctions.isPlayerPlayingAndAlive(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                    List<UUID> swallowedPlayers = taotieComp.getSwallowedPlayers();
                    for (UUID swallowedUuid : swallowedPlayers) {
                        ServerPlayerEntity swallowed = (ServerPlayerEntity) world.getPlayerByUuid(swallowedUuid);
                        if (GameFunctions.isPlayerPlayingAndAlive(swallowed)) {
                            GameFunctions.killPlayer(swallowed, false, taotie, DEATH_REASON_DIGESTED);
                        }
                    }
                }
            }
        });
    }


    public void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.MORPH_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());

            if (payload.player() == null) return;
            if (abilityPlayerComponent.cooldown > 0) return;
            PlayerEntity abilityTarget = context.player().getWorld().getPlayerByUuid(payload.player());

            if (abilityTarget != null && gameWorldComponent.isRole(context.player(), VOODOO) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 30);
                abilityPlayerComponent.sync();
                VoodooPlayerComponent voodooPlayerComponent = (VoodooPlayerComponent) VoodooPlayerComponent.KEY.get(context.player());
                voodooPlayerComponent.setTarget(payload.player());
                ServerPlayerEntity recordTarget = abilityTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                GameRecordManager.recordSkillUse(context.player(), VOODOO_ID, recordTarget, null);

            }
            // 变形者允许变形为已死亡玩家（旁观者），只需目标在线即可
            if (abilityTarget != null && gameWorldComponent.isRole(context.player(), MORPHLING) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (!gameWorldComponent.getAllPlayers().contains(payload.player())) return;
                MorphlingPlayerComponent morphlingPlayerComponent = (MorphlingPlayerComponent) MorphlingPlayerComponent.KEY.get(context.player());
                // 服务端验证冷却是否结束，防止作弊
                if (morphlingPlayerComponent.getMorphTicks() != 0) return;
                morphlingPlayerComponent.startMorph(payload.player());
                ServerPlayerEntity recordTarget = abilityTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "morph");
                extra.putUuid("disguise_as", payload.player());
                GameRecordManager.recordSkillUse(context.player(), MORPHLING_ID, recordTarget, extra);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(MorphCorpseToggleC2SPacket.ID, (payload, context) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            if (!gameWorldComponent.isRole(context.player(), MORPHLING)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(context.player())) return;

            MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(context.player());
            comp.toggleCorpseMode();
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.VULTURE_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(context.player());

            if (gameWorldComponent.isRole(context.player(), VULTURE) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (abilityPlayerComponent.getCooldown() > 0) return;
                List<PlayerBodyEntity> playerBodyEntities = context.player().getWorld().getEntitiesByType(TypeFilter.equals(PlayerBodyEntity.class), context.player().getBoundingBox().expand(5), (playerBodyEntity -> {
                    return playerBodyEntity.getUuid().equals(payload.playerBody());
                }));
                if (!playerBodyEntities.isEmpty()) {
                    PlayerBodyEntity body = playerBodyEntities.getFirst();
                    UUID bodyPlayerUuid = body.getPlayerUuid();
                    Vec3d bodyPos = body.getPos();
                    abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 5));
                    VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(context.player());
                    vulturePlayerComponent.addBody(body.getUuid());

                    // 生成粒子效果
                    if (context.player().getWorld() instanceof ServerWorld serverWorld) {
                        Vec3d pos = body.getPos();
                        serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.5, pos.z, 30, 0.3, 0.3, 0.3, 0.02);
                        serverWorld.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.01);
                    }

                    // 吃掉尸体后获得速度加成
                    context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, GameConstants.getInTicks(0, 10), 2, false, false, true));

                    // 移除尸体
                    body.discard();
                    ServerPlayerEntity recordTarget = null;
                    if (bodyPlayerUuid != null) {
                        PlayerEntity targetPlayer = context.player().getServerWorld().getPlayerByUuid(bodyPlayerUuid);
                        if (targetPlayer instanceof ServerPlayerEntity serverTarget) {
                            recordTarget = serverTarget;
                        }
                    }
                    NbtCompound extra = new NbtCompound();
                    extra.putUuid("body_uuid", body.getUuid());
                    if (bodyPlayerUuid != null) {
                        extra.putUuid("body_player_uuid", bodyPlayerUuid);
                        extra.putUuid("target", bodyPlayerUuid);
                    }
                    GameRecordManager.putPos(extra, "body_pos", bodyPos);
                    GameRecordManager.recordSkillUse(context.player(), VULTURE_ID, recordTarget, extra);
                }

            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SWAP_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            if (gameWorldComponent.isRole(context.player(), SWAPPER) && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (payload.player() != null) {
                    if (context.player().getWorld().getPlayerByUuid(payload.player()) != null) {
                        if (payload.player2() != null) {
                            if (context.player().getWorld().getPlayerByUuid(payload.player2()) != null) {

                                PlayerEntity player1 = context.player().getWorld().getPlayerByUuid(payload.player());
                                PlayerEntity player2 = context.player().getWorld().getPlayerByUuid(payload.player2());

                                if(player1 == null)
                                    return;
                                if(player2 == null)
                                    return;
                                if(SwallowedPlayerComponent.isPlayerSwallowed(player1))
                                    return;
                                if(SwallowedPlayerComponent.isPlayerSwallowed(player2))
                                    return;

                                if(player1.isSleeping()){
                                    player1.wakeUp();
                                }
                                if(player2.isSleeping()){
                                    player2.wakeUp();
                                }

                                if (player1.hasVehicle()) {
                                    player1.stopRiding();
                                }
                                if (player2.hasVehicle()) {
                                    player2.stopRiding();
                                }

                                Vec3d swappedPos1 = player1.getPos();
                                Vec3d swappedPos2 = player2.getPos();
                                var x1 = swappedPos1.x;
                                var y1 = swappedPos1.y;
                                var z1 = swappedPos1.z;
                                var yaw1 = player1.getYaw();
                                var pitch1 = player1.getPitch();
                                var world1 = (ServerWorld) player1.getWorld();
                                var x2 = swappedPos2.x;
                                var y2 = swappedPos2.y;
                                var z2 = swappedPos2.z;
                                var yaw2 = player2.getYaw();
                                var pitch2 = player2.getPitch();
                                var world2 = (ServerWorld) player2.getWorld();

//                                if (!context.player().getWorld().isSpaceEmpty(player1)) return;
//                                if (!context.player().getWorld().isSpaceEmpty(player2)) return;

                                Set<PositionFlag> movementFlags = EnumSet.noneOf(PositionFlag.class);
                                boolean swapped = false;
                                // 传送前在两个玩家的原位置播放粒子和音效
                                world1.sendEntityStatus(player1, EntityStatuses.ADD_PORTAL_PARTICLES);
                                world1.playSound(null, x1, y1, z1, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                world2.sendEntityStatus(player2, EntityStatuses.ADD_PORTAL_PARTICLES);
                                world2.playSound(null, x2, y2, z2, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                if (player1.teleport(world2, x2, y2, z2, movementFlags, yaw2, pitch2)) {
                                    AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(context.player());
                                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 0);
                                    abilityPlayerComponent.sync();
                                    if (!player1.isFallFlying()) {
                                        player1.setVelocity(player1.getVelocity().multiply(1.0, 0.0, 1.0));
                                        player1.setOnGround(true);
                                    }
                                    if (player2.teleport(world1, x1, y1, z1, movementFlags, yaw1, pitch1)) {
                                        if (!player2.isFallFlying()) {
                                            player2.setVelocity(player1.getVelocity().multiply(1.0, 0.0, 1.0));
                                            player2.setOnGround(true);
                                        }
                                        swapped = true;
                                    }
                                }
                                if (swapped) {
                                    // 传送后在目标位置播放音效（player1 现在在 pos2，player2 现在在 pos1）
                                    world2.playSound(null, x2, y2, z2, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                    world1.playSound(null, x1, y1, z1, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                    NbtCompound extra = new NbtCompound();
                                    extra.putString("action", "swap");
                                    extra.putUuid("target1", player1.getUuid());
                                    extra.putUuid("target2", player2.getUuid());
                                    GameRecordManager.putPos(extra, "target1_pos", swappedPos1);
                                    GameRecordManager.putPos(extra, "target2_pos", swappedPos2);
                                    ServerPlayerEntity recordTarget = player1 instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                                    GameRecordManager.recordSkillUse(context.player(), SWAPPER_ID, recordTarget, extra);
                                }
                            }
                        }
                    }
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ABILITY_PACKET, (payload, context) -> {
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            if (gameWorldComponent.isRole(context.player(), RECALLER) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                RecallerPlayerComponent recallerPlayerComponent = RecallerPlayerComponent.KEY.get(context.player());
                PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(context.player());
                if (!recallerPlayerComponent.placed) {
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0,10);
                    recallerPlayerComponent.setPosition();
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "place");
                    extra.putDouble("x", recallerPlayerComponent.x);
                    extra.putDouble("y", recallerPlayerComponent.y);
                    extra.putDouble("z", recallerPlayerComponent.z);
                    GameRecordManager.recordSkillUse(context.player(), RECALLER_ID, null, extra);
                }
                else if (playerShopComponent.balance >= 100) {
                    playerShopComponent.balance -= 100;
                    playerShopComponent.sync();
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0,30);
                    double targetX = recallerPlayerComponent.x;
                    double targetY = recallerPlayerComponent.y;
                    double targetZ = recallerPlayerComponent.z;
                    recallerPlayerComponent.teleport();
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "teleport");
                    extra.putDouble("x", targetX);
                    extra.putDouble("y", targetY);
                    extra.putDouble("z", targetZ);
                    GameRecordManager.recordSkillUse(context.player(), RECALLER_ID, null, extra);
                }

            }
            if (gameWorldComponent.isRole(context.player(), PHANTOM) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 30 * 20,0,true,false,true));
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 30);
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "invisible");
                GameRecordManager.recordSkillUse(context.player(), PHANTOM_ID, null, extra);
            }
            // Pathogen infection ability
            if (gameWorldComponent.isRole(context.player(), PATHOGEN) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerPlayingAndAlive(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                // Find nearest uninfected player within 3 blocks (with line of sight)
                PlayerEntity nearestTarget = null;
                double nearestDistance = 9.0; // 3^2 = 9

                for (UUID playerUuid : gameWorldComponent.getAllPlayers()) {
                    PlayerEntity player = context.player().getWorld().getPlayerByUuid(playerUuid);
                    if (player == null || player.equals(context.player())) continue;
                    if (!GameFunctions.isPlayerPlayingAndAlive(player)) continue;

                    InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                    if (infected.isInfected()) continue;

                    double distance = context.player().squaredDistanceTo(player);
                    if (distance < nearestDistance) {
                        // 检查视线（不能隔墙感染）
                        if (context.player().canSee(player)) {
                            nearestDistance = distance;
                            nearestTarget = player;
                        }
                    }
                }

                // Infect the nearest target
                if (nearestTarget != null) {
                    InfectedPlayerComponent targetInfected = InfectedPlayerComponent.KEY.get(nearestTarget);
                    targetInfected.setInfected(true, context.player().getUuid());

                    // Set cooldown based on player count (calculated at game start)
                    PathogenPlayerComponent pathogenComp = PathogenPlayerComponent.KEY.get(context.player());
                    abilityPlayerComponent.setCooldown(pathogenComp.getBaseCooldownTicks());
                    ServerPlayerEntity recordTarget = nearestTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
                    NbtCompound extra = new NbtCompound();
                    extra.putString("action", "infect");
                    GameRecordManager.recordSkillUse(context.player(), PATHOGEN_ID, recordTarget, extra);
                }
            }
        });

        // 刺客猜测角色（唯一需要服务器处理的数据包）
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ASSASSIN_GUESS_ROLE_PACKET, (payload, context) -> {
            ServerPlayerEntity assassin = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(assassin.getWorld());

            // 验证角色和状态
            if (!gameWorldComponent.isRole(assassin, ASSASSIN)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(assassin)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(assassin)) return;

            AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(assassin);
            if (!assassinComp.canGuess()) return;

            // 验证目标
            ServerPlayerEntity target = (ServerPlayerEntity) assassin.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;

            // 🔒 关键安全验证：防止恶意客户端猜测不可猜测的角色
            if (target.equals(assassin)) return;  // 不能猜测自己
            if (gameWorldComponent.isRole(target, WatheRoles.VIGILANTE)) return;  // 义警不能被猜测
            Role targetRole = gameWorldComponent.getRole(target);
            if (targetRole == null) return;
            if (WatheRoles.SPECIAL_ROLES.contains(targetRole)) return;  // 特殊角色不能被猜测
            if (targetRole.equals(ASSASSIN)) return;  // 不能猜测其他刺客

            // 判断猜测是否正确
            boolean guessedCorrectly = targetRole.identifier().equals(payload.guessedRole());



            // 执行结果
            if (guessedCorrectly) {
                // 发送消息给刺客（通过 actionbar 显示）- 先发送消息再击杀
                assassin.sendMessage(
                    net.minecraft.text.Text.translatable("tip.assassin.guess_correct", target.getName())
                        .formatted(net.minecraft.util.Formatting.GREEN, net.minecraft.util.Formatting.BOLD),
                    true
                );

                // 播放枪响音效（对所有玩家可见）
                assassin.getWorld().playSound(
                        null,  // 所有人都能听到
                        target.getX(), target.getY(), target.getZ(),
                        WatheSounds.ITEM_REVOLVER_SHOOT,
                        SoundCategory.PLAYERS,
                        2.0F,  // 音量
                        1.0F   // 音调
                );

                // 猜对：杀死目标
                GameFunctions.killPlayer(target, true, assassin, DEATH_REASON_ASSASSINATED);
            } else {
                // 发送消息（通过 actionbar 显示）- 先发送消息再自杀
                assassin.sendMessage(
                    net.minecraft.text.Text.translatable("tip.assassin.guess_wrong", target.getName())
                        .formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD),
                    true
                );

                // 播放枪响音效（对所有玩家可见）
                assassin.getWorld().playSound(
                        null,  // 所有人都能听到
                        assassin.getX(), assassin.getY(), assassin.getZ(),
                        WatheSounds.ITEM_REVOLVER_SHOOT,
                        SoundCategory.PLAYERS,
                        2.0F,  // 音量
                        1.0F   // 音调
                );
                // 猜错：自己死亡
                GameFunctions.killPlayer(assassin, true, null, DEATH_REASON_ASSASSIN_MISFIRE);
            }

            NbtCompound extra = new NbtCompound();
            extra.putString("action", "guess");
            extra.putString("guessed_role", payload.guessedRole().toString());
            extra.putBoolean("correct", guessedCorrectly);
            GameRecordManager.recordSkillUse(assassin, ASSASSIN_ID, target, extra);

            // 消耗猜测次数，设置冷却
            assassinComp.useGuess();
        });

        // 记者标记目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.REPORTER_MARK_PACKET, (payload, context) -> {
            ServerPlayerEntity reporter = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(reporter.getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(reporter);

            // 验证角色和状态
            if (!gameWorldComponent.isRole(reporter, REPORTER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(reporter)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(reporter)) return;
            if (abilityPlayerComponent.cooldown > 0) return;

            // 验证目标
            if (payload.targetPlayer() == null) return;
            PlayerEntity target = reporter.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(reporter)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;

            // 验证距离（3格内）
            double distance = reporter.squaredDistanceTo(target);
            if (distance > 9.0) return; // 3^2 = 9

            // 验证视线
            if (!reporter.canSee(target)) return;

            // 设置标记
            ReporterPlayerComponent reporterComp = ReporterPlayerComponent.KEY.get(reporter);
            reporterComp.setMarkedTarget(target.getUuid());
            // 设置冷却30秒
            abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 30));
            NbtCompound extra = new NbtCompound();
            extra.putString("action", "mark");
            GameRecordManager.recordSkillUse(reporter, REPORTER_ID, target instanceof ServerPlayerEntity serverTarget ? serverTarget : null, extra);
        });

        // 饕餮吞噬目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.TAOTIE_SWALLOW_PACKET, (payload, context) -> {
            ServerPlayerEntity taotie = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(taotie.getWorld());

            // 验证角色和状态
            if (!gameWorldComponent.isRole(taotie, TAOTIE)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(taotie)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(taotie)) return;

            TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
            if (taotieComp.getSwallowCooldown() > 0) return;

            // 验证目标
            if (payload.targetPlayer() == null) return;
            ServerPlayerEntity target = (ServerPlayerEntity) taotie.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(taotie)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;

            // 验证距离（3格内）
            double distance = taotie.squaredDistanceTo(target);
            if (distance > TaotiePlayerComponent.SWALLOW_DISTANCE_SQUARED) return;

            // 验证视线
            if (!taotie.canSee(target)) return;

            // 执行吞噬
            if (taotieComp.swallowPlayer(target)) {
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "swallow");
                GameRecordManager.recordSkillUse(taotie, TAOTIE_ID, target, extra);
            }
        });

        // 静语者沉默目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SILENCER_SILENCE_PACKET, (payload, context) -> {
            ServerPlayerEntity silencer = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(silencer.getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(silencer);
            SilencerPlayerComponent silencerComp = SilencerPlayerComponent.KEY.get(silencer);

            // 验证角色和状态
            if (!gameWorldComponent.isRole(silencer, SILENCER)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(silencer)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(silencer)) return;
            if (abilityPlayerComponent.cooldown > 0) return;

            // 检查是否已有标记目标
            if (silencerComp.hasMarkedTarget()) {
                // 已有标记 → 执行沉默（不判断瞄准/距离/视线）
                ServerPlayerEntity target = (ServerPlayerEntity) silencer.getWorld().getPlayerByUuid(silencerComp.getMarkedTargetUuid());
                // 清除标记
                silencerComp.clearMark();

                if (target == null) return;
                if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
                if (SwallowedPlayerComponent.isPlayerSwallowed(target)) return;

                // 检查目标是否已被沉默
                SilencedPlayerComponent silencedComp = SilencedPlayerComponent.KEY.get(target);
                if (silencedComp.isSilenced()) return;

                // 执行沉默
                silencedComp.applySilence(silencer.getUuid());

                // 设置冷却45秒
                abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 45));

                // 给静语者发送成功提示
                silencer.sendMessage(
                    Text.translatable("tip.silencer.success", target.getName())
                        .formatted(net.minecraft.util.Formatting.GRAY),
                    true
                );

                // 记录技能使用
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "silence");
                GameRecordManager.recordSkillUse(silencer, SILENCER_ID, target, extra);
                return;
            }

            // 没有有效标记 → 进行标记（需要瞄准目标）
            if (payload.targetPlayer() == null) return;
            ServerPlayerEntity target = (ServerPlayerEntity) silencer.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(silencer)) return;
            if (!GameFunctions.isPlayerPlayingAndAlive(target)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(target)) return;

            // 验证距离（3格内）
            double distance = silencer.squaredDistanceTo(target);
            if (distance > 9.0) return; // 3^2 = 9

            // 验证视线
            if (!silencer.canSee(target)) return;

            // 检查目标是否已被沉默
            SilencedPlayerComponent silencedComp = SilencedPlayerComponent.KEY.get(target);
            if (silencedComp.isSilenced()) {
                silencer.sendMessage(
                    Text.translatable("tip.silencer.already_silenced", target.getName())
                        .formatted(net.minecraft.util.Formatting.GRAY),
                    true
                );
                return;
            }

            // 标记目标（通过组件，自动同步到客户端）
            silencerComp.markTarget(target.getUuid(), target.getName().getString());

            // 给静语者发送标记提示
            silencer.sendMessage(
                Text.translatable("tip.silencer.marked", target.getName())
                    .formatted(net.minecraft.util.Formatting.GRAY),
                true
            );
        });
    }

    /**
     * 注册 DLC 回放格式化器
     */
    private void registerReplayFormatters() {
        // 小丑时刻/禁锢 全局事件格式化器（显示触发者）
        ReplayEventFormatter jesterTriggerFormatter = (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String eventId = data.getString("event");
            Identifier id = Identifier.tryParse(eventId);
            String translationKey = id != null ? "replay.global." + id.getNamespace() + "." + id.getPath() : "replay.global.unknown";

            if (data.containsUuid("trigger")) {
                Text triggerText = ReplayGenerator.formatPlayerName(data.getUuid("trigger"), playerInfoCache);
                return Text.translatable(translationKey, actorText, triggerText);
            }
            return Text.translatable(translationKey, actorText);
        };
        ReplayRegistry.registerGlobalEventFormatter(Identifier.of(MOD_ID, "jester_moment_start"), jesterTriggerFormatter);
        ReplayRegistry.registerGlobalEventFormatter(Identifier.of(MOD_ID, "jester_stasis_start"), jesterTriggerFormatter);

        // death_blocked 格式化器
        ReplayRegistry.registerFormatter("death_blocked", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            String blockReason = data.getString("block_reason");

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            // 铁人药剂由专属 iron_man_activated 事件处理，此处返回null避免重复
            if ("iron_man_buff".equals(blockReason)) return null;

            // 根据 block_reason 选择翻译键
            String translationKey = switch (blockReason) {
                case "corrupt_cop_moment" -> "replay.death_blocked.corrupt_cop_moment";
                case "taotie_moment" -> "replay.death_blocked.taotie_moment";
                case "jester_stasis" -> "replay.death_blocked.jester_stasis";
                default -> "replay.death_blocked.unknown";
            };

            return Text.translatable(translationKey, actorText);
        });

        // iron_man_activated 铁人药剂生效格式化器
        ReplayRegistry.registerFormatter("iron_man_activated", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            String action = data.getString("action");

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            String translationKey = switch (action) {
                case "block_swallow" -> "replay.iron_man_activated.block_swallow";
                case "block_damage" -> "replay.iron_man_activated.block_damage";
                default -> "replay.iron_man_activated.block_damage";
            };

            return Text.translatable(translationKey, actorText);
        });

        // voodoo_chain_death 格式化器
        ReplayRegistry.registerFormatter("voodoo_chain_death", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;

            if (actorUuid == null || targetUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);

            return Text.translatable("replay.voodoo_chain_death", actorText, targetText);
        });

        // death_in_stomach 格式化器
        ReplayRegistry.registerFormatter("death_in_stomach", (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;

            if (actorUuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);

            return Text.translatable("replay.death_in_stomach", actorText);
        });

        // 交换者技能格式化器（显示交换的两个玩家）
        ReplayRegistry.registerSkillFormatter(SWAPPER_ID, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID target1Uuid = data.containsUuid("target1") ? data.getUuid("target1") : null;
            UUID target2Uuid = data.containsUuid("target2") ? data.getUuid("target2") : null;

            if (actorUuid == null || target1Uuid == null || target2Uuid == null) return null;

            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text target1Text = ReplayGenerator.formatPlayerName(target1Uuid, playerInfoCache);
            Text target2Text = ReplayGenerator.formatPlayerName(target2Uuid, playerInfoCache);

            return Text.translatable("replay.skill.noellesroles.swapper.target", actorText, target1Text, target2Text);
        });

        // silencer skill 格式化器（通过 skillUse 系统自动处理，这里只需要注册翻译键）
        // 翻译键 replay.skill.noellesroles.silencer.target 在语言文件中定义

        // ===== 物品使用格式化器 =====

        Identifier fineDrinkId = Registries.ITEM.getId(ModItems.FINE_DRINK);
        Identifier timedBombId = Registries.ITEM.getId(ModItems.TIMED_BOMB);
        Identifier antidoteId = Registries.ITEM.getId(ModItems.ANTIDOTE);
        Identifier ironManVialId = Registries.ITEM.getId(ModItems.IRON_MAN_VIAL);
        Identifier poisonNeedleId = Registries.ITEM.getId(ModItems.POISON_NEEDLE);
        Identifier poisonGasBombId = Registries.ITEM.getId(ModItems.POISON_GAS_BOMB);

        // 上等佳酿 - 喝下 / 放置到餐盘
        ReplayRegistry.registerItemUseFormatter(fineDrinkId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            String action = data.getString("action");
            if ("place".equals(action)) {
                return Text.translatable("replay.item_use.noellesroles.fine_drink.place", actorText);
            }
            return Text.translatable("replay.item_use.noellesroles.fine_drink.drink", actorText);
        });

        // 定时炸弹 - 传递
        ReplayRegistry.registerItemUseFormatter(timedBombId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.timed_bomb.transfer", actorText, targetText);
        });

        // 解毒剂 - 解毒
        ReplayRegistry.registerItemUseFormatter(antidoteId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.antidote", actorText, targetText);
        });

        // 铁人药剂 - 注射
        ReplayRegistry.registerItemUseFormatter(ironManVialId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.iron_man_vial", actorText, targetText);
        });

        // 毒针 - 刺
        ReplayRegistry.registerItemUseFormatter(poisonNeedleId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (actorUuid == null || targetUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.poison_needle", actorText, targetText);
        });

        // 毒气弹 - 投掷
        ReplayRegistry.registerItemUseFormatter(poisonGasBombId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.poison_gas_bomb", actorText);
        });

        // ===== 餐盘拿取格式化器 =====

        // 上等佳酿 - 从餐盘拿取
        ReplayRegistry.registerPlatterTakeFormatter(fineDrinkId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.platter_take.noellesroles.fine_drink", actorText);
        });

        // 催化剂
        Identifier catalystId = Registries.ITEM.getId(ModItems.CATALYST);
        ReplayRegistry.registerItemUseFormatter(catalystId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
            if (targetUuid != null) {
                Text targetText = ReplayGenerator.formatPlayerName(targetUuid, playerInfoCache);
                return Text.translatable("replay.item_use.noellesroles.catalyst", actorText, targetText);
            }
            return Text.translatable("replay.item_use.noellesroles.catalyst.no_target", actorText);
        });

        // 投掷斧 - 使用
        Identifier throwingAxeId = Registries.ITEM.getId(ModItems.THROWING_AXE);
        ReplayRegistry.registerItemUseFormatter(throwingAxeId, (event, match, world) -> {
            var playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            NbtCompound data = event.data();
            UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
            if (actorUuid == null) return null;
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, playerInfoCache);
            return Text.translatable("replay.item_use.noellesroles.throwing_axe", actorText);
        });

    }

}
