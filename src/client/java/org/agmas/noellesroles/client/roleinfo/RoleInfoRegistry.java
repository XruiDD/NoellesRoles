package org.agmas.noellesroles.client.roleinfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.agmas.noellesroles.client.NoellesrolesClient;

import java.util.*;

/**
 * Registry for role information.
 * Uses hardcoded in-mod registrations only.
 */
public class RoleInfoRegistry {
    private static Map<String, RoleInfoData> roleInfoMap = new LinkedHashMap<>();

    /**
     * Load hardcoded role info. Call once during client initialization.
     */
    public static void load() {
        roleInfoMap = createDefaults();
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

    // ==================== Default Hardcoded Registry ====================

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

        // ===================== Wathe 原版职业 =====================

        // 杀手
        RoleInfoData killer = r("wathe", "killer");
        sk(killer, "wathe", "killer", "instinct", "sprint"); // 本能透视
        sk(killer, "wathe", "killer", "kill", null); // 击杀
        m.put("wathe:killer", killer);

        // 漏网之鱼
        RoleInfoData looseEnd = r("wathe", "loose_end");
        sk(looseEnd, "wathe", "loose_end", "instinct", "sprint"); // 本能透视
        sk(looseEnd, "wathe", "loose_end", "kill", null); // 击杀
        m.put("wathe:loose_end", looseEnd);

        // 平民
        RoleInfoData civilian = r("wathe", "civilian");
        sk(civilian, "wathe", "civilian", "survive", null); // 存活目标
        m.put("wathe:civilian", civilian);

        // 义警
        RoleInfoData vigilante = r("wathe", "vigilante");
        sk(vigilante, "wathe", "vigilante", "gun", null); // 手枪击杀
        m.put("wathe:vigilante", vigilante);

        // ===================== 杀手阵营 =====================

        // 变形者
        RoleInfoData morphling = r("noellesroles", "morphling");
        sk(morphling, "noellesroles", "morphling", "morph", "inventory"); // 变形成目标
        sk(morphling, "noellesroles", "morphling", "corpse_mode", "ability"); // 尸体伪装
        m.put("noellesroles:morphling", morphling);

        // 幽灵
        RoleInfoData phantom = r("noellesroles", "phantom");
        sk(phantom, "noellesroles", "phantom", "invisibility", "ability"); // 隐身
        m.put("noellesroles:phantom", phantom);

        // 交换者
        RoleInfoData swapper = r("noellesroles", "swapper");
        sk(swapper, "noellesroles", "swapper", "swap", "ability"); // 交换两名玩家位置
        m.put("noellesroles:swapper", swapper);

        // 亡语杀手
        RoleInfoData insaneKiller = r("noellesroles", "the_insane_damned_paranoid_killer");
        sk(insaneKiller, "noellesroles", "the_insane_damned_paranoid_killer", "insanity", null); // 听见死者哀嚎
        m.put("noellesroles:the_insane_damned_paranoid_killer", insaneKiller);

        // 炸弹客
        RoleInfoData bomber = r("noellesroles", "bomber");
        sk(bomber, "noellesroles", "bomber", "plant_bomb", "use"); // 安装定时炸弹
        sk(bomber, "noellesroles", "bomber", "bomb_vision", null); // 透视炸弹携带者
        m.put("noellesroles:bomber", bomber);

        // 刺客
        RoleInfoData assassin = r("noellesroles", "assassin");
        sk(assassin, "noellesroles", "assassin", "guess_identity", "ability"); // 猜测身份并刺杀
        m.put("noellesroles:assassin", assassin);

        // 清道夫
        RoleInfoData scavenger = r("noellesroles", "scavenger");
        sk(scavenger, "noellesroles", "scavenger", "hidden_kill", null); // 尸体隐藏
        sk(scavenger, "noellesroles", "scavenger", "instant_knife", null); // 刀杀无蓄力
        sk(scavenger, "noellesroles", "scavenger", "reset_cd", null); // 花费金币重置刀冷却
        m.put("noellesroles:scavenger", scavenger);

        // 连环杀手
        RoleInfoData serialKiller = r("noellesroles", "serial_killer");
        sk(serialKiller, "noellesroles", "serial_killer", "target_lock", null); // 锁定追杀目标
        sk(serialKiller, "noellesroles", "serial_killer", "bonus_kill", null); // 击杀目标获得奖励
        m.put("noellesroles:serial_killer", serialKiller);

        // 静语者
        RoleInfoData silencer = r("noellesroles", "silencer");
        sk(silencer, "noellesroles", "silencer", "silence", "ability"); // 沉默目标
        m.put("noellesroles:silencer", silencer);

        // 毒师
        RoleInfoData poisoner = r("noellesroles", "poisoner");
        sk(poisoner, "noellesroles", "poisoner", "poison_needle", "use"); // 毒针注毒
        sk(poisoner, "noellesroles", "poisoner", "gas_bomb", "use"); // 投掷毒气弹
        sk(poisoner, "noellesroles", "poisoner", "catalyst", "use"); // 催化毒发并重置冷却
        m.put("noellesroles:poisoner", poisoner);

        // 强盗
        RoleInfoData bandit = r("noellesroles", "bandit");
        sk(bandit, "noellesroles", "bandit", "throwing_axe", "use"); // 投掷飞斧
        m.put("noellesroles:bandit", bandit);

        // ===================== 乘客阵营 =====================

        // 列车长
        RoleInfoData conductor = r("noellesroles", "conductor");
        sk(conductor, "noellesroles", "conductor", "master_key", "use"); // 使用万能钥匙开门
        m.put("noellesroles:conductor", conductor);

        // 超级宾格鲁斯
        RoleInfoData awesomeBinglus = r("noellesroles", "awesome_binglus");
        m.put("noellesroles:awesome_binglus", awesomeBinglus);

        // 酒保
        RoleInfoData bartender = r("noellesroles", "bartender");
        sk(bartender, "noellesroles", "bartender", "see_drinkers", null); // 观察喝过酒的玩家
        sk(bartender, "noellesroles", "bartender", "fine_drink", null); // 上等佳酿
        m.put("noellesroles:bartender", bartender);

        // 大嗓门
        RoleInfoData noisemaker = r("noellesroles", "noisemaker");
        sk(noisemaker, "noellesroles", "noisemaker", "broadcast", "ability"); // 全图语音广播
        sk(noisemaker, "noellesroles", "noisemaker", "death_scream", null); // 死亡尖叫播报
        m.put("noellesroles:noisemaker", noisemaker);

        // 巫毒师
        RoleInfoData voodoo = r("noellesroles", "voodoo");
        sk(voodoo, "noellesroles", "voodoo", "bind_curse", "ability"); // 绑定巫毒诅咒
        m.put("noellesroles:voodoo", voodoo);

        // 验尸官
        RoleInfoData coroner = r("noellesroles", "coroner");
        sk(coroner, "noellesroles", "coroner", "examine_body", null); // 验尸获取信息
        m.put("noellesroles:coroner", coroner);

        // 回溯者
        RoleInfoData recaller = r("noellesroles", "recaller");
        sk(recaller, "noellesroles", "recaller", "save_position", "ability"); // 标记回溯坐标
        sk(recaller, "noellesroles", "recaller", "teleport", "ability"); // 传送回标记点
        m.put("noellesroles:recaller", recaller);

        // 计时员
        RoleInfoData timeKeeper = r("noellesroles", "time_keeper");
        sk(timeKeeper, "noellesroles", "time_keeper", "see_time", null); // 查看剩余时间
        sk(timeKeeper, "noellesroles", "time_keeper", "reduce_time", null); // 花费金币减少时间
        m.put("noellesroles:time_keeper", timeKeeper);

        // 卧底
        RoleInfoData undercover = r("noellesroles", "undercover");
        sk(undercover, "noellesroles", "undercover", "disguise", null); // 伪装混入杀手
        m.put("noellesroles:undercover", undercover);

        // 毒理学家
        RoleInfoData toxicologist = r("noellesroles", "toxicologist");
        sk(toxicologist, "noellesroles", "toxicologist", "see_poisoned", null); // 观察中毒玩家
        sk(toxicologist, "noellesroles", "toxicologist", "antidote", "use"); // 使用解毒剂
        m.put("noellesroles:toxicologist", toxicologist);

        // 教授
        RoleInfoData professor = r("noellesroles", "professor");
        sk(professor, "noellesroles", "professor", "iron_man_vial", "use"); // 注射铁人药剂
        m.put("noellesroles:professor", professor);

        // 乘务员
        RoleInfoData attendant = r("noellesroles", "attendant");
        sk(attendant, "noellesroles", "attendant", "manifest", null); // 查阅乘客登记表
        m.put("noellesroles:attendant", attendant);

        // 记者
        RoleInfoData reporter = r("noellesroles", "reporter");
        sk(reporter, "noellesroles", "reporter", "mark_target", "ability"); // 标记并透视目标
        m.put("noellesroles:reporter", reporter);

        // 保镖
        RoleInfoData bodyguard = r("noellesroles", "bodyguard");
        sk(bodyguard, "noellesroles", "bodyguard", "protect", null); // 守护目标并替死
        m.put("noellesroles:bodyguard", bodyguard);

        // 生存大师
        RoleInfoData survivalMaster = r("noellesroles", "survival_master");
        sk(survivalMaster, "noellesroles", "survival_master", "stealth", null); // 免疫杀手本能透视
        sk(survivalMaster, "noellesroles", "survival_master", "survival_moment", null); // 触发生存时刻
        m.put("noellesroles:survival_master", survivalMaster);

        // 工程师
        RoleInfoData engineer = r("noellesroles", "engineer");
        sk(engineer, "noellesroles", "engineer", "sense_doors", null); // 感知异常门状态
        sk(engineer, "noellesroles", "engineer", "repair_tool", "use"); // 修复/上锁/解锁门
        m.put("noellesroles:engineer", engineer);

        // ===================== 中立阵营 =====================

        // 小丑
        RoleInfoData jester = r("noellesroles", "jester");
        sk(jester, "noellesroles", "jester", "provoke", null); // 被无辜者枪击触发条件
        sk(jester, "noellesroles", "jester", "psycho_mode", null); // 进入疯魔模式
        m.put("noellesroles:jester", jester);

        // 秃鹫
        RoleInfoData vulture = r("noellesroles", "vulture");
        sk(vulture, "noellesroles", "vulture", "eat_body", "ability"); // 吃掉尸体
        sk(vulture, "noellesroles", "vulture", "body_vision", null); // 吃尸后短暂透视
        m.put("noellesroles:vulture", vulture);

        // 黑警
        RoleInfoData corruptCop = r("noellesroles", "corrupt_cop");
        sk(corruptCop, "noellesroles", "corrupt_cop", "block_victory", null); // 存活时阻止其他阵营胜利
        sk(corruptCop, "noellesroles", "corrupt_cop", "moment", null); // 触发黑警时刻
        m.put("noellesroles:corrupt_cop", corruptCop);

        // 病原体
        RoleInfoData pathogen = r("noellesroles", "pathogen");
        sk(pathogen, "noellesroles", "pathogen", "infect", "ability"); // 感染玩家
        sk(pathogen, "noellesroles", "pathogen", "compass", null); // 罗盘追踪目标
        m.put("noellesroles:pathogen", pathogen);

        // 饕餮
        RoleInfoData taotie = r("noellesroles", "taotie");
        sk(taotie, "noellesroles", "taotie", "swallow_skill", "ability"); // 吞噬玩家
        sk(taotie, "noellesroles", "taotie", "moment", null); // 触发饕餮时刻
        m.put("noellesroles:taotie", taotie);

        return m;
    }
}
