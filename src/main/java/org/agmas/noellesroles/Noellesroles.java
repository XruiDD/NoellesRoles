package org.agmas.noellesroles;

import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerMoodComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerPsychoComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerShopComponent;
import dev.doctor4t.trainmurdermystery.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import dev.doctor4t.trainmurdermystery.event.CanSeePoison;
import dev.doctor4t.trainmurdermystery.event.CheckWinCondition;
import dev.doctor4t.trainmurdermystery.event.KillPlayer;
import dev.doctor4t.trainmurdermystery.event.PlayerPoisoned;
import dev.doctor4t.trainmurdermystery.event.ResetPlayer;
import dev.doctor4t.trainmurdermystery.event.RoleAssigned;
import dev.doctor4t.trainmurdermystery.event.ShouldDropOnDeath;
import dev.doctor4t.trainmurdermystery.event.ShouldPunishGunShooter;
import dev.doctor4t.trainmurdermystery.event.TaskComplete;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import dev.doctor4t.trainmurdermystery.index.TMMSounds;
import dev.doctor4t.trainmurdermystery.util.AnnounceWelcomePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;
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
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.agmas.noellesroles.packet.ScavengerResetKnifeCDC2SPacket;
import org.agmas.noellesroles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.bomber.BomberShopHandler;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.scavenger.ScavengerPlayerComponent;
import org.agmas.noellesroles.scavenger.ScavengerShopHandler;

