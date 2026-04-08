package org.agmas.noellesroles.client.roleinfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.agmas.noellesroles.client.NoellesrolesClient;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Registry for role information.
 * Loads from config/noellesroles_roleinfo.json on first init.
 * If the file does not exist, generates a default config with all roles.
 */
public class RoleInfoRegistry {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final String CONFIG_FILE = "noellesroles_roleinfo.json";
    private static Map<String, RoleInfoData> roleInfoMap = new LinkedHashMap<>();

    /**
     * Load role info config. Call once during client initialization.
     */
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<LinkedHashMap<String, RoleInfoData>>() {}.getType();
                roleInfoMap = GSON.fromJson(r, type);
                if (roleInfoMap == null) {
                    roleInfoMap = createDefaults();
                    save(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                roleInfoMap = createDefaults();
                save(path);
            }
        } else {
            roleInfoMap = createDefaults();
            save(path);
        }
    }

    private static void save(Path path) {
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(roleInfoMap, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get role info by full identifier string (e.g. "noellesroles:morphling").
     */
    public static RoleInfoData get(String roleIdentifier) {
        return roleInfoMap.get(roleIdentifier);
    }

    public static Map<String, RoleInfoData> getAll() {
        return Collections.unmodifiableMap(roleInfoMap);
    }

    /**
     * Resolve a keybind ID to the localized key name string.
     */
    public static String resolveKeybind(String keybindId) {
        if (keybindId == null || keybindId.isEmpty()) return "";
        MinecraftClient mc = MinecraftClient.getInstance();
        return switch (keybindId) {
            case "ability" -> NoellesrolesClient.abilityBind.getBoundKeyLocalizedText().getString();
            case "role_info" -> NoellesrolesClient.roleInfoBind.getBoundKeyLocalizedText().getString();
            case "inventory" -> mc.options.inventoryKey.getBoundKeyLocalizedText().getString();
            case "use" -> mc.options.useKey.getBoundKeyLocalizedText().getString();
            case "attack" -> mc.options.attackKey.getBoundKeyLocalizedText().getString();
            case "sprint" -> mc.options.sprintKey.getBoundKeyLocalizedText().getString();
            default -> keybindId;
        };
    }

    /**
     * Resolve a config string to Text.
     * Supports:
     * - "tr:<translation.key>" => translatable
     * - "translation.key"       => translatable if exists, otherwise readable fallback
     * - plain text              => literal
     */
    public static Text resolveText(String raw) {
        if (raw == null || raw.isEmpty()) return Text.empty();

        if (raw.startsWith("tr:")) {
            String key = raw.substring(3);
            if (Language.getInstance().hasTranslation(key)) {
                return Text.translatable(key);
            }
            return Text.literal(humanizeKey(key));
        }

        if (raw.contains(".")) {
            if (Language.getInstance().hasTranslation(raw)) {
                return Text.translatable(raw);
            }
            return Text.literal(humanizeKey(raw));
        }

        return Text.literal(raw);
    }

    private static String humanizeKey(String key) {
        int i = key.lastIndexOf('.');
        String tail = i >= 0 ? key.substring(i + 1) : key;
        return tail.replace('_', ' ');
    }

    /**
     * Get the resolved trigger text for a skill.
     * If triggerKeybind is set, the key name is resolved and inserted when possible.
     */
    public static Text getTriggerText(RoleInfoData.SkillInfoData skill) {
        if (skill == null || skill.triggerKey == null) return Text.empty();

        String keyName = resolveKeybind(skill.triggerKeybind);
        boolean hasKeybind = skill.triggerKeybind != null && !skill.triggerKeybind.isEmpty();

        String key = skill.triggerKey.startsWith("tr:") ? skill.triggerKey.substring(3) : skill.triggerKey;
        if (skill.triggerKey.startsWith("tr:") || skill.triggerKey.contains(".")) {
            if (Language.getInstance().hasTranslation(key)) {
                return hasKeybind ? Text.translatable(key, keyName) : Text.translatable(key);
            }
            if (hasKeybind) {
                return Text.literal(humanizeKey(key) + " [" + keyName + "]");
            }
            return Text.literal(humanizeKey(key));
        }

        if (hasKeybind && skill.triggerKey.contains("%s")) {
            return Text.literal(skill.triggerKey.formatted(keyName));
        }

        return Text.literal(skill.triggerKey);
    }

    // ==================== Default Config Generation ====================

    private static RoleInfoData r(String ns, String id) {
        // Use existing announcement translations for role name and goals when available.
        return new RoleInfoData(
                "tr:announcement.role." + id,
                "tr:roleinfo.faction." + inferFaction(id),
                "tr:announcement.goals." + id,
                "tr:announcement.goals." + id
        );
    }

    private static String inferFaction(String roleId) {
        Set<String> killer = Set.of(
                "morphling", "phantom", "swapper", "the_insane_damned_paranoid_killer", "bomber", "assassin",
                "scavenger", "serial_killer", "silencer", "poisoner", "bandit"
        );
        Set<String> neutral = Set.of("jester", "vulture", "corrupt_cop", "pathogen", "taotie");
        if (killer.contains(roleId)) return "killer";
        if (neutral.contains(roleId)) return "neutral";
        return "innocent";
    }

    private static void sk(RoleInfoData role, String ns, String roleId, String skillId, String keybind) {
        String skillName = "tr:roleinfo.skill." + roleId + "." + skillId + ".name";
        String trigger = "tr:roleinfo.skill." + roleId + "." + skillId + ".trigger";
        String effect = "tr:roleinfo.skill." + roleId + "." + skillId + ".effect";
        role.skill(skillId, skillName, trigger, keybind, effect);
    }

    private static Map<String, RoleInfoData> createDefaults() {
        Map<String, RoleInfoData> m = new LinkedHashMap<>();

        // ===================== Vanilla Wathe Roles =====================

        // Killer
        RoleInfoData killer = r("wathe", "killer");
        sk(killer, "wathe", "killer", "instinct", "sprint");
        sk(killer, "wathe", "killer", "kill", null);
        m.put("wathe:killer", killer);

        // Loose End
        RoleInfoData looseEnd = r("wathe", "loose_end");
        sk(looseEnd, "wathe", "loose_end", "instinct", "sprint");
        sk(looseEnd, "wathe", "loose_end", "kill", null);
        m.put("wathe:loose_end", looseEnd);

        // Civilian
        RoleInfoData civilian = r("wathe", "civilian");
        sk(civilian, "wathe", "civilian", "survive", null);
        m.put("wathe:civilian", civilian);

        // Vigilante
        RoleInfoData vigilante = r("wathe", "vigilante");
        sk(vigilante, "wathe", "vigilante", "gun", null);
        m.put("wathe:vigilante", vigilante);

        // ===================== Killer Faction =====================

        // Morphling
        RoleInfoData morphling = r("noellesroles", "morphling");
        sk(morphling, "noellesroles", "morphling", "morph", "inventory");
        sk(morphling, "noellesroles", "morphling", "corpse_mode", "ability");
        m.put("noellesroles:morphling", morphling);

        // Phantom
        RoleInfoData phantom = r("noellesroles", "phantom");
        sk(phantom, "noellesroles", "phantom", "invisibility", "ability");
        m.put("noellesroles:phantom", phantom);

        // Swapper
        RoleInfoData swapper = r("noellesroles", "swapper");
        sk(swapper, "noellesroles", "swapper", "swap", "ability");
        m.put("noellesroles:swapper", swapper);

        // The Insane Damned Paranoid Killer
        RoleInfoData insaneKiller = r("noellesroles", "the_insane_damned_paranoid_killer");
        sk(insaneKiller, "noellesroles", "the_insane_damned_paranoid_killer", "insanity", null);
        m.put("noellesroles:the_insane_damned_paranoid_killer", insaneKiller);

        // Bomber
        RoleInfoData bomber = r("noellesroles", "bomber");
        sk(bomber, "noellesroles", "bomber", "plant_bomb", "use");
        sk(bomber, "noellesroles", "bomber", "bomb_vision", null);
        m.put("noellesroles:bomber", bomber);

        // Assassin
        RoleInfoData assassin = r("noellesroles", "assassin");
        sk(assassin, "noellesroles", "assassin", "guess_identity", "ability");
        m.put("noellesroles:assassin", assassin);

        // Scavenger
        RoleInfoData scavenger = r("noellesroles", "scavenger");
        sk(scavenger, "noellesroles", "scavenger", "hidden_kill", null);
        sk(scavenger, "noellesroles", "scavenger", "instant_knife", null);
        sk(scavenger, "noellesroles", "scavenger", "reset_cd", null);
        m.put("noellesroles:scavenger", scavenger);

        // Serial Killer
        RoleInfoData serialKiller = r("noellesroles", "serial_killer");
        sk(serialKiller, "noellesroles", "serial_killer", "target_lock", null);
        sk(serialKiller, "noellesroles", "serial_killer", "bonus_kill", null);
        m.put("noellesroles:serial_killer", serialKiller);

        // Silencer
        RoleInfoData silencer = r("noellesroles", "silencer");
        sk(silencer, "noellesroles", "silencer", "silence", "ability");
        m.put("noellesroles:silencer", silencer);

        // Poisoner
        RoleInfoData poisoner = r("noellesroles", "poisoner");
        sk(poisoner, "noellesroles", "poisoner", "poison_needle", "use");
        sk(poisoner, "noellesroles", "poisoner", "gas_bomb", "use");
        sk(poisoner, "noellesroles", "poisoner", "catalyst", "use");
        m.put("noellesroles:poisoner", poisoner);

        // Bandit
        RoleInfoData bandit = r("noellesroles", "bandit");
        sk(bandit, "noellesroles", "bandit", "throwing_axe", "use");
        m.put("noellesroles:bandit", bandit);

        // ===================== Passenger Faction =====================

        // Conductor
        RoleInfoData conductor = r("noellesroles", "conductor");
        sk(conductor, "noellesroles", "conductor", "master_key", "use");
        m.put("noellesroles:conductor", conductor);

        // Bartender
        RoleInfoData bartender = r("noellesroles", "bartender");
        sk(bartender, "noellesroles", "bartender", "see_drinkers", null);
        sk(bartender, "noellesroles", "bartender", "fine_drink", null);
        m.put("noellesroles:bartender", bartender);

        // Noisemaker
        RoleInfoData noisemaker = r("noellesroles", "noisemaker");
        sk(noisemaker, "noellesroles", "noisemaker", "broadcast", "ability");
        sk(noisemaker, "noellesroles", "noisemaker", "death_scream", null);
        m.put("noellesroles:noisemaker", noisemaker);

        // Voodoo
        RoleInfoData voodoo = r("noellesroles", "voodoo");
        sk(voodoo, "noellesroles", "voodoo", "bind_curse", "ability");
        m.put("noellesroles:voodoo", voodoo);

        // Coroner
        RoleInfoData coroner = r("noellesroles", "coroner");
        sk(coroner, "noellesroles", "coroner", "examine_body", null);
        m.put("noellesroles:coroner", coroner);

        // Recaller
        RoleInfoData recaller = r("noellesroles", "recaller");
        sk(recaller, "noellesroles", "recaller", "save_position", "ability");
        sk(recaller, "noellesroles", "recaller", "teleport", "ability");
        m.put("noellesroles:recaller", recaller);

        // Time Keeper
        RoleInfoData timeKeeper = r("noellesroles", "time_keeper");
        sk(timeKeeper, "noellesroles", "time_keeper", "see_time", null);
        sk(timeKeeper, "noellesroles", "time_keeper", "reduce_time", null);
        m.put("noellesroles:time_keeper", timeKeeper);

        // Undercover
        RoleInfoData undercover = r("noellesroles", "undercover");
        sk(undercover, "noellesroles", "undercover", "disguise", null);
        m.put("noellesroles:undercover", undercover);

        // Toxicologist
        RoleInfoData toxicologist = r("noellesroles", "toxicologist");
        sk(toxicologist, "noellesroles", "toxicologist", "see_poisoned", null);
        sk(toxicologist, "noellesroles", "toxicologist", "antidote", "use");
        m.put("noellesroles:toxicologist", toxicologist);

        // Professor
        RoleInfoData professor = r("noellesroles", "professor");
        sk(professor, "noellesroles", "professor", "iron_man_vial", "use");
        m.put("noellesroles:professor", professor);

        // Attendant
        RoleInfoData attendant = r("noellesroles", "attendant");
        sk(attendant, "noellesroles", "attendant", "manifest", null);
        m.put("noellesroles:attendant", attendant);

        // Reporter
        RoleInfoData reporter = r("noellesroles", "reporter");
        sk(reporter, "noellesroles", "reporter", "mark_target", "ability");
        m.put("noellesroles:reporter", reporter);

        // Bodyguard
        RoleInfoData bodyguard = r("noellesroles", "bodyguard");
        sk(bodyguard, "noellesroles", "bodyguard", "protect", null);
        m.put("noellesroles:bodyguard", bodyguard);

        // Survival Master
        RoleInfoData survivalMaster = r("noellesroles", "survival_master");
        sk(survivalMaster, "noellesroles", "survival_master", "stealth", null);
        sk(survivalMaster, "noellesroles", "survival_master", "survival_moment", null);
        m.put("noellesroles:survival_master", survivalMaster);

        // Engineer
        RoleInfoData engineer = r("noellesroles", "engineer");
        sk(engineer, "noellesroles", "engineer", "sense_doors", null);
        sk(engineer, "noellesroles", "engineer", "repair_tool", "use");
        m.put("noellesroles:engineer", engineer);

        // ===================== Neutral Faction =====================

        // Jester
        RoleInfoData jester = r("noellesroles", "jester");
        sk(jester, "noellesroles", "jester", "provoke", null);
        sk(jester, "noellesroles", "jester", "psycho_mode", null);
        m.put("noellesroles:jester", jester);

        // Vulture
        RoleInfoData vulture = r("noellesroles", "vulture");
        sk(vulture, "noellesroles", "vulture", "eat_body", "ability");
        sk(vulture, "noellesroles", "vulture", "body_vision", null);
        m.put("noellesroles:vulture", vulture);

        // Corrupt Cop
        RoleInfoData corruptCop = r("noellesroles", "corrupt_cop");
        sk(corruptCop, "noellesroles", "corrupt_cop", "block_victory", null);
        sk(corruptCop, "noellesroles", "corrupt_cop", "moment", null);
        m.put("noellesroles:corrupt_cop", corruptCop);

        // Pathogen
        RoleInfoData pathogen = r("noellesroles", "pathogen");
        sk(pathogen, "noellesroles", "pathogen", "infect", "ability");
        sk(pathogen, "noellesroles", "pathogen", "compass", null);
        m.put("noellesroles:pathogen", pathogen);

        // Taotie
        RoleInfoData taotie = r("noellesroles", "taotie");
        sk(taotie, "noellesroles", "taotie", "swallow_skill", "ability");
        sk(taotie, "noellesroles", "taotie", "moment", null);
        m.put("noellesroles:taotie", taotie);

        return m;
    }
}
