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
    public Map<String, SkillInfoData> skills;

    public RoleInfoData() {
        this.skills = new LinkedHashMap<>();
    }

    public RoleInfoData(String nameKey, String factionKey, String descriptionKey, String winConditionKey) {
        this.nameKey = nameKey;
        this.factionKey = factionKey;
        this.descriptionKey = descriptionKey;
        this.winConditionKey = winConditionKey;
        this.skills = new LinkedHashMap<>();
    }

    /**
     * Fluent API for adding a skill entry.
     *
     * @param id            Skill ID (e.g., "morph")
     * @param nameKey       Translation key for skill name
     * @param triggerKey    Translation key for trigger description (may contain %s for keybind name)
     * @param triggerKeybind Keybind ID to resolve: "ability", "inventory", "use", "attack", or null for passive
     * @param effectKey     Translation key for skill effect description
     */
    public RoleInfoData skill(String id, String nameKey, String triggerKey, String triggerKeybind, String effectKey) {
        SkillInfoData s = new SkillInfoData();
        s.nameKey = nameKey;
        s.triggerKey = triggerKey;
        s.triggerKeybind = triggerKeybind;
        s.effectKey = effectKey;
        skills.put(id, s);
        return this;
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
