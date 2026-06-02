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
import takeyourminestream.ijustseen.ui.widget.MessageScaleSliderWidget;
import takeyourminestream.ijustseen.ui.widget.MessageSoundVolumeSliderWidget;
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
    
    // Параметры интерфейса
    private static final int HEADER_HEIGHT = 46;
    private static final int FOOTER_HEIGHT = 36;
    private static final int CATEGORY_BUTTON_HEIGHT = 22;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRY_SPACING = 6;
    private static final int CONTENT_PADDING = 10;
    private static final int CATEGORY_TOP_MARGIN = 12;
    private static final int CATEGORY_TO_CONTENT_GAP = 6;
    private static final int CONTENT_TO_FOOTER_GAP = 2;
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
        int y = CATEGORY_TOP_MARGIN;
        
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
            ConfigCategory category = ConfigCategory.values()[i];
            button.active = category != currentCategory;
        }
    }
    
    private void createConfigEntries() {
        configEntries.clear();
        TextRenderer textRenderer = this.textRenderer;
        
        // Общие настройки
        TextFieldWidget channelNameField = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, Text.translatable("takeyourminestream.config.channel_name"));
        channelNameField.setText(ModConfig.getTWITCH_CHANNEL_NAME());
        channelNameField.setChangedListener(s -> ConfigManager.getInstance().setConfigValue("twitchChannelName", s));
        this.addDrawableChild(channelNameField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.channel_name", "takeyourminestream.config.channel_name.desc", ConfigEntryType.TEXT_FIELD, channelNameField, ConfigCategory.GENERAL));
        
        ButtonWidget automoderationButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_AUTOMODERATION() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_AUTOMODERATION(!ModConfig.isENABLE_AUTOMODERATION());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_AUTOMODERATION() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(automoderationButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.automoderation", "takeyourminestream.config.automoderation.desc", ConfigEntryType.TOGGLE, automoderationButton, ConfigCategory.GENERAL));

        ButtonWidget messageSoundButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_MESSAGE_SOUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_MESSAGE_SOUND(!ModConfig.isENABLE_MESSAGE_SOUND());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_MESSAGE_SOUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(messageSoundButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_sound", "takeyourminestream.config.message_sound.desc", ConfigEntryType.TOGGLE, messageSoundButton, ConfigCategory.GENERAL));

        MessageSoundVolumeSliderWidget messageSoundVolumeSlider = new MessageSoundVolumeSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(messageSoundVolumeSlider);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_sound_volume", "takeyourminestream.config.message_sound_volume.desc", ConfigEntryType.SLIDER, messageSoundVolumeSlider, ConfigCategory.GENERAL));

        ButtonWidget autoConnectIrcButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isAUTO_CONNECT_IRC_ON_JOIN() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setAUTO_CONNECT_IRC_ON_JOIN(!ModConfig.isAUTO_CONNECT_IRC_ON_JOIN());
                btn.setMessage(Text.translatable(ModConfig.isAUTO_CONNECT_IRC_ON_JOIN() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(autoConnectIrcButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.auto_connect_irc", "takeyourminestream.config.auto_connect_irc.desc", ConfigEntryType.TOGGLE, autoConnectIrcButton, ConfigCategory.GENERAL));

        // Кнопка настроек банвордов под флагом автомодерации
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

        TextFieldWidget chanceForSpawnField = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, Text.translatable("takeyourminestream.config.chance_for_spawn"));
        chanceForSpawnField.setText(ConfigManager.getInstance().getConfigValue("chanceForSpawn").toString());
        chanceForSpawnField.setChangedListener(s -> {
            if (s.matches("\\d+")) {
                try {
                    int value = Integer.parseInt(s);
                    if (0 <= value && value <= 100) {
                        ConfigManager.getInstance().setConfigValue("chanceForSpawn", value);
                    }
                } catch (Exception ignored) {}
            }
        });
        this.addDrawableChild(chanceForSpawnField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.chance_for_spawn", "takeyourminestream.config.chance_for_spawn.desc", ConfigEntryType.TEXT_FIELD, chanceForSpawnField, ConfigCategory.GENERAL));
        
        // Настройки сообщений
        TextFieldWidget messageLifetimeField = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, Text.translatable("takeyourminestream.config.message_lifetime_seconds"));
        Object lifeSec = ConfigManager.getInstance().getConfigValue("messageLifetimeSeconds");
        double lifeSeconds = lifeSec instanceof Number ? ((Number) lifeSec).doubleValue() : (ModConfig.getMESSAGE_LIFETIME_TICKS() / 20.0);
        messageLifetimeField.setText(String.format(java.util.Locale.ROOT, "%.2f", lifeSeconds));
        messageLifetimeField.setChangedListener(s -> {
            try { ConfigManager.getInstance().setConfigValue("messageLifetimeSeconds", Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(messageLifetimeField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_lifetime_seconds", "takeyourminestream.config.message_lifetime_seconds.desc", ConfigEntryType.TEXT_FIELD, messageLifetimeField, ConfigCategory.MESSAGES));
        
        TextFieldWidget messageFallField = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, Text.translatable("takeyourminestream.config.message_fall_seconds"));
        Object fallSec = ConfigManager.getInstance().getConfigValue("messageFallSeconds");
        double fallSeconds = fallSec instanceof Number ? ((Number) fallSec).doubleValue() : (ModConfig.getMESSAGE_FALL_TICKS() / 20.0);
        messageFallField.setText(String.format(java.util.Locale.ROOT, "%.2f", fallSeconds));
        messageFallField.setChangedListener(s -> {
            try { ConfigManager.getInstance().setConfigValue("messageFallSeconds", Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(messageFallField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_fall_seconds", "takeyourminestream.config.message_fall_seconds.desc", ConfigEntryType.TEXT_FIELD, messageFallField, ConfigCategory.MESSAGES));
        
        MessageScaleSliderWidget messageScaleSlider = new MessageScaleSliderWidget(0, 0, CONTROL_WIDTH, 20);
        this.addDrawableChild(messageScaleSlider);
        configEntries.add(new ConfigEntry("takeyourminestream.config.message_scale", "takeyourminestream.config.message_scale.desc", ConfigEntryType.SLIDER, messageScaleSlider, ConfigCategory.MESSAGES));
        
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
        
        // Новый флаг: отображение фона сообщений
        ButtonWidget showBgButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isSHOW_MESSAGE_BACKGROUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setSHOW_MESSAGE_BACKGROUND(!ModConfig.isSHOW_MESSAGE_BACKGROUND());
                btn.setMessage(Text.translatable(ModConfig.isSHOW_MESSAGE_BACKGROUND() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(showBgButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.show_message_bg", "takeyourminestream.config.show_message_bg.desc", ConfigEntryType.TOGGLE, showBgButton, ConfigCategory.MESSAGES));
        
        // Настройки поведения
        ButtonWidget freezingButton = ButtonWidget.builder(
            Text.translatable(ModConfig.isENABLE_FREEZING_ON_VIEW() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"),
            btn -> {
                ModConfig.setENABLE_FREEZING_ON_VIEW(!ModConfig.isENABLE_FREEZING_ON_VIEW());
                btn.setMessage(Text.translatable(ModConfig.isENABLE_FREEZING_ON_VIEW() ? "takeyourminestream.config.on" : "takeyourminestream.config.off"));
            }
        ).dimensions(0, 0, CONTROL_WIDTH, 20).build();
        this.addDrawableChild(freezingButton);
        configEntries.add(new ConfigEntry("takeyourminestream.config.freezing_on_view", "takeyourminestream.config.freezing_on_view.desc", ConfigEntryType.TOGGLE, freezingButton, ConfigCategory.BEHAVIOR));
        
        // Новый флаг: Следовать за игроком (для 3D режимов)
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

        TextFieldWidget maxFreezeDistanceField = new TextFieldWidget(textRenderer, 0, 0, CONTROL_WIDTH, 20, Text.translatable("takeyourminestream.config.max_freeze_distance"));
        maxFreezeDistanceField.setText(String.valueOf(ModConfig.getMAX_FREEZE_DISTANCE()));
        maxFreezeDistanceField.setChangedListener(s -> {
            try { ConfigManager.getInstance().setConfigValue("maxFreezeDistance", Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(maxFreezeDistanceField);
        configEntries.add(new ConfigEntry("takeyourminestream.config.max_freeze_distance", "takeyourminestream.config.max_freeze_distance.desc", ConfigEntryType.TEXT_FIELD, maxFreezeDistanceField, ConfigCategory.BEHAVIOR));
    }
    
    private void updateCategoryVisibility() {
        clampScrollOffset();
        for (ConfigEntry entry : configEntries) {
            setWidgetVisible(entry.widget, entry.category == currentCategory);
        }
        updateEntryPositions();
    }
    
    private void updateEntryPositions() {
        int contentTop = getContentTop();
        int contentBottom = getContentBottom();
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
        int centerX = this.width / 2;
        int buttonY = this.height - FOOTER_HEIGHT + 8;
        int buttonWidth = 100;
        int buttonSpacing = 10;
        
        // Кнопка "История сообщений"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("takeyourminestream.config.message_history"), btn -> {
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            if (messageSpawner != null) {
                var lifecycleManager = messageSpawner.getLifecycleManager();
                this.client.setScreen(new MessageHistoryScreen(this, lifecycleManager));
            }
        }).dimensions(centerX - buttonWidth * 3 / 2 - buttonSpacing, buttonY, buttonWidth, 20).build());

        // Кнопка переключения Twitch
        this.addDrawableChild(ButtonWidget.builder(getTwitchToggleButtonText(), btn -> {
            handleTwitchToggle();
            btn.setMessage(getTwitchToggleButtonText());
        }).dimensions(centerX - buttonWidth / 2, buttonY, buttonWidth, 20).build());

        // Кнопка "Готово"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), btn -> {
            if (!ModConfig.getTWITCH_CHANNEL_NAME().equals(initialChannelName)) {
                TwitchManager.getInstance(ConfigManager.getInstance()).onChannelNameChanged(ModConfig.getTWITCH_CHANNEL_NAME());
            }
            ConfigManager.getInstance().saveConfig();
            this.close();
        }).dimensions(centerX + buttonWidth / 2 + buttonSpacing, buttonY, buttonWidth, 20).build());
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

        List<Object> temporarilyHidden = new ArrayList<>();
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) continue;
            if (entry.widget instanceof ButtonWidget) {
                ButtonWidget w = (ButtonWidget) entry.widget;
                if (w.visible) {
                    w.visible = false;
                    temporarilyHidden.add(w);
                }
            } else if (entry.widget instanceof TextFieldWidget) {
                TextFieldWidget w = (TextFieldWidget) entry.widget;
                if (w.visible) {
                    w.visible = false;
                    temporarilyHidden.add(w);
                }
            } else if (entry.widget instanceof MessageScaleSliderWidget) {
                MessageScaleSliderWidget w = (MessageScaleSliderWidget) entry.widget;
                if (w.visible) {
                    w.visible = false;
                    temporarilyHidden.add(w);
                }
            } else if (entry.widget instanceof MessageSoundVolumeSliderWidget) {
                MessageSoundVolumeSliderWidget w = (MessageSoundVolumeSliderWidget) entry.widget;
                if (w.visible) {
                    w.visible = false;
                    temporarilyHidden.add(w);
                }
            }
        }
        
        super.render(context, mouseX, mouseY, delta);

        for (Object widget : temporarilyHidden) {
            setWidgetVisible(widget, true);
        }
        
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        
        int contentTop = getContentTop();
        int contentBottom = getContentBottom();
        
        context.fill(CONTENT_PADDING, contentTop, this.width - CONTENT_PADDING, contentBottom, 0x4A000000);
        
        context.enableScissor(CONTENT_PADDING, contentTop, this.width - CONTENT_PADDING, contentBottom);
        renderLabels(context, mouseX, mouseY, contentTop, contentBottom);
        renderConfigWidgets(context, mouseX, mouseY, delta, contentTop, contentBottom);
        context.disableScissor();
        renderScrollbar(context, contentTop, contentBottom);

        int descTop = contentBottom + 4;
        int descBottom = Math.min(this.height - FOOTER_HEIGHT + 2, descTop + DESCRIPTION_HEIGHT);
        context.fill(CONTENT_PADDING, descTop, this.width - CONTENT_PADDING, descBottom, 0x35000000);

        Text description = hoveredDescriptionKey == null
            ? Text.translatable("takeyourminestream.config.title")
            : Text.translatable(hoveredDescriptionKey);

        context.drawText(
            this.textRenderer,
            description,
            CONTENT_PADDING + 8,
            descTop + DESCRIPTION_HEIGHT - this.textRenderer.fontHeight - 2,
            0xFFDDDDDD,
            false
        );
    }

    private void renderConfigWidgets(DrawContext context, int mouseX, int mouseY, float delta, int contentTop, int contentBottom) {
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) continue;

            if (entry.widget instanceof ButtonWidget) {
                ButtonWidget widget = (ButtonWidget) entry.widget;
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            } else if (entry.widget instanceof TextFieldWidget) {
                TextFieldWidget widget = (TextFieldWidget) entry.widget;
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            } else if (entry.widget instanceof MessageScaleSliderWidget) {
                MessageScaleSliderWidget widget = (MessageScaleSliderWidget) entry.widget;
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            } else if (entry.widget instanceof MessageSoundVolumeSliderWidget) {
                MessageSoundVolumeSliderWidget widget = (MessageSoundVolumeSliderWidget) entry.widget;
                if (widget.visible && isElementVisible(widget.getY(), contentTop, contentBottom)) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }
    }
    
    private void renderLabels(DrawContext context, int mouseX, int mouseY, int contentTop, int contentBottom) {
        int labelColor = 0xFFFFFFFF;
        int fontHeight = this.textRenderer.fontHeight;
        
        int labelX = CONTENT_PADDING + 10;
        int rowLeft = CONTENT_PADDING + 4;
        int rowRight = this.width - CONTENT_PADDING - 4;
        
        int baseY = contentTop + CONTENT_PADDING - scrollOffset;
        int currentY = baseY;
        
        for (ConfigEntry entry : configEntries) {
            if (entry.category != currentCategory) continue;
            
            if (isElementVisible(currentY, contentTop, contentBottom)) {
                boolean hovered = mouseX >= rowLeft && mouseX <= rowRight && mouseY >= currentY && mouseY <= currentY + ENTRY_HEIGHT;
                context.fill(rowLeft, currentY, rowRight, currentY + ENTRY_HEIGHT, hovered ? 0x30FFFFFF : 0x20000000);
                context.drawText(this.textRenderer, Text.translatable(entry.labelKey), 
                    labelX, currentY + (20 - fontHeight) / 2, labelColor, true);

                if (hovered) {
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
        
        if (totalContentHeight <= visibleContentHeight) return;
        
        int padding = 10;
        int scrollbarX = this.width - padding - 6;
        int scrollbarWidth = 4;
        int scrollbarHeight = contentBottom - contentTop;
        
        // Фон скроллбара
        context.fill(scrollbarX, contentTop, scrollbarX + scrollbarWidth, contentBottom, 0x40FFFFFF);
        
        // Ползунок скроллбара
        int thumbHeight = Math.max(10, (visibleContentHeight * scrollbarHeight) / totalContentHeight);
        int maxScroll = totalContentHeight - visibleContentHeight;
        int thumbY = contentTop + (scrollOffset * (scrollbarHeight - thumbHeight)) / maxScroll;
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentTop = getContentTop();
        int contentBottom = getContentBottom();
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
        return CATEGORY_TOP_MARGIN + CATEGORY_BUTTON_HEIGHT;
    }

    private int getContentTop() {
        return getCategoryButtonsBottom() + CATEGORY_TO_CONTENT_GAP;
    }

    private int getContentBottom() {
        return this.height - FOOTER_HEIGHT - CONTENT_TO_FOOTER_GAP - DESCRIPTION_HEIGHT - 2;
    }

    private int getVisibleContentHeight() {
        return getContentBottom() - getContentTop();
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