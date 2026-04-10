package org.agmas.noellesroles.client.roleinfo;

import dev.doctor4t.wathe.client.WatheClient;
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
            case "instinct" -> WatheClient.instinctKeybind.getBoundKeyLocalizedText().getString();
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

    private static RoleInfoData r(String id) {
        return new RoleInfoData(
                id,
                "tr:announcement.role." + id,
                "tr:roleinfo.faction." + inferFaction(id),
                "tr:announcement.goals." + id,
                "tr:" + winConditionKeyForRole(id)
        );
    }

    private static String winConditionKeyForRole(String roleId) {
        String faction = inferFaction(roleId);
        if ("neutral".equals(faction)) {
            return neutralWinConditionKey(roleId);
        }
        return "roleinfo.win_condition.default." + faction;
    }

    private static String neutralWinConditionKey(String roleId) {
        return switch (roleId) {
            case "vulture" -> "roleinfo.win_condition.vulture";
            case "jester" -> "roleinfo.win_condition.jester";
            case "pathogen" -> "roleinfo.win_condition.pathogen";
            case "corrupt_cop" -> "roleinfo.win_condition.corrupt_cop";
            case "taotie" -> "roleinfo.win_condition.taotie";
            case "ferryman" -> "roleinfo.win_condition.ferryman";
            default -> "roleinfo.win_condition.default.neutral";
        };
    }

    private static String inferFaction(String roleId) {
        Set<String> killer = Set.of(
                "morphling", "phantom", "swapper", "the_insane_damned_paranoid_killer", "bomber", "assassin",
                "scavenger", "serial_killer", "silencer", "poisoner", "bandit", "hunter", "commander"
        );
        Set<String> neutral = Set.of("jester", "vulture", "corrupt_cop", "pathogen", "taotie", "ferryman");
        if (killer.contains(roleId)) return "killer";
        if (neutral.contains(roleId)) return "neutral";
        return "innocent";
    }

    private static Map<String, RoleInfoData> createDefaults() {
        Map<String, RoleInfoData> m = new LinkedHashMap<>();

        // ===================== Wathe 原版职业 =====================

        // 杀手
        m.put("wathe:killer", r("killer")
                .addSkill("instinct", "instinct") // 本能透视
                .addSkill("shop", "inventory")); // 商店

        // 平民
        m.put("wathe:civilian", r("civilian")
                .addSkill("survive")); // 存活目标

        // 义警
        m.put("wathe:vigilante", r("vigilante")
                .addSkill("gun")); // 持有手枪

        // ===================== 杀手阵营 =====================

        // 变形者
        m.put("noellesroles:morphling", r("morphling")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("morph", "inventory") // 变形成目标
                .addSkill("corpse_mode", "ability")); // 尸体伪装

        // 幽灵
        m.put("noellesroles:phantom", r("phantom")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("invisibility", "ability")); // 隐身

        // 交换者
        m.put("noellesroles:swapper", r("swapper")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("swap", "inventory")); // 交换两名玩家位置

        // 亡语杀手
        m.put("noellesroles:the_insane_damned_paranoid_killer", r("the_insane_damned_paranoid_killer")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("insanity")); // 听见死者哀嚎

        // 炸弹客
        m.put("noellesroles:bomber", r("bomber")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("plant_bomb", "use") // 安装定时炸弹
                .addSkill("bomb_vision")); // 透视炸弹携带者

        // 刺客
        m.put("noellesroles:assassin", r("assassin")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("guess_identity", "ability")); // 猜测身份并刺杀

        // 清道夫
        m.put("noellesroles:scavenger", r("scavenger")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("hidden_kill") // 尸体隐藏
                .addSkill("instant_knife")); // 刀杀无蓄力

        // 连环杀手
        m.put("noellesroles:serial_killer", r("serial_killer")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("target_lock") // 锁定追杀目标
                .addSkill("bonus_kill")); // 击杀目标获得奖励

        // 静语者
        m.put("noellesroles:silencer", r("silencer")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("silence", "ability")); // 沉默目标

        // 毒师
        m.put("noellesroles:poisoner", r("poisoner")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory") // 商店
                .addSkill("immune_gas_bomb", "use")); // 免疫毒气

        // 强盗
        m.put("noellesroles:bandit", r("bandit")
                .addSkill("instinct", "instinct") // 本能
                .addSkill("shop", "inventory")); // 商店

        // 指挥官
        m.put("noellesroles:commander", r("commander")
                .addSkill("killer_id")
                .addSkill("threat_mark", "ability")
                .addSkill("last_bullet")
                .addSkill("shop", "inventory"));

        // ===================== 乘客阵营 =====================

        // 列车长
        m.put("noellesroles:conductor", r("conductor")
                .addSkill("master_key", "use")); // 使用万能钥匙开门

        // 超级宾格鲁斯
        m.put("noellesroles:awesome_binglus", r("awesome_binglus")); // 便签

        // 酒保
        m.put("noellesroles:bartender", r("bartender")
                .addSkill("see_drinkers") // 观察喝过酒的玩家
                .addSkill("shop")); // 佳酿商店

        // 大嗓门
        m.put("noellesroles:noisemaker", r("noisemaker")
                .addSkill("death_scream")); // 死亡尖叫播报

        // 巫毒师
        m.put("noellesroles:voodoo", r("voodoo")
                .addSkill("bind_curse", "inventory")); // 绑定巫毒诅咒

        // 验尸官
        m.put("noellesroles:coroner", r("coroner")
                .addSkill("examine_body")); // 验尸获取信息

        // 回溯者
        m.put("noellesroles:recaller", r("recaller")
                .addSkill("earn_money", "ability") // 赚钱
                .addSkill("teleport", "ability")); // 传送回标记点

        // 计时员
        m.put("noellesroles:time_keeper", r("time_keeper")
                .addSkill("see_time") // 查看剩余时间
                .addSkill("reduce_time")); // 花费金币减少时间

        // 卧底
        m.put("noellesroles:undercover", r("undercover")
                .addSkill("no_sanity") // 没有理智值
                .addSkill("disguise")); // 伪装混入杀手

        // 毒理学家
        m.put("noellesroles:toxicologist", r("toxicologist")
                .addSkill("see_poisoned") // 观察中毒玩家
                .addSkill("antidote", "use")); // 使用解毒剂

        // 教授
        m.put("noellesroles:professor", r("professor")
                .addSkill("iron_man_vial", "use")); // 注射铁人药剂

        // 乘务员
        m.put("noellesroles:attendant", r("attendant")
                .addSkill("manifest")); // 查阅乘客登记表

        // 记者
        m.put("noellesroles:reporter", r("reporter")
                .addSkill("mark_target", "ability")); // 标记并透视目标

        // 老兵
        m.put("noellesroles:veteran", r("veteran")
                .addSkill("knife") // 携带刀
                .addSkill("immune_blackout")); // 免疫停电

        // 保镖
        m.put("noellesroles:bodyguard", r("bodyguard")
                .addSkill("protect") // 守护目标并替死
                .addSkill("see_target")); // 可以透视要保护的目标

        // 生存大师
        m.put("noellesroles:survival_master", r("survival_master")
                .addSkill("stealth") // 免疫杀手本能透视
                .addSkill("survival_moment")); // 触发生存时刻

        m.put("noellesroles:riot_patrol", r("riot_patrol")
                .addSkill("riot_shield", "use")
                .addSkill("riot_fork", "use"));

        m.put("noellesroles:hunter", r("hunter")
                .addSkill("trap", "use")
                .addSkill("shotgun", "inventory"));

        m.put("noellesroles:orthopedist", r("orthopedist")
                .addSkill("ancient_healing", "ability")
                .addSkill("swift_stride"));

        // 圣徒
        m.put("noellesroles:saint", r("saint")
                .addSkill("hellfire")
                .addSkill("divine_focus"));

        // ===================== 中立阵营 =====================

        // 小丑
        m.put("noellesroles:jester", r("jester")
                .addSkill("no_sanity") // 没有理智值
                .addSkill("psycho_mode")); // 进入疯魔模式

        // 秃鹫
        m.put("noellesroles:vulture", r("vulture")
                .addSkill("no_sanity") // 没有理智值
                .addSkill("neutral_master_key") // 中立万能钥匙
                .addSkill("eat_body", "ability") // 吃掉尸体
                .addSkill("body_vision")); // 透视尸体

        // 黑警
        m.put("noellesroles:corrupt_cop", r("corrupt_cop")
                .addSkill("corrupt_cop_items") // 黑警初始道具
                .addSkill("no_sanity") // 没有理智值
                .addSkill("see_time") // 查看剩余时间
                .addSkill("moment")); // 触发黑警时刻

        // 病原体
        m.put("noellesroles:pathogen", r("pathogen")
                .addSkill("no_sanity") // 没有理智值
                .addSkill("neutral_master_key") // 中立万能钥匙
                .addSkill("infect", "ability") // 感染玩家
                .addSkill("see_infected", "instinct") // 透视被感染玩家
                .addSkill("compass")); // 罗盘追踪目标

        // 饕餮
        m.put("noellesroles:taotie", r("taotie")
                .addSkill("no_sanity") // 没有理智值
                .addSkill("swallow_skill", "ability") // 吞噬玩家
                .addSkill("moment")); // 触发饕餮时刻

        m.put("noellesroles:ferryman", r("ferryman")
                .addSkill("netherwalker", "ability")
                .addSkill("ferry", "ability"));

        return m;
    }
}
