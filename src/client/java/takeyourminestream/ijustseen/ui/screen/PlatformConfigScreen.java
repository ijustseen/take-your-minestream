package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.integration.chat.ChatConnectionManager;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.integration.chat.PlatformRoleKind;
import takeyourminestream.ijustseen.ui.gui.ConfigUiHelper;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.MessageEmoteGuiRenderer;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.ui.gui.PlatformBanner;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import takeyourminestream.ijustseen.utils.ScreenNavigationCompat;

import java.util.ArrayList;
import java.util.List;

/** Настройки одной платформы: баннер, канал, показ сообщений, звук и события. */
public class PlatformConfigScreen extends Screen {
    private static final Identifier ICON_BACK =
        Identifier.of("take-your-stream-chat", "textures/gui/icon_back.png");
    private static final int BUTTON_ICON_SIZE = 10;
    private static final int BUTTON_ICON_GAP = 3;

    private static final int TITLE_Y = 6;
    private static final int BANNER_Y = 22;
    private static final int BANNER_HEIGHT = 34;
    private static final int BANNER_TO_PANEL_GAP = 6;
    private static final int CONTENT_PADDING = 10;
    private static final int SIDE_MARGIN = 24;
    private static final int MAIN_PANEL_BOTTOM_MARGIN = 12;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRY_SPACING = 6;
    private static final int CONTROL_WIDTH = 170;
    private static final int CONTROL_HEIGHT = 20;
    private static final int DESCRIPTION_HEIGHT = 20;
    private static final int DESCRIPTION_TO_BUTTON_GAP = 3;
    private static final int SCROLL_TO_DESCRIPTION_GAP = 4;
    private static final int FOOTER_BUTTON_HEIGHT = 20;
    private static final int FOOTER_BUTTON_BOTTOM_PADDING = 10;
    private static final int FOOTER_ZONE_HEIGHT = FOOTER_BUTTON_HEIGHT + FOOTER_BUTTON_BOTTOM_PADDING;
    private static final int FOOTER_BUTTON_PADDING = 12;

    private final @Nullable Screen parent;
    private final ChatPlatform platform;
    private final List<Entry> entries = new ArrayList<>();
    private ButtonWidget backButton;
    private ButtonWidget enabledToggle;
    private String hoveredDescriptionKey;
    private int scrollOffset;

    private static class Entry {
        final String labelKey;
        final String descriptionKey;
        final Object widget;
        java.util.function.BooleanSupplier enabledWhen;

        Entry(String labelKey, String descriptionKey, Object widget) {
            this.labelKey = labelKey;
            this.descriptionKey = descriptionKey;
            this.widget = widget;
        }

        Entry enabledWhen(java.util.function.BooleanSupplier condition) {
            this.enabledWhen = condition;
            return this;
        }

        boolean isEnabled() {
            return enabledWhen == null || enabledWhen.getAsBoolean();
        }
    }

    public PlatformConfigScreen(@Nullable Screen parent, ChatPlatform platform) {
        super(Text.translatable("takeyourstreamchat.config.platform_settings", platform.getDisplayName()));
        this.parent = parent;
        this.platform = platform;
    }

    /** Ключ конфига с каналом/ником платформы. */
    public static String channelConfigKey(ChatPlatform platform) {
        return switch (platform) {
            case TWITCH -> "twitchChannelName";
            case YOUTUBE -> "youtubeChannel";
            case KICK -> "kickChannel";
            case TIKTOK -> "tiktokUsername";
        };
    }

    private static String channelLabelKey(ChatPlatform platform) {
        return switch (platform) {
            case TWITCH -> "takeyourstreamchat.config.channel_name";
            case YOUTUBE -> "takeyourstreamchat.config.youtube_channel";
            case KICK -> "takeyourstreamchat.config.kick_channel";
            case TIKTOK -> "takeyourstreamchat.config.tiktok_username";
        };
    }

