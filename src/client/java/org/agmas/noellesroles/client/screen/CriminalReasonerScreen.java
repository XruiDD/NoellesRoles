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
    private static final int SUSPECT_COLUMNS = 6;
    private static final int SUSPECT_SPACING_X = 36;
    private static final int SUSPECT_SPACING_Y = 45;
    private static final int SUSPECT_SECTION_HEADER_HEIGHT = 20;
    private static final int SUSPECT_SECTION_GAP = 28;
    private static final int SUSPECT_VIEW_TOP_OFFSET = 72;
    private static final int SUSPECT_VIEW_BOTTOM_GAP = 4;

    private final ClientPlayerEntity player;
    private UUID selectedVictim;
    private UUID selectedSuspect;
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
        int centerY = this.height / 2;

        addBackButton(centerX, this.height - 45);

        if (victims.isEmpty()) {
            return;
        }

        // 第一阶段用死者列表驱动网格，让界面风格与刺客菜单保持一致。
        int columns = 6;
        int spacingX = 36;
        int spacingY = 45;
        int totalRows = (int) Math.ceil((double) victims.size() / columns);
        int startX = centerX - ((Math.min(victims.size(), columns) * spacingX) / 2) + 9;
        int startY = centerY - (totalRows * spacingY / 2) + 20;

        for (int i = 0; i < victims.size(); i++) {
            UUID victimUuid = victims.get(i);
            int row = i / columns;
            int col = i % columns;

            addDrawableChild(new CriminalReasonerPlayerWidget(
                    startX + col * spacingX,
                    startY + row * spacingY,
                    victimUuid,
                    selectedUuid -> {
                        selectedVictim = selectedUuid;
                        selectedSuspect = null;
                        this.clearAndInit();
                    }
            ));
        }
    }

    private void initSuspectSelection(GameWorldComponent gameWorld) {
        List<UUID> aliveSuspects = getAliveReasoningTargets(gameWorld);
        List<UUID> deadSuspects = getDeadReasoningTargets(gameWorld);

        int centerX = this.width / 2;
        int aliveRows = Math.max(1, (int) Math.ceil((double) aliveSuspects.size() / SUSPECT_COLUMNS));
        int deadRows = Math.max(1, (int) Math.ceil((double) deadSuspects.size() / SUSPECT_COLUMNS));
        int totalContentRows = aliveRows + deadRows;
        int contentHeight = totalContentRows * SUSPECT_SPACING_Y + SUSPECT_SECTION_GAP + SUSPECT_SECTION_HEADER_HEIGHT * 2;
        int viewTop = getSuspectViewTop();
        int viewBottom = getSuspectViewBottom();
        int viewHeight = Math.max(1, viewBottom - viewTop);
        int startY = viewTop - suspectScrollOffset;

        // 第二步将活人与死人拆成上下两块，并在内容过长时整体滚动，避免顶到标题和按钮。
        suspectMaxScroll = Math.max(0, contentHeight - viewHeight);
        suspectScrollOffset = Math.max(0, Math.min(suspectScrollOffset, suspectMaxScroll));
        startY = viewTop - suspectScrollOffset;

        int aliveHeaderY = startY;
        int aliveGridY = aliveHeaderY + SUSPECT_SECTION_HEADER_HEIGHT;
        addSuspectSection(aliveSuspects, Text.translatable("screen.criminal_reasoner.section.alive"), centerX, aliveHeaderY, aliveGridY);

        int deadHeaderY = aliveGridY + aliveRows * SUSPECT_SPACING_Y + SUSPECT_SECTION_GAP;
        int deadGridY = deadHeaderY + SUSPECT_SECTION_HEADER_HEIGHT;
        addSuspectSection(deadSuspects, Text.translatable("screen.criminal_reasoner.section.dead"), centerX, deadHeaderY, deadGridY);

        int buttonY = this.height - 42;

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

        addDrawable(new SectionLabel(centerX, headerY, title));
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
        context.fill(0, 0, this.width, this.height, 0xF0000000);

        int accentColor = 0xFF000000 | (Noellesroles.CRIMINAL_REASONER.color() & 0x00FFFFFF);
        context.fill(0, 0, this.width, 20, accentColor);
        context.fill(0, this.height - 20, this.width, this.height, accentColor);

        if (selectedVictim != null) {
            CriminalReasonerPlayerWidget.setClipBounds(0, getSuspectViewTop(), this.width, getSuspectViewBottom());
        } else {
            CriminalReasonerPlayerWidget.clearClipBounds();
        }
        super.render(context, mouseX, mouseY, delta);
        CriminalReasonerPlayerWidget.clearClipBounds();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (selectedVictim == null) {
            drawCenteredTitle(context, font, Text.translatable("screen.criminal_reasoner.title.select_victim"), centerX, centerY - 80);
            drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.subtitle.select_victim"), centerX, centerY - 65);

            if (children().size() <= 1) {
                drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.empty_victims"), centerX, centerY);
            }
            return;
        }

        Text victimName = getPlayerName(selectedVictim);
        drawCenteredTitle(context, font, Text.translatable("screen.criminal_reasoner.title.select_suspect", victimName), centerX, centerY - 115);
        drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.subtitle.select_suspect"), centerX, centerY - 100);

        if (selectedSuspect != null) {
            Text suspectName = getPlayerName(selectedSuspect);
            drawCenteredSubTitle(context, font, Text.translatable("screen.criminal_reasoner.subtitle.current_pair", victimName, suspectName), centerX, centerY - 85);
        }
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
        if (selectedVictim != null && suspectMaxScroll > 0) {
            // 鼠标滚轮控制第二步内容区整体滚动，标题和底部按钮保持固定。
            int nextOffset = suspectScrollOffset - (int) Math.round(verticalAmount * 24.0);
            nextOffset = Math.max(0, Math.min(nextOffset, suspectMaxScroll));
            if (nextOffset != suspectScrollOffset) {
                suspectScrollOffset = nextOffset;
                this.clearAndInit();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawCenteredTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        context.drawCenteredTextWithShadow(font, text, 0, 0, 0xFFFFFF);
        context.getMatrices().pop();
    }

    private void drawCenteredSubTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.drawCenteredTextWithShadow(font, text, x, y, 0xAAAAAA);
    }

    private int getSuspectViewTop() {
        return this.height / 2 - SUSPECT_VIEW_TOP_OFFSET;
    }

    private int getSuspectViewBottom() {
        return this.height - 42 - SUSPECT_VIEW_BOTTOM_GAP;
    }

    private record SectionLabel(int x, int y, Text text) implements net.minecraft.client.gui.Drawable {
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            context.enableScissor(0, MinecraftClient.getInstance().currentScreen instanceof CriminalReasonerScreen screen ? screen.getSuspectViewTop() : Integer.MIN_VALUE,
                    MinecraftClient.getInstance().currentScreen != null ? MinecraftClient.getInstance().currentScreen.width : Integer.MAX_VALUE,
                    MinecraftClient.getInstance().currentScreen instanceof CriminalReasonerScreen screen ? screen.getSuspectViewBottom() : Integer.MAX_VALUE);
            context.drawCenteredTextWithShadow(font, this.text, this.x, this.y, 0xD0D0D0);
            context.disableScissor();
        }
    }
}