import java.awt.*;
import java.lang.reflect.Constructor;
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
    // 炸弹死亡原因
    public static Identifier DEATH_REASON_BOMB = Identifier.of(MOD_ID, "bomb");
    // 刺客死亡原因
    public static Identifier DEATH_REASON_ASSASSINATED = Identifier.of(MOD_ID, "assassinated");  // 被刺客猜中身份
    public static Identifier DEATH_REASON_ASSASSIN_MISFIRE = Identifier.of(MOD_ID, "assassin_misfire");  // 刺客猜错自己死亡

    public static Role SWAPPER = TMMRoles.registerRole(new Role(SWAPPER_ID, new Color(57, 4, 170).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    public static Role PHANTOM =TMMRoles.registerRole(new Role(PHANTOM_ID, new Color(80, 5, 5, 192).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    public static Role MORPHLING =TMMRoles.registerRole(new Role(MORPHLING_ID, new Color(170, 2, 61).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    public static Role THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES = TMMRoles.registerRole(new Role(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID, new Color(255, 0, 0, 192).getRGB(),false,true, Role.MoodType.FAKE,Integer.MAX_VALUE,true));
    // 爆破手角色 - 杀手阵营，无法购买刀和枪，只能用炸弹
    public static Role BOMBER = TMMRoles.registerRole(new Role(BOMBER_ID, new Color(50, 50, 50).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 刺客角色 - 杀手阵营，可以猜测玩家身份
    public static Role ASSASSIN = TMMRoles.registerRole(new Role(ASSASSIN_ID, new Color(139, 0, 0).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));
    // 清道夫角色 - 杀手阵营，杀人后尸体对其他人不可见（秃鹫和中立除外），杀人奖励+50金币，只能买刀，可以花100金币重置刀CD
    public static Role SCAVENGER = TMMRoles.registerRole(new Role(SCAVENGER_ID, new Color(101, 67, 33).getRGB(), false, true, Role.MoodType.FAKE, Integer.MAX_VALUE, true));


    public static HashMap<Role, RoleAnnouncementTexts.RoleAnnouncementText> roleRoleAnnouncementTextHashMap = new HashMap<>();
    public static Role TIMEKEEPER = TMMRoles.registerRole(new Role(TIMEKEEPER_ID, new Color(0, 38, 255).getRGB(), true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), true));
    public static Role UNDERCOVER = TMMRoles.registerRole(new Role(UNDERCOVER_ID, new Color(192, 192, 192).getRGB(), true, false, Role.MoodType.NONE, GameConstants.getInTicks(0, 10), false));
    public static Role CONDUCTOR =TMMRoles.registerRole(new Role(CONDUCTOR_ID, new Color(255, 205, 84).getRGB(),true,false, Role.MoodType.REAL,TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role AWESOME_BINGLUS = TMMRoles.registerRole(new Role(AWESOME_BINGLUS_ID, new Color(155, 255, 168).getRGB(),true,false, Role.MoodType.REAL,TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role BARTENDER =TMMRoles.registerRole(new Role(BARTENDER_ID, new Color(217,241,240).getRGB(),true,false, Role.MoodType.REAL,TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role NOISEMAKER =TMMRoles.registerRole(new Role(NOISEMAKER_ID, new Color(200, 255, 0).getRGB(),true,false, Role.MoodType.REAL,TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role VOODOO =TMMRoles.registerRole(new Role(VOODOO_ID, new Color(128, 114, 253).getRGB(),true,false,Role.MoodType.REAL, TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role CORONER =TMMRoles.registerRole(new Role(CORONER_ID, new Color(122, 122, 122).getRGB(),true,false,Role.MoodType.REAL, TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role RECALLER = TMMRoles.registerRole(new Role(RECALLER_ID, new Color(158, 255, 255).getRGB(),true,false,Role.MoodType.REAL, TMMRoles.CIVILIAN.getMaxSprintTime(),false));
    public static Role TOXICOLOGIST = TMMRoles.registerRole(new Role(TOXICOLOGIST_ID, new Color(184, 41, 90).getRGB(), true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), false));


    // 小丑角色 - 中立阵营，被无辜者杀死时获胜
    public static Role JESTER = TMMRoles.registerRole(new Role(JESTER_ID, 0xF8C8DC, false, false, Role.MoodType.FAKE, TMMRoles.CIVILIAN.getMaxSprintTime(), false));
    public static Role VULTURE =TMMRoles.registerRole(new Role(VULTURE_ID, new Color(181, 103, 0).getRGB(),false,false,Role.MoodType.FAKE,GameConstants.getInTicks(0, 20),false));
    // 黑警角色 - 中立阵营，杀光所有人获胜，阻止其他阵营获胜
    public static Role CORRUPT_COP = TMMRoles.registerRole(new Role(CORRUPT_COP_ID, new Color(25, 50, 100).getRGB(), false, false, Role.MoodType.FAKE, TMMRoles.CIVILIAN.getMaxSprintTime(), false));
    // 病原体角色 - 中立阵营，感染所有存活玩家获胜
    public static Role PATHOGEN = TMMRoles.registerRole(new Role(PATHOGEN_ID, 0x7FFF00, false, false, Role.MoodType.FAKE, TMMRoles.CIVILIAN.getMaxSprintTime(), false));

    public static final CustomPayload.Id<MorphC2SPacket> MORPH_PACKET = MorphC2SPacket.ID;
    public static final CustomPayload.Id<SwapperC2SPacket> SWAP_PACKET = SwapperC2SPacket.ID;
    public static final CustomPayload.Id<AbilityC2SPacket> ABILITY_PACKET = AbilityC2SPacket.ID;
    public static final CustomPayload.Id<VultureEatC2SPacket> VULTURE_PACKET = VultureEatC2SPacket.ID;
    public static final CustomPayload.Id<AssassinGuessRoleC2SPacket> ASSASSIN_GUESS_ROLE_PACKET = AssassinGuessRoleC2SPacket.ID;
    public static final CustomPayload.Id<ScavengerResetKnifeCDC2SPacket> SCAVENGER_RESET_KNIFE_CD_PACKET = ScavengerResetKnifeCDC2SPacket.ID;
    public static final ArrayList<Role> VANNILA_ROLES = new ArrayList<>();
    public static final ArrayList<Identifier> VANNILA_ROLE_IDS = new ArrayList<>();

    @Override
    public void onInitialize() {
        VANNILA_ROLES.add(TMMRoles.KILLER);
        VANNILA_ROLES.add(TMMRoles.VIGILANTE);
        VANNILA_ROLES.add(TMMRoles.CIVILIAN);
        VANNILA_ROLES.add(TMMRoles.LOOSE_END);
        VANNILA_ROLE_IDS.add(TMMRoles.LOOSE_END.identifier());
        VANNILA_ROLE_IDS.add(TMMRoles.VIGILANTE.identifier());
        VANNILA_ROLE_IDS.add(TMMRoles.CIVILIAN.identifier());
        VANNILA_ROLE_IDS.add(TMMRoles.KILLER.identifier());
        NoellesRolesConfig.HANDLER.load();
        ModItems.init();
        ModSounds.init();
        PayloadTypeRegistry.playC2S().register(MorphC2SPacket.ID, MorphC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityC2SPacket.ID, AbilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapperC2SPacket.ID, SwapperC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VultureEatC2SPacket.ID, VultureEatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AssassinGuessRoleC2SPacket.ID, AssassinGuessRoleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ScavengerResetKnifeCDC2SPacket.ID, ScavengerResetKnifeCDC2SPacket.CODEC);

        registerEvents();

        BartenderShopHandler.register();
        BomberShopHandler.register();
        ScavengerShopHandler.register();

        registerPackets();
        //NoellesRolesEntities.init();

    }


    public void registerEvents() {
        // Master key should drop on death
        ShouldDropOnDeath.EVENT.register(stack -> {
            if (stack.isOf(ModItems.MASTER_KEY)) {
                return true;
            }
            return false;
        });

        // Bartender defense vial - convert poison to armor
        PlayerPoisoned.BEFORE.register((player, ticks, poisoner) -> {
            if (poisoner == null) return null;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

            // If poisoner is a bartender, cancel the poison and give armor instead
            if (gameWorldComponent.isRole(poisoner, Noellesroles.BARTENDER)) {
                if (player.getWorld().getPlayerByUuid(poisoner) == null) return null;

                BartenderPlayerComponent bartenderPlayerComponent = BartenderPlayerComponent.KEY.get(player);
                bartenderPlayerComponent.giveArmor();

                // Cancel the poisoning
                return PlayerPoisoned.PoisonResult.cancel();
            }

            return null;
        });

        KillPlayer.BEFORE.register(((victim, killer, deathReason) -> {
            if (deathReason == GameConstants.DeathReasons.FELL_OUT_OF_TRAIN) return null;
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());
            BartenderPlayerComponent bartenderPlayerComponent = BartenderPlayerComponent.KEY.get(victim);
            if (bartenderPlayerComponent.armor > 0 && deathReason != GameConstants.DeathReasons.SHOT_INNOCENT) {
                victim.getWorld().playSound(victim, victim.getBlockPos(), TMMSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5.0F, 1.0F);
                bartenderPlayerComponent.armor--;
                return KillPlayer.KillResult.cancel();
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
                player.giveItemStack(TMMItems.CROWBAR.getDefaultStack());
            }
            if (role.equals(CONDUCTOR)) {
                player.giveItemStack(ModItems.MASTER_KEY.getDefaultStack());
            }
            if (role.equals(AWESOME_BINGLUS)) {
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
                player.giveItemStack(TMMItems.NOTE.getDefaultStack());
            }
            if (role.equals(JESTER)) {
                JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);
                jesterComponent.reset();
                player.giveItemStack(TMMItems.CROWBAR.getDefaultStack());
            }
            if (role.equals(CORRUPT_COP)) {
                CorruptCopPlayerComponent corruptCopComponent = CorruptCopPlayerComponent.KEY.get(player);
                corruptCopComponent.reset();
                player.giveItemStack(TMMItems.REVOLVER.getDefaultStack());
                player.giveItemStack(TMMItems.CROWBAR.getDefaultStack());
            }
            if (role.equals(PATHOGEN)) {
                player.giveItemStack(TMMItems.CROWBAR.getDefaultStack());
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
            if (role.equals(SCAVENGER)) {
                ScavengerPlayerComponent scavengerComp = ScavengerPlayerComponent.KEY.get(player);
                scavengerComp.reset();
                // 清道夫开局获得刀和撬棍
                player.giveItemStack(TMMItems.KNIFE.getDefaultStack());
                player.giveItemStack(TMMItems.CROWBAR.getDefaultStack());
            }
        });
        ResetPlayer.EVENT.register(player -> {
            BartenderPlayerComponent.KEY.get(player).reset();
            MorphlingPlayerComponent.KEY.get(player).reset();
            VoodooPlayerComponent.KEY.get(player).reset();
            RecallerPlayerComponent.KEY.get(player).reset();
            VulturePlayerComponent.KEY.get(player).reset();
            JesterPlayerComponent.KEY.get(player).reset();
            CorruptCopPlayerComponent.KEY.get(player).reset();
            InfectedPlayerComponent.KEY.get(player).reset();
            BomberPlayerComponent.KEY.get(player).reset();
            AssassinPlayerComponent.KEY.get(player).reset();
            ScavengerPlayerComponent.KEY.get(player).reset();
        });

        // Bartender and Recaller get +50 coins when completing tasks
        TaskComplete.EVENT.register((player, taskType) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
            Role role = gameWorldComponent.getRole(player);
            if (role != null && (role.equals(BARTENDER) || role.equals(RECALLER))) {
                PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(player);
                playerShopComponent.addToBalance(50);
            }
        });

        // Jester win condition - when killed by innocent, jester wins
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) -> {
            for (UUID uuid : gameComponent.getAllWithRole(JESTER)) {
                PlayerEntity jester = world.getPlayerByUuid(uuid);
                if (jester != null) {
                    JesterPlayerComponent component = JesterPlayerComponent.KEY.get(jester);
                    if (component.won) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) jester);
                    }
                }
            }
            return null;
        });

        // Vulture win condition - when eaten enough bodies
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) -> {
            for (UUID uuid : gameComponent.getAllWithRole(VULTURE)) {
                PlayerEntity vulture = world.getPlayerByUuid(uuid);
                if (vulture != null) {
                    VulturePlayerComponent component = VulturePlayerComponent.KEY.get(vulture);
                    if (component.hasWon()) {
                        return CheckWinCondition.WinResult.neutralWin((ServerPlayerEntity) vulture);
                    }
                }
            }
            return null;
        });

        // Jester kill detection - when jester is killed by an innocent, mark as won
        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(victim.getWorld());
            // 爆破手击杀奖励+100金币
            if (killer != null && gameComponent.isRole(killer, BOMBER)) {
                PlayerShopComponent.KEY.get(killer).addToBalance(100);
            }
            // 清道夫击杀奖励+50金币（总共150，因为默认杀人100）
            if (killer != null && gameComponent.isRole(killer, SCAVENGER)) {
                PlayerShopComponent.KEY.get(killer).addToBalance(50);
                // 记录清道夫杀死的玩家，用于隐藏尸体
                ScavengerPlayerComponent scavengerComp = ScavengerPlayerComponent.KEY.get(killer);
                scavengerComp.addHiddenBody(victim.getUuid());
            }
            // Check if victim is a jester
            if (!gameComponent.isRole(victim, JESTER)) return;

            // Check if killer is an innocent
            if (killer != null) {
                Role killerRole = gameComponent.getRole(killer);
                if (killerRole != null && killerRole.isInnocent()) {
                    // Jester wins!
                    JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(victim);
                    jesterComponent.won = true;
                    jesterComponent.sync();
                }
            }
        });

        // Corrupt Cop - cancel gun punishment for killing anyone
        ShouldPunishGunShooter.EVENT.register((shooter, victim) -> {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(shooter.getWorld());
            if (gameComponent.isRole(shooter, CORRUPT_COP)) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            return null;
        });

        // Pathogen win condition - when all living players (except pathogen) are infected
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) -> {
            for (UUID uuid : gameComponent.getAllWithRole(PATHOGEN)) {
                PlayerEntity pathogen = world.getPlayerByUuid(uuid);
                if (pathogen != null && !GameFunctions.isPlayerEliminated((ServerPlayerEntity) pathogen)) {
                    // Check if all living players (except pathogen) are infected
                    boolean allInfected = true;
                    for (UUID playerUuid : gameComponent.getAllPlayers()) {
                        // 跳过病原体自己
                        if (playerUuid.equals(uuid)) continue;

                        PlayerEntity player = world.getPlayerByUuid(playerUuid);
                        if (player == null) continue;
                        if (GameFunctions.isPlayerEliminated((ServerPlayerEntity) player)) continue;

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
            return null;
        });

        // Corrupt Cop win condition - block other factions and check for victory
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) -> {
            // Find living corrupt cop
            ServerPlayerEntity livingCorruptCop = null;
            for (UUID uuid : gameComponent.getAllWithRole(CORRUPT_COP)) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(uuid);
                if (player != null && !GameFunctions.isPlayerEliminated(player)) {
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
                if (gameComponent.hasAnyRole(player) && !GameFunctions.isPlayerEliminated(player)) {
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

    }


    public void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.MORPH_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());

            if (payload.player() == null) return;
            if (abilityPlayerComponent.cooldown > 0) return;
            if (context.player().getWorld().getPlayerByUuid(payload.player()) == null) return;

            if (gameWorldComponent.isRole(context.player(), VOODOO) && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 30);
                abilityPlayerComponent.sync();
                VoodooPlayerComponent voodooPlayerComponent = (VoodooPlayerComponent) VoodooPlayerComponent.KEY.get(context.player());
                voodooPlayerComponent.setTarget(payload.player());

            }
            if (gameWorldComponent.isRole(context.player(), MORPHLING) && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
                MorphlingPlayerComponent morphlingPlayerComponent = (MorphlingPlayerComponent) MorphlingPlayerComponent.KEY.get(context.player());
                morphlingPlayerComponent.startMorph(payload.player());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.VULTURE_PACKET, (payload, context) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(context.player());

            if (gameWorldComponent.isRole(context.player(), VULTURE) && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
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
            if (gameWorldComponent.isRole(context.player(), SWAPPER) && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
                if (payload.player() != null) {
                    if (context.player().getWorld().getPlayerByUuid(payload.player()) != null) {
                        if (payload.player2() != null) {
                            if (context.player().getWorld().getPlayerByUuid(payload.player2()) != null) {
                                PlayerEntity player1 = context.player().getWorld().getPlayerByUuid(payload.player2());
                                PlayerEntity player2 = context.player().getWorld().getPlayerByUuid(payload.player());
                                Vec3d swapperPos = context.player().getWorld().getPlayerByUuid(payload.player2()).getPos();
                                Vec3d swappedPos = context.player().getWorld().getPlayerByUuid(payload.player()).getPos();
                                if (!context.player().getWorld().isSpaceEmpty(player1)) return;
                                if (!context.player().getWorld().isSpaceEmpty(player2)) return;
                                context.player().getWorld().getPlayerByUuid(payload.player2()).refreshPositionAfterTeleport(swappedPos.x, swappedPos.y, swappedPos.z);
                                context.player().getWorld().getPlayerByUuid(payload.player()).refreshPositionAfterTeleport(swapperPos.x, swapperPos.y, swapperPos.z);
                            }
                        }
                    }
                }
                AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 0);
                abilityPlayerComponent.sync();
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ABILITY_PACKET, (payload, context) -> {
            AbilityPlayerComponent abilityPlayerComponent = (AbilityPlayerComponent) AbilityPlayerComponent.KEY.get(context.player());
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(context.player().getWorld());
            if (gameWorldComponent.isRole(context.player(), RECALLER) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
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
            if (gameWorldComponent.isRole(context.player(), PHANTOM) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
                context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 30 * 20,0,true,false,true));
                abilityPlayerComponent.cooldown = GameConstants.getInTicks(1, 30);
            }
            // Pathogen infection ability
            if (gameWorldComponent.isRole(context.player(), PATHOGEN) && abilityPlayerComponent.cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(context.player())) {
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

                    // Set 15 second cooldown
                    abilityPlayerComponent.cooldown = GameConstants.getInTicks(0, 15);
                    abilityPlayerComponent.sync();

                    // Play coughing sound centered on the infected target (nearby players can hear)
                    if (context.player().getWorld() instanceof ServerWorld serverWorld) {
                        Vec3d pos = nearestTarget.getPos();
                        serverWorld.playSound(
                            null,
                            nearestTarget.getBlockPos(),
                            SoundEvents.ENTITY_PANDA_SNEEZE,
                            SoundCategory.PLAYERS,
                            2.0F,
                            0.8F
                        );
                    }
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

            AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(assassin);
            if (!assassinComp.canGuess()) return;

            // 验证目标
            ServerPlayerEntity target = (ServerPlayerEntity) assassin.getWorld().getPlayerByUuid(payload.targetPlayer());
            if (target == null) return;
            if (GameFunctions.isPlayerEliminated(target)) return;

            // 🔒 关键安全验证：防止恶意客户端猜测不可猜测的角色
            if (target.equals(assassin)) return;  // 不能猜测自己
            if (gameWorldComponent.isRole(target, TMMRoles.VIGILANTE)) return;  // 义警不能被猜测
            Role targetRole = gameWorldComponent.getRole(target);
            if (targetRole == null) return;
            if (TMMRoles.SPECIAL_ROLES.contains(targetRole)) return;  // 特殊角色不能被猜测
            if (targetRole.equals(ASSASSIN)) return;  // 不能猜测其他刺客

            // 判断猜测是否正确
            boolean guessedCorrectly = targetRole.identifier().equals(payload.guessedRole());

            // 播放枪响音效（对所有玩家可见）
            assassin.getWorld().playSound(
                null,  // 所有人都能听到
                assassin.getX(), assassin.getY(), assassin.getZ(),
                TMMSounds.ITEM_REVOLVER_SHOOT,
                SoundCategory.PLAYERS,
                2.0F,  // 音量
                1.0F   // 音调
            );

            // 执行结果
            if (guessedCorrectly) {
                // 发送消息给刺客（通过 actionbar 显示）- 先发送消息再击杀
                assassin.sendMessage(
                    net.minecraft.text.Text.translatable("tip.assassin.guess_correct", target.getName())
                        .formatted(net.minecraft.util.Formatting.GREEN, net.minecraft.util.Formatting.BOLD),
                    true
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

                // 猜错：自己死亡
                GameFunctions.killPlayer(assassin, true, null, DEATH_REASON_ASSASSIN_MISFIRE);
            }

            // 消耗猜测次数，设置冷却
            assassinComp.useGuess();
        });

        // 清道夫：重置刀CD
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SCAVENGER_RESET_KNIFE_CD_PACKET, (payload, context) -> {
            ServerPlayerEntity scavenger = context.player();
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(scavenger.getWorld());

            // 验证角色和状态
            if (!gameWorldComponent.isRole(scavenger, SCAVENGER)) return;
            if (!GameFunctions.isPlayerAliveAndSurvival(scavenger)) return;

            PlayerShopComponent playerShopComponent = PlayerShopComponent.KEY.get(scavenger);

            // 检查金币是否足够（100金币）
            if (playerShopComponent.balance < 100) {
                scavenger.sendMessage(
                    net.minecraft.text.Text.translatable("tip.scavenger.not_enough_money")
                        .formatted(net.minecraft.util.Formatting.RED),
                    true
                );
                return;
            }

            // 扣除100金币
            playerShopComponent.balance -= 100;
            playerShopComponent.sync();

            // 重置刀的CD（通过移除并重新给予刀物品）
            // 遍历玩家物品栏，找到刀并移除
            for (int i = 0; i < scavenger.getInventory().size(); i++) {
                net.minecraft.item.ItemStack stack = scavenger.getInventory().getStack(i);
                if (stack.isOf(TMMItems.KNIFE)) {
                    scavenger.getInventory().removeStack(i);
                    break;
                }
            }

            // 给予新的刀（CD已重置）
            scavenger.giveItemStack(TMMItems.KNIFE.getDefaultStack());

            // 播放音效反馈
            scavenger.getWorld().playSound(
                null,
                scavenger.getX(), scavenger.getY(), scavenger.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS,
                0.5F,
                1.5F
            );

            // 发送成功消息
            scavenger.sendMessage(
                net.minecraft.text.Text.translatable("tip.scavenger.knife_cd_reset")
                    .formatted(net.minecraft.util.Formatting.GREEN),
                true
            );
        });
    }



}