    private static String channelValue(ChatPlatform platform) {
        Object value = ConfigManager.getInstance().getConfigValue(channelConfigKey(platform));
        return value instanceof String s ? s : "";
    }

    @Override
    protected void init() {
        entries.clear();
        createChannelEntry();
        createPlatformToggles();
        createBackButton();
        updateEntryPositions();
    }

    private void createChannelEntry() {
        String labelKey = channelLabelKey(platform);
        TextFieldWidget field = new TextFieldWidget(
            this.textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            CONTROL_HEIGHT,
            Text.translatable(labelKey)
        );
        field.setText(channelValue(platform));
        field.setChangedListener(value -> {
            ConfigManager.getInstance().setConfigValue(channelConfigKey(platform), value);
            syncEnabledToggleAvailability();
        });
        this.addDrawableChild(field);
        entries.add(new Entry(labelKey, labelKey + ".desc", field));
    }

    private void createPlatformToggles() {
        String platformKey = platform.getIconKey();

        enabledToggle = addToggle(
            "takeyourstreamchat.config.platform_enabled",
            "takeyourstreamchat.config.platform_enabled.desc",
            () -> ModConfig.isPLATFORM_ENABLED(platformKey),
            value -> {
                ModConfig.setPLATFORM_ENABLED(platformKey, value);
                ChatConnectionManager.getInstance(ConfigManager.getInstance()).reconnectChangedPlatforms();
            }
        );
        addToggle(
            "takeyourstreamchat.config.platform_show_messages",
            "takeyourstreamchat.config.platform_show_messages.desc",
            () -> ModConfig.isPLATFORM_SHOW_MESSAGES(platformKey),
            value -> ModConfig.setPLATFORM_SHOW_MESSAGES(platformKey, value)
        );
        addToggle(
            "takeyourstreamchat.config.platform_message_sound",
            "takeyourstreamchat.config.platform_message_sound.desc",
            () -> ModConfig.isPLATFORM_MESSAGE_SOUND(platformKey),
            value -> ModConfig.setPLATFORM_MESSAGE_SOUND(platformKey, value)
        );

        for (PlatformRoleKind role : PlatformRoleKind.filtersFor(platform)) {
            addToggle(
                role.labelKey(platform),
                role.labelKey(platform) + ".desc",
                () -> ModConfig.isPLATFORM_ROLE(platformKey, role),
                value -> ModConfig.setPLATFORM_ROLE(platformKey, role, value)
            );
            if (!role.isAll()) {
                lastEntry().enabledWhen(() -> !ModConfig.isPLATFORM_ROLE_ALL(platformKey));
            }
        }

        if (platform == ChatPlatform.TIKTOK) {
            addToggle(
                "takeyourstreamchat.config.tiktok_gift_events",
                "takeyourstreamchat.config.tiktok_gift_events.desc",
                ModConfig::isTIKTOK_GIFT_EVENTS,
                ModConfig::setTIKTOK_GIFT_EVENTS
            );
            addToggle(
                "takeyourstreamchat.config.tiktok_follow_events",
                "takeyourstreamchat.config.tiktok_follow_events.desc",
                ModConfig::isTIKTOK_FOLLOW_EVENTS,
                ModConfig::setTIKTOK_FOLLOW_EVENTS
            );
        }

        syncEnabledToggleAvailability();
    }

    private ButtonWidget addToggle(
        String labelKey,
        String descriptionKey,
        java.util.function.BooleanSupplier getter,
        java.util.function.Consumer<Boolean> setter
    ) {
        ButtonWidget button = ButtonWidget.builder(
            ConfigUiHelper.onOffText(getter.getAsBoolean()),
            btn -> {
                setter.accept(!getter.getAsBoolean());
                btn.setMessage(ConfigUiHelper.onOffText(getter.getAsBoolean()));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT).build();
        this.addDrawableChild(button);
        entries.add(new Entry(labelKey, descriptionKey, button));
        return button;
    }

    private Entry lastEntry() {
        return entries.get(entries.size() - 1);
    }

    /** Платформу нельзя включить с пустым ником: выключаем и блокируем тумблер. */
    private void syncEnabledToggleAvailability() {
        if (enabledToggle == null) {
            return;
        }
        String platformKey = platform.getIconKey();
        boolean hasChannel = !channelValue(platform).trim().isEmpty();
        if (!hasChannel && ModConfig.isPLATFORM_ENABLED(platformKey)) {
            ModConfig.setPLATFORM_ENABLED(platformKey, false);
        }
        enabledToggle.active = hasChannel;
        enabledToggle.setMessage(ConfigUiHelper.onOffText(ModConfig.isPLATFORM_ENABLED(platformKey)));
    }

    private void createBackButton() {
        Text label = Text.translatable("takeyourstreamchat.config.back");
        int width = BUTTON_ICON_SIZE + BUTTON_ICON_GAP
            + this.textRenderer.getWidth(label) + FOOTER_BUTTON_PADDING * 2;
        backButton = ButtonWidget.builder(label, btn -> this.close())
            .dimensions((this.width - width) / 2, getFooterZoneTop(), width, FOOTER_BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(backButton);
    }

    private void updateEntryPositions() {
        clampScrollOffset();
        int contentTop = getMainPanelTop();
        int contentBottom = getScrollBottom();
        int controlX = this.width - SIDE_MARGIN - CONTROL_WIDTH;
        int y = contentTop + CONTENT_PADDING - scrollOffset;

        for (Entry entry : entries) {
            boolean visible = isElementVisible(y, contentTop, contentBottom);
            setWidgetPosition(entry.widget, controlX, y);
            setWidgetVisible(entry.widget, visible);
            setWidgetEnabled(entry.widget, entry.isEnabled());
            y += ENTRY_HEIGHT + ENTRY_SPACING;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateEntryPositions();
        hoveredDescriptionKey = null;

        java.util.Set<ButtonWidget> hiddenButtons = ScreenUiHelper.hideButtons(this);
        List<Object> temporarilyHidden = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.widget instanceof TextFieldWidget w && w.visible) {
                w.visible = false;
                temporarilyHidden.add(w);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        ScreenUiHelper.restoreButtons(hiddenButtons);
        for (Object widget : temporarilyHidden) {
            setWidgetVisible(widget, true);
        }

        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width, TITLE_Y);
        drawBanner(context);

        int panelTop = getMainPanelTop();
        int panelBottom = getMainPanelBottom();
        int scrollBottom = getScrollBottom();

        ModUiTheme.drawBorderedPanel(
            context,
            CONTENT_PADDING,
            panelTop,
            this.width - CONTENT_PADDING * 2,
            panelBottom - panelTop
        );

        context.enableScissor(CONTENT_PADDING, panelTop, this.width - CONTENT_PADDING, scrollBottom);
        renderLabels(context, mouseX, mouseY, panelTop, scrollBottom);
        renderWidgets(context, mouseX, mouseY, delta, panelTop, scrollBottom);
        context.disableScissor();
        renderScrollbar(context, panelTop, scrollBottom);

        drawFixedPanelFooter(context);

        Text description = hoveredDescriptionKey == null
            ? Text.translatable("takeyourstreamchat.config.hint_default")
            : Text.translatable(hoveredDescriptionKey);
        context.drawTextWithShadow(
            this.textRenderer,
            description,
            CONTENT_PADDING + 10,
            getDescriptionTop() + (DESCRIPTION_HEIGHT - this.textRenderer.fontHeight) / 2,
            ModUiTheme.TEXT_SECONDARY
        );

        if (backButton != null) {
            backButton.setPosition(backButton.getX(), getFooterZoneTop());
            boolean hovered = ModUiTheme.isHovered(
                mouseX,
                mouseY,
                backButton.getX(),
                backButton.getY(),
                backButton.getWidth(),
                backButton.getHeight()
            );
            ModUiTheme.drawButton(
                context,
                this.textRenderer,
                backButton.getX(),
                backButton.getY(),
                backButton.getWidth(),
                backButton.getHeight(),
                Text.empty(),
                hovered,
                true,
                false,
                true
            );
            Text label = Text.translatable("takeyourstreamchat.config.back");
            int textWidth = this.textRenderer.getWidth(label);
            int totalWidth = BUTTON_ICON_SIZE + BUTTON_ICON_GAP + textWidth;
            int startX = backButton.getX() + (backButton.getWidth() - totalWidth) / 2;
            MessageEmoteGuiRenderer.drawGuiIcon(
                context,
                ICON_BACK,
                startX,
                backButton.getY() + (backButton.getHeight() - BUTTON_ICON_SIZE) / 2,
                BUTTON_ICON_SIZE
            );
            context.drawTextWithShadow(
                this.textRenderer,
                label,
                startX + BUTTON_ICON_SIZE + BUTTON_ICON_GAP,
                ModUiTheme.centeredTextY(backButton.getY(), backButton.getHeight(), this.textRenderer.fontHeight),
                hovered ? ModUiTheme.TEXT_PRIMARY : ModUiTheme.TEXT_SECONDARY
            );
        }
    }

    private void drawBanner(DrawContext context) {
        PlatformBanner.draw(
            context,
            this.textRenderer,
            platform,
            CONTENT_PADDING,
            BANNER_Y,
            this.width - CONTENT_PADDING * 2,
            BANNER_HEIGHT,
            false
        );
    }

    private void renderLabels(DrawContext context, int mouseX, int mouseY, int contentTop, int contentBottom) {
        int labelX = CONTENT_PADDING + 10;
        int rowLeft = CONTENT_PADDING + 4;
        int rowRight = this.width - CONTENT_PADDING - 4;
        int y = contentTop + CONTENT_PADDING - scrollOffset;

        for (Entry entry : entries) {
            if (isElementVisible(y, contentTop, contentBottom)) {
                boolean rowHovered = mouseX >= rowLeft && mouseX <= rowRight
                    && mouseY >= y && mouseY <= y + ENTRY_HEIGHT;
                ModUiTheme.drawListRow(context, rowLeft, y, rowRight, y + ENTRY_HEIGHT, rowHovered);
                context.drawTextWithShadow(
                    this.textRenderer,
                    Text.translatable(entry.labelKey),
                    labelX,
                    y + (ENTRY_HEIGHT - this.textRenderer.fontHeight) / 2,
                    entry.isEnabled() ? ModUiTheme.TEXT_PRIMARY : ModUiTheme.TEXT_HINT
                );
                if (rowHovered) {
                    hoveredDescriptionKey = entry.descriptionKey;
                }
            }
            y += ENTRY_HEIGHT + ENTRY_SPACING;
        }
    }

    private void renderWidgets(
        DrawContext context,
        int mouseX,
        int mouseY,
        float delta,
        int contentTop,
        int contentBottom
    ) {
        for (Entry entry : entries) {
            if (entry.widget instanceof ButtonWidget widget) {
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    boolean hovered = ModUiTheme.isHovered(
                        mouseX,
                        mouseY,
                        widget.getX(),
                        widget.getY(),
                        widget.getWidth(),
                        widget.getHeight()
                    );
                    ModUiTheme.drawButton(
                        context,
                        this.textRenderer,
                        widget.getX(),
                        widget.getY(),
                        widget.getWidth(),
                        widget.getHeight(),
                        widget.getMessage(),
                        hovered,
                        widget.active,
                        false,
                        true
                    );
                }
            } else if (entry.widget instanceof TextFieldWidget widget) {
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    ModUiTheme.drawInputFrame(
                        context,
                        widget.getX(),
                        widget.getY(),
                        widget.getWidth(),
                        widget.getHeight(),
                        widget.isFocused()
                    );
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }
    }

    /** Непрокручиваемый низ панели: заливка фона, чтобы список не просвечивал. */
    private void drawFixedPanelFooter(DrawContext context) {
        int innerLeft = CONTENT_PADDING + 1;
        int innerRight = this.width - CONTENT_PADDING - 1;
        int fixedTop = getDescriptionTop();
        int fixedBottom = getMainPanelBottom() - 1;

        context.fill(innerLeft, fixedTop, innerRight, fixedBottom, ModUiTheme.PANEL_BG);
        context.fill(innerLeft, fixedTop, innerRight, fixedTop + 1, ModUiTheme.PANEL_BORDER);
    }

    private void renderScrollbar(DrawContext context, int contentTop, int contentBottom) {
        int totalContentHeight = getTotalContentHeight();
        if (totalContentHeight <= contentBottom - contentTop) {
            return;
        }
        GuiScrollbar.draw(context, this.width - 16, contentTop, contentBottom, scrollOffset, totalContentHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentTop = getMainPanelTop();
        int contentBottom = getScrollBottom();
        if (mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int maxScroll = Math.max(0, getTotalContentHeight() - getVisibleContentHeight());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 20)));
        updateEntryPositions();
        return true;
    }

    private int getTotalContentHeight() {
        int count = entries.size();
        if (count == 0) {
            return CONTENT_PADDING * 2;
        }
        return count * ENTRY_HEIGHT + (count - 1) * ENTRY_SPACING + CONTENT_PADDING * 2;
    }

    private int getMainPanelTop() {
        return BANNER_Y + BANNER_HEIGHT + BANNER_TO_PANEL_GAP;
    }

    private int getMainPanelBottom() {
        return this.height - MAIN_PANEL_BOTTOM_MARGIN;
    }

    private int getFooterZoneTop() {
        return getMainPanelBottom() - FOOTER_ZONE_HEIGHT;
    }

    private int getDescriptionTop() {
        return getFooterZoneTop() - DESCRIPTION_HEIGHT - DESCRIPTION_TO_BUTTON_GAP;
    }

    private int getScrollBottom() {
        return getDescriptionTop() - SCROLL_TO_DESCRIPTION_GAP;
    }

    private int getVisibleContentHeight() {
        return getScrollBottom() - getMainPanelTop();
    }

    private void clampScrollOffset() {
        int maxScroll = Math.max(0, getTotalContentHeight() - getVisibleContentHeight());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private boolean isElementVisible(int elementY, int contentTop, int contentBottom) {
        return elementY + ENTRY_HEIGHT > contentTop && elementY < contentBottom;
    }

    private void setWidgetEnabled(Object widget, boolean enabled) {
        if (widget instanceof ButtonWidget button && widget != enabledToggle) {
            button.active = enabled;
        } else if (widget instanceof TextFieldWidget field) {
            field.setEditable(enabled);
        }
    }

    private void setWidgetVisible(Object widget, boolean visible) {
        if (widget instanceof ButtonWidget) {
            ((ButtonWidget) widget).visible = visible;
        } else if (widget instanceof TextFieldWidget) {
            ((TextFieldWidget) widget).visible = visible;
        }
    }

    private void setWidgetPosition(Object widget, int x, int y) {
        int centeredY = y + (ENTRY_HEIGHT - CONTROL_HEIGHT) / 2;
        if (widget instanceof ButtonWidget) {
            ((ButtonWidget) widget).setPosition(x, centeredY);
        } else if (widget instanceof TextFieldWidget) {
            ((TextFieldWidget) widget).setPosition(x, centeredY);
        }
    }

    @Override
    public void close() {
        ConfigManager.getInstance().saveConfig();
        ChatConnectionManager.getInstance(ConfigManager.getInstance()).reconnectChangedPlatforms();
        ScreenNavigationCompat.open(this.client, this.parent);
    }
}
