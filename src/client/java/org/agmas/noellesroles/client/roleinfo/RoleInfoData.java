package org.agmas.noellesroles.client.roleinfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data class for role information.
 * String values are translation keys resolved via Minecraft i18n.
 */
public class RoleInfoData {
    public String nameKey;
    public String factionKey;
    public String descriptionKey;
    public String winConditionKey;
    private String roleId;
    public Map<String, SkillInfoData> skills;

    public RoleInfoData(String roleId, String nameKey, String factionKey, String descriptionKey, String winConditionKey) {
        this.roleId = roleId;
        this.nameKey = nameKey;
        this.factionKey = factionKey;
        this.descriptionKey = descriptionKey;
        this.winConditionKey = winConditionKey;
        this.skills = new LinkedHashMap<>();
    }

    /**
     * Fluent shortcut to add a role skill using the default translation key pattern.
     */
    public RoleInfoData addSkill(String skillId, String triggerKeybind) {
        String base = "tr:roleinfo.skill." + roleId + "." + skillId + ".";
        SkillInfoData s = new SkillInfoData();
        s.nameKey = base + "name";
        s.triggerKey = base + "trigger";
        s.triggerKeybind = triggerKeybind;
        s.effectKey = base + "effect";
        skills.put(skillId, s);
        return this;
    }

    public RoleInfoData addSkill(String skillId) {
        return addSkill(skillId, null);
    }

    public static class SkillInfoData {
        /** Translation key for skill name */
        public String nameKey;
        /** Translation key for trigger description (may contain %s placeholder for resolved keybind name) */
        public String triggerKey;
        /**
         * Keybind ID to resolve and pass as %s to triggerKey.
         * Supported values: "ability", "inventory", "use", "attack", "sprint", or null/empty for passive (no substitution).
         */
        public String triggerKeybind;
        /** Translation key for skill effect description */
        public String effectKey;
    }
}
