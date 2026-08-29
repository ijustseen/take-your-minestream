package takeyourminestream.ijustseen.config;

/**
 * Статический фасад над {@link ConfigManager#getConfigData()}.
 */
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

    public static boolean isTWITCH_ENABLED() {
        Object value = ConfigManager.getInstance().getConfigValue("twitchEnabled");
        return value == null || (Boolean) value;
    }

    public static void setTWITCH_ENABLED(boolean value) {
        ConfigManager.getInstance().setConfigValue("twitchEnabled", value);
    }

    public static boolean isYOUTUBE_ENABLED() {
        Object value = ConfigManager.getInstance().getConfigValue("youtubeEnabled");
        return value != null && (Boolean) value;
    }

    public static void setYOUTUBE_ENABLED(boolean value) {
        ConfigManager.getInstance().setConfigValue("youtubeEnabled", value);
    }

    public static String getYOUTUBE_CHANNEL() {
        Object value = ConfigManager.getInstance().getConfigValue("youtubeChannel");
        return value instanceof String s ? s : "";
    }

    public static void setYOUTUBE_CHANNEL(String value) {
        ConfigManager.getInstance().setConfigValue("youtubeChannel", value);
    }

    public static boolean isKICK_ENABLED() {
        Object value = ConfigManager.getInstance().getConfigValue("kickEnabled");
        return value != null && (Boolean) value;
    }

    public static void setKICK_ENABLED(boolean value) {
        ConfigManager.getInstance().setConfigValue("kickEnabled", value);
    }

    public static String getKICK_CHANNEL() {
        Object value = ConfigManager.getInstance().getConfigValue("kickChannel");
        return value instanceof String s ? s : "";
    }

    public static void setKICK_CHANNEL(String value) {
        ConfigManager.getInstance().setConfigValue("kickChannel", value);
    }

    public static boolean isTIKTOK_ENABLED() {
        Object value = ConfigManager.getInstance().getConfigValue("tiktokEnabled");
        return value != null && (Boolean) value;
    }

    public static void setTIKTOK_ENABLED(boolean value) {
        ConfigManager.getInstance().setConfigValue("tiktokEnabled", value);
    }

    public static String getTIKTOK_USERNAME() {
        Object value = ConfigManager.getInstance().getConfigValue("tiktokUsername");
        return value instanceof String s ? s : "";
    }

    public static void setTIKTOK_USERNAME(String value) {
        ConfigManager.getInstance().setConfigValue("tiktokUsername", value);
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
        return takeyourminestream.ijustseen.core.MessagePanelConstants.MESSAGE_FALL_TICKS;
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

    public static int getMESSAGE_SPAWN_MIN_DISTANCE() {
        Object value = ConfigManager.getInstance().getConfigValue("messageSpawnMinDistance");
        return value instanceof Number number ? number.intValue() : 2;
    }

    public static int getMESSAGE_SPAWN_MAX_DISTANCE() {
        Object value = ConfigManager.getInstance().getConfigValue("messageSpawnMaxDistance");
        return value instanceof Number number ? number.intValue() : 5;
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
    
    public static takeyourminestream.ijustseen.config.MessageSpawnMode getMESSAGE_SPAWN_MODE() {
        return (takeyourminestream.ijustseen.config.MessageSpawnMode) ConfigManager.getInstance().getConfigValue("messageSpawnMode");
    }
    
    public static void setMESSAGE_SPAWN_MODE(takeyourminestream.ijustseen.config.MessageSpawnMode value) {
        ConfigManager.getInstance().setConfigValue("messageSpawnMode", value);
    }
    
    public static takeyourminestream.ijustseen.config.MessageScale getMESSAGE_SCALE() {
        return (takeyourminestream.ijustseen.config.MessageScale) ConfigManager.getInstance().getConfigValue("messageScale");
    }
    
    public static void setMESSAGE_SCALE(takeyourminestream.ijustseen.config.MessageScale value) {
        ConfigManager.getInstance().setConfigValue("messageScale", value);
    }

    public static boolean isSHOW_MESSAGE_BACKGROUND() {
        return (Boolean) ConfigManager.getInstance().getConfigValue("showMessageBackground");
    }

    public static void setSHOW_MESSAGE_BACKGROUND(boolean value) {
        ConfigManager.getInstance().setConfigValue("showMessageBackground", value);
    }

    public static boolean isENABLE_COLOR_EMOJIS() {
        Object value = ConfigManager.getInstance().getConfigValue("enableColorEmojis");
        return value == null || (Boolean) value;
    }

    public static void setENABLE_COLOR_EMOJIS(boolean value) {
        ConfigManager.getInstance().setConfigValue("enableColorEmojis", value);
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

    public static takeyourminestream.ijustseen.config.ChatRoleFilter getCHAT_ROLE_FILTER() {
        Object value = ConfigManager.getInstance().getConfigValue("chatRoleFilter");
        return value instanceof takeyourminestream.ijustseen.config.ChatRoleFilter filter ? filter : takeyourminestream.ijustseen.config.ChatRoleFilter.ALL;
    }

    public static void setCHAT_ROLE_FILTER(takeyourminestream.ijustseen.config.ChatRoleFilter value) {
        ConfigManager.getInstance().setConfigValue("chatRoleFilter", value);
    }

    public static boolean isROLE_FILTER_SUBSCRIBERS() {
        Object value = ConfigManager.getInstance().getConfigValue("roleFilterSubscribers");
        return value instanceof Boolean b && b;
    }

    public static void setROLE_FILTER_SUBSCRIBERS(boolean value) {
        ConfigManager.getInstance().setConfigValue("roleFilterSubscribers", value);
    }

    public static boolean isROLE_FILTER_VIP() {
        Object value = ConfigManager.getInstance().getConfigValue("roleFilterVip");
        return value instanceof Boolean b && b;
    }

    public static void setROLE_FILTER_VIP(boolean value) {
        ConfigManager.getInstance().setConfigValue("roleFilterVip", value);
    }

    public static boolean isROLE_FILTER_MODS() {
        Object value = ConfigManager.getInstance().getConfigValue("roleFilterMods");
        return value instanceof Boolean b && b;
    }

    public static void setROLE_FILTER_MODS(boolean value) {
        ConfigManager.getInstance().setConfigValue("roleFilterMods", value);
    }

    public static HudAnchor getHUD_ANCHOR() {
        Object value = ConfigManager.getInstance().getConfigValue("hudAnchor");
        return value instanceof HudAnchor anchor ? anchor : HudAnchor.TOP_RIGHT;
    }

    public static void setHUD_ANCHOR(HudAnchor value) {
        ConfigManager.getInstance().setConfigValue("hudAnchor", value);
    }

    public static int getHUD_OFFSET_X() {
        Object value = ConfigManager.getInstance().getConfigValue("hudOffsetX");
        return value instanceof Number number ? Math.max(0, Math.min(400, number.intValue())) : 0;
    }

    public static void setHUD_OFFSET_X(int value) {
        ConfigManager.getInstance().setConfigValue("hudOffsetX", value);
    }

    public static int getHUD_OFFSET_Y() {
        Object value = ConfigManager.getInstance().getConfigValue("hudOffsetY");
        return value instanceof Number number ? Math.max(0, Math.min(400, number.intValue())) : 0;
    }

    public static void setHUD_OFFSET_Y(int value) {
        ConfigManager.getInstance().setConfigValue("hudOffsetY", value);
    }

    public static boolean isENABLE_USERNAME_BLOCKLIST() {
        Object value = ConfigManager.getInstance().getConfigValue("enableUsernameBlocklist");
        return value == null || (Boolean) value;
    }

    public static void setENABLE_USERNAME_BLOCKLIST(boolean value) {
        ConfigManager.getInstance().setConfigValue("enableUsernameBlocklist", value);
    }

    public static int getPANEL_BASE_COLOR_RGB() {
        Object value = ConfigManager.getInstance().getConfigValue("panelBaseColorRgb");
        return value instanceof Number number ? number.intValue() & 0xFFFFFF : 0x000000;
    }

    public static void setPANEL_BASE_COLOR_RGB(int value) {
        ConfigManager.getInstance().setConfigValue("panelBaseColorRgb", value);
    }

    public static boolean isPANEL_BORDER_FROM_PLATFORM() {
        Object value = ConfigManager.getInstance().getConfigValue("panelBorderFromPlatform");
        return !(value instanceof Boolean b) || b;
    }

    public static void setPANEL_BORDER_FROM_PLATFORM(boolean value) {
        ConfigManager.getInstance().setConfigValue("panelBorderFromPlatform", value);
    }

    public static int getPANEL_BORDER_COLOR_RGB() {
        Object value = ConfigManager.getInstance().getConfigValue("panelBorderColorRgb");
        return value instanceof Number number
            ? number.intValue() & 0xFFFFFF
            : takeyourminestream.ijustseen.core.MessagePanelConstants.DEFAULT_BORDER_RGB;
    }

    public static void setPANEL_BORDER_COLOR_RGB(int value) {
        ConfigManager.getInstance().setConfigValue("panelBorderColorRgb", value);
    }

    public static boolean isSHOW_ROLE_BADGES() {
        Object value = ConfigManager.getInstance().getConfigValue("showRoleBadges");
        return !(value instanceof Boolean b) || b;
    }

    public static void setSHOW_ROLE_BADGES(boolean value) {
        ConfigManager.getInstance().setConfigValue("showRoleBadges", value);
    }

    /** Включена ли платформа ({@code platformKey} — ключ из {@code ChatPlatform.getIconKey()}). */
    public static boolean isPLATFORM_ENABLED(String platformKey) {
        Object value = ConfigManager.getInstance().getConfigValue(platformKey + "Enabled");
        if (value instanceof Boolean b) {
            return b;
        }
        // Twitch включён по умолчанию — исторически это единственная платформа мода
        return "twitch".equals(platformKey);
    }

    public static void setPLATFORM_ENABLED(String platformKey, boolean value) {
        ConfigManager.getInstance().setConfigValue(platformKey + "Enabled", value);
    }

    /** Фильтры ролей задаются отдельно для каждого источника. */
    public static boolean isPLATFORM_ROLE(String platformKey, takeyourminestream.ijustseen.integration.chat.PlatformRoleKind kind) {
        return getCurrentConfig().isPlatformRoleFilter(platformKey, kind.key());
    }

    public static void setPLATFORM_ROLE(
        String platformKey,
        takeyourminestream.ijustseen.integration.chat.PlatformRoleKind kind,
        boolean value
    ) {
        setPlatformRoleFilter(platformKey, kind.key(), value);
    }

    public static boolean isPLATFORM_ROLE_ALL(String platformKey) {
        return getCurrentConfig().isPlatformRoleFilter(platformKey, ModConfigData.ROLE_ALL);
    }

    public static void setPLATFORM_ROLE_ALL(String platformKey, boolean value) {
        setPlatformRoleFilter(platformKey, ModConfigData.ROLE_ALL, value);
    }

    public static boolean isPLATFORM_ROLE_FOLLOWERS(String platformKey) {
        return getCurrentConfig().isPlatformRoleFilter(platformKey, ModConfigData.ROLE_FOLLOWERS);
    }

    public static void setPLATFORM_ROLE_FOLLOWERS(String platformKey, boolean value) {
        setPlatformRoleFilter(platformKey, ModConfigData.ROLE_FOLLOWERS, value);
    }

    public static boolean isPLATFORM_ROLE_SUBSCRIBERS(String platformKey) {
        return getCurrentConfig().isPlatformRoleFilter(platformKey, ModConfigData.ROLE_SUBSCRIBERS);
    }

    public static void setPLATFORM_ROLE_SUBSCRIBERS(String platformKey, boolean value) {
        setPlatformRoleFilter(platformKey, ModConfigData.ROLE_SUBSCRIBERS, value);
    }

    public static boolean isPLATFORM_ROLE_VIP(String platformKey) {
        return getCurrentConfig().isPlatformRoleFilter(platformKey, ModConfigData.ROLE_VIP);
    }

    public static void setPLATFORM_ROLE_VIP(String platformKey, boolean value) {
        setPlatformRoleFilter(platformKey, ModConfigData.ROLE_VIP, value);
    }

    public static boolean isPLATFORM_ROLE_MODS(String platformKey) {
        return getCurrentConfig().isPlatformRoleFilter(platformKey, ModConfigData.ROLE_MODS);
    }

    public static void setPLATFORM_ROLE_MODS(String platformKey, boolean value) {
        setPlatformRoleFilter(platformKey, ModConfigData.ROLE_MODS, value);
    }

    private static void setPlatformRoleFilter(String platformKey, String role, boolean value) {
        getCurrentConfig().setPlatformRoleFilter(platformKey, role, value);
        ConfigManager.getInstance().saveConfig();
    }

    /** Показывать ли сообщения платформы ({@code platformKey} — ключ из {@code ChatPlatform.getIconKey()}). */
    public static boolean isPLATFORM_SHOW_MESSAGES(String platformKey) {
        Object value = ConfigManager.getInstance().getConfigValue(platformKey + "ShowMessages");
        return !(value instanceof Boolean b) || b;
    }

    public static void setPLATFORM_SHOW_MESSAGES(String platformKey, boolean value) {
        ConfigManager.getInstance().setConfigValue(platformKey + "ShowMessages", value);
    }

    /** Проигрывать ли звук для сообщений платформы. */
    public static boolean isPLATFORM_MESSAGE_SOUND(String platformKey) {
        Object value = ConfigManager.getInstance().getConfigValue(platformKey + "MessageSound");
        return !(value instanceof Boolean b) || b;
    }

    public static void setPLATFORM_MESSAGE_SOUND(String platformKey, boolean value) {
        ConfigManager.getInstance().setConfigValue(platformKey + "MessageSound", value);
    }

    public static boolean isTIKTOK_GIFT_EVENTS() {
        Object value = ConfigManager.getInstance().getConfigValue("tiktokGiftEvents");
        return value instanceof Boolean b && b;
    }

    public static void setTIKTOK_GIFT_EVENTS(boolean value) {
        ConfigManager.getInstance().setConfigValue("tiktokGiftEvents", value);
    }

    public static boolean isTIKTOK_FOLLOW_EVENTS() {
        Object value = ConfigManager.getInstance().getConfigValue("tiktokFollowEvents");
        return value instanceof Boolean b && b;
    }

    public static void setTIKTOK_FOLLOW_EVENTS(boolean value) {
        ConfigManager.getInstance().setConfigValue("tiktokFollowEvents", value);
    }

    public static takeyourminestream.ijustseen.config.UnpinMode getUNPIN_MODE() {
        Object value = ConfigManager.getInstance().getConfigValue("unpinMode");
        return value instanceof takeyourminestream.ijustseen.config.UnpinMode mode ? mode : takeyourminestream.ijustseen.config.UnpinMode.PIN_ICON;
    }

    public static void setUNPIN_MODE(takeyourminestream.ijustseen.config.UnpinMode value) {
        ConfigManager.getInstance().setConfigValue("unpinMode", value);
    }
} 