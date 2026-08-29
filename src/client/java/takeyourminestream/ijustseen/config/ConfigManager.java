package takeyourminestream.ijustseen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.core.storage.StoragePaths;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import takeyourminestream.ijustseen.utils.PlayerMessageCompat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManager implements IConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE;

    static {
        StoragePaths.migrateLegacyModRootIfNeeded();
        Path modRoot = StoragePaths.getModRootDir();
        Path newConfigPath = modRoot.resolve("take-your-stream-chat.json");
        Path legacyConfigInNewFolder = modRoot.resolve("take-your-minestream.json");
        Path legacyConfigPath = FabricLoader.getInstance().getConfigDir().resolve("take-your-minestream.json");
        Path legacyConfigInFolder = StoragePaths.getLegacyModRootDir().resolve("take-your-minestream.json");
        Path legacyGameFolderConfig = StoragePaths.getLegacyGameModRootDir().resolve("take-your-minestream.json");

        try {
            StoragePaths.ensureModRootDir();
        } catch (IOException ignored) {
        }
        StoragePaths.migrateFileIfNeeded(legacyConfigPath, newConfigPath);
        StoragePaths.migrateFileIfNeeded(legacyConfigInFolder, newConfigPath);
        StoragePaths.migrateFileIfNeeded(legacyGameFolderConfig, newConfigPath);
        StoragePaths.migrateFileIfNeeded(legacyConfigInNewFolder, newConfigPath);
        CONFIG_FILE = newConfigPath.toFile();
    }
    
    private static ConfigManager instance;
    private ModConfigData configData;
    private final Map<String, Object> configCache = new HashMap<>();

    private ConfigManager() {
        this.configData = new ModConfigData();
        loadConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    @Override
    public void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ModConfigData loadedData = GSON.fromJson(reader, ModConfigData.class);
                if (loadedData != null) {
                    this.configData = loadedData;
                    this.configData.migrateLegacyRoleFilter();
                    updateConfigCache();
                    LOGGER.info("Configuration loaded");
                }
            } catch (IOException e) {
                LOGGER.severe("Failed to load configuration: " + e.getMessage());
                sendPlayerMessage("§cFailed to load configuration");
            }
        } else {
            LOGGER.info("Configuration file not found, using defaults");
            saveConfig(); // Создаем файл с значениями по умолчанию
        }
    }

    /** Сбрасывает JSON-конфиг к значениям {@link ModConfigData} по умолчанию. Списки банвордов/ников/regexp не трогает. */
    public void resetToDefaults() {
        this.configData = new ModConfigData();
        this.configData.migrateLegacyRoleFilter();
        saveConfig();
        LOGGER.info("Configuration reset to defaults");
    }

    @Override
    public void saveConfig() {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(configData, writer);
            updateConfigCache();
            LOGGER.info("Configuration saved");
        } catch (IOException e) {
            LOGGER.severe("Failed to save configuration: " + e.getMessage());
            sendPlayerMessage("§cFailed to save configuration");
        }
    }

    @Override
    public Object getConfigValue(String key) {
        return configCache.get(key);
    }

    @Override
    public void setConfigValue(String key, Object value) {
        switch (key) {
            case "chanceForSpawn":
                configData.setChanceForSpawn((Integer) value);
                break;
            case "messageHistoryMaxSize":
                configData.setMessageHistoryMaxSize((Integer) value);
                trimMessageHistory();
                break;
            case "twitchChannelName":
                configData.setTwitchChannelName((String) value);
                break;
            case "twitchEnabled":
                configData.setTwitchEnabled((Boolean) value);
                break;
            case "youtubeEnabled":
                configData.setYoutubeEnabled((Boolean) value);
                break;
            case "youtubeChannel":
                configData.setYoutubeChannel((String) value);
                break;
            case "kickEnabled":
                configData.setKickEnabled((Boolean) value);
                break;
            case "kickChannel":
                configData.setKickChannel((String) value);
                break;
            case "tiktokEnabled":
                configData.setTiktokEnabled((Boolean) value);
                break;
            case "tiktokUsername":
                configData.setTiktokUsername((String) value);
                break;
            case "messageLifetimeTicks":
                configData.setMessageLifetimeTicks((Integer) value);
                break;
            case "messageFallTicks":
                configData.setMessageFallTicks((Integer) value);
                break;
            case "messageLifetimeSeconds":
                if (value instanceof Number) configData.setMessageLifetimeSeconds(((Number) value).doubleValue());
                break;
            case "messageFallSeconds":
                if (value instanceof Number) configData.setMessageFallSeconds(((Number) value).doubleValue());
                break;
            case "enableFreezingOnView":
                configData.setEnableFreezingOnView((Boolean) value);
                break;
            case "maxFreezeDistance":
                configData.setMaxFreezeDistance((Double) value);
                break;
            case "messageSpawnMinDistance":
                configData.setMessageSpawnMinDistance((Integer) value);
                break;
            case "messageSpawnMaxDistance":
                configData.setMessageSpawnMaxDistance((Integer) value);
                break;
            case "messagesInFrontOfPlayerOnly":
                configData.setMessagesInFrontOfPlayerOnly((Boolean) value);
                break;
            case "messageSpawnMode":
                configData.setMessageSpawnMode((takeyourminestream.ijustseen.config.MessageSpawnMode) value);
                break;
            case "enableAutomoderation":
                configData.setEnableAutomoderation((Boolean) value);
                break;
            case "messageScale":
                configData.setMessageScale((takeyourminestream.ijustseen.config.MessageScale) value);
                break;
            case "showMessageBackground":
                configData.setShowMessageBackground((Boolean) value);
                break;
            case "followPlayer":
                configData.setFollowPlayer((Boolean) value);
                break;
            case "enableClickToRemove":
                configData.setEnableClickToRemove((Boolean) value);
                break;
            case "enableMessageSound":
                configData.setEnableMessageSound((Boolean) value);
                break;
            case "messageSoundVolume":
                if (value instanceof Number) configData.setMessageSoundVolume(((Number) value).doubleValue());
                break;
            case "autoConnectIrcOnJoin":
                configData.setAutoConnectIrcOnJoin((Boolean) value);
                break;
            case "chatRoleFilter":
                configData.setChatRoleFilter((takeyourminestream.ijustseen.config.ChatRoleFilter) value);
                break;
            case "roleFilterSubscribers":
                configData.setRoleFilterSubscribers((Boolean) value);
                break;
            case "roleFilterVip":
                configData.setRoleFilterVip((Boolean) value);
                break;
            case "roleFilterMods":
                configData.setRoleFilterMods((Boolean) value);
                break;
            case "hudAnchor":
                configData.setHudAnchor((HudAnchor) value);
                break;
            case "hudOffsetX":
                configData.setHudOffsetX((Integer) value);
                break;
            case "hudOffsetY":
                configData.setHudOffsetY((Integer) value);
                break;
            case "enableUsernameBlocklist":
                configData.setEnableUsernameBlocklist((Boolean) value);
                break;
            case "unpinMode":
                configData.setUnpinMode((UnpinMode) value);
                break;
            case "enableColorEmojis":
                configData.setEnableColorEmojis((Boolean) value);
                break;
            case "panelBaseColorRgb":
                configData.setPanelBaseColorRgb(((Number) value).intValue());
                break;
            case "panelBorderFromPlatform":
                configData.setPanelBorderFromPlatform((Boolean) value);
                break;
            case "panelBorderColorRgb":
                configData.setPanelBorderColorRgb(((Number) value).intValue());
                break;
            case "showRoleBadges":
                configData.setShowRoleBadges((Boolean) value);
                break;
            case "twitchShowMessages":
                configData.setTwitchShowMessages((Boolean) value);
                break;
            case "twitchMessageSound":
                configData.setTwitchMessageSound((Boolean) value);
                break;
            case "youtubeShowMessages":
                configData.setYoutubeShowMessages((Boolean) value);
                break;
            case "youtubeMessageSound":
                configData.setYoutubeMessageSound((Boolean) value);
                break;
            case "kickShowMessages":
                configData.setKickShowMessages((Boolean) value);
                break;
            case "kickMessageSound":
                configData.setKickMessageSound((Boolean) value);
                break;
            case "tiktokShowMessages":
                configData.setTiktokShowMessages((Boolean) value);
                break;
            case "tiktokMessageSound":
                configData.setTiktokMessageSound((Boolean) value);
                break;
            case "tiktokGiftEvents":
                configData.setTiktokGiftEvents((Boolean) value);
                break;
            case "tiktokFollowEvents":
                configData.setTiktokFollowEvents((Boolean) value);
                break;
            default:
                LOGGER.warning("Unknown configuration key: " + key);
                return;
        }
        updateConfigCache();
        saveConfig();
    }

    private void updateConfigCache() {
        configCache.clear();
        configCache.put("chanceForSpawn", configData.getChanceForSpawn());
        configCache.put("messageHistoryMaxSize", configData.getMessageHistoryMaxSize());
        configCache.put("twitchChannelName", configData.getTwitchChannelName());
        configCache.put("twitchEnabled", configData.isTwitchEnabled());
        configCache.put("youtubeEnabled", configData.isYoutubeEnabled());
        configCache.put("youtubeChannel", configData.getYoutubeChannel());
        configCache.put("kickEnabled", configData.isKickEnabled());
        configCache.put("kickChannel", configData.getKickChannel());
        configCache.put("tiktokEnabled", configData.isTiktokEnabled());
        configCache.put("tiktokUsername", configData.getTiktokUsername());
        configCache.put("messageLifetimeTicks", configData.getMessageLifetimeTicks());
        configCache.put("messageFallTicks", configData.getMessageFallTicks());
        configCache.put("messageLifetimeSeconds", configData.getMessageLifetimeSeconds());
        configCache.put("messageFallSeconds", configData.getMessageFallSeconds());
        configCache.put("enableFreezingOnView", configData.isEnableFreezingOnView());
        configCache.put("maxFreezeDistance", configData.getMaxFreezeDistance());
        configCache.put("messageSpawnMinDistance", configData.getMessageSpawnMinDistance());
        configCache.put("messageSpawnMaxDistance", configData.getMessageSpawnMaxDistance());
        configCache.put("messagesInFrontOfPlayerOnly", configData.isMessagesInFrontOfPlayerOnly());
        configCache.put("messageSpawnMode", configData.getMessageSpawnMode());
        configCache.put("enableAutomoderation", configData.isEnableAutomoderation());
        configCache.put("particleMinCount", configData.getParticleMinCount());
        configCache.put("particleMaxCount", configData.getParticleMaxCount());
        configCache.put("particleLifetimeTicks", configData.getParticleLifetimeTicks());
        configCache.put("nickColors", configData.getNickColors());
        configCache.put("messageScale", configData.getMessageScale());
        configCache.put("showMessageBackground", configData.isShowMessageBackground());
        configCache.put("followPlayer", configData.isFollowPlayer());
        configCache.put("enableClickToRemove", configData.isEnableClickToRemove());
        configCache.put("enableMessageSound", configData.isEnableMessageSound());
        configCache.put("messageSoundVolume", configData.getMessageSoundVolume());
        configCache.put("autoConnectIrcOnJoin", configData.isAutoConnectIrcOnJoin());
        configCache.put("chatRoleFilter", configData.getChatRoleFilter());
        configCache.put("roleFilterSubscribers", configData.isRoleFilterSubscribers());
        configCache.put("roleFilterVip", configData.isRoleFilterVip());
        configCache.put("roleFilterMods", configData.isRoleFilterMods());
        configCache.put("hudAnchor", configData.getHudAnchor());
        configCache.put("hudOffsetX", configData.getHudOffsetX());
        configCache.put("hudOffsetY", configData.getHudOffsetY());
        configCache.put("enableUsernameBlocklist", configData.isEnableUsernameBlocklist());
        configCache.put("unpinMode", configData.getUnpinMode());
        configCache.put("enableColorEmojis", configData.isEnableColorEmojis());
        configCache.put("panelBaseColorRgb", configData.getPanelBaseColorRgb());
        configCache.put("panelBorderFromPlatform", configData.isPanelBorderFromPlatform());
        configCache.put("panelBorderColorRgb", configData.getPanelBorderColorRgb());
        configCache.put("showRoleBadges", configData.isShowRoleBadges());
        configCache.put("twitchShowMessages", configData.isTwitchShowMessages());
        configCache.put("twitchMessageSound", configData.isTwitchMessageSound());
        configCache.put("youtubeShowMessages", configData.isYoutubeShowMessages());
        configCache.put("youtubeMessageSound", configData.isYoutubeMessageSound());
        configCache.put("kickShowMessages", configData.isKickShowMessages());
        configCache.put("kickMessageSound", configData.isKickMessageSound());
        configCache.put("tiktokShowMessages", configData.isTiktokShowMessages());
        configCache.put("tiktokMessageSound", configData.isTiktokMessageSound());
        configCache.put("tiktokGiftEvents", configData.isTiktokGiftEvents());
        configCache.put("tiktokFollowEvents", configData.isTiktokFollowEvents());
    }

    public ModConfigData getConfigData() {
        return configData;
    }

    private void trimMessageHistory() {
        MessageSpawner spawner = TakeYourMineStreamClient.getStaticMessageSpawner();
        if (spawner != null) {
            spawner.getLifecycleManager().enforceHistoryLimit();
        }
    }

    private void sendPlayerMessage(String message) {
        PlayerMessageCompat.send(MinecraftClient.getInstance(), message);
    }
} 