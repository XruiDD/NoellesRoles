package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.CriminalReasonerPlayerWidget;
import org.agmas.noellesroles.criminalreasoner.CriminalReasonerPlayerComponent;
import org.agmas.noellesroles.packet.CriminalReasonerReasonC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CriminalReasonerScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 20;
    private static final int BACKGROUND_OVERLAY_COLOR = 0xB0000000;
    private static final int SUSPECT_COLUMNS = 6;
    private static final int SUSPECT_SPACING_X = 36;
    private static final int SUSPECT_SPACING_Y = 45;
    private static final int SUSPECT_SECTION_HEADER_HEIGHT = 20;
    private static final int SUSPECT_SECTION_GAP = 28;
    private static final double SUSPECT_SCROLL_STEP = 24.0;
    private static final double VICTIM_SCROLL_STEP = 24.0;

    private final ClientPlayerEntity player;
    private UUID selectedVictim;
    private UUID selectedSuspect;
    private int victimScrollOffset;
    private int victimMaxScroll;
    private int suspectScrollOffset;
    private int suspectMaxScroll;

    public CriminalReasonerScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.criminal_reasoner.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.CRIMINAL_REASONER)
                || !GameFunctions.isPlayerPlayingAndAlive(player)
                || SwallowedPlayerComponent.isPlayerSwallowed(player)
                || AbilityPlayerComponent.KEY.get(player).getCooldown() > 0) {
            this.close();
            return;
        }

        if (selectedVictim == null) {
            initVictimSelection(gameWorld);
        } else {
            initSuspectSelection(gameWorld);
        }
    }

    private void initVictimSelection(GameWorldComponent gameWorld) {
        List<UUID> victims = getDeadPlayers(gameWorld);
        int centerX = this.width / 2;

        addBackButton(centerX, RoleScreenHelper.getMenuButtonY(this.height));

        if (victims.isEmpty()) {
            victimScrollOffset = 0;
            victimMaxScroll = 0;
            return;
        }

        // 第一阶段用死者列表驱动网格，让界面风格与刺客菜单保持一致。
        int startX = RoleScreenHelper.getGridStartX(victims.size(), SUSPECT_COLUMNS, SUSPECT_SPACING_X, centerX);
        int rows = Math.max(1, getRowCount(victims.size(), SUSPECT_COLUMNS));
        int contentHeight = rows * SUSPECT_SPACING_Y + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
        int viewTop = RoleScreenHelper.getMenuViewTop(this.height);
        int viewBottom = RoleScreenHelper.getMenuViewBottom(this.height);
        int viewHeight = Math.max(1, viewBottom - viewTop);
        victimMaxScroll = Math.max(0, contentHeight - viewHeight);
        victimScrollOffset = Math.max(0, Math.min(victimScrollOffset, victimMaxScroll));
        int startY = viewTop + RoleScreenHelper.MENU_CONTENT_SHIFT_Y - victimScrollOffset;

        for (int i = 0; i < victims.size(); i++) {
            UUID victimUuid = victims.get(i);
            int row = i / SUSPECT_COLUMNS;
            int col = i % SUSPECT_COLUMNS;

            CriminalReasonerPlayerWidget widget = new CriminalReasonerPlayerWidget(
                    startX + col * SUSPECT_SPACING_X,
                    startY + row * SUSPECT_SPACING_Y,
                    victimUuid,
                    selectedUuid -> {
                        selectedVictim = selectedUuid;
                        selectedSuspect = null;
                        this.clearAndInit();
                    }
            );
            widget.visible = widget.getY() + 16 > viewTop && widget.getY() < viewBottom;
            addDrawableChild(widget);
        }
    }

    private void initSuspectSelection(GameWorldComponent gameWorld) {
        List<UUID> aliveSuspects = getAliveReasoningTargets(gameWorld);
        List<UUID> deadSuspects = getDeadReasoningTargets(gameWorld);

        int centerX = this.width / 2;
        int aliveRows = Math.max(1, getRowCount(aliveSuspects.size(), SUSPECT_COLUMNS));
        int deadRows = Math.max(1, getRowCount(deadSuspects.size(), SUSPECT_COLUMNS));
        int totalContentRows = aliveRows + deadRows;
        int contentHeight = totalContentRows * SUSPECT_SPACING_Y + SUSPECT_SECTION_GAP + SUSPECT_SECTION_HEADER_HEIGHT * 2 + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
        int viewTop = RoleScreenHelper.getMenuViewTop(this.height);
        int viewBottom = RoleScreenHelper.getMenuViewBottom(this.height);
        int viewHeight = Math.max(1, viewBottom - viewTop);

        // 第二步将活人与死人拆成上下两块，并在内容过长时整体滚动，避免顶到标题和按钮。
        suspectMaxScroll = Math.max(0, contentHeight - viewHeight);
        suspectScrollOffset = Math.max(0, Math.min(suspectScrollOffset, suspectMaxScroll));
        int startY = viewTop + RoleScreenHelper.MENU_CONTENT_SHIFT_Y - suspectScrollOffset;

        int aliveHeaderY = startY;
        int aliveGridY = aliveHeaderY + SUSPECT_SECTION_HEADER_HEIGHT;
        addSuspectSection(aliveSuspects, Text.translatable("screen.criminal_reasoner.section.alive"), centerX, aliveHeaderY, aliveGridY);

        int deadHeaderY = aliveGridY + aliveRows * SUSPECT_SPACING_Y + SUSPECT_SECTION_GAP;
        int deadGridY = deadHeaderY + SUSPECT_SECTION_HEADER_HEIGHT;
        addSuspectSection(deadSuspects, Text.translatable("screen.criminal_reasoner.section.dead"), centerX, deadHeaderY, deadGridY);

        int buttonY = RoleScreenHelper.getMenuButtonY(this.height);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.criminal_reasoner.button.back"), button -> {
                    selectedVictim = null;
                    selectedSuspect = null;
                    suspectScrollOffset = 0;
                    suspectMaxScroll = 0;
                    this.clearAndInit();
                })
                .dimensions(centerX - 86, buttonY, 80, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.criminal_reasoner.button.confirm"), button -> submitReasoning())
                .dimensions(centerX + 6, buttonY, 80, 20)
                .build());
    }

    private void addBackButton(int centerX, int y) {
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.criminal_reasoner.button.close"), button -> this.close())
                .dimensions(centerX - 40, y, 80, 20)
                .build());
    }

    private List<UUID> getDeadPlayers(GameWorldComponent gameWorld) {
        List<UUID> result = new ArrayList<>();
        CriminalReasonerPlayerComponent criminalReasonerComponent = CriminalReasonerPlayerComponent.KEY.get(player);
        for (UUID uuid : WatheClient.PLAYER_ENTRIES_CACHE.keySet()) {
            // 已经被成功推理过的死者不再出现在第一步列表中。
            if (gameWorld.isPlayerDead(uuid) && !criminalReasonerComponent.hasSolvedVictim(uuid)) {
                result.add(uuid);
            }
        }
        return result;
    }

    private void addSuspectSection(List<UUID> suspects, Text title, int centerX, int headerY, int gridY) {
        int startX = centerX - ((Math.min(Math.max(suspects.size(), 1), SUSPECT_COLUMNS) * SUSPECT_SPACING_X) / 2) + 9;

        for (int i = 0; i < suspects.size(); i++) {
            UUID suspectUuid = suspects.get(i);
            int row = i / SUSPECT_COLUMNS;
            int col = i % SUSPECT_COLUMNS;

            addDrawableChild(new CriminalReasonerPlayerWidget(
                    startX + col * SUSPECT_SPACING_X,
                    gridY + row * SUSPECT_SPACING_Y,
                    suspectUuid,
                    selectedUuid -> {
                        selectedSuspect = selectedUuid;
                        this.clearAndInit();
                    }
            ));
        }

        addDrawable(new SectionLabel(centerX, headerY, title, RoleScreenHelper.getMenuViewTop(this.height), this.width, RoleScreenHelper.getMenuViewBottom(this.height)));
    }

    private List<UUID> getAliveReasoningTargets(GameWorldComponent gameWorld) {
        return new ArrayList<>(gameWorld.getAllAlivePlayers());
    }

    private List<UUID> getDeadReasoningTargets(GameWorldComponent gameWorld) {
        List<UUID> result = new ArrayList<>();
        for (UUID uuid : WatheClient.PLAYER_ENTRIES_CACHE.keySet()) {
            if (gameWorld.isPlayerDead(uuid)) {
                result.add(uuid);
            }
        }
        return result;
    }

    private void submitReasoning() {
        if (selectedVictim == null || selectedSuspect == null || player == null) {
            return;
        }
        if (AbilityPlayerComponent.KEY.get(player).getCooldown() > 0) {
            this.close();
            return;
        }

        // 菜单确认后将“死者-嫌疑人”配对提交给服务端，由服务端统一判定成功与冷却时间。
        ClientPlayNetworking.send(new CriminalReasonerReasonC2SPacket(selectedVictim, selectedSuspect));
        this.close();
    }

    private Text getPlayerName(UUID uuid) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (entry != null && entry.getDisplayName() != null) {
            return entry.getDisplayName();
        }
        return entry != null ? Text.literal(entry.getProfile().getName()) : Text.translatable("screen.criminal_reasoner.unknown_player");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int accentColor = 0xFF000000 | (Noellesroles.CRIMINAL_REASONER.color() & 0x00FFFFFF);

        if (selectedVictim != null) {
            CriminalReasonerPlayerWidget.setClipBounds(0, RoleScreenHelper.getMenuViewTop(this.height), this.width, RoleScreenHelper.getMenuViewBottom(this.height));
        } else {
            CriminalReasonerPlayerWidget.clearClipBounds();
        }
        super.render(context, mouseX, mouseY, delta);
        CriminalReasonerPlayerWidget.clearClipBounds();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (selectedVictim == null) {
            RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.criminal_reasoner.title.select_victim"), centerX, RoleScreenHelper.getMenuTitleY(centerY));
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.subtitle.select_victim"), centerX, RoleScreenHelper.getMenuSubtitleY(centerY));

            if (children().size() <= 1) {
                RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.empty_victims"), centerX, centerY);
            }
            context.fill(0, 0, this.width, TOP_BAR_HEIGHT, accentColor);
            context.fill(0, this.height - BOTTOM_BAR_HEIGHT, this.width, this.height, accentColor);
            return;
        }

        Text victimName = getPlayerName(selectedVictim);
        RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.criminal_reasoner.title.select_suspect", victimName), centerX, RoleScreenHelper.getMenuTitleY(centerY));
        RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.subtitle.select_suspect"), centerX, RoleScreenHelper.getMenuSubtitleY(centerY));

        if (selectedSuspect != null) {
            Text suspectName = getPlayerName(selectedSuspect);
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.subtitle.current_pair", victimName, suspectName), centerX, RoleScreenHelper.getMenuStatusY(centerY));
        }
        context.fill(0, 0, this.width, TOP_BAR_HEIGHT, accentColor);
        context.fill(0, this.height - BOTTOM_BAR_HEIGHT, this.width, this.height, accentColor);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedVictim == null && victimMaxScroll > 0) {
            int nextOffset = victimScrollOffset - (int) Math.round(verticalAmount * VICTIM_SCROLL_STEP);
            nextOffset = Math.max(0, Math.min(nextOffset, victimMaxScroll));
            if (nextOffset != victimScrollOffset) {
                victimScrollOffset = nextOffset;
                this.clearAndInit();
                return true;
            }
        }
        if (selectedVictim != null && suspectMaxScroll > 0) {
            // 鼠标滚轮控制第二步内容区整体滚动，标题和底部按钮保持固定。
            int nextOffset = suspectScrollOffset - (int) Math.round(verticalAmount * SUSPECT_SCROLL_STEP);
            nextOffset = Math.max(0, Math.min(nextOffset, suspectMaxScroll));
            if (nextOffset != suspectScrollOffset) {
                suspectScrollOffset = nextOffset;
                this.clearAndInit();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, BACKGROUND_OVERLAY_COLOR);
    }

    private int getRowCount(int itemCount, int columns) {
        return (itemCount + columns - 1) / columns;
    }

    private record SectionLabel(int x, int y, Text text, int clipTop, int clipRight, int clipBottom) implements net.minecraft.client.gui.Drawable {
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            context.enableScissor(0, clipTop, clipRight, clipBottom);
            context.drawCenteredTextWithShadow(font, this.text, this.x, this.y, 0xD0D0D0);
            context.disableScissor();
        }
    }
}
