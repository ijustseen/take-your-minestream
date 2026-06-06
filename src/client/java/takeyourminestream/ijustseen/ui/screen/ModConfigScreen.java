package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.client.font.TextRenderer;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.config.MessageScale;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ChatRoleFilter;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.integration.twitch.TwitchManager;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.ui.widget.ChanceForSpawnSliderWidget;
import takeyourminestream.ijustseen.ui.widget.ConfigIntTextFieldWidget;
import takeyourminestream.ijustseen.ui.widget.MessageScaleSliderWidget;
import takeyourminestream.ijustseen.ui.widget.MessageSoundVolumeSliderWidget;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import java.util.ArrayList;
import java.util.List;

public class ModConfigScreen extends Screen {
    private final @Nullable Screen parent;
    private String initialChannelName;
    private String hoveredDescriptionKey;
    
    // Категории настроек
    private enum ConfigCategory {
        GENERAL("takeyourminestream.config.category.general"),
        MESSAGES("takeyourminestream.config.category.messages"),
        BEHAVIOR("takeyourminestream.config.category.behavior");
        
        private final String translationKey;
        
        ConfigCategory(String translationKey) {
            this.translationKey = translationKey;
        }
        
        public Text getText() {
            return Text.translatable(translationKey);
        }
    }
    
    private ConfigCategory currentCategory = ConfigCategory.GENERAL;
    private List<ButtonWidget> categoryButtons = new ArrayList<>();
    private List<ConfigEntry> configEntries = new ArrayList<>();
    private ButtonWidget historyButton;
    private ButtonWidget twitchButton;
    private ButtonWidget doneButton;
    
    // Параметры интерфейса
    private static final int TITLE_Y = 6;
    private static final int CATEGORY_Y = 24;
    private static final int HEADER_HEIGHT = 52;
    private static final int MAIN_PANEL_BOTTOM_MARGIN = 12;
    private static final int SCROLL_TO_DESCRIPTION_GAP = 4;
    private static final int DESCRIPTION_TO_BUTTON_GAP = 3;
    private static final int CATEGORY_BUTTON_HEIGHT = 22;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRY_SPACING = 6;
    private static final int CONTENT_PADDING = 10;
    private static final int FOOTER_BUTTON_HEIGHT = 20;
    private static final int FOOTER_BUTTON_BOTTOM_PADDING = 10;
    private static final int FOOTER_ZONE_HEIGHT = FOOTER_BUTTON_HEIGHT + FOOTER_BUTTON_BOTTOM_PADDING;
    private static final int FOOTER_BUTTON_PADDING = 12;
    private static final int FOOTER_BUTTON_GAP = 8;
    private static final int CATEGORY_TO_CONTENT_GAP = 6;
    private static final int SIDE_MARGIN = 24;
    private static final int LABEL_WIDTH = 200;
    private static final int CONTROL_WIDTH = 170;
    private static final int CONTROL_HEIGHT = 20;
    private static final int DESCRIPTION_HEIGHT = 20;
    
    private int scrollOffset = 0;

    // Класс для представления элемента конфигурации
    private static class ConfigEntry {
        public final String labelKey;
        public final String descriptionKey;
        public final ConfigEntryType type;
        public final Object widget;
        public final ConfigCategory category;
        
        public ConfigEntry(String labelKey, String descriptionKey, ConfigEntryType type, Object widget, ConfigCategory category) {
            this.labelKey = labelKey;
            this.descriptionKey = descriptionKey;
            this.type = type;
            this.widget = widget;
            this.category = category;
        }
    }
    
    private enum ConfigEntryType {
        TEXT_FIELD, BUTTON, TOGGLE, SLIDER
    }

    public ModConfigScreen() {
        this(null);
    }

    public ModConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourminestream.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        initialChannelName = ModConfig.getTWITCH_CHANNEL_NAME();
        
        // Создаем кнопки категорий
        createCategoryButtons();
        
        // Создаем элементы конфигурации
        createConfigEntries();
        
        // Создаем кнопки внизу экрана
        createBottomButtons();
        
