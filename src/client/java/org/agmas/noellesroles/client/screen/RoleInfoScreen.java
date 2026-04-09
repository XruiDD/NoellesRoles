package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.roleinfo.RoleInfoData;
import org.agmas.noellesroles.client.roleinfo.RoleInfoRegistry;

import java.util.List;
import java.util.Map;

/**
 * Full-screen role information display.
 * Shows the player's current role name, faction, description, win condition, and skills.
 * Supports mouse-wheel scrolling for long content.
 */
public class RoleInfoScreen extends Screen {
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private Role currentRole;
    private RoleInfoData roleInfo;

    private static final int PADDING = 30;
    private static final int LINE_HEIGHT = 12;
    private static final int SECTION_GAP = 8;

    public RoleInfoScreen() {
        super(Text.translatable("roleinfo.screen.title"));
    }

    @Override
    protected void init() {
        super.init();

        if (MinecraftClient.getInstance().player == null) {
            this.close();
            return;
        }

        GameWorldComponent gwc = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());

        // Find the player's current role
        currentRole = null;
        for (Role role : WatheRoles.ROLES) {
            if (gwc.isRole(MinecraftClient.getInstance().player, role)) {
                currentRole = role;
                break;
            }
        }

        if (currentRole != null) {
            roleInfo = RoleInfoRegistry.get(currentRole.identifier().toString());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        // Top and bottom accent bars
        super.render(context, mouseX, mouseY, delta);
        context.fillGradient(0, 0, this.width, 25, 0x55333355, 0x00333355);
        context.fillGradient(0, this.height - 25, this.width, this.height, 0x00333355, 0x55333355);

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int contentWidth = Math.min(this.width - PADDING * 2, 320);
        int centerX = this.width / 2;
        int startX = centerX - contentWidth / 2;

        if (currentRole == null || roleInfo == null) {
            context.drawCenteredTextWithShadow(font,
                    Text.translatable("roleinfo.no_info"),
                    centerX, this.height / 2, 0xFFAAAAAA);
            return;
        }

        // Scissor region for scrollable content
        context.enableScissor(0, 8, this.width, this.height - 20);

        int y = 25 - scrollOffset;
        int roleColor = currentRole.color() | 0xFF000000;

        // ========== Role Name (large, colored) ==========
        Text roleName = RoleInfoRegistry.resolveText(roleInfo.nameKey);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);
        context.drawCenteredTextWithShadow(font, roleName, 0, 0, roleColor);
        context.getMatrices().pop();
        y += 26;

        // ========== Faction ==========
        Text factionText = Text.translatable("roleinfo.faction_label",
                RoleInfoRegistry.resolveText(roleInfo.factionKey));
        context.drawCenteredTextWithShadow(font, factionText, centerX, y, 0xFFBBBBBB);
        y += LINE_HEIGHT + SECTION_GAP;

        // ========== Separator ==========
        context.fill(startX, y, startX + contentWidth, y + 1, 0xFF444466);
        y += SECTION_GAP;

        // ========== Description ==========
        Text descLabel = Text.translatable("roleinfo.description_label");
        context.drawTextWithShadow(font, descLabel, startX, y, 0xFFDDDD66);
        y += LINE_HEIGHT;

        Text descText = RoleInfoRegistry.resolveText(roleInfo.descriptionKey);
        List<OrderedText> descLines = font.wrapLines(descText, contentWidth - 8);
        for (OrderedText line : descLines) {
            context.drawTextWithShadow(font, line, startX + 8, y, 0xFFDDDDDD);
            y += LINE_HEIGHT;
        }
        y += SECTION_GAP;

        // ========== Win Condition ==========
        Text winLabel = Text.translatable("roleinfo.win_condition_label");
        context.drawTextWithShadow(font, winLabel, startX, y, 0xFFFFCC00);
        y += LINE_HEIGHT;

        Text winText = RoleInfoRegistry.resolveText(roleInfo.winConditionKey);
        List<OrderedText> winLines = font.wrapLines(winText, contentWidth - 8);
        for (OrderedText line : winLines) {
            context.drawTextWithShadow(font, line, startX + 8, y, 0xFFEEEE88);
            y += LINE_HEIGHT;
        }
        y += SECTION_GAP;

        // ========== Skills Section ==========
        if (roleInfo.skills != null && !roleInfo.skills.isEmpty()) {
            // Separator
            context.fill(startX, y, startX + contentWidth, y + 1, 0xFF444466);
            y += SECTION_GAP;

            // Skills header
            Text skillsHeader = Text.translatable("roleinfo.skills_header");
            context.drawCenteredTextWithShadow(font, skillsHeader, centerX, y, 0xFFFFAA00);
            y += LINE_HEIGHT + SECTION_GAP;

            for (Map.Entry<String, RoleInfoData.SkillInfoData> entry : roleInfo.skills.entrySet()) {
                RoleInfoData.SkillInfoData skill = entry.getValue();

                // Skill name with star icon
                Text skillName = Text.literal("* ").append(RoleInfoRegistry.resolveText(skill.nameKey));
                context.drawTextWithShadow(font, skillName, startX, y, 0xFF00CCFF);
                y += LINE_HEIGHT;

                // Trigger
                Text triggerText = RoleInfoRegistry.getTriggerText(skill);
                Text triggerLabel = Text.translatable("roleinfo.skill.trigger_label", triggerText);
                List<OrderedText> triggerLines = font.wrapLines(triggerLabel, contentWidth - 16);
                for (OrderedText line : triggerLines) {
                    context.drawTextWithShadow(font, line, startX + 12, y, 0xFF88CC88);
                    y += LINE_HEIGHT;
                }

                // Effect
                Text effectText = RoleInfoRegistry.resolveText(skill.effectKey);
                Text effectLabel = Text.translatable("roleinfo.skill.effect_label", effectText);
                List<OrderedText> effectLines = font.wrapLines(effectLabel, contentWidth - 16);
                for (OrderedText line : effectLines) {
                    context.drawTextWithShadow(font, line, startX + 12, y, 0xFFBBBBBB);
                    y += LINE_HEIGHT;
                }

                y += 6; // gap between skills
            }
        }

        y += 20; // bottom padding
        contentHeight = y + scrollOffset;

        context.disableScissor();

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) (verticalAmount * 20);
        int maxScroll = Math.max(0, contentHeight - this.height + 30);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (NoellesrolesClient.roleInfoBind != null
                && NoellesrolesClient.roleInfoBind.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
