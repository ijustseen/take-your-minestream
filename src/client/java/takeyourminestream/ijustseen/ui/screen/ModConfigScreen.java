package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.font.TextRenderer;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.config.MessageScale;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ChatRoleFilter;
import takeyourminestream.ijustseen.config.UnpinMode;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.integration.chat.ChatConnectionManager;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.ui.widget.ChanceForSpawnSliderWidget;
import takeyourminestream.ijustseen.ui.widget.ConfigIntTextFieldWidget;
import takeyourminestream.ijustseen.ui.widget.MessageScaleSliderWidget;
import takeyourminestream.ijustseen.ui.widget.MessageSoundVolumeSliderWidget;
import takeyourminestream.ijustseen.ui.gui.ConfigUiHelper;
import takeyourminestream.ijustseen.ui.gui.ChatConnectToggleHelper;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import takeyourminestream.ijustseen.ui.widget.PlatformChannelRow;
import java.util.ArrayList;
import java.util.List;

public class ModConfigScreen extends Screen {
    private final @Nullable Screen parent;
    private String hoveredDescriptionKey;
    
    // Категории настроек
    private enum ConfigCategory {
        GENERAL("takeyourstreamchat.config.category.general", "icon_tab_general"),
        MESSAGES("takeyourstreamchat.config.category.messages", "icon_tab_messages"),
        BEHAVIOR("takeyourstreamchat.config.category.behavior", "icon_tab_world");
        
        private final String translationKey;
        private final Identifier icon;
        
        ConfigCategory(String translationKey, String iconName) {
            this.translationKey = translationKey;
            this.icon = Identifier.of("take-your-stream-chat", "textures/gui/" + iconName + ".png");
        }
        
        public Text getText() {
            return Text.translatable(translationKey);
        }

        public Identifier getIcon() {
            return icon;
        }
    }
    
    private static final Identifier ICON_HISTORY =
        Identifier.of("take-your-stream-chat", "textures/gui/icon_history.png");
    private static final int BUTTON_ICON_SIZE = 10;
    private static final int BUTTON_ICON_GAP = 3;

