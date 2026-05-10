package org.agmas.noellesroles.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.configscreen.ConfigCategoryDefinition;
import org.agmas.noellesroles.client.configscreen.ConfigOptionDefinition;
import org.agmas.noellesroles.client.configscreen.ConfigScreenState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class NoellesRolesConfigScreen extends Screen {
    private static final String CLIENT_CATEGORY_ID = "client";
    private static final int TOP_MARGIN = 34;
    private static final int BOTTOM_MARGIN = 54;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 4;
    private static final int CATEGORY_WIDTH = 150;
    private static final int CATEGORY_GAP = 10;
    private static final int RESET_BUTTON_WIDTH = 46;
    private static final int CONTROL_GAP = 4;
    private static final int CONTROL_WIDTH = 100;
    private static final int SEARCH_HEIGHT = 20;

    private final Screen parent;
    private final List<ConfigCategoryDefinition> categories;
    private final List<ButtonWidget> categoryButtons = new ArrayList<>();
    private final List<OptionRow> allRows = new ArrayList<>();
    private final List<OptionRow> visibleRows = new ArrayList<>();

    private ConfigScreenState savedSnapshot;
    private ConfigScreenState workingCopy;
    private final ConfigScreenState defaultState = ConfigScreenState.defaults();

    private TextFieldWidget searchField;
    private ButtonWidget doneButton;
    private ButtonWidget cancelButton;

    private int selectedCategoryIndex;
    private int scrollOffset;
    private int maxScroll;

    public NoellesRolesConfigScreen(Screen parent, List<ConfigCategoryDefinition> categories) {
        super(Text.translatable("config_screen.title"));
        this.parent = parent;
        this.categories = List.copyOf(categories);
        this.savedSnapshot = ConfigScreenState.capture();
        this.workingCopy = this.savedSnapshot.copy();
    }

    @Override
    protected void init() {
        super.init();
        String previousSearch = this.searchField != null ? this.searchField.getText() : "";
        clearChildren();
        clearGeneratedWidgets();
        ensureAccessibleCategorySelected();

        Layout layout = getLayout();
        this.searchField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                layout.searchX,
                layout.searchY,
                layout.searchWidth,
                SEARCH_HEIGHT,
                Text.translatable("config_screen.search")
        ));
        this.searchField.setPlaceholder(Text.translatable("config_screen.search.placeholder"));
        this.searchField.setText(previousSearch);
        this.searchField.setChangedListener(value -> {
            scrollOffset = 0;
            refreshVisibleRows();
        });

        initCategoryButtons(layout);
        initOptionRows();
        initBottomButtons(layout);

        refreshVisibleRows();
        refreshCategoryButtons();
        syncRowsFromConfig();
        updateDoneButtonState();
    }

    private void clearGeneratedWidgets() {
        this.categoryButtons.clear();
        this.allRows.clear();
        this.visibleRows.clear();
    }

    private void initCategoryButtons(Layout layout) {
        int buttonX = layout.categoryX;
        int buttonY = layout.listTop;
        int buttonWidth = layout.categoryWidth;
        for (int i = 0; i < categories.size(); i++) {
            ConfigCategoryDefinition category = categories.get(i);
            int categoryIndex = i;
            ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(category.title(), widget -> {
                        if (!canAccessCategory(category)) {
                            return;
                        }
                        selectedCategoryIndex = categoryIndex;
                        scrollOffset = 0;
                        refreshVisibleRows();
                        refreshCategoryButtons();
                    })
                    .dimensions(buttonX, buttonY + i * (20 + 4), buttonWidth, 20)
                    .tooltip(Tooltip.of(getCategoryTooltip(category)))
                    .build());
            categoryButtons.add(button);
        }
    }

    private void initOptionRows() {
        for (ConfigCategoryDefinition category : categories) {
            for (ConfigOptionDefinition<?> option : category.options()) {
                allRows.add(new OptionRow(category.id(), option));
            }
        }
    }

    private void initBottomButtons(Layout layout) {
        int buttonY = this.height - 27;
        int buttonWidth = 150;
        int gap = 4;
        int totalWidth = buttonWidth * 2 + gap;
        int startX = this.width / 2 - totalWidth / 2;

        this.cancelButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("config_screen.button.cancel"), button -> cancelAndClose())
                .dimensions(startX, buttonY, buttonWidth, 20)
                .build());
        this.doneButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("config_screen.button.done"), button -> saveAndClose())
                .dimensions(startX + buttonWidth + gap, buttonY, buttonWidth, 20)
                .build());
    }

    private void refreshCategoryButtons() {
        ensureAccessibleCategorySelected();
        for (int i = 0; i < categoryButtons.size(); i++) {
            ButtonWidget button = categoryButtons.get(i);
            ConfigCategoryDefinition category = categories.get(i);
            Text title = category.title();
            button.setMessage(i == selectedCategoryIndex ? Text.literal("> ").append(title) : title);
            button.active = canAccessCategory(category);
            button.setTooltip(Tooltip.of(getCategoryTooltip(category)));
        }
    }

    private void refreshVisibleRows() {
        ensureAccessibleCategorySelected();
        visibleRows.clear();
        String filter = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategoryId = categories.get(Math.max(0, Math.min(selectedCategoryIndex, categories.size() - 1))).id();
        for (OptionRow row : allRows) {
            boolean matchesCategory = row.categoryId.equals(selectedCategoryId);
            boolean matchesSearch = row.definition.matchesFilter(filter);
            row.setDrawn(matchesCategory && matchesSearch);
            if (matchesCategory && matchesSearch) {
                visibleRows.add(row);
            }
        }

        Layout layout = getLayout();
        int contentHeight = visibleRows.size() * (ROW_HEIGHT + ROW_GAP);
        maxScroll = Math.max(0, contentHeight - layout.listHeight + ROW_GAP);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        updateRowVisibility();
    }

    private void syncRowsFromConfig() {
        for (OptionRow row : allRows) {
            row.refreshFromConfig();
        }
        updateDoneButtonState();
    }

    private void updateRowVisibility() {
        Layout layout = getLayout();
        for (int i = 0; i < visibleRows.size(); i++) {
            OptionRow row = visibleRows.get(i);
            int rowY = layout.listTop + i * (ROW_HEIGHT + ROW_GAP) - scrollOffset;
            boolean insideViewport = rowY + ROW_HEIGHT > layout.listTop && rowY < layout.listBottom;
            row.setBounds(layout.optionsX, rowY, layout.optionsWidth, ROW_HEIGHT);
            row.setVisible(insideViewport);
        }

        for (OptionRow row : allRows) {
            if (!visibleRows.contains(row)) {
                row.setVisible(false);
            }
        }
    }

    private void updateDoneButtonState() {
        boolean hasInvalidRow = allRows.stream().anyMatch(row -> !row.isValid());
        if (doneButton != null) {
            doneButton.active = !hasInvalidRow;
            doneButton.setTooltip(hasInvalidRow ? Tooltip.of(Text.translatable("config_screen.tooltip.invalid_input")) : null);
        }
    }

    private void applyChanges() {
        if (allRows.stream().anyMatch(row -> !row.isValid())) {
            return;
        }
        restoreRestrictedValuesFromSnapshot();
        this.workingCopy.apply(canEditRestrictedSettings());
        this.savedSnapshot = this.workingCopy.copy();
    }

    private void restoreSavedSnapshot() {
        this.workingCopy = this.savedSnapshot.copy();
        syncRowsFromConfig();
    }

    private void saveAndClose() {
        applyChanges();
        if (allRows.stream().anyMatch(row -> !row.isValid())) {
            return;
        }
        exitScreen();
    }

    private void cancelAndClose() {
        restoreSavedSnapshot();
        exitScreen();
    }

    private void exitScreen() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
    }

    @Override
    protected void applyBlur(float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        ensureAccessibleCategorySelected();
        Layout layout = getLayout();
        updateRowVisibility();

        context.fill(layout.categoryX - 2, layout.listTop - 2, layout.categoryX + layout.categoryWidth + 2, layout.listBottom + 2, 0x66000000);
        context.fill(layout.optionsX - 2, layout.listTop - 2, layout.optionsX + layout.optionsWidth + 2, layout.listBottom + 2, 0x66000000);

        context.enableScissor(layout.optionsX - 2, layout.listTop - 2, layout.optionsX + layout.optionsWidth + 2, layout.listBottom + 2);
        for (OptionRow row : visibleRows) {
            row.renderRow(context, mouseX, mouseY);
        }
        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, categories.get(selectedCategoryIndex).description(), this.width / 2, 24, 0xA0A0A0);

        if (visibleRows.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("config_screen.empty"),
                    layout.optionsX + layout.optionsWidth / 2,
                    layout.listTop + layout.listHeight / 2 - 4,
                    0xFFFFFF
            );
        } else if (maxScroll > 0) {
            renderScrollbar(context, layout);
        }

        List<Text> tooltipLines = getHoveredTooltipLines(mouseX, mouseY);
        if (!tooltipLines.isEmpty()) {
            context.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }

    private void renderScrollbar(DrawContext context, Layout layout) {
        int trackX = layout.optionsX + layout.optionsWidth - 6;
        int trackY = layout.listTop;
        int trackHeight = layout.listHeight;
        context.fill(trackX, trackY, trackX + 4, trackY + trackHeight, 0xFF000000);

        int thumbHeight = Math.max(18, (int) ((trackHeight / (float) (maxScroll + trackHeight)) * trackHeight));
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (maxScroll == 0 ? 0 : (int) (thumbTravel * (scrollOffset / (float) maxScroll)));
        context.fill(trackX + 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = getLayout();
        if (isWithin(mouseX, mouseY, layout.optionsX, layout.listTop, layout.optionsWidth, layout.listHeight) && maxScroll > 0) {
            int nextScroll = scrollOffset - (int) (verticalAmount * (ROW_HEIGHT + ROW_GAP));
            scrollOffset = Math.max(0, Math.min(nextScroll, maxScroll));
            updateRowVisibility();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        cancelAndClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NoellesrolesClient.configScreenBind != null
                && NoellesrolesClient.configScreenBind.matchesKey(keyCode, scanCode)
                && !isEditingTextField()) {
            NoellesrolesClient.markConfigScreenKeyHandled();
            cancelAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isEditingTextField() {
        return this.getFocused() instanceof TextFieldWidget
                || (this.searchField != null && this.searchField.isFocused());
    }

    private void ensureAccessibleCategorySelected() {
        if (!categories.isEmpty() && !canAccessCategory(categories.get(selectedCategoryIndex))) {
            selectedCategoryIndex = findFirstAccessibleCategoryIndex();
        }
    }

    private int findFirstAccessibleCategoryIndex() {
        for (int i = 0; i < categories.size(); i++) {
            if (canAccessCategory(categories.get(i))) {
                return i;
            }
        }
        return 0;
    }

    private boolean canAccessCategory(ConfigCategoryDefinition category) {
        return !isRestrictedCategory(category.id()) || canEditRestrictedSettings();
    }

    private boolean isRestrictedCategory(String categoryId) {
        return !CLIENT_CATEGORY_ID.equals(categoryId);
    }

    private boolean canEditRestrictedSettings() {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return true;
        }
        if (this.client.isInSingleplayer()) {
            return true;
        }
        if (this.client.getNetworkHandler() == null) {
            return false;
        }
        return this.client.getNetworkHandler().getCommandDispatcher().getRoot().getChild("fogradius") != null
                || this.client.getNetworkHandler().getCommandDispatcher().getRoot().getChild("hallucination") != null;
    }

    private Text getCategoryTooltip(ConfigCategoryDefinition category) {
        if (canAccessCategory(category)) {
            return category.description();
        }
        return Text.translatable("config_screen.tooltip.category_requires_permission");
    }

    private void restoreRestrictedValuesFromSnapshot() {
        if (canEditRestrictedSettings()) {
            return;
        }
        for (ConfigCategoryDefinition category : categories) {
            if (!isRestrictedCategory(category.id())) {
                continue;
            }
            for (ConfigOptionDefinition<?> option : category.options()) {
                copyOptionValue(option, savedSnapshot, workingCopy);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void copyOptionValue(ConfigOptionDefinition<?> option, ConfigScreenState from, ConfigScreenState to) {
        copyOptionValueUnchecked((ConfigOptionDefinition) option, from, to);
    }

    private <T> void copyOptionValueUnchecked(ConfigOptionDefinition<T> option, ConfigScreenState from, ConfigScreenState to) {
        option.setValue(to, option.getValue(from));
    }

    private Layout getLayout() {
        int centerWidth = Math.min(308, this.width - 40);
        int leftWidth = Math.min(CATEGORY_WIDTH, Math.max(110, (this.width - 60) / 3));
        int rightX = this.width / 2 - centerWidth / 2;
        int categoryX = rightX - CATEGORY_GAP - leftWidth;
        int listTop = TOP_MARGIN + SEARCH_HEIGHT + 10;
        int listBottom = this.height - BOTTOM_MARGIN;
        int listHeight = Math.max(20, listBottom - listTop);

        return new Layout(
                Math.max(10, categoryX),
                leftWidth,
                rightX,
                centerWidth,
                rightX,
                TOP_MARGIN,
                centerWidth,
                listTop,
                listBottom,
                listHeight
        );
    }

    private List<Text> getHoveredTooltipLines(int mouseX, int mouseY) {
        for (OptionRow row : visibleRows) {
            if (row.shouldShowDescriptionTooltip(mouseX, mouseY)) {
                return row.getDescriptionTooltip();
            }
            if (row.shouldShowPermissionTooltip(mouseX, mouseY)) {
                return row.getPermissionTooltip();
            }
        }
        return List.of();
    }

    private record Layout(
            int categoryX,
            int categoryWidth,
            int optionsX,
            int optionsWidth,
            int searchX,
            int searchY,
            int searchWidth,
            int listTop,
            int listBottom,
            int listHeight
    ) {
    }

    private final class OptionRow {
        private final String categoryId;
        private final ConfigOptionDefinition<?> definition;
        private final ClickableWidget control;
        private final ButtonWidget resetButton;

        private int x;
        private int y;
        private int width;
        private int height;
        private int controlX;
        private int resetX;
        private boolean drawn;
        private boolean visible;
        private boolean valid = true;

        private OptionRow(String categoryId, ConfigOptionDefinition<?> definition) {
            this.categoryId = categoryId;
            this.definition = definition;
            this.control = createControl(definition);
            this.resetButton = createResetButton();
            NoellesRolesConfigScreen.this.addDrawableChild(this.control);
            NoellesRolesConfigScreen.this.addDrawableChild(this.resetButton);
        }

        private ButtonWidget createResetButton() {
            return ButtonWidget.builder(Text.translatable("config_screen.button.reset"), button -> {
                        resetToDefault();
                        refreshFromConfig();
                    })
                    .dimensions(0, 0, RESET_BUTTON_WIDTH, 20)
                    .tooltip(Tooltip.of(Text.translatable("config_screen.tooltip.reset")))
                    .build();
        }

        private ClickableWidget createControl(ConfigOptionDefinition<?> option) {
            if (option instanceof ConfigOptionDefinition.ToggleOptionDefinition toggleOption) {
                return ButtonWidget.builder(Text.empty(), button -> {
                            boolean currentValue = toggleOption.getValue(workingCopy);
                            toggleOption.setValue(workingCopy, !currentValue);
                            refreshFromConfig();
                        })
                        .dimensions(0, 0, CONTROL_WIDTH, 20)
                        .build();
            }

            if (option instanceof ConfigOptionDefinition.EnumOptionDefinition<?> enumOption) {
                return ButtonWidget.builder(Text.empty(), button -> cycleEnumValue(enumOption))
                        .dimensions(0, 0, CONTROL_WIDTH, 20)
                        .build();
            }

            if (option instanceof ConfigOptionDefinition.NumberOptionDefinition numberOption) {
                return new NumberSliderWidget(this, numberOption);
            }

            if (option instanceof ConfigOptionDefinition.TextOptionDefinition textOption) {
                TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, option.label());
                field.setMaxLength(textOption.maxLength());
                field.setPlaceholder(textOption.placeholder());
                field.setChangedListener(value -> textOption.setValue(workingCopy, value));
                return field;
            }

            throw new IllegalStateException("Unsupported option type: " + option.type());
        }

        private <E> void cycleEnumValue(ConfigOptionDefinition.EnumOptionDefinition<E> option) {
            List<E> values = option.values();
            E current = option.getValue(workingCopy);
            int index = Math.max(0, values.indexOf(current));
            int nextIndex = (index + 1) % values.size();
            option.setValue(workingCopy, values.get(nextIndex));
            refreshFromConfig();
        }

        private void refreshFromConfig() {
            if (definition instanceof ConfigOptionDefinition.ToggleOptionDefinition toggleOption) {
                boolean enabled = toggleOption.getValue(workingCopy);
                control.setMessage(enabled ? Text.translatable("config_screen.toggle.on") : Text.translatable("config_screen.toggle.off"));
                control.setTooltip(null);
                valid = true;
                return;
            }

            if (definition instanceof ConfigOptionDefinition.EnumOptionDefinition<?> enumOption) {
                Object current = enumOption.getValue(workingCopy);
                control.setMessage(getEnumValueText(enumOption, current));
                control.setTooltip(Tooltip.of(Text.translatable("config_screen.tooltip.cycle_enum")));
                valid = true;
                return;
            }

            if (definition instanceof ConfigOptionDefinition.NumberOptionDefinition numberOption && control instanceof NumberSliderWidget slider) {
                int value = numberOption.getValue(workingCopy);
                slider.setIntValue(value);
                valid = true;
                return;
            }

            if (definition instanceof ConfigOptionDefinition.TextOptionDefinition textOption && control instanceof TextFieldWidget field) {
                field.setText(textOption.getValue(workingCopy));
                valid = true;
            }
        }

        @SuppressWarnings("unchecked")
        private Text getEnumValueText(ConfigOptionDefinition.EnumOptionDefinition<?> option, Object value) {
            return ((ConfigOptionDefinition.EnumOptionDefinition<Object>) option).getValueText(value);
        }

        private void setDrawn(boolean drawn) {
            this.drawn = drawn;
        }

        private void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

            this.resetX = x + width - RESET_BUTTON_WIDTH;
            this.controlX = this.resetX - CONTROL_GAP - CONTROL_WIDTH;
            int controlY = y + 2;
            this.control.setX(this.controlX);
            this.control.setY(controlY);
            this.control.setWidth(CONTROL_WIDTH);
            this.resetButton.setX(this.resetX);
            this.resetButton.setY(controlY);
            this.resetButton.setWidth(RESET_BUTTON_WIDTH);
        }

        private void setVisible(boolean visible) {
            this.visible = drawn && visible;
            this.control.visible = this.visible;
            this.control.active = this.visible && canEditOption();
            this.resetButton.visible = this.visible;
            this.resetButton.active = this.visible && canEditOption() && !isAtDefaultValue();
        }

        private boolean isValid() {
            return valid;
        }

        private boolean canEditOption() {
            return !isRestrictedCategory(this.categoryId) || canEditRestrictedSettings();
        }

        private boolean shouldShowDescriptionTooltip(int mouseX, int mouseY) {
            return drawn
                    && visible
                    && canEditOption()
                    && isWithin(mouseX, mouseY, x, y, Math.max(0, controlX - x - 6), height)
                    && !isWithin(mouseX, mouseY, controlX, y, CONTROL_WIDTH, height)
                    && !isWithin(mouseX, mouseY, resetX, y, RESET_BUTTON_WIDTH, height);
        }

        private boolean shouldShowPermissionTooltip(int mouseX, int mouseY) {
            return drawn
                    && visible
                    && !canEditOption()
                    && isWithin(mouseX, mouseY, x, y, Math.max(0, controlX - x - 6), height)
                    && !isWithin(mouseX, mouseY, controlX, y, CONTROL_WIDTH, height)
                    && !isWithin(mouseX, mouseY, resetX, y, RESET_BUTTON_WIDTH, height);
        }

        private List<Text> getDescriptionTooltip() {
            return List.of(definition.label(), definition.description());
        }

        private List<Text> getPermissionTooltip() {
            return List.of(
                    definition.label(),
                    Text.translatable("config_screen.tooltip.option_requires_permission")
            );
        }

        private boolean isAtDefaultValue() {
            return Objects.equals(definition.getValue(workingCopy), definition.getDefaultValue(defaultState));
        }

        private void resetToDefault() {
            copyOptionValue(definition, defaultState, workingCopy);
            updateDoneButtonState();
        }

        private void renderRow(DrawContext context, int mouseX, int mouseY) {
            if (!drawn || !visible) {
                return;
            }

            boolean hovered = isWithin(mouseX, mouseY, x, y, width, height);
            boolean editable = canEditOption();
            context.fill(x - 4, y, x + width, y + height, hovered ? 0x44FFFFFF : 0x22000000);
            if (!editable) {
                context.fill(x - 4, y, x + width, y + height, 0x55000000);
            }

            int titleColor = editable ? (valid ? 0xFFFFFF : 0xFF8080) : 0xA0A0A0;
            context.drawTextWithShadow(textRenderer, definition.label(), x, y + 6, titleColor);
        }
    }

    private final class NumberSliderWidget extends SliderWidget {
        private final OptionRow owner;
        private final ConfigOptionDefinition.NumberOptionDefinition option;
        private boolean syncing;

        private NumberSliderWidget(OptionRow owner, ConfigOptionDefinition.NumberOptionDefinition option) {
            super(0, 0, CONTROL_WIDTH, 20, Text.empty(), 0.0D);
            this.owner = owner;
            this.option = option;
            setIntValue(option.getValue(workingCopy));
        }

        private void setIntValue(int actualValue) {
            this.syncing = true;
            this.value = toSliderValue(actualValue);
            updateMessage();
            this.syncing = false;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(String.valueOf(getActualValue())));
        }

        @Override
        protected void applyValue() {
            if (this.syncing) {
                return;
            }
            option.setValue(workingCopy, getActualValue());
            owner.valid = true;
            updateDoneButtonState();
        }

        private int getActualValue() {
            int min = option.minValue();
            int max = option.maxValue();
            return min + (int) Math.round(this.value * (max - min));
        }

        private double toSliderValue(int actualValue) {
            int min = option.minValue();
            int max = option.maxValue();
            if (max <= min) {
                return 0.0D;
            }
            return (actualValue - min) / (double) (max - min);
        }
    }
}