        // Обновляем видимость элементов для текущей категории
        updateCategoryVisibility();
    }
    
    private void createCategoryButtons() {
        categoryButtons.clear();
        int count = ConfigCategory.values().length;
        int buttonSpacing = 6;
        int availableWidth = this.width - SIDE_MARGIN * 2 - buttonSpacing * (count - 1);
        int buttonWidth = Math.max(86, Math.min(140, availableWidth / count));
        int totalWidth = count * buttonWidth + buttonSpacing * (count - 1);
        int startX = (this.width - totalWidth) / 2;
        int y = CATEGORY_Y;
        
        for (int i = 0; i < count; i++) {
            ConfigCategory category = ConfigCategory.values()[i];
            ButtonWidget button = ButtonWidget.builder(
                category.getText(),
                btn -> {
                    currentCategory = category;
                    updateCategoryVisibility();
                    updateCategoryButtons();
                }
            ).dimensions(startX + i * (buttonWidth + buttonSpacing), y, buttonWidth, CATEGORY_BUTTON_HEIGHT).build();
            
            categoryButtons.add(button);
            this.addDrawableChild(button);
        }
        
        updateCategoryButtons();
    }
    
    private void updateCategoryButtons() {
        for (int i = 0; i < categoryButtons.size(); i++) {
            ButtonWidget button = categoryButtons.get(i);
            button.active = true;
        }
    }
    
    private void createConfigEntries() {
        configEntries.clear();
        TextRenderer textRenderer = this.textRenderer;

        // Общие: Twitch, фильтрация чата, частота появления
        TextFieldWidget channelNameField = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, Text.translatable("takeyourminestream.config.channel_name"));
        channelNameField.setText(ModConfig.getTWITCH_CHANNEL_NAME());
        channelNameField.setChangedListener(s -> ConfigManager.getInstance().setConfigValue("twitchChannelName", s));
        this.addDrawableChild(channelNameField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.channel_name", "takeyourminestream.config.channel_name.desc", ConfigEntryType.TEXT_FIELD, channelNameField, ConfigCategory.GENERAL));

        ButtonWidget autoConnectIrcButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isAUTO_CONNECT_IRC_ON_JOIN() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setAUTO_CONNECT_IRC_ON_JOIN(!ModConfig.isAUTO_CONNECT_IRC_ON_JOIN());
                btn.setMessage(Text.translatable(ModConfig.isAUTO_CONNECT_IRC_ON_JOIN() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(autoConnectIrcButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.auto_connect_irc", "takeyourminestream.config.auto_connect_irc.desc", ConfigEntryType.TOGGLE, autoConnectIrcButton, ConfigCategory.GENERAL));

        ButtonWidget automoderationButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_AUTOMODERATION() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_AUTOMODERATION(!ModConfig.isENABLE_AUTOMODERATION());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_AUTOMODERATION() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(automoderationButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.automoderation", "takeyourminestream.config.automoderation.desc", ConfigEntryType.TOGGLE, automoderationButton, ConfigCategory.GENERAL));

        ButtonWidget banwordsButton = ButtonWidget.builder(
            Text.translatable("takeyourminestream.config.banwords_config"),
            btn -> this.client.setScreen(new BanwordConfigScreen(this))
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(banwordsButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.banwords", "takeyourminestream.config.banwords.desc", ConfigEntryType.BUTTON, banwordsButton, ConfigCategory.GENERAL));

        ButtonWidget regexpButton = ButtonWidget.builder(
            Text.translatable("takeyourminestream.config.regexps_config"),
            btn -> this.client.setScreen(new RegexpConfigScreen(this))
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(regexpButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.regexps", "takeyourminestream.config.regexps.desc", ConfigEntryType.BUTTON, regexpButton, ConfigCategory.GENERAL));

        ButtonWidget roleFilterButton = ButtonWidget.builder(
            getRoleFilterButtonText(),
            btn -> {
                ChatRoleFilter nextFilter = ModConfig.getCHAT_ROLE_FILTER().next();
                ModConfig.setCHAT_ROLE_FILTER(nextFilter);
                btn.setMessage(getRoleFilterButtonText());
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(roleFilterButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.role_filter", "takeyourminestream.config.role_filter.desc", ConfigEntryType.BUTTON, roleFilterButton, ConfigCategory.GENERAL));

        ButtonWidget usernameBlocklistToggle = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_USERNAME_BLOCKLIST() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_USERNAME_BLOCKLIST(!ModConfig.isENABLE_USERNAME_BLOCKLIST());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_USERNAME_BLOCKLIST() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(usernameBlocklistToggle);
        configEntries.add(new ConfigEntry("takeyourminestream.config.username_blocklist", "takeyourminestream.config.username_blocklist.desc", ConfigEntryType.TOGGLE, usernameBlocklistToggle, ConfigCategory.GENERAL));

        ButtonWidget blockedUsersButton = ButtonWidget.builder(
            Text.translatable("takeyourminestream.config.blocked_users_config"),
            btn -> this.client.setScreen(new BlockedUsernameConfigScreen(this))
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(blockedUsersButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.blocked_users", "takeyourminestream.config.blocked_users.desc", ConfigEntryType.BUTTON, blockedUsersButton, ConfigCategory.GENERAL));

        ChanceForSpawnSliderWidget chanceForSpawnSlider = new ChanceForSpawnSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(chanceForSpawnSlider);
        configEntries.add(new ConfigEntry("takeyourminestream.config.chance_for_spawn", "takeyourminestream.config.chance_for_spawn.desc", ConfigEntryType.SLIDER, chanceForSpawnSlider, ConfigCategory.GENERAL));

        ConfigIntTextFieldWidget messageHistoryMaxField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourminestream.config.message_history_max"),
            "messageHistoryMaxSize",
            10,
            500
        );
        this.addDrawableChild(messageHistoryMaxField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_history_max", "takeyourminestream.config.message_history_max.desc", ConfigEntryType.TEXT_FIELD, messageHistoryMaxField, ConfigCategory.GENERAL));

        // Сообщения: вид, время жизни, звук
        ButtonWidget spawnModeButton = ButtonWidget.builder(
            getSpawnModeButtonText(),
            btn -> {
                var currentMode = ModConfig.getMESSAGE_SPAWN_MODE();
                var nextMode = currentMode.next();
                ModConfig.setMESSAGE_SPAWN_MODE(nextMode);
                btn.setMessage(getSpawnModeButtonText());
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(spawnModeButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.spawn_mode_label", "takeyourminestream.config.spawn_mode.desc", ConfigEntryType.BUTTON, spawnModeButton, ConfigCategory.MESSAGES));

        MessageScaleSliderWidget messageScaleSlider = new MessageScaleSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(messageScaleSlider);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_scale", "takeyourminestream.config.message_scale.desc", ConfigEntryType.SLIDER, messageScaleSlider, ConfigCategory.MESSAGES));

        ConfigIntTextFieldWidget messageLifetimeField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourminestream.config.message_lifetime_seconds"),
            "messageLifetimeSeconds",
            1,
            600
        );
        this.addDrawableChild(messageLifetimeField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_lifetime_seconds", "takeyourminestream.config.message_lifetime_seconds.desc", ConfigEntryType.TEXT_FIELD, messageLifetimeField, ConfigCategory.MESSAGES));

        ConfigIntTextFieldWidget messageFallField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourminestream.config.message_fall_seconds"),
            "messageFallSeconds",
            0,
            120
        );
        this.addDrawableChild(messageFallField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_fall_seconds", "takeyourminestream.config.message_fall_seconds.desc", ConfigEntryType.TEXT_FIELD, messageFallField, ConfigCategory.MESSAGES));

        ButtonWidget showBgButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isSHOW_MESSAGE_BACKGROUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setSHOW_MESSAGE_BACKGROUND(!ModConfig.isSHOW_MESSAGE_BACKGROUND());
                btn.setMessage(Text.translatable(ModConfig.isSHOW_MESSAGE_BACKGROUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(showBgButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.show_message_bg", "takeyourminestream.config.show_message_bg.desc", ConfigEntryType.TOGGLE, showBgButton, ConfigCategory.MESSAGES));

        ButtonWidget messageSoundButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_MESSAGE_SOUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_MESSAGE_SOUND(!ModConfig.isENABLE_MESSAGE_SOUND());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_MESSAGE_SOUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(messageSoundButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_sound", "takeyourminestream.config.message_sound.desc", ConfigEntryType.TOGGLE, messageSoundButton, ConfigCategory.MESSAGES));

        MessageSoundVolumeSliderWidget messageSoundVolumeSlider = new MessageSoundVolumeSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(messageSoundVolumeSlider);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_sound_volume", "takeyourminestream.config.message_sound_volume.desc", ConfigEntryType.SLIDER, messageSoundVolumeSlider, ConfigCategory.MESSAGES));

        // Поведение в мире
        ButtonWidget freezingButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_FREEZING_ON_VIEW() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_FREEZING_ON_VIEW(!ModConfig.isENABLE_FREEZING_ON_VIEW());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_FREEZING_ON_VIEW() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(freezingButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.freezing_on_view", "takeyourminestream.config.freezing_on_view.desc", ConfigEntryType.TOGGLE, freezingButton, ConfigCategory.BEHAVIOR));

        ConfigIntTextFieldWidget maxFreezeDistanceField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourminestream.config.max_freeze_distance"),
            "maxFreezeDistance",
            1,
            128
        );
        this.addDrawableChild(maxFreezeDistanceField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.max_freeze_distance", "takeyourminestream.config.max_freeze_distance.desc", ConfigEntryType.TEXT_FIELD, maxFreezeDistanceField, ConfigCategory.BEHAVIOR));

        ButtonWidget followPlayerButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isFOLLOW_PLAYER() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setFOLLOW_PLAYER(!ModConfig.isFOLLOW_PLAYER());
                btn.setMessage(Text.translatable(ModConfig.isFOLLOW_PLAYER() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(followPlayerButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.follow_player", "takeyourminestream.config.follow_player.desc", ConfigEntryType.TOGGLE, followPlayerButton, ConfigCategory.BEHAVIOR));

        ButtonWidget clickToRemoveButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_CLICK_TO_REMOVE() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_CLICK_TO_REMOVE(!ModConfig.isENABLE_CLICK_TO_REMOVE());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_CLICK_TO_REMOVE() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(clickToRemoveButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.click_to_remove", "takeyourminestream.config.click_to_remove.desc", ConfigEntryType.TOGGLE, clickToRemoveButton, ConfigCategory.BEHAVIOR));
    }
    
    private void updateCategoryVisibility() {
        clampScrollOffset();
        for (ConfigEntry entry : configEntries) {
            setWidgetVisible(entry.widget, entry.category == currentCategory);
        }
        updateEntryPositions();
    }
    
    private void updateEntryPositions() {
        int contentTop = getMainPanelTop();
        int contentBottom = getScrollBottom();
        int y = contentTop + CONTENT_PADDING - scrollOffset;
        int rightX = this.width - SIDE_MARGIN - CONTROL_WIDTH;
        
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) {
                setWidgetVisible(entry.widget, false);
                continue;
            }

            setWidgetPosition(entry.widget, rightX, y);
            boolean visibleInViewport = isElementVisible(y, contentTop, contentBottom);
            setWidgetVisible(entry.widget, visibleInViewport);
            
            y += ENTRY_HEIGHT + ENTRY_SPACING;
        }
    }
    

    
    private void createBottomButtons() {
        historyButton = ButtonWidget.builder(Text.translatable("takeyourminestream.config.message_history"), btn -> {
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            if (messageSpawner != null) {
                this.client.setScreen(new MessageHistoryScreen(this, messageSpawner.getLifecycleManager()));
            }
        }).dimensions(0, 0, 1, FOOTER_BUTTON_HEIGHT).build();

        twitchButton = ButtonWidget.builder(getTwitchToggleButtonText(), btn -> {
            handleTwitchToggle();
            btn.setMessage(getTwitchToggleButtonText());
            layoutFooterButtons();
        }).dimensions(0, 0, 1, FOOTER_BUTTON_HEIGHT).build();

        doneButton = ButtonWidget.builder(Text.translatable("gui.done"), btn -> {
            if (!ModConfig.getTWITCH_CHANNEL_NAME().equals(initialChannelName)) {
                TwitchManager.getInstance(ConfigManager.getInstance()).onChannelNameChanged(ModConfig.getTWITCH_CHANNEL_NAME());
            }
            ConfigManager.getInstance().saveConfig();
            this.close();
        }).dimensions(0, 0, 1, FOOTER_BUTTON_HEIGHT).build();

        this.addDrawableChild(historyButton);
        this.addDrawableChild(twitchButton);
        this.addDrawableChild(doneButton);
        layoutFooterButtons();
    }

    private void layoutFooterButtons() {
        if (historyButton == null || twitchButton == null || doneButton == null || this.textRenderer == null) {
            return;
        }

        Text historyLabel = Text.translatable("takeyourminestream.config.message_history");
        Text twitchLabel = getTwitchToggleButtonText();
        Text doneLabel = Text.translatable("gui.done");

        int historyW = this.textRenderer.getWidth(historyLabel) + FOOTER_BUTTON_PADDING * 2;
        int twitchW = this.textRenderer.getWidth(twitchLabel) + FOOTER_BUTTON_PADDING * 2;
        int doneW = this.textRenderer.getWidth(doneLabel) + FOOTER_BUTTON_PADDING * 2;
        int buttonY = getFooterZoneTop();

        int totalWidth = historyW + twitchW + doneW + FOOTER_BUTTON_GAP * 2;
        int x = Math.max(8, (this.width - totalWidth) / 2);

        historyButton.setPosition(x, buttonY);
        historyButton.setDimensions(historyW, FOOTER_BUTTON_HEIGHT);
        x += historyW + FOOTER_BUTTON_GAP;

        twitchButton.setPosition(x, buttonY);
        twitchButton.setDimensions(twitchW, FOOTER_BUTTON_HEIGHT);
        x += twitchW + FOOTER_BUTTON_GAP;

        doneButton.setPosition(x, buttonY);
        doneButton.setDimensions(doneW, FOOTER_BUTTON_HEIGHT);
    }


    private Text getRoleFilterButtonText() {
        return switch (ModConfig.getCHAT_ROLE_FILTER()) {
            case SUBSCRIBERS -> Text.translatable("takeyourminestream.config.role_filter.subscribers");
            case VIP -> Text.translatable("takeyourminestream.config.role_filter.vip");
            case MODS -> Text.translatable("takeyourminestream.config.role_filter.mods");
            case SUB_OR_VIP -> Text.translatable("takeyourminestream.config.role_filter.sub_or_vip");
            case SUB_OR_MOD -> Text.translatable("takeyourminestream.config.role_filter.sub_or_mod");
            default -> Text.translatable("takeyourminestream.config.role_filter.all");
        };
    }

    private Text getSpawnModeButtonText() {
        var mode = ModConfig.getMESSAGE_SPAWN_MODE();
        switch (mode) {
            case AROUND_PLAYER:
                return Text.translatable("takeyourminestream.config.around_player");
            case FRONT_OF_PLAYER:
                return Text.translatable("takeyourminestream.config.fop_only");
            case HUD_WIDGET:
                return Text.translatable("takeyourminestream.config.hud_widget");
            default:
                return Text.translatable("takeyourminestream.config.around_player");
        }
    }
    

    
    private Text getTwitchToggleButtonText() {
        var twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
        boolean isConnected = twitchManager.isConnected();
        
        String statusKey = isConnected ? "takeyourminestream.config.twitch_on" : "takeyourminestream.config.twitch_off";
        MutableText statusText = Text.translatable(statusKey);
        
        // Добавляем цветной индикатор
        MutableText indicator = Text.literal(" ●").formatted(isConnected ? Formatting.GREEN : Formatting.RED);
        
        return statusText.append(indicator);
    }
    
    private void handleTwitchToggle() {
        try {
            var twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            
            if (twitchManager.isConnected()) {
                twitchManager.disconnect();
            } else {
                if (messageSpawner != null) {
                    twitchManager.connect(messageSpawner);
                }
            }
        } catch (Exception e) {
            // Логируем ошибку, но не показываем игроку в GUI
            TakeYourMineStreamClient.LOGGER.error("Twitch connection error: ", e);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateEntryPositions();
        hoveredDescriptionKey = null;

        java.util.Set<ButtonWidget> hiddenButtons = ScreenUiHelper.hideButtons(this);
        List<Object> temporarilyHidden = new ArrayList<>();
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) {
                continue;
            }
            if (entry.widget instanceof TextFieldWidget w && w.visible) {
                w.visible = false;
                temporarilyHidden.add(w);
            } else if (entry.widget instanceof ChanceForSpawnSliderWidget w && w.visible) {
                w.visible = false;
                temporarilyHidden.add(w);
            } else if (entry.widget instanceof MessageScaleSliderWidget w && w.visible) {
                w.visible = false;
                temporarilyHidden.add(w);
            } else if (entry.widget instanceof MessageSoundVolumeSliderWidget w && w.visible) {
                w.visible = false;
                temporarilyHidden.add(w);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        ScreenUiHelper.restoreButtons(hiddenButtons);
        for (Object widget : temporarilyHidden) {
            setWidgetVisible(widget, true);
        }
        
        layoutFooterButtons();
        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width, TITLE_Y);

        int panelTop = getMainPanelTop();
        int panelBottom = getMainPanelBottom();
        int scrollTop = panelTop;
        int scrollBottom = getScrollBottom();

        ModUiTheme.drawBorderedPanel(
            context,
            CONTENT_PADDING,
            panelTop,
            this.width - CONTENT_PADDING * 2,
            panelBottom - panelTop
        );

        context.enableScissor(CONTENT_PADDING, scrollTop, this.width - CONTENT_PADDING, scrollBottom);
        renderLabels(context, mouseX, mouseY, scrollTop, scrollBottom);
        renderConfigWidgets(context, mouseX, mouseY, delta, scrollTop, scrollBottom);
        context.disableScissor();
        renderScrollbar(context, scrollTop, scrollBottom);

        drawFixedPanelFooter(context);

        Text description = hoveredDescriptionKey == null
            ? Text.translatable("takeyourminestream.config.hint_default")
            : Text.translatable(hoveredDescriptionKey);

        int descTop = getDescriptionTop();
        context.drawTextWithShadow(
            this.textRenderer,
            description,
            CONTENT_PADDING + 10,
            descTop + (DESCRIPTION_HEIGHT - this.textRenderer.fontHeight) / 2,
            ModUiTheme.TEXT_SECONDARY
        );

        ButtonWidget selectedTab = null;
        for (int i = 0; i < categoryButtons.size(); i++) {
            if (ConfigCategory.values()[i] == currentCategory) {
                selectedTab = categoryButtons.get(i);
                break;
            }
        }
        ScreenUiHelper.renderButtons(context, mouseX, mouseY, categoryButtons, selectedTab);
        if (historyButton != null) {
            ScreenUiHelper.renderButtons(
                context,
                mouseX,
                mouseY,
                java.util.List.of(historyButton, twitchButton, doneButton),
                null
            );
        }
    }

    /** Непрокручиваемый низ панели: заливка фона панели, чтобы список настроек не просвечивал. */
    private void drawFixedPanelFooter(DrawContext context) {
        int innerLeft = CONTENT_PADDING + 1;
        int innerRight = this.width - CONTENT_PADDING - 1;
        int fixedTop = getDescriptionTop();
        int fixedBottom = getMainPanelBottom() - 1;

        context.fill(innerLeft, fixedTop, innerRight, fixedBottom, ModUiTheme.PANEL_BG);
        context.fill(innerLeft, fixedTop, innerRight, fixedTop + 1, ModUiTheme.PANEL_BORDER);
    }

    private void renderConfigWidgets(DrawContext context, int mouseX, int mouseY, float delta, int contentTop, int contentBottom) {
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) {
                continue;
            }

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
            } else if (entry.widget instanceof ChanceForSpawnSliderWidget widget) {
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    ModUiTheme.drawInputFrame(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), false);
                    widget.render(context, mouseX, mouseY, delta);
                }
            } else if (entry.widget instanceof MessageScaleSliderWidget widget) {
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    ModUiTheme.drawInputFrame(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), false);
                    widget.render(context, mouseX, mouseY, delta);
                }
            } else if (entry.widget instanceof MessageSoundVolumeSliderWidget widget) {
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    ModUiTheme.drawInputFrame(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), false);
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }
    }
    
    private void renderLabels(DrawContext context, int mouseX, int mouseY, int contentTop, int contentBottom) {
        int labelColor = ModUiTheme.TEXT_PRIMARY;
        int fontHeight = this.textRenderer.fontHeight;
        
        int labelX = CONTENT_PADDING + 10;
        int rowLeft = CONTENT_PADDING + 4;
        int rowRight = this.width - CONTENT_PADDING - 4;
        
        int baseY = contentTop + CONTENT_PADDING - scrollOffset;
        int currentY = baseY;
        
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) continue;
            
            if (isElementVisible(currentY, contentTop, contentBottom)) {
                boolean rowHovered = mouseX >= rowLeft && mouseX <= rowRight && mouseY >= currentY && mouseY <= currentY + ENTRY_HEIGHT;
                ModUiTheme.drawListRow(context, rowLeft, currentY, rowRight, currentY + ENTRY_HEIGHT, rowHovered);
                context.drawTextWithShadow(this.textRenderer, Text.translatable(entry.labelKey),
                    labelX, currentY + (20 - fontHeight) / 2, labelColor);

                if (rowHovered) {
                    hoveredDescriptionKey = entry.descriptionKey;
                }
            }
            currentY += ENTRY_HEIGHT + ENTRY_SPACING;
        }
    }
    
    private int getMaxLabelWidth() {
        int maxWidth = 0;
        
        // Проверяем все лейблы и находим максимальную ширину
        for (ConfigEntry entry : configEntries) {
            if (entry.category == currentCategory) {
                String labelText = Text.translatable(entry.labelKey).getString();
                int width = this.textRenderer.getWidth(labelText);
                maxWidth = Math.max(maxWidth, width);
            }
        }
        
        return maxWidth;
    }
    
    private boolean isElementVisible(int elementY, int contentTop, int contentBottom) {
        return elementY + ENTRY_HEIGHT > contentTop && elementY < contentBottom;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    private int getTotalContentHeight() {
        int count = 0;
        for (ConfigEntry entry : configEntries) {
            if (entry.category == currentCategory) {
                count++;
            }
        }
        if (count == 0) {
            return CONTENT_PADDING * 2;
        }
        return count * ENTRY_HEIGHT + (count - 1) * ENTRY_SPACING + CONTENT_PADDING * 2;
    }
    
    private void renderScrollbar(DrawContext context, int contentTop, int contentBottom) {
        int totalContentHeight = getTotalContentHeight();
        int visibleContentHeight = contentBottom - contentTop;
        if (totalContentHeight <= visibleContentHeight) {
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

        int totalContentHeight = getTotalContentHeight();
        int visibleContentHeight = getVisibleContentHeight();
        int maxScroll = Math.max(0, totalContentHeight - visibleContentHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * 20)));
        updateEntryPositions();
        return true;
    }

    private int getCategoryButtonsBottom() {
        return CATEGORY_Y + CATEGORY_BUTTON_HEIGHT;
    }

    private int getMainPanelTop() {
        return getCategoryButtonsBottom() + CATEGORY_TO_CONTENT_GAP;
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

    private void setWidgetVisible(Object widget, boolean visible) {
        if (widget instanceof ButtonWidget) {
            ((ButtonWidget) widget).visible = visible;
        } else if (widget instanceof TextFieldWidget) {
            ((TextFieldWidget) widget).visible = visible;
        } else if (widget instanceof ChanceForSpawnSliderWidget) {
            ((ChanceForSpawnSliderWidget) widget).visible = visible;
        } else if (widget instanceof MessageScaleSliderWidget) {
            ((MessageScaleSliderWidget) widget).visible = visible;
        } else if (widget instanceof MessageSoundVolumeSliderWidget) {
            ((MessageSoundVolumeSliderWidget) widget).visible = visible;
        }
    }

    private void setWidgetPosition(Object widget, int x, int y) {
        int centeredY = y + (ENTRY_HEIGHT - CONTROL_HEIGHT) / 2;
        if (widget instanceof ButtonWidget) {
            ((ButtonWidget) widget).setPosition(x, centeredY);
        } else if (widget instanceof TextFieldWidget) {
            ((TextFieldWidget) widget).setPosition(x, centeredY);
        } else if (widget instanceof ChanceForSpawnSliderWidget) {
            ((ChanceForSpawnSliderWidget) widget).setPosition(x, centeredY);
        } else if (widget instanceof MessageScaleSliderWidget) {
            ((MessageScaleSliderWidget) widget).setPosition(x, centeredY);
        } else if (widget instanceof MessageSoundVolumeSliderWidget) {
            ((MessageSoundVolumeSliderWidget) widget).setPosition(x, centeredY);
        }
    }

    @Override
    public void close() {
        if (this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            this.client.setScreen(null);
        }
    }
} 