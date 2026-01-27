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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
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
import org.agmas.noellesroles.packet.CorruptCopMomentS2CPacket;
import org.agmas.noellesroles.packet.ReporterMarkC2SPacket;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.packet.TaotieSwallowC2SPacket;
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
    // 炸弹死亡原因
    public static Identifier DEATH_REASON_BOMB = Identifier.of(MOD_ID, "bomb");
    // 刺客死亡原因
    public static Identifier DEATH_REASON_ASSASSINATED = Identifier.of(MOD_ID, "assassinated");  // 被刺客猜中身份
    public static Identifier DEATH_REASON_ASSASSIN_MISFIRE = Identifier.of(MOD_ID, "assassin_misfire");  // 刺客猜错自己死亡
    public static Identifier DEATH_REASON_JESTER_TIMEOUT = Identifier.of(MOD_ID, "jester_timeout");
    // 饕餮吞噬死亡原因（游戏结束时被消化）
    public static Identifier DEATH_REASON_DIGESTED = Identifier.of(MOD_ID, "digested");

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


    // 小丑角色 - 中立阵营，被无辜者杀死时获胜
    public static Role JESTER = WatheRoles.registerRole(new Role(JESTER_ID, new Color(248, 200, 220).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role VULTURE =WatheRoles.registerRole(new Role(VULTURE_ID, new Color(181, 103, 0).getRGB(),false,false,Role.MoodType.FAKE,GameConstants.getInTicks(0, 40),false));
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
    public static final ArrayList<Role> VANNILA_ROLES = new ArrayList<>();
    public static final ArrayList<Identifier> VANNILA_ROLE_IDS = new ArrayList<>();
    // 中立万能钥匙可用角色集合
    private static final Set<Role> NEUTRAL_MASTER_KEY_ROLES = Set.of(VULTURE, PATHOGEN, TAOTIE);

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
        PayloadTypeRegistry.playC2S().register(AbilityC2SPacket.ID, AbilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapperC2SPacket.ID, SwapperC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VultureEatC2SPacket.ID, VultureEatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AssassinGuessRoleC2SPacket.ID, AssassinGuessRoleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ReporterMarkC2SPacket.ID, ReporterMarkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TaotieSwallowC2SPacket.ID, TaotieSwallowC2SPacket.CODEC);
        // 注册S2C数据包
        PayloadTypeRegistry.playS2C().register(CorruptCopMomentS2CPacket.ID, CorruptCopMomentS2CPacket.CODEC);

        registerEvents();

        BartenderShopHandler.register();
        BomberShopHandler.register();
        ScavengerShopHandler.register();
        TimekeeperShopHandler.register();

        registerPackets();
        //NoellesRolesEntities.init();

    }


    public void registerEvents() {

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GameWorldComponent.KEY.get(server.getOverworld()).setRoleEnabled(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES, false);
            GameWorldComponent.KEY.get(server.getOverworld()).setRoleEnabled(AWESOME_BINGLUS, false);
        });

        // Master key should drop on death
        ShouldDropOnDeath.EVENT.register((stack, victim) -> stack.isOf(ModItems.MASTER_KEY));

        KillPlayer.BEFORE.register(((victim, killer, deathReason) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());

            // 黑警被杀时结束黑警时刻
            if (gameWorldComponent.isRole(victim, CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(victim);
                if(corruptCopComp.isCorruptCopMomentActive() && deathReason == DEATH_REASON_ASSASSINATED){
                    return KillPlayer.KillResult.cancel();
                }
            }

            // 饕餮时刻时无法被刺客杀死
            if (gameWorldComponent.isRole(victim, TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(victim);
                if(taotieComp.isTaotieMomentActive() && deathReason == DEATH_REASON_ASSASSINATED){
                    return KillPlayer.KillResult.cancel();
                }
            }

            if (gameWorldComponent.isRole(victim, JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                if (jesterComponent.inStasis) {
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
                ironManComp.removeBuff();
                return KillPlayer.KillResult.cancel();
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
            if (gameWorldComponent.isRole((PlayerEntity) player, Noellesroles.BARTENDER)) {
                return true;
            }
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
            for (UUID uuid : gameComponent.getAllWithRole(VULTURE)) {
                PlayerEntity vulture = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerAliveAndSurvival(vulture)) {
                    VulturePlayerComponent component = VulturePlayerComponent.KEY.get(vulture);
                    if (component.hasWon()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) vulture);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(PATHOGEN)) {
                PlayerEntity pathogen = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerAliveAndSurvival(pathogen)) {
                    boolean allInfected = true;
                    for (UUID playerUuid : gameComponent.getAllPlayers()) {
                        if (playerUuid.equals(uuid)) continue;
                        PlayerEntity player = world.getPlayerByUuid(playerUuid);
                        if (player == null) continue;
                        if (!GameFunctions.isPlayerAliveAndSurvival(player)) continue;
                        InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(player);
                        if (!infected.isInfected()) {
                            allInfected = false;
                            break;
                        }
                    }
                    if (allInfected && GameFunctions.isPlayerAliveAndSurvival(pathogen)) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) pathogen);
                    }
                }
            }

            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = world.getPlayerByUuid(uuid);
                if (GameFunctions.isPlayerAliveAndSurvival(jester)) {
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
                if (GameFunctions.isPlayerAliveAndSurvival(taotie)) {
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
                if (GameFunctions.isPlayerAliveAndSurvival(player)) {
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
                if (GameFunctions.isPlayerAliveAndSurvival(player)) {
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

            // 连环杀手处理：击杀目标奖励和目标更换
            if (victim.getWorld() instanceof ServerWorld serverWorld) {
                for (UUID uuid : gameComponent.getAllWithRole(SERIAL_KILLER)) {
                    PlayerEntity serialKiller = serverWorld.getPlayerByUuid(uuid);
                    if (GameFunctions.isPlayerAliveAndSurvival(serialKiller)) {
                        SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(serialKiller);

                        // 如果被杀者是连环杀手的目标
                        if (serialKillerComp.isCurrentTarget(victim.getUuid())) {
                            // 如果是连环杀手亲自击杀的，给予额外金钱奖励
                            if (killer != null && killer.getUuid().equals(serialKiller.getUuid())) {
                                PlayerShopComponent.KEY.get(killer).addToBalance(SerialKillerPlayerComponent.getBonusMoney());
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
                if(GameFunctions.isPlayerAliveAndSurvival(bomber)){
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
                    if (voodooPlayerComponent.target != null && (deathReason != DEATH_REASON_ASSASSINATED || !gameComponent.isRole(victim ,ASSASSIN))) {
                        PlayerEntity voodooed = victim.getWorld().getPlayerByUuid(voodooPlayerComponent.target);
                        if (voodooed != null) {
                            if (GameFunctions.isPlayerAliveAndSurvival(voodooed) && voodooed != victim) {
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
                for (UUID uuid : gameComponent.getAllWithRole(CORRUPT_COP)) {
                    PlayerEntity corruptCop = serverWorld.getPlayerByUuid(uuid);
                    if (GameFunctions.isPlayerAliveAndSurvival(corruptCop)) {
                        CorruptCopPlayerComponent corruptCopComp = CorruptCopPlayerComponent.KEY.get(corruptCop);
                        // 计算当前存活人数
                        int aliveCount = 0;
                        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                            if (GameFunctions.isPlayerAliveAndSurvival(p)) {
                                aliveCount++;
                            }
                        }
                        corruptCopComp.checkAndTriggerMoment(aliveCount);
                    }
                }

                // 检查是否应该触发饕餮时刻
                for (UUID uuid : gameComponent.getAllWithRole(TAOTIE)) {
                    PlayerEntity taotie = serverWorld.getPlayerByUuid(uuid);
                    if (GameFunctions.isPlayerAliveAndSurvival(taotie)) {
                        TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                        // 计算当前存活人数
                        int aliveCountForTaotie = 0;
                        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                            if (GameFunctions.isPlayerAliveAndSurvival(p)) {
                                SwallowedPlayerComponent swallowed = SwallowedPlayerComponent.KEY.get(p);
                                if (!swallowed.isSwallowed()) {
                                    aliveCountForTaotie++;
                                }
                            }
                        }
                        taotieComp.checkAndTriggerMoment(aliveCountForTaotie);
                    }
                }
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

        // 游戏胜利确定时，杀死所有被饕餮吞噬的玩家
        GameEvents.ON_WIN_DETERMINED.register((world, gameComponent, winStatus, neutralWinner) -> {
            for (UUID taotieUuid : gameComponent.getAllWithRole(TAOTIE)) {
                PlayerEntity taotie = world.getPlayerByUuid(taotieUuid);
                if (taotie != null && GameFunctions.isPlayerAliveAndSurvival(taotie)) {
                    TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                    List<UUID> swallowedPlayers = taotieComp.getSwallowedPlayers();
                    for (UUID swallowedUuid : swallowedPlayers) {
                        PlayerEntity swallowed = world.getPlayerByUuid(swallowedUuid);
                        if (swallowed != null && GameFunctions.isPlayerAliveAndSurvival(swallowed)) {
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
            if (context.player().getWorld().getPlayerByUuid(payload.player()) == null) return;

            if (gameWorldComponent.isRole(context.player(), VOODOO) && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 30);
                abilityPlayerComponent.sync();
                VoodooPlayerComponent voodooPlayerComponent = (VoodooPlayerComponent) VoodooPlayerComponent.KEY.get(context.player());
                voodooPlayerComponent.setTarget(payload.player());

            }
            if (gameWorldComponent.isRole(context.player(), MORPHLING) && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                MorphlingPlayerComponent morphlingPlayerComponent = (MorphlingPlayerComponent) MorphlingPlayerComponent.KEY.get(context.player());
                // 服务端验证冷却是否结束，防止作弊
                if (morphlingPlayerComponent.getMorphTicks() != 0) return;
                morphlingPlayerComponent.startMorph(payload.player());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.VULTURE_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(context.player());

            if (gameWorldComponent.isRole(context.player(), VULTURE) && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                if (abilityPlayerComponent.getCooldown() > 0) return;
                List<PlayerBodyEntity> playerBodyEntities = context.player().getWorld().getEntitiesByType(TypeFilter.equals(PlayerBodyEntity.class), context.player().getBoundingBox().expand(5), (playerBodyEntity -> {
                    return playerBodyEntity.getUuid().equals(payload.playerBody());
                }));
                if (!playerBodyEntities.isEmpty()) {
                    PlayerBodyEntity body = playerBodyEntities.getFirst();
                    abilityPlayerComponent.setCooldown(GameConstants.getInTicks(0, 5));
                    VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(context.player());
                    vulturePlayerComponent.addBody(body.getUuid());

                    // 生成粒子效果
                    if (context.player().getWorld() instanceof ServerWorld serverWorld) {
                        Vec3d pos = body.getPos();
                        serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.5, pos.z, 30, 0.3, 0.3, 0.3, 0.02);
                        serverWorld.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.01);
                    }

                    // 移除尸体
                    body.discard();
                }

            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SWAP_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            if (gameWorldComponent.isRole(context.player(), SWAPPER) && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
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
                                    }
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
            if (gameWorldComponent.isRole(context.player(), RECALLER) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                RecallerPlayerComponent recallerPlayerComponent = RecallerPlayerComponent.KEY.get(context.player());
                PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(context.player());
                if (!recallerPlayerComponent.placed) {
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0,10);
                    recallerPlayerComponent.setPosition();
                }
                else if (playerShopComponent.balance >= 100) {
                    playerShopComponent.balance -= 100;
                    playerShopComponent.sync();
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0,30);
                    recallerPlayerComponent.teleport();
                }

            }
            if (gameWorldComponent.isRole(context.player(), PHANTOM) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 30 * 20,0,true,false,true));
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 30);
            }
            // Pathogen infection ability
            if (gameWorldComponent.isRole(context.player(), PATHOGEN) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(context.player()) && !SwallowedPlayerComponent.isPlayerSwallowed(context.player())) {
                // Find nearest uninfected player within 3 blocks (with line of sight)
                PlayerEntity nearestTarget = null;
                double nearestDistance = 9.0; // 3^2 = 9

                for (UUID playerUuid : gameWorldComponent.getAllPlayers()) {
                    PlayerEntity player = context.player().getWorld().getPlayerByUuid(playerUuid);
                    if (player == null || player.equals(context.player())) continue;
                    if (!GameFunctions.isPlayerAliveAndSurvival(player)) continue;

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
                }
            }
        });

        // 刺客猜测角色（唯一需要服务器处理的数据包）
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ASSASSIN_GUESS_ROLE_PACKET, (payload, context) -> {
            ServerPlayerEntity assassin = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(assassin.getWorld());

            // 验证角色和状态
            if (!gameWorldComponent.isRole(assassin, ASSASSIN)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(assassin)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(assassin)) return;

            AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(assassin);
            if (!assassinComp.canGuess()) return;

            // 验证目标
            ServerPlayerEntity target = (ServerPlayerEntity) assassin.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;

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
            if (!GameFunctions.isPlayerAliveAndSurvival(reporter)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(reporter)) return;
            if (abilityPlayerComponent.cooldown > 0) return;

            // 验证目标
            if (payload.targetPlayer() == null) return;
            PlayerEntity target = reporter.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(reporter)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;

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
        });

        // 饕餮吞噬目标
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.TAOTIE_SWALLOW_PACKET, (payload, context) -> {
            ServerPlayerEntity taotie = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(taotie.getWorld());

            // 验证角色和状态
            if (!gameWorldComponent.isRole(taotie, TAOTIE)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(taotie)) return;
            if (SwallowedPlayerComponent.isPlayerSwallowed(taotie)) return;

            TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
            if (taotieComp.getSwallowCooldown() > 0) return;

            // 验证目标
            if (payload.targetPlayer() == null) return;
            ServerPlayerEntity target = (ServerPlayerEntity) taotie.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (target.equals(taotie)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;

            // 验证距离（3格内）
            double distance = taotie.squaredDistanceTo(target);
            if (distance > TaotiePlayerComponent.SWALLOW_DISTANCE_SQUARED) return;

            // 验证视线
            if (!taotie.canSee(target)) return;

            // 执行吞噬
            taotieComp.swallowPlayer(target);
        });
    }



}