    private ConfigCategory currentCategory = ConfigCategory.GENERAL;
    private List<ButtonWidget> categoryButtons = new ArrayList<>();
    private List<ConfigEntry> configEntries = new ArrayList<>();
    private ButtonWidget historyButton;
    private ButtonWidget chatToggleButton;
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
    private static final int TOGGLE_BUTTON_WIDTH = 44;
    private static final int PLATFORM_FIELD_GAP = 6;
    private static final int PLATFORM_FIELD_WIDTH = CONTROL_WIDTH - TOGGLE_BUTTON_WIDTH - PLATFORM_FIELD_GAP;
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
        TEXT_FIELD, BUTTON, TOGGLE, SLIDER, PLATFORM_ROW
    }

    public ModConfigScreen() {
        this(null);
    }

    public ModConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourstreamchat.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
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
        // В HUD-режиме настройки "В мире" не применимы — вкладка недоступна
        boolean hudMode = ModConfig.getMESSAGE_SPAWN_MODE() == MessageSpawnMode.HUD_WIDGET;
        for (int i = 0; i < categoryButtons.size(); i++) {
            ConfigCategory category = ConfigCategory.values()[i];
            categoryButtons.get(i).active = category != ConfigCategory.BEHAVIOR || !hudMode;
        }
        if (hudMode && currentCategory == ConfigCategory.BEHAVIOR) {
            currentCategory = ConfigCategory.GENERAL;
            updateCategoryVisibility();
        }
    }
    
    private void createConfigEntries() {
        configEntries.clear();
        TextRenderer textRenderer = this.textRenderer;

        // Подключения к платформам: поле + вкл/выкл справа
        addPlatformRow(
            "takeyourstreamchat.config.channel_name",
            "takeyourstreamchat.config.channel_name.desc",
            ModConfig.getTWITCH_CHANNEL_NAME(),
            "twitchChannelName",
            ModConfig::isTWITCH_ENABLED,
            ModConfig::setTWITCH_ENABLED,
            "twitch"
        );
        addPlatformRow(
            "takeyourstreamchat.config.youtube_channel",
            "takeyourstreamchat.config.youtube_channel.desc",
            ModConfig.getYOUTUBE_CHANNEL(),
            "youtubeChannel",
            ModConfig::isYOUTUBE_ENABLED,
            ModConfig::setYOUTUBE_ENABLED,
            "youtube"
        );
        addPlatformRow(
            "takeyourstreamchat.config.kick_channel",
            "takeyourstreamchat.config.kick_channel.desc",
            ModConfig.getKICK_CHANNEL(),
            "kickChannel",
            ModConfig::isKICK_ENABLED,
            ModConfig::setKICK_ENABLED,
            "kick"
        );
        addPlatformRow(
            "takeyourstreamchat.config.tiktok_username",
            "takeyourstreamchat.config.tiktok_username.desc",
            ModConfig.getTIKTOK_USERNAME(),
            "tiktokUsername",
            ModConfig::isTIKTOK_ENABLED,
            ModConfig::setTIKTOK_ENABLED,
            "tiktok"
        );

        addToggleEntry(
            "takeyourstreamchat.config.auto_connect_irc",
            "takeyourstreamchat.config.auto_connect_irc.desc",
            ModConfig::isAUTO_CONNECT_IRC_ON_JOIN,
            value -> ModConfig.setAUTO_CONNECT_IRC_ON_JOIN(value),
            ConfigCategory.GENERAL
        );

        ChanceForSpawnSliderWidget chanceForSpawnSlider = new ChanceForSpawnSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(chanceForSpawnSlider);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.chance_for_spawn", "takeyourstreamchat.config.chance_for_spawn.desc", ConfigEntryType.SLIDER, chanceForSpawnSlider, ConfigCategory.GENERAL));

        ButtonWidget roleFilterButton = ButtonWidget.builder(
            getRoleFilterButtonText(),
            btn -> {
                ChatRoleFilter nextFilter = ModConfig.getCHAT_ROLE_FILTER().next();
                ModConfig.setCHAT_ROLE_FILTER(nextFilter);
                btn.setMessage(getRoleFilterButtonText());
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(roleFilterButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.role_filter", "takeyourstreamchat.config.role_filter.desc", ConfigEntryType.BUTTON, roleFilterButton, ConfigCategory.GENERAL));

        addToggleEntry(
            "takeyourstreamchat.config.automoderation",
            "takeyourstreamchat.config.automoderation.desc",
            ModConfig::isENABLE_AUTOMODERATION,
            ModConfig::setENABLE_AUTOMODERATION,
            ConfigCategory.GENERAL
        );

        ButtonWidget banwordsButton = ButtonWidget.builder(
            Text.translatable("takeyourstreamchat.config.banwords_config"),
            btn -> this.client.setScreen(new BanwordConfigScreen(this))
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(banwordsButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.banwords", "takeyourstreamchat.config.banwords.desc", ConfigEntryType.BUTTON, banwordsButton, ConfigCategory.GENERAL));

        ButtonWidget regexpButton = ButtonWidget.builder(
            Text.translatable("takeyourstreamchat.config.regexps_config"),
            btn -> this.client.setScreen(new RegexpConfigScreen(this))
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(regexpButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.regexps", "takeyourstreamchat.config.regexps.desc", ConfigEntryType.BUTTON, regexpButton, ConfigCategory.GENERAL));

        addToggleEntry(
            "takeyourstreamchat.config.username_blocklist",
            "takeyourstreamchat.config.username_blocklist.desc",
            ModConfig::isENABLE_USERNAME_BLOCKLIST,
            ModConfig::setENABLE_USERNAME_BLOCKLIST,
            ConfigCategory.GENERAL
        );

        ButtonWidget blockedUsersButton = ButtonWidget.builder(
            Text.translatable("takeyourstreamchat.config.blocked_users_config"),
            btn -> this.client.setScreen(new BlockedUsernameConfigScreen(this))
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(blockedUsersButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.blocked_users", "takeyourstreamchat.config.blocked_users.desc", ConfigEntryType.BUTTON, blockedUsersButton, ConfigCategory.GENERAL));

        // Сообщения: вид, время жизни, звук
        ButtonWidget spawnModeButton = ButtonWidget.builder(
            getSpawnModeButtonText(),
            btn -> {
                var currentMode = ModConfig.getMESSAGE_SPAWN_MODE();
                var nextMode = currentMode.next();
                ModConfig.setMESSAGE_SPAWN_MODE(nextMode);
                btn.setMessage(getSpawnModeButtonText());
                updateCategoryButtons();
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(spawnModeButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.spawn_mode_label", "takeyourstreamchat.config.spawn_mode.desc", ConfigEntryType.BUTTON, spawnModeButton, ConfigCategory.MESSAGES));

        MessageScaleSliderWidget messageScaleSlider = new MessageScaleSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(messageScaleSlider);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.message_scale", "takeyourstreamchat.config.message_scale.desc", ConfigEntryType.SLIDER, messageScaleSlider, ConfigCategory.MESSAGES));

        ConfigIntTextFieldWidget messageLifetimeField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourstreamchat.config.message_lifetime_seconds"),
            "messageLifetimeSeconds",
            1,
            600
        );
        this.addDrawableChild(messageLifetimeField);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.message_lifetime_seconds", "takeyourstreamchat.config.message_lifetime_seconds.desc", ConfigEntryType.TEXT_FIELD, messageLifetimeField, ConfigCategory.MESSAGES));

        ButtonWidget showBgButton = createToggleButton(ModConfig::isSHOW_MESSAGE_BACKGROUND, ModConfig::setSHOW_MESSAGE_BACKGROUND);
        this.addDrawableChild(showBgButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.show_message_bg", "takeyourstreamchat.config.show_message_bg.desc", ConfigEntryType.TOGGLE, showBgButton, ConfigCategory.MESSAGES));

        ButtonWidget colorEmojiButton = createToggleButton(ModConfig::isENABLE_COLOR_EMOJIS, ModConfig::setENABLE_COLOR_EMOJIS);
        this.addDrawableChild(colorEmojiButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.color_emojis", "takeyourstreamchat.config.color_emojis.desc", ConfigEntryType.TOGGLE, colorEmojiButton, ConfigCategory.MESSAGES));

        ButtonWidget messageSoundButton = createToggleButton(ModConfig::isENABLE_MESSAGE_SOUND, ModConfig::setENABLE_MESSAGE_SOUND);
        this.addDrawableChild(messageSoundButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.message_sound", "takeyourstreamchat.config.message_sound.desc", ConfigEntryType.TOGGLE, messageSoundButton, ConfigCategory.MESSAGES));

        MessageSoundVolumeSliderWidget messageSoundVolumeSlider = new MessageSoundVolumeSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(messageSoundVolumeSlider);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.message_sound_volume", "takeyourstreamchat.config.message_sound_volume.desc", ConfigEntryType.SLIDER, messageSoundVolumeSlider, ConfigCategory.MESSAGES));

        ConfigIntTextFieldWidget messageHistoryMaxField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourstreamchat.config.message_history_max"),
            "messageHistoryMaxSize",
            10,
            500
        );
        this.addDrawableChild(messageHistoryMaxField);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.message_history_max", "takeyourstreamchat.config.message_history_max.desc", ConfigEntryType.TEXT_FIELD, messageHistoryMaxField, ConfigCategory.MESSAGES));

        // Поведение в мире
        ConfigIntTextFieldWidget spawnMinDistanceField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourstreamchat.config.spawn_min_distance"),
            "messageSpawnMinDistance",
            1,
            64
        );
        this.addDrawableChild(spawnMinDistanceField);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.spawn_min_distance", "takeyourstreamchat.config.spawn_min_distance.desc", ConfigEntryType.TEXT_FIELD, spawnMinDistanceField, ConfigCategory.BEHAVIOR));

        ConfigIntTextFieldWidget spawnMaxDistanceField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourstreamchat.config.spawn_max_distance"),
            "messageSpawnMaxDistance",
            1,
            64
        );
        this.addDrawableChild(spawnMaxDistanceField);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.spawn_max_distance", "takeyourstreamchat.config.spawn_max_distance.desc", ConfigEntryType.TEXT_FIELD, spawnMaxDistanceField, ConfigCategory.BEHAVIOR));

        ButtonWidget freezingButton = createToggleButton(ModConfig::isENABLE_FREEZING_ON_VIEW, ModConfig::setENABLE_FREEZING_ON_VIEW);
        this.addDrawableChild(freezingButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.freezing_on_view", "takeyourstreamchat.config.freezing_on_view.desc", ConfigEntryType.TOGGLE, freezingButton, ConfigCategory.BEHAVIOR));

        ConfigIntTextFieldWidget maxFreezeDistanceField = new ConfigIntTextFieldWidget(
            textRenderer,
            0,
            0,
            CONTROL_WIDTH,
            20,
            Text.translatable("takeyourstreamchat.config.max_freeze_distance"),
            "maxFreezeDistance",
            1,
            128
        );
        this.addDrawableChild(maxFreezeDistanceField);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.max_freeze_distance", "takeyourstreamchat.config.max_freeze_distance.desc", ConfigEntryType.TEXT_FIELD, maxFreezeDistanceField, ConfigCategory.BEHAVIOR));

        ButtonWidget followPlayerButton = createToggleButton(ModConfig::isFOLLOW_PLAYER, ModConfig::setFOLLOW_PLAYER);
        this.addDrawableChild(followPlayerButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.follow_player", "takeyourstreamchat.config.follow_player.desc", ConfigEntryType.TOGGLE, followPlayerButton, ConfigCategory.BEHAVIOR));

        ButtonWidget clickToRemoveButton = createToggleButton(ModConfig::isENABLE_CLICK_TO_REMOVE, ModConfig::setENABLE_CLICK_TO_REMOVE);
        this.addDrawableChild(clickToRemoveButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.click_to_remove", "takeyourstreamchat.config.click_to_remove.desc", ConfigEntryType.TOGGLE, clickToRemoveButton, ConfigCategory.BEHAVIOR));

        ButtonWidget unpinModeButton = ButtonWidget.builder(
            getUnpinModeButtonText(),
            btn -> {
                UnpinMode nextMode = ModConfig.getUNPIN_MODE().next();
                ModConfig.setUNPIN_MODE(nextMode);
                btn.setMessage(getUnpinModeButtonText());
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(unpinModeButton);
        configEntries.add(new ConfigEntry("takeyourstreamchat.config.unpin_mode", "takeyourstreamchat.config.unpin_mode.desc", ConfigEntryType.BUTTON, unpinModeButton, ConfigCategory.BEHAVIOR));
    }

    private ButtonWidget createToggleButton(
        java.util.function.BooleanSupplier getter,
        java.util.function.Consumer<Boolean> setter
    ) {
        return ButtonWidget.builder(
            ConfigUiHelper.onOffText(getter.getAsBoolean()),
            btn -> {
                setter.accept(!getter.getAsBoolean());
                btn.setMessage(ConfigUiHelper.onOffText(getter.getAsBoolean()));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT).build();
    }

    private void addToggleEntry(
        String labelKey,
        String descriptionKey,
        java.util.function.BooleanSupplier getter,
        java.util.function.Consumer<Boolean> setter,
        ConfigCategory category
    ) {
        ButtonWidget button = createToggleButton(getter, setter);
        this.addDrawableChild(button);
        configEntries.add(new ConfigEntry(labelKey, descriptionKey, ConfigEntryType.TOGGLE, button, category));
    }

    private void addPlatformRow(
        String labelKey,
        String descriptionKey,
        String initialValue,
        String configKey,
        java.util.function.BooleanSupplier enabledGetter,
        java.util.function.Consumer<Boolean> enabledSetter,
        String platformIconKey
    ) {
        TextFieldWidget field = new TextFieldWidget(
            this.textRenderer,
            0,
            0,
            PLATFORM_FIELD_WIDTH,
            CONTROL_HEIGHT,
            Text.translatable(labelKey)
        );
        field.setText(initialValue);
        this.addDrawableChild(field);

        ButtonWidget toggle = ButtonWidget.builder(
            ConfigUiHelper.onOffText(enabledGetter.getAsBoolean()),
            btn -> {
                boolean enabled = !enabledGetter.getAsBoolean();
                enabledSetter.accept(enabled);
                btn.setMessage(ConfigUiHelper.onOffText(enabled));
                applyConnectionSettingsFromConfig(enabled);
            }
        ).dimensions(0, 0, TOGGLE_BUTTON_WIDTH, CONTROL_HEIGHT).build();
        this.addDrawableChild(toggle);

        // Платформу нельзя включить с пустым ником: выключаем и блокируем тумблер
        Runnable syncToggleAvailability = () -> {
            boolean hasText = !field.getText().trim().isEmpty();
            if (!hasText && enabledGetter.getAsBoolean()) {
                enabledSetter.accept(false);
            }
            toggle.active = hasText;
            toggle.setMessage(ConfigUiHelper.onOffText(enabledGetter.getAsBoolean()));
        };
        field.setChangedListener(value -> {
            ConfigManager.getInstance().setConfigValue(configKey, value);
            syncToggleAvailability.run();
        });
        syncToggleAvailability.run();

        PlatformChannelRow row = new PlatformChannelRow(field, toggle, platformIconKey);
        configEntries.add(new ConfigEntry(labelKey, descriptionKey, ConfigEntryType.PLATFORM_ROW, row, ConfigCategory.GENERAL));
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

            if (entry.type == ConfigEntryType.PLATFORM_ROW && entry.widget instanceof PlatformChannelRow row) {
                positionPlatformRow(row, rightX, y);
                boolean visibleInViewport = isElementVisible(y, contentTop, contentBottom);
                setWidgetVisible(row.field, visibleInViewport);
                setWidgetVisible(row.toggle, visibleInViewport);
            } else {
                setWidgetPosition(entry.widget, rightX, y);
                boolean visibleInViewport = isElementVisible(y, contentTop, contentBottom);
                setWidgetVisible(entry.widget, visibleInViewport);
            }
            
            y += ENTRY_HEIGHT + ENTRY_SPACING;
        }
    }

    private void positionPlatformRow(PlatformChannelRow row, int rightX, int y) {
        int centeredY = y + (ENTRY_HEIGHT - CONTROL_HEIGHT) / 2;
        int toggleX = rightX + CONTROL_WIDTH - TOGGLE_BUTTON_WIDTH;
        row.field.setPosition(rightX, centeredY);
        row.field.setDimensions(PLATFORM_FIELD_WIDTH, CONTROL_HEIGHT);
        row.toggle.setPosition(toggleX, centeredY);
        row.toggle.setDimensions(TOGGLE_BUTTON_WIDTH, CONTROL_HEIGHT);
    }
    

    
    private void createBottomButtons() {
        historyButton = ButtonWidget.builder(Text.translatable("takeyourstreamchat.config.message_history"), btn -> {
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            if (messageSpawner != null) {
                this.client.setScreen(new MessageHistoryScreen(this, messageSpawner.getLifecycleManager()));
            }
        }).dimensions(0, 0, 1, FOOTER_BUTTON_HEIGHT).build();

        chatToggleButton = ButtonWidget.builder(
            ChatConnectToggleHelper.buttonLabel(),
            btn -> {
                ChatConnectToggleHelper.toggle();
                btn.setMessage(ChatConnectToggleHelper.buttonLabel());
            }
        ).dimensions(0, 0, 1, FOOTER_BUTTON_HEIGHT).build();

        doneButton = ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close())
            .dimensions(0, 0, 1, FOOTER_BUTTON_HEIGHT).build();

        this.addDrawableChild(historyButton);
        this.addDrawableChild(chatToggleButton);
        this.addDrawableChild(doneButton);
        layoutFooterButtons();
    }

    private void layoutFooterButtons() {
        if (historyButton == null || chatToggleButton == null || doneButton == null || this.textRenderer == null) {
            return;
        }

        Text historyLabel = Text.translatable("takeyourstreamchat.config.message_history");
        Text chatLabel = ChatConnectToggleHelper.buttonLabel();
        Text doneLabel = Text.translatable("gui.done");

        int historyW = BUTTON_ICON_SIZE + BUTTON_ICON_GAP
            + this.textRenderer.getWidth(historyLabel) + FOOTER_BUTTON_PADDING * 2;
        int chatW = this.textRenderer.getWidth(chatLabel) + FOOTER_BUTTON_PADDING * 2;
        int doneW = this.textRenderer.getWidth(doneLabel) + FOOTER_BUTTON_PADDING * 2;
        int buttonY = getFooterZoneTop();

        int totalWidth = historyW + chatW + doneW + FOOTER_BUTTON_GAP * 2;
        int x = Math.max(8, (this.width - totalWidth) / 2);

        historyButton.setPosition(x, buttonY);
        historyButton.setDimensions(historyW, FOOTER_BUTTON_HEIGHT);
        x += historyW + FOOTER_BUTTON_GAP;

        chatToggleButton.setPosition(x, buttonY);
        chatToggleButton.setDimensions(chatW, FOOTER_BUTTON_HEIGHT);
        chatToggleButton.setMessage(chatLabel);
        x += chatW + FOOTER_BUTTON_GAP;

        doneButton.setPosition(x, buttonY);
        doneButton.setDimensions(doneW, FOOTER_BUTTON_HEIGHT);
    }


    private Text getRoleFilterButtonText() {
        return switch (ModConfig.getCHAT_ROLE_FILTER()) {
            case SUBSCRIBERS -> Text.translatable("takeyourstreamchat.config.role_filter.subscribers");
            case VIP -> Text.translatable("takeyourstreamchat.config.role_filter.vip");
            case MODS -> Text.translatable("takeyourstreamchat.config.role_filter.mods");
            case SUB_OR_VIP -> Text.translatable("takeyourstreamchat.config.role_filter.sub_or_vip");
            case SUB_OR_MOD -> Text.translatable("takeyourstreamchat.config.role_filter.sub_or_mod");
            default -> Text.translatable("takeyourstreamchat.config.role_filter.all");
        };
    }

    private Text getSpawnModeButtonText() {
        var mode = ModConfig.getMESSAGE_SPAWN_MODE();
        switch (mode) {
            case AROUND_PLAYER:
                return Text.translatable("takeyourstreamchat.config.around_player");
            case FRONT_OF_PLAYER:
                return Text.translatable("takeyourstreamchat.config.fop_only");
            case HUD_WIDGET:
                return Text.translatable("takeyourstreamchat.config.hud_widget");
            default:
                return Text.translatable("takeyourstreamchat.config.around_player");
        }
    }

    private Text getUnpinModeButtonText() {
        return ModConfig.getUNPIN_MODE() == UnpinMode.WHOLE_MESSAGE
            ? Text.translatable("takeyourstreamchat.config.unpin_mode.whole_message")
            : Text.translatable("takeyourstreamchat.config.unpin_mode.pin_icon");
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
            } else if (entry.widget instanceof PlatformChannelRow row) {
                if (row.field.visible) {
                    row.field.visible = false;
                    temporarilyHidden.add(row.field);
                }
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
            ? Text.translatable("takeyourstreamchat.config.hint_default")
            : Text.translatable(hoveredDescriptionKey);

        int descTop = getDescriptionTop();
        context.drawTextWithShadow(
            this.textRenderer,
            description,
            CONTENT_PADDING + 10,
            descTop + (DESCRIPTION_HEIGHT - this.textRenderer.fontHeight) / 2,
            ModUiTheme.TEXT_SECONDARY
        );

        for (int i = 0; i < categoryButtons.size(); i++) {
            ButtonWidget button = categoryButtons.get(i);
            ConfigCategory category = ConfigCategory.values()[i];
            drawIconButton(
                context,
                button,
                category.getIcon(),
                category.getText(),
                isButtonHovered(button, mouseX, mouseY),
                category == currentCategory
            );
        }
        if (historyButton != null) {
            drawIconButton(
                context,
                historyButton,
                ICON_HISTORY,
                Text.translatable("takeyourstreamchat.config.message_history"),
                isButtonHovered(historyButton, mouseX, mouseY),
                false
            );
            if (chatToggleButton != null) {
                boolean chatConnected = ChatConnectionManager.getInstance(ConfigManager.getInstance()).isConnected();
                ModUiTheme.drawConnectionToggleButton(
                    context,
                    this.textRenderer,
                    chatToggleButton,
                    chatConnected,
                    isButtonHovered(chatToggleButton, mouseX, mouseY)
                );
            }
            if (doneButton != null) {
                ScreenUiHelper.renderButtons(
                    context,
                    mouseX,
                    mouseY,
                    java.util.List.of(doneButton),
                    null
                );
            }
        }
    }

    private static boolean isButtonHovered(ButtonWidget button, int mouseX, int mouseY) {
        return button.active && ModUiTheme.isHovered(
            mouseX,
            mouseY,
            button.getX(),
            button.getY(),
            button.getWidth(),
            button.getHeight()
        );
    }

    /** Кнопка в стиле ModUiTheme с пиксельной иконкой слева от подписи. */
    private void drawIconButton(
        DrawContext context,
        ButtonWidget button,
        Identifier icon,
        Text label,
        boolean hovered,
        boolean selected
    ) {
        ModUiTheme.drawButton(
            context,
            this.textRenderer,
            button.getX(),
            button.getY(),
            button.getWidth(),
            button.getHeight(),
            Text.empty(),
            hovered,
            button.active,
            selected,
            true
        );
        int textWidth = this.textRenderer.getWidth(label);
        int totalWidth = BUTTON_ICON_SIZE + BUTTON_ICON_GAP + textWidth;
        int startX = button.getX() + Math.max(4, (button.getWidth() - totalWidth) / 2);
        int iconY = button.getY() + (button.getHeight() - BUTTON_ICON_SIZE) / 2;
        takeyourminestream.ijustseen.ui.gui.MessageEmoteGuiRenderer.drawGuiIcon(
            context, icon, startX, iconY, BUTTON_ICON_SIZE
        );
        int textColor = button.active
            ? (hovered || selected ? ModUiTheme.TEXT_PRIMARY : ModUiTheme.TEXT_SECONDARY)
            : ModUiTheme.TEXT_HINT;
        context.drawTextWithShadow(
            this.textRenderer,
            label,
            startX + BUTTON_ICON_SIZE + BUTTON_ICON_GAP,
            ModUiTheme.centeredTextY(button.getY(), button.getHeight(), this.textRenderer.fontHeight),
            textColor
        );
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
            } else if (entry.widget instanceof PlatformChannelRow row) {
                if (row.field.visible && isElementVisible(row.field.getY(), contentTop, contentBottom)) {
                    ModUiTheme.drawInputFrame(
                        context,
                        row.field.getX(),
                        row.field.getY(),
                        row.field.getWidth(),
                        row.field.getHeight(),
                        row.field.isFocused()
                    );
                    row.field.render(context, mouseX, mouseY, delta);
                }
                if (row.toggle.visible && isElementVisible(row.toggle.getY(), contentTop, contentBottom)) {
                    boolean hovered = ModUiTheme.isHovered(
                        mouseX,
                        mouseY,
                        row.toggle.getX(),
                        row.toggle.getY(),
                        row.toggle.getWidth(),
                        row.toggle.getHeight()
                    );
                    ModUiTheme.drawButton(
                        context,
                        this.textRenderer,
                        row.toggle.getX(),
                        row.toggle.getY(),
                        row.toggle.getWidth(),
                        row.toggle.getHeight(),
                        row.toggle.getMessage(),
                        hovered,
                        row.toggle.active,
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

                int textX = labelX;
                if (entry.widget instanceof PlatformChannelRow row && row.platformIconKey != null) {
                    // Цветная полоска и пиксельная иконка платформы
                    int accent = 0xFF000000
                        | takeyourminestream.ijustseen.integration.chat.ChatPlatform.accentColorForIconKey(row.platformIconKey);
                    context.fill(rowLeft, currentY, rowLeft + 2, currentY + ENTRY_HEIGHT, accent);
                    textX = takeyourminestream.ijustseen.ui.gui.MessageEmoteGuiRenderer.drawPlatformIcon(
                        context,
                        row.platformIconKey,
                        labelX,
                        currentY + (20 - takeyourminestream.ijustseen.ui.gui.MessageEmoteGuiRenderer.PLATFORM_ICON_SIZE) / 2
                    );
                }

                context.drawTextWithShadow(this.textRenderer, Text.translatable(entry.labelKey),
                    textX, currentY + (20 - fontHeight) / 2, labelColor);

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
        if (widget instanceof PlatformChannelRow row) {
            row.field.visible = visible;
            row.toggle.visible = visible;
        } else if (widget instanceof ButtonWidget) {
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
        syncPlatformFieldsToConfig();
        applyConnectionSettingsFromConfig(false);
        ConfigManager.getInstance().saveConfig();
        if (this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            this.client.setScreen(null);
        }
    }

    /** Сохраняет ники/каналы из полей платформ перед закрытием экрана. */
    private void syncPlatformFieldsToConfig() {
        for (ConfigEntry entry : configEntries) {
            if (entry.type != ConfigEntryType.PLATFORM_ROW || !(entry.widget instanceof PlatformChannelRow row)) {
                continue;
            }
            String configKey = switch (row.platformIconKey) {
                case "twitch" -> "twitchChannelName";
                case "youtube" -> "youtubeChannel";
                case "kick" -> "kickChannel";
                case "tiktok" -> "tiktokUsername";
                default -> null;
            };
            if (configKey != null) {
                ConfigManager.getInstance().setConfigValue(configKey, row.field.getText());
            }
        }
    }

    /** Отключает выключенные платформы; переподключает изменённые каналы. */
    private static void applyConnectionSettingsFromConfig(boolean forceReconnect) {
        ChatConnectionManager.getInstance(ConfigManager.getInstance()).reconnectChangedPlatforms(forceReconnect);
    }
} 