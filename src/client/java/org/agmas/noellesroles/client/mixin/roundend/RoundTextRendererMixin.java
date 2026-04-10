package org.agmas.noellesroles.client.mixin.roundend;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.List;

@Mixin(RoundTextRenderer.class)
public abstract class RoundTextRendererMixin {
    private static final int BASE_PLAYER_ROWS = 4;
    private static final int DEFAULT_PLAYER_COLUMNS = 6;
    private static final int CONTENT_CENTER_OFFSET_Y = -40;
    private static final int PLAYER_CARD_SPACING_X = 36;
    private static final int PLAYER_CARD_SPACING_Y = 28;
    private static final int WINNER_SECTION_COLOR = 0x55AA55;
    private static final int LOSER_SECTION_COLOR = 0xFF5555;
    private static final int WINNER_TITLE_Y = 14;
    private static final int SECTION_TITLE_GAP = 8;
    private static final int PLAYER_SECTION_START_Y = 16;
    private static final int LOSER_SECTION_GAP_Y = 14;
    private static final int SCREEN_SIDE_MARGIN = 24;
    private static final int SCREEN_TOP_MARGIN = 20;
    private static final int SCREEN_BOTTOM_MARGIN = 12;
    private static final int PLAYER_CARD_TEXT_BOTTOM = 18;
    private static final int ORIGINAL_PLAYER_CARD_BOTTOM = 26;
    private static final int END_TEXT_TOP_Y = -12;
    private static final int SUBTITLE_TOP_Y = -4;
    private static final int ROLE_TEXT_MAX_WIDTH = 36;
    private static final int ROLE_TEXT_LINE_HEIGHT = 9;
    private static final int MULTILINE_ROLE_TEXT_OFFSET_Y = -8;
    private static final int ROLE_TEXT_MAX_CHARS_PER_LINE = 4;

    @Shadow
    private static int endTime;

    @Shadow
    private static RoleAnnouncementTexts.RoleAnnouncementText role;

    @Invoker("renderPlayerCard")
    private static void noellesroles$invokeRenderPlayerCard(
            DrawContext context,
            TextRenderer textRenderer,
            GameRoundEndComponent.RoundEndData data,
            int x,
            int y
    ) {
        throw new AssertionError();
    }

    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderAdaptiveRoundEnd(
            TextRenderer textRenderer,
            ClientPlayerEntity player,
            DrawContext context,
            CallbackInfo ci
    ) {
        if (player == null || endTime <= 0 || endTime >= 120) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRunning()) {
            return;
        }

        GameRoundEndComponent roundEnd = GameRoundEndComponent.KEY.get(player.getScoreboard());
        if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NONE) {
            return;
        }

        if (roundEnd.getRoundGameMode() == WatheGameModes.DISCOVERY
                || roundEnd.getRoundGameMode() == WatheGameModes.LOOSE_ENDS) {
            return;
        }

        Text endText = noellesroles$getEndText(roundEnd);
        if (endText == null) {
            return;
        }

        List<GameRoundEndComponent.RoundEndData> winners = new ArrayList<>();
        List<GameRoundEndComponent.RoundEndData> losers = new ArrayList<>();
        for (GameRoundEndComponent.RoundEndData playerData : roundEnd.getPlayers()) {
            if (playerData.isWinner()) {
                winners.add(playerData);
            } else {
                losers.add(playerData);
            }
        }

        boolean useOriginalLayout = noellesroles$canUseOriginalLayout(
                winners.size(),
                losers.size(),
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight()
        );

        SectionLayout winnerLayout;
        SectionLayout loserLayout;
        int loserTitleY;
        int shiftUp;
        float cardScale;
        if (useOriginalLayout) {
            winnerLayout = noellesroles$createOriginalLayout(winners.size());
            loserLayout = noellesroles$createOriginalLayout(losers.size());
            loserTitleY = noellesroles$getOriginalLoserTitleY(winnerLayout.rows());
            shiftUp = 0;
            cardScale = 1.0f;
        } else {
            int maxColumns = noellesroles$getMaxColumns(context.getScaledWindowWidth());
            float[] layoutPlan = noellesroles$findLayoutPlan(
                    winners.size(),
                    losers.size(),
                    maxColumns,
                    context.getScaledWindowHeight()
            );
            int extraColumns = Math.round(layoutPlan[0]);
            winnerLayout = noellesroles$createLayout(winners.size(), maxColumns, extraColumns);
            loserLayout = noellesroles$createLayout(losers.size(), maxColumns, extraColumns);
            loserTitleY = Math.round(layoutPlan[1]);
            shiftUp = Math.round(layoutPlan[2]);
            cardScale = layoutPlan[3];
        }

        context.getMatrices().push();
        context.getMatrices().translate(
                context.getScaledWindowWidth() / 2f,
                context.getScaledWindowHeight() / 2f + CONTENT_CENTER_OFFSET_Y - shiftUp,
                0f
        );

        noellesroles$drawScaledCenteredText(context, textRenderer, endText, 2.6f, END_TEXT_TOP_Y, 0xFFFFFF);

        String subtitleKey = noellesroles$getResultSubtitleKey(roundEnd);
        noellesroles$drawScaledCenteredText(
                context,
                textRenderer,
                Text.translatable(subtitleKey),
                1.2f,
                SUBTITLE_TOP_Y,
                0xFFFFFF
        );

        noellesroles$drawSection(
                context,
                textRenderer,
                null,
                WINNER_SECTION_COLOR,
                winners,
                winnerLayout,
                WINNER_TITLE_Y,
                PLAYER_SECTION_START_Y,
                cardScale
        );

        if (!losers.isEmpty()) {
            noellesroles$drawSection(
                    context,
                    textRenderer,
                    Text.translatable("announcement.result.losers"),
                    LOSER_SECTION_COLOR,
                    losers,
                    loserLayout,
                    loserTitleY,
                    loserTitleY + LOSER_SECTION_GAP_Y,
                    cardScale
            );
        }

        context.getMatrices().pop();
        ci.cancel();
    }

    @WrapOperation(
            method = "renderPlayerCard",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"
            )
    )
    private static int noellesroles$wrapRoundEndRoleName(
            DrawContext context,
            TextRenderer textRenderer,
            Text text,
            int x,
            int y,
            int color,
            Operation<Integer> original
    ) {
        List<OrderedText> wrappedLines = noellesroles$wrapRoleName(textRenderer, text);
        if (wrappedLines.size() <= 1) {
            return original.call(context, textRenderer, text, x, y, color);
        }

        int centerX = x + textRenderer.getWidth(text) / 2;
        int startY = y + MULTILINE_ROLE_TEXT_OFFSET_Y;
        int currentY = startY;
        int drawn = 0;
        for (OrderedText line : wrappedLines) {
            int centeredX = centerX - textRenderer.getWidth(line) / 2;
            drawn = context.drawTextWithShadow(textRenderer, line, centeredX, currentY, color);
            currentY += ROLE_TEXT_LINE_HEIGHT;
        }
        return drawn;
    }

    private static List<OrderedText> noellesroles$wrapRoleName(TextRenderer textRenderer, Text text) {
        String raw = text.getString();
        if (raw.codePointCount(0, raw.length()) <= ROLE_TEXT_MAX_CHARS_PER_LINE) {
            return textRenderer.wrapLines(text, ROLE_TEXT_MAX_WIDTH);
        }

        List<OrderedText> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        int currentCount = 0;

        for (int offset = 0; offset < raw.length(); ) {
            int codePoint = raw.codePointAt(offset);
            currentLine.appendCodePoint(codePoint);
            currentCount++;
            offset += Character.charCount(codePoint);

            if (currentCount >= ROLE_TEXT_MAX_CHARS_PER_LINE) {
                lines.add(Text.literal(currentLine.toString()).asOrderedText());
                currentLine.setLength(0);
                currentCount = 0;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(Text.literal(currentLine.toString()).asOrderedText());
        }

        return lines;
    }

    @Unique
    private static Text noellesroles$getEndText(GameRoundEndComponent roundEnd) {
        if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NEUTRAL) {
            for (GameRoundEndComponent.RoundEndData data : roundEnd.getPlayers()) {
                if (data.isWinner()) {
                    return RoleAnnouncementTexts.getForRole(data.role()).winText;
                }
            }
            return null;
        }

        return role.getEndText(roundEnd.getWinStatus(), Text.empty());
    }

    @Unique
    private static String noellesroles$getResultSubtitleKey(GameRoundEndComponent roundEnd) {
        if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NEUTRAL) {
            for (GameRoundEndComponent.RoundEndData data : roundEnd.getPlayers()) {
                if (data.isWinner()) {
                    return "game.win." + data.role().getPath();
                }
            }
        }

        return "game.win." + roundEnd.getWinStatus().name().toLowerCase();
    }

    @Unique
    private static void noellesroles$drawScaledCenteredText(
            DrawContext context,
            TextRenderer textRenderer,
            Text text,
            float scale,
            int y,
            int color
    ) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1f);
        context.drawTextWithShadow(textRenderer, text, -textRenderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }

    @Unique
    private static void noellesroles$drawSection(
            DrawContext context,
            TextRenderer textRenderer,
            Text title,
            int titleColor,
            List<GameRoundEndComponent.RoundEndData> players,
            SectionLayout layout,
            int titleY,
            int cardsStartY,
            float cardScale
    ) {
        if (title != null) {
            context.drawTextWithShadow(textRenderer, title, -textRenderer.getWidth(title) / 2, titleY, titleColor);
        }

        for (int index = 0; index < players.size(); index++) {
            GameRoundEndComponent.RoundEndData data = players.get(index);
            int row = index / layout.columns();
            int column = index % layout.columns();
            int columnsThisRow = row == layout.rows() - 1
                    ? noellesroles$getLastRowColumns(players.size(), layout.columns())
                    : layout.columns();
            int rowWidth = columnsThisRow * PLAYER_CARD_SPACING_X;
            int startX = -rowWidth / 2;
            int x = noellesroles$scaleCoordinate(
                    startX + column * PLAYER_CARD_SPACING_X + PLAYER_CARD_SPACING_X / 2 - 8,
                    cardScale
            );
            int y = cardsStartY + noellesroles$scaleCoordinate(row * PLAYER_CARD_SPACING_Y, cardScale);

            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0f);
            context.getMatrices().scale(cardScale, cardScale, 1f);
            noellesroles$invokeRenderPlayerCard(context, textRenderer, data, 0, 0);
            context.getMatrices().pop();
        }
    }

    @Unique
    private static int noellesroles$getLastRowColumns(int playerCount, int columns) {
        if (playerCount == 0) {
            return 0;
        }

        int remainder = playerCount % columns;
        return remainder == 0 ? columns : remainder;
    }

    @Unique
    private static int noellesroles$getSectionBottom(int cardsStartY, int rows) {
        return cardsStartY + Math.max(1, rows) * PLAYER_CARD_SPACING_Y - PLAYER_CARD_SPACING_Y + PLAYER_CARD_TEXT_BOTTOM;
    }

    @Unique
    private static boolean noellesroles$canUseOriginalLayout(
            int winnerCount,
            int loserCount,
            int screenWidth,
            int screenHeight
    ) {
        SectionLayout winnerLayout = noellesroles$createOriginalLayout(winnerCount);
        SectionLayout loserLayout = noellesroles$createOriginalLayout(loserCount);

        if (!noellesroles$sectionFitsOriginalWidth(winnerCount, winnerLayout, screenWidth)) {
            return false;
        }

        if (!noellesroles$sectionFitsOriginalWidth(loserCount, loserLayout, screenWidth)) {
            return false;
        }

        int localVisibleBottom = screenHeight - (screenHeight / 2 + CONTENT_CENTER_OFFSET_Y);
        int contentBottom = loserCount <= 0
                ? noellesroles$getOriginalSectionBottom(PLAYER_SECTION_START_Y, winnerLayout.rows())
                : noellesroles$getOriginalSectionBottom(
                noellesroles$getOriginalLoserTitleY(winnerLayout.rows()) + LOSER_SECTION_GAP_Y,
                loserLayout.rows()
        );
        return contentBottom <= localVisibleBottom;
    }

    @Unique
    private static boolean noellesroles$sectionFitsOriginalWidth(
            int playerCount,
            SectionLayout layout,
            int screenWidth
    ) {
        for (int row = 0; row < layout.rows(); row++) {
            int columnsThisRow = row == layout.rows() - 1
                    ? noellesroles$getLastRowColumns(playerCount, layout.columns())
                    : layout.columns();
            if (columnsThisRow * PLAYER_CARD_SPACING_X > screenWidth) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private static int noellesroles$getOriginalSectionBottom(int cardsStartY, int rows) {
        return cardsStartY + Math.max(0, rows - 1) * PLAYER_CARD_SPACING_Y + ORIGINAL_PLAYER_CARD_BOTTOM;
    }

    @Unique
    private static int noellesroles$getOriginalLoserTitleY(int winnerRows) {
        return PLAYER_SECTION_START_Y + Math.max(1, winnerRows) * PLAYER_CARD_SPACING_Y + SECTION_TITLE_GAP;
    }

    @Unique
    private static SectionLayout noellesroles$createOriginalLayout(int playerCount) {
        if (playerCount <= 0) {
            return new SectionLayout(1, 0);
        }

        int columns = Math.max(DEFAULT_PLAYER_COLUMNS, (playerCount + BASE_PLAYER_ROWS - 1) / BASE_PLAYER_ROWS);
        int rows = Math.min(BASE_PLAYER_ROWS, (playerCount + columns - 1) / columns);
        return new SectionLayout(columns, rows);
    }

    @Unique
    private static float[] noellesroles$findLayoutPlan(int winnerCount, int loserCount, int maxColumns, int screenHeight) {
        int localVisibleTop = SCREEN_TOP_MARGIN - (screenHeight / 2 + CONTENT_CENTER_OFFSET_Y);
        int localVisibleBottom = screenHeight - SCREEN_BOTTOM_MARGIN - (screenHeight / 2 + CONTENT_CENTER_OFFSET_Y);
        int minContentTop = Math.min(END_TEXT_TOP_Y, SUBTITLE_TOP_Y);
        int maxShiftUp = Math.max(0, minContentTop - localVisibleTop);

        float[] fallbackPlan = null;
        for (int extraColumns = 0; extraColumns < maxColumns; extraColumns++) {
            SectionLayout winnerLayout = noellesroles$createLayout(winnerCount, maxColumns, extraColumns);
            SectionLayout loserLayout = noellesroles$createLayout(loserCount, maxColumns, extraColumns);
            int loserTitleY = PLAYER_SECTION_START_Y
                    + Math.max(1, winnerLayout.rows()) * PLAYER_CARD_SPACING_Y
                    + SECTION_TITLE_GAP;
            int contentBottom = loserCount <= 0
                    ? noellesroles$getSectionBottom(PLAYER_SECTION_START_Y, winnerLayout.rows())
                    : noellesroles$getSectionBottom(loserTitleY + LOSER_SECTION_GAP_Y, loserLayout.rows());
            int requiredShiftUp = Math.max(0, contentBottom - localVisibleBottom);
            int appliedShiftUp = Math.min(requiredShiftUp, maxShiftUp);
            float[] candidatePlan = new float[]{extraColumns, loserTitleY, appliedShiftUp, 1.0f};
            fallbackPlan = candidatePlan;
            if (contentBottom - appliedShiftUp <= localVisibleBottom) {
                return candidatePlan;
            }
        }

        if (fallbackPlan == null) {
            return new float[]{0f, PLAYER_SECTION_START_Y + PLAYER_CARD_SPACING_Y + SECTION_TITLE_GAP, 0f, 1.0f};
        }

        int fallbackExtraColumns = Math.round(fallbackPlan[0]);
        SectionLayout winnerLayout = noellesroles$createLayout(winnerCount, maxColumns, fallbackExtraColumns);
        SectionLayout loserLayout = noellesroles$createLayout(loserCount, maxColumns, fallbackExtraColumns);
        float maxScale = noellesroles$computeCardScaleToFit(
                winnerLayout.rows(),
                loserLayout.rows(),
                loserCount > 0,
                localVisibleBottom + Math.round(fallbackPlan[2])
        );
        int scaledLoserTitleY = noellesroles$getLoserTitleY(winnerLayout.rows(), maxScale);
        return new float[]{fallbackPlan[0], scaledLoserTitleY, fallbackPlan[2], maxScale};
    }

    @Unique
    private static SectionLayout noellesroles$createLayout(int playerCount, int maxColumns, int extraColumns) {
        if (playerCount <= 0) {
            return new SectionLayout(1, 0);
        }

        int columns = noellesroles$getBaseColumns(playerCount, maxColumns);
        columns = Math.min(Math.max(1, maxColumns), columns + extraColumns);
        int rows = (playerCount + columns - 1) / columns;
        return new SectionLayout(columns, rows);
    }

    @Unique
    private static int noellesroles$getBaseColumns(int playerCount, int maxColumns) {
        int columns = Math.max(1, (playerCount + BASE_PLAYER_ROWS - 1) / BASE_PLAYER_ROWS);
        return Math.min(columns, Math.max(1, Math.min(DEFAULT_PLAYER_COLUMNS, maxColumns)));
    }

    @Unique
    private static float noellesroles$computeCardScaleToFit(
            int winnerRows,
            int loserRows,
            boolean hasLosers,
            int availableBottom
    ) {
        float fixedHeight = hasLosers
                ? PLAYER_SECTION_START_Y + SECTION_TITLE_GAP + LOSER_SECTION_GAP_Y
                : PLAYER_SECTION_START_Y;
        float scalableHeight = hasLosers
                ? Math.max(1, winnerRows) * PLAYER_CARD_SPACING_Y
                + Math.max(0, loserRows - 1) * PLAYER_CARD_SPACING_Y
                + PLAYER_CARD_TEXT_BOTTOM
                : Math.max(0, winnerRows - 1) * PLAYER_CARD_SPACING_Y + PLAYER_CARD_TEXT_BOTTOM;

        if (scalableHeight <= 0f) {
            return 1.0f;
        }

        float scale = (availableBottom - fixedHeight) / scalableHeight;
        return Math.max(0.05f, Math.min(1.0f, scale));
    }

    @Unique
    private static int noellesroles$getLoserTitleY(int winnerRows, float cardScale) {
        return PLAYER_SECTION_START_Y
                + noellesroles$scaleCoordinate(Math.max(1, winnerRows) * PLAYER_CARD_SPACING_Y, cardScale)
                + SECTION_TITLE_GAP;
    }

    @Unique
    private static int noellesroles$scaleCoordinate(int value, float scale) {
        return Math.round(value * scale);
    }

    @Unique
    private static int noellesroles$getMaxColumns(int screenWidth) {
        int usableWidth = Math.max(PLAYER_CARD_SPACING_X, screenWidth - SCREEN_SIDE_MARGIN * 2);
        return Math.max(1, usableWidth / PLAYER_CARD_SPACING_X);
    }

    @Unique
    private record SectionLayout(int columns, int rows) {
    }
}
