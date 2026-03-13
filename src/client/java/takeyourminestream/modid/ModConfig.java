package takeyourminestream.modid;

import takeyourminestream.modid.config.ModConfigData;

/**
 * Утилитарный класс для доступа к конфигурации
 * @deprecated Используйте ConfigManager.getInstance().getConfigData() вместо этого класса
 */
@Deprecated
public class ModConfig {
    /**
     * Получает текущую конфигурацию
     * @return объект конфигурации
     */
    public static ModConfigData getCurrentConfig() {
        return ConfigManager.getInstance().getConfigData();
    }

    // Обратная совместимость - статические поля теперь возвращают значения из ConfigManager
    public static String getTWITCH_CHANNEL_NAME() {
        return (String) ConfigManager.getInstance().getConfigValue("twitchChannelName");
    }

    public static int getMESSAGE_LIFETIME_TICKS() {
        // Приоритет секунд; fallback на тики
        Object sec = ConfigManager.getInstance().getConfigValue("messageLifetimeSeconds");
        if (sec instanceof Number) {
            return (int) Math.round(((Number) sec).doubleValue() * 20.0);
        }
        return (Integer) ConfigManager.getInstance().getConfigValue("messageLifetimeTicks");
    }

    public static int getMESSAGE_FALL_TICKS() {
        Object sec = ConfigManager.getInstance().getConfigValue("messageFallSeconds");
        if (sec instanceof Number) {
            return (int) Math.round(((Number) sec).doubleValue() * 20.0);
        }
        return (Integer) ConfigManager.getInstance().getConfigValue("messageFallTicks");
    }

    public static String[] getNICK_COLORS() {
        return (String[]) ConfigManager.getInstance().getConfigValue("nickColors");
    }

    public static boolean isENABLE_FREEZING_ON_VIEW() {
        return (Boolean) ConfigManager.getInstance().getConfigValue("enableFreezingOnView");
    }

    public static double getMAX_FREEZE_DISTANCE() {
        return (Double) ConfigManager.getInstance().getConfigValue("maxFreezeDistance");
    }

    public static boolean isMESSAGES_IN_FRONT_OF_PLAYER_ONLY() {
        return (Boolean) ConfigManager.getInstance().getConfigValue("messagesInFrontOfPlayerOnly");
    }

    public static int getPARTICLE_MIN_COUNT() {
        return (Integer) ConfigManager.getInstance().getConfigValue("particleMinCount");
    }

    public static int getPARTICLE_MAX_COUNT() {
        return (Integer) ConfigManager.getInstance().getConfigValue("particleMaxCount");
    }

    public static int getPARTICLE_LIFETIME_TICKS() {
        return (Integer) ConfigManager.getInstance().getConfigValue("particleLifetimeTicks");
    }

    public static boolean isENABLE_AUTOMODERATION() {
        return (Boolean) ConfigManager.getInstance().getConfigValue("enableAutomoderation");
    }

    public static void setENABLE_FREEZING_ON_VIEW(boolean value) {
        ConfigManager.getInstance().setConfigValue("enableFreezingOnView", value);
    }

    public static void setMESSAGES_IN_FRONT_OF_PLAYER_ONLY(boolean value) {
        ConfigManager.getInstance().setConfigValue("messagesInFrontOfPlayerOnly", value);
    }

    public static void setENABLE_AUTOMODERATION(boolean value) {
        ConfigManager.getInstance().setConfigValue("enableAutomoderation", value);
    }
    
    public static takeyourminestream.modid.config.MessageSpawnMode getMESSAGE_SPAWN_MODE() {
        return (takeyourminestream.modid.config.MessageSpawnMode) ConfigManager.getInstance().getConfigValue("messageSpawnMode");
    }
    
    public static void setMESSAGE_SPAWN_MODE(takeyourminestream.modid.config.MessageSpawnMode value) {
        ConfigManager.getInstance().setConfigValue("messageSpawnMode", value);
    }
    
    public static takeyourminestream.modid.config.MessageScale getMESSAGE_SCALE() {
        return (takeyourminestream.modid.config.MessageScale) ConfigManager.getInstance().getConfigValue("messageScale");
    }
    
    public static void setMESSAGE_SCALE(takeyourminestream.modid.config.MessageScale value) {
        ConfigManager.getInstance().setConfigValue("messageScale", value);
    }

    public static boolean isSHOW_MESSAGE_BACKGROUND() {
        return (Boolean) ConfigManager.getInstance().getConfigValue("showMessageBackground");
    }

    public static void setSHOW_MESSAGE_BACKGROUND(boolean value) {
        ConfigManager.getInstance().setConfigValue("showMessageBackground", value);
    }

    public static boolean isFOLLOW_PLAYER() {
        return (Boolean) ConfigManager.getInstance().getConfigValue("followPlayer");
    }

    public static void setFOLLOW_PLAYER(boolean value) {
        ConfigManager.getInstance().setConfigValue("followPlayer", value);
    }

    public static boolean isENABLE_CLICK_TO_REMOVE() {
        Object value = ConfigManager.getInstance().getConfigValue("enableClickToRemove");
        return value != null ? (Boolean) value : true; // По умолчанию true
    }

    public static void setENABLE_CLICK_TO_REMOVE(boolean value) {
        ConfigManager.getInstance().setConfigValue("enableClickToRemove", value);
    }

    public static boolean isENABLE_MESSAGE_SOUND() {
        Object value = ConfigManager.getInstance().getConfigValue("enableMessageSound");
        return value != null ? (Boolean) value : true;
    }

    public static void setENABLE_MESSAGE_SOUND(boolean value) {
        ConfigManager.getInstance().setConfigValue("enableMessageSound", value);
    }

    public static double getMESSAGE_SOUND_VOLUME() {
        Object value = ConfigManager.getInstance().getConfigValue("messageSoundVolume");
        return value instanceof Number ? ((Number) value).doubleValue() : 0.45;
    }

    public static void setMESSAGE_SOUND_VOLUME(double value) {
        ConfigManager.getInstance().setConfigValue("messageSoundVolume", value);
    }

    public static boolean isAUTO_CONNECT_IRC_ON_JOIN() {
        Object value = ConfigManager.getInstance().getConfigValue("autoConnectIrcOnJoin");
        return value != null && (Boolean) value;
    }

    public static void setAUTO_CONNECT_IRC_ON_JOIN(boolean value) {
        ConfigManager.getInstance().setConfigValue("autoConnectIrcOnJoin", value);
    }
} 