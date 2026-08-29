package takeyourminestream.ijustseen.config;

/**
 * Модель данных конфигурации мода
 */
public class ModConfigData {
    private String twitchChannelName = "ijustseen";
    private boolean twitchEnabled = true;
    private boolean youtubeEnabled = false;
    private String youtubeChannel = "";
    private boolean kickEnabled = false;
    private String kickChannel = "";
    private boolean tiktokEnabled = false;
    private String tiktokUsername = "";
    // Старые поля для обратной совместимости (тики)
    private int messageLifetimeTicks = 80;
    private int messageFallTicks = 20;
    // Новые поля: секунды (предпочтительное хранение для UI)
    private double messageLifetimeSeconds = 4.0; // 80 тиков @20 TPS
    private double messageFallSeconds = 1.0;     // 20 тиков @20 TPS
    private boolean enableFreezingOnView = true;
    private double maxFreezeDistance = 15.0;
    private int messageSpawnMinDistance = 2;
    private int messageSpawnMaxDistance = 5;
    private MessageSpawnMode messageSpawnMode = MessageSpawnMode.FRONT_OF_PLAYER;
    private boolean enableAutomoderation = false;
    private int particleMinCount = 10;
    private int particleMaxCount = 20;
    private int particleLifetimeTicks = 20;
    private String[] nickColors = {"§c", "§9", "§a", "§5"};
    private MessageScale messageScale = MessageScale.NORMAL;
    private boolean showMessageBackground = true;
    private boolean enableColorEmojis = true;
    private boolean followPlayer = false;
    private boolean enableClickToRemove = true;
    private boolean enableMessageSound = true;
    private double messageSoundVolume = 0.45;
    private boolean autoConnectIrcOnJoin = false;
    private int chanceForSpawn = 100;
    private int messageHistoryMaxSize = 100;
    private ChatRoleFilter chatRoleFilter = ChatRoleFilter.ALL;
    private boolean roleFilterSubscribers = false;
    private boolean roleFilterVip = false;
    private boolean roleFilterMods = false;
    private boolean enableUsernameBlocklist = true;
    private UnpinMode unpinMode = UnpinMode.PIN_ICON;
    private HudAnchor hudAnchor = HudAnchor.TOP_RIGHT;
    private int hudOffsetX = 0;
    private int hudOffsetY = 0;
    // Цвета панели сообщения
    private int panelBaseColorRgb = 0x000000;
    private boolean panelBorderFromPlatform = true;
    private int panelBorderColorRgb = takeyourminestream.ijustseen.core.MessagePanelConstants.DEFAULT_BORDER_RGB;
    private boolean showRoleBadges = true;
    // Пер-платформенные настройки: показ сообщений и звук
    private boolean twitchShowMessages = true;
    private boolean twitchMessageSound = true;
    private boolean youtubeShowMessages = true;
    private boolean youtubeMessageSound = true;
    private boolean kickShowMessages = true;
    private boolean kickMessageSound = true;
    private boolean tiktokShowMessages = true;
    private boolean tiktokMessageSound = true;
    // Опциональные события TikTok
    private boolean tiktokGiftEvents = false;
    private boolean tiktokFollowEvents = false;
    /** Фильтры ролей отдельно для каждого источника: ключ «&lt;платформа&gt;.&lt;роль&gt;». */
    private java.util.Map<String, Boolean> platformRoleFilters = new java.util.LinkedHashMap<>();
    private boolean platformRoleFiltersMigrated = false;
    private boolean platformRoleFiltersV2 = false;

    public static final String ROLE_ALL = "all";
    public static final String ROLE_FOLLOWERS = "followers";
    public static final String ROLE_SUBSCRIBERS = "subs";
    public static final String ROLE_VIP = "vip";
    public static final String ROLE_MODS = "mods";

    /** Ключи платформ (см. {@code ChatPlatform.getIconKey()}). */
    private static final String[] PLATFORM_KEYS = {"twitch", "youtube", "kick", "tiktok"};

    // Геттеры
    public int getChanceForSpawn() { return chanceForSpawn; }
    public int getMessageHistoryMaxSize() { return messageHistoryMaxSize; }
    public String getTwitchChannelName() { return twitchChannelName; }
    public boolean isTwitchEnabled() { return twitchEnabled; }
    public boolean isYoutubeEnabled() { return youtubeEnabled; }
    public String getYoutubeChannel() { return youtubeChannel; }
    public boolean isKickEnabled() { return kickEnabled; }
    public String getKickChannel() { return kickChannel; }
    public boolean isTiktokEnabled() { return tiktokEnabled; }
    public String getTiktokUsername() { return tiktokUsername; }
    public int getMessageLifetimeTicks() { return messageLifetimeTicks; }
    public int getMessageFallTicks() { return messageFallTicks; }
    public double getMessageLifetimeSeconds() { return messageLifetimeSeconds; }
    public double getMessageFallSeconds() { return messageFallSeconds; }
    public boolean isEnableFreezingOnView() { return enableFreezingOnView; }
    public double getMaxFreezeDistance() { return maxFreezeDistance; }
    public int getMessageSpawnMinDistance() { return messageSpawnMinDistance; }
    public int getMessageSpawnMaxDistance() { return messageSpawnMaxDistance; }
    public MessageSpawnMode getMessageSpawnMode() { return messageSpawnMode; }
    public boolean isEnableAutomoderation() { return enableAutomoderation; }
    public int getParticleMinCount() { return particleMinCount; }
    public int getParticleMaxCount() { return particleMaxCount; }
    public int getParticleLifetimeTicks() { return particleLifetimeTicks; }
    public String[] getNickColors() { return nickColors; }
    public MessageScale getMessageScale() { return messageScale; }
    public boolean isShowMessageBackground() { return showMessageBackground; }
    public boolean isEnableColorEmojis() { return enableColorEmojis; }
    public boolean isFollowPlayer() { return followPlayer; }
    public boolean isEnableClickToRemove() { return enableClickToRemove; }
    public boolean isEnableMessageSound() { return enableMessageSound; }
    public double getMessageSoundVolume() { return messageSoundVolume; }
    public boolean isAutoConnectIrcOnJoin() { return autoConnectIrcOnJoin; }
    public ChatRoleFilter getChatRoleFilter() { return chatRoleFilter != null ? chatRoleFilter : ChatRoleFilter.ALL; }
    public boolean isRoleFilterSubscribers() { return roleFilterSubscribers; }
    public boolean isRoleFilterVip() { return roleFilterVip; }
    public boolean isRoleFilterMods() { return roleFilterMods; }
    public boolean isEnableUsernameBlocklist() { return enableUsernameBlocklist; }
    public UnpinMode getUnpinMode() { return unpinMode != null ? unpinMode : UnpinMode.PIN_ICON; }
    public HudAnchor getHudAnchor() { return hudAnchor != null ? hudAnchor : HudAnchor.TOP_RIGHT; }
    public int getHudOffsetX() { return hudOffsetX; }
    public int getHudOffsetY() { return hudOffsetY; }
    public int getPanelBaseColorRgb() { return panelBaseColorRgb & 0xFFFFFF; }
    public boolean isPanelBorderFromPlatform() { return panelBorderFromPlatform; }
    public int getPanelBorderColorRgb() { return panelBorderColorRgb & 0xFFFFFF; }
    public boolean isShowRoleBadges() { return showRoleBadges; }
    public boolean isTwitchShowMessages() { return twitchShowMessages; }
    public boolean isTwitchMessageSound() { return twitchMessageSound; }
    public boolean isYoutubeShowMessages() { return youtubeShowMessages; }
    public boolean isYoutubeMessageSound() { return youtubeMessageSound; }
    public boolean isKickShowMessages() { return kickShowMessages; }
    public boolean isKickMessageSound() { return kickMessageSound; }
    public boolean isTiktokShowMessages() { return tiktokShowMessages; }
    public boolean isTiktokMessageSound() { return tiktokMessageSound; }
    public boolean isTiktokGiftEvents() { return tiktokGiftEvents; }
    public boolean isTiktokFollowEvents() { return tiktokFollowEvents; }

    // Сеттеры
    public void setChanceForSpawn(int chanceForSpawn) { this.chanceForSpawn = chanceForSpawn; }
    public void setMessageHistoryMaxSize(int messageHistoryMaxSize) { this.messageHistoryMaxSize = Math.max(10, messageHistoryMaxSize); }
    public void setTwitchChannelName(String twitchChannelName) { this.twitchChannelName = twitchChannelName; }
    public void setTwitchEnabled(boolean twitchEnabled) { this.twitchEnabled = twitchEnabled; }
    public void setYoutubeEnabled(boolean youtubeEnabled) { this.youtubeEnabled = youtubeEnabled; }
    public void setYoutubeChannel(String youtubeChannel) { this.youtubeChannel = youtubeChannel != null ? youtubeChannel : ""; }
    public void setKickEnabled(boolean kickEnabled) { this.kickEnabled = kickEnabled; }
    public void setKickChannel(String kickChannel) { this.kickChannel = kickChannel != null ? kickChannel : ""; }
    public void setTiktokEnabled(boolean tiktokEnabled) { this.tiktokEnabled = tiktokEnabled; }
    public void setTiktokUsername(String tiktokUsername) { this.tiktokUsername = tiktokUsername != null ? tiktokUsername : ""; }
    public void setMessageLifetimeTicks(int messageLifetimeTicks) { this.messageLifetimeTicks = messageLifetimeTicks; }
    public void setMessageFallTicks(int messageFallTicks) { this.messageFallTicks = messageFallTicks; }
    public void setMessageLifetimeSeconds(double messageLifetimeSeconds) { this.messageLifetimeSeconds = messageLifetimeSeconds; }
    public void setMessageFallSeconds(double messageFallSeconds) { this.messageFallSeconds = messageFallSeconds; }
    public void setEnableFreezingOnView(boolean enableFreezingOnView) { this.enableFreezingOnView = enableFreezingOnView; }
    public void setMaxFreezeDistance(double maxFreezeDistance) { this.maxFreezeDistance = maxFreezeDistance; }
    public void setMessageSpawnMinDistance(int messageSpawnMinDistance) { this.messageSpawnMinDistance = Math.max(1, messageSpawnMinDistance); }
    public void setMessageSpawnMaxDistance(int messageSpawnMaxDistance) { this.messageSpawnMaxDistance = Math.max(1, messageSpawnMaxDistance); }
    public void setMessageSpawnMode(MessageSpawnMode messageSpawnMode) { this.messageSpawnMode = messageSpawnMode; }
    public void setEnableAutomoderation(boolean enableAutomoderation) { this.enableAutomoderation = enableAutomoderation; }
    public void setParticleMinCount(int particleMinCount) { this.particleMinCount = particleMinCount; }
    public void setParticleMaxCount(int particleMaxCount) { this.particleMaxCount = particleMaxCount; }
    public void setParticleLifetimeTicks(int particleLifetimeTicks) { this.particleLifetimeTicks = particleLifetimeTicks; }
    public void setNickColors(String[] nickColors) { this.nickColors = nickColors; }
    public void setMessageScale(MessageScale messageScale) { this.messageScale = messageScale; }
    public void setShowMessageBackground(boolean showMessageBackground) { this.showMessageBackground = showMessageBackground; }
    public void setEnableColorEmojis(boolean enableColorEmojis) { this.enableColorEmojis = enableColorEmojis; }
    public void setFollowPlayer(boolean followPlayer) { this.followPlayer = followPlayer; }
    public void setEnableClickToRemove(boolean enableClickToRemove) { this.enableClickToRemove = enableClickToRemove; }
    public void setEnableMessageSound(boolean enableMessageSound) { this.enableMessageSound = enableMessageSound; }
    public void setMessageSoundVolume(double messageSoundVolume) { this.messageSoundVolume = messageSoundVolume; }
    public void setAutoConnectIrcOnJoin(boolean autoConnectIrcOnJoin) { this.autoConnectIrcOnJoin = autoConnectIrcOnJoin; }
    public void setChatRoleFilter(ChatRoleFilter chatRoleFilter) {
        this.chatRoleFilter = chatRoleFilter != null ? chatRoleFilter : ChatRoleFilter.ALL;
        applyLegacyRoleFilter(this.chatRoleFilter);
    }
    public void setRoleFilterSubscribers(boolean roleFilterSubscribers) {
        this.roleFilterSubscribers = roleFilterSubscribers;
        syncLegacyRoleFilter();
    }
    public void setRoleFilterVip(boolean roleFilterVip) {
        this.roleFilterVip = roleFilterVip;
        syncLegacyRoleFilter();
    }
    public void setRoleFilterMods(boolean roleFilterMods) {
        this.roleFilterMods = roleFilterMods;
        syncLegacyRoleFilter();
    }
    public void setEnableUsernameBlocklist(boolean enableUsernameBlocklist) { this.enableUsernameBlocklist = enableUsernameBlocklist; }
    public void setUnpinMode(UnpinMode unpinMode) { this.unpinMode = unpinMode != null ? unpinMode : UnpinMode.PIN_ICON; }
    public void setHudAnchor(HudAnchor hudAnchor) { this.hudAnchor = hudAnchor != null ? hudAnchor : HudAnchor.TOP_RIGHT; }
    public void setHudOffsetX(int hudOffsetX) { this.hudOffsetX = Math.max(0, Math.min(400, hudOffsetX)); }
    public void setHudOffsetY(int hudOffsetY) { this.hudOffsetY = Math.max(0, Math.min(400, hudOffsetY)); }
    public void setPanelBaseColorRgb(int panelBaseColorRgb) { this.panelBaseColorRgb = panelBaseColorRgb & 0xFFFFFF; }
    public void setPanelBorderFromPlatform(boolean panelBorderFromPlatform) { this.panelBorderFromPlatform = panelBorderFromPlatform; }
    public void setPanelBorderColorRgb(int panelBorderColorRgb) { this.panelBorderColorRgb = panelBorderColorRgb & 0xFFFFFF; }
    public void setShowRoleBadges(boolean showRoleBadges) { this.showRoleBadges = showRoleBadges; }
    public void setTwitchShowMessages(boolean twitchShowMessages) { this.twitchShowMessages = twitchShowMessages; }
    public void setTwitchMessageSound(boolean twitchMessageSound) { this.twitchMessageSound = twitchMessageSound; }
    public void setYoutubeShowMessages(boolean youtubeShowMessages) { this.youtubeShowMessages = youtubeShowMessages; }
    public void setYoutubeMessageSound(boolean youtubeMessageSound) { this.youtubeMessageSound = youtubeMessageSound; }
    public void setKickShowMessages(boolean kickShowMessages) { this.kickShowMessages = kickShowMessages; }
    public void setKickMessageSound(boolean kickMessageSound) { this.kickMessageSound = kickMessageSound; }
    public void setTiktokShowMessages(boolean tiktokShowMessages) { this.tiktokShowMessages = tiktokShowMessages; }
    public void setTiktokMessageSound(boolean tiktokMessageSound) { this.tiktokMessageSound = tiktokMessageSound; }
    public void setTiktokGiftEvents(boolean tiktokGiftEvents) { this.tiktokGiftEvents = tiktokGiftEvents; }
    public void setTiktokFollowEvents(boolean tiktokFollowEvents) { this.tiktokFollowEvents = tiktokFollowEvents; }

    /** Показывать ли роль {@code role} для источника {@code platformKey}. Нет ключа — включено. */
    public boolean isPlatformRoleFilter(String platformKey, String role) {
        if (platformRoleFilters == null) {
            return true;
        }
        Boolean value = platformRoleFilters.get(platformRoleKey(platformKey, role));
        return value == null || value;
    }

    public void setPlatformRoleFilter(String platformKey, String role, boolean value) {
        if (platformRoleFilters == null) {
            platformRoleFilters = new java.util.LinkedHashMap<>();
        }
        platformRoleFilters.put(platformRoleKey(platformKey, role), value);
    }

    private boolean hasPlatformRoleKey(String platformKey, String role) {
        return platformRoleFilters != null && platformRoleFilters.containsKey(platformRoleKey(platformKey, role));
    }

    private static String platformRoleKey(String platformKey, String role) {
        return platformKey + '.' + role;
    }

    /** Переносит старый одиночный enum в независимые флаги, если флаги ещё не заданы. */
    public void migrateLegacyRoleFilter() {
        if (roleFilterSubscribers || roleFilterVip || roleFilterMods) {
            syncLegacyRoleFilter();
            migratePlatformRoleFilters();
            migratePlatformRoleFiltersV2();
            return;
        }
        applyLegacyRoleFilter(chatRoleFilter);
        syncLegacyRoleFilter();
        migratePlatformRoleFilters();
        migratePlatformRoleFiltersV2();
    }

    /** Один раз копирует общие фильтры ролей в пер-платформенные (фильтры стали настройкой источника). */
    private void migratePlatformRoleFilters() {
        if (platformRoleFiltersMigrated) {
            return;
        }
        platformRoleFiltersMigrated = true;
        if (!roleFilterSubscribers && !roleFilterVip && !roleFilterMods) {
            return;
        }
        for (String platformKey : PLATFORM_KEYS) {
            setPlatformRoleFilter(platformKey, ROLE_SUBSCRIBERS, roleFilterSubscribers);
            setPlatformRoleFilter(platformKey, ROLE_VIP, roleFilterVip);
            setPlatformRoleFilter(platformKey, ROLE_MODS, roleFilterMods);
        }
    }

    /**
     * V2: явный свитч «показывать всех» (по умолчанию вкл) и все роли тоже вкл.
     * Если в v1 уже был узкий фильтр — «все» выключаем, недостающие роли оставляем выкл.
     */
    private void migratePlatformRoleFiltersV2() {
        if (platformRoleFiltersV2) {
            return;
        }
        platformRoleFiltersV2 = true;
        for (String platformKey : PLATFORM_KEYS) {
            boolean hadFilter = hasPlatformRoleKey(platformKey, ROLE_SUBSCRIBERS)
                || hasPlatformRoleKey(platformKey, ROLE_VIP)
                || hasPlatformRoleKey(platformKey, ROLE_MODS)
                || hasPlatformRoleKey(platformKey, ROLE_FOLLOWERS);
            boolean anyRoleOn = Boolean.TRUE.equals(platformRoleFilters.get(platformRoleKey(platformKey, ROLE_SUBSCRIBERS)))
                || Boolean.TRUE.equals(platformRoleFilters.get(platformRoleKey(platformKey, ROLE_VIP)))
                || Boolean.TRUE.equals(platformRoleFilters.get(platformRoleKey(platformKey, ROLE_MODS)))
                || Boolean.TRUE.equals(platformRoleFilters.get(platformRoleKey(platformKey, ROLE_FOLLOWERS)));
            if (hadFilter && anyRoleOn) {
                setPlatformRoleFilter(platformKey, ROLE_ALL, false);
                if (!hasPlatformRoleKey(platformKey, ROLE_SUBSCRIBERS)) {
                    setPlatformRoleFilter(platformKey, ROLE_SUBSCRIBERS, false);
                }
                if (!hasPlatformRoleKey(platformKey, ROLE_VIP)) {
                    setPlatformRoleFilter(platformKey, ROLE_VIP, false);
                }
                if (!hasPlatformRoleKey(platformKey, ROLE_MODS)) {
                    setPlatformRoleFilter(platformKey, ROLE_MODS, false);
                }
                if (!hasPlatformRoleKey(platformKey, ROLE_FOLLOWERS)) {
                    setPlatformRoleFilter(platformKey, ROLE_FOLLOWERS, false);
                }
            } else {
                setPlatformRoleFilter(platformKey, ROLE_ALL, true);
                setPlatformRoleFilter(platformKey, ROLE_FOLLOWERS, true);
                setPlatformRoleFilter(platformKey, ROLE_SUBSCRIBERS, true);
                setPlatformRoleFilter(platformKey, ROLE_VIP, true);
                setPlatformRoleFilter(platformKey, ROLE_MODS, true);
            }
        }
    }

    private void applyLegacyRoleFilter(ChatRoleFilter filter) {
        roleFilterSubscribers = false;
        roleFilterVip = false;
        roleFilterMods = false;
        if (filter == null || filter == ChatRoleFilter.ALL) {
            return;
        }
        roleFilterSubscribers = filter == ChatRoleFilter.SUBSCRIBERS
            || filter == ChatRoleFilter.SUB_OR_VIP
            || filter == ChatRoleFilter.SUB_OR_MOD
            || filter == ChatRoleFilter.SUB_OR_VIP_OR_MOD;
        roleFilterVip = filter == ChatRoleFilter.VIP
            || filter == ChatRoleFilter.SUB_OR_VIP
            || filter == ChatRoleFilter.VIP_OR_MOD
            || filter == ChatRoleFilter.SUB_OR_VIP_OR_MOD;
        roleFilterMods = filter == ChatRoleFilter.MODS
            || filter == ChatRoleFilter.SUB_OR_MOD
            || filter == ChatRoleFilter.VIP_OR_MOD
            || filter == ChatRoleFilter.SUB_OR_VIP_OR_MOD;
    }

    private void syncLegacyRoleFilter() {
        if (!roleFilterSubscribers && !roleFilterVip && !roleFilterMods) {
            chatRoleFilter = ChatRoleFilter.ALL;
        } else if (roleFilterSubscribers && roleFilterVip && roleFilterMods) {
            chatRoleFilter = ChatRoleFilter.SUB_OR_VIP_OR_MOD;
        } else if (roleFilterSubscribers && roleFilterVip) {
            chatRoleFilter = ChatRoleFilter.SUB_OR_VIP;
        } else if (roleFilterSubscribers && roleFilterMods) {
            chatRoleFilter = ChatRoleFilter.SUB_OR_MOD;
        } else if (roleFilterVip && roleFilterMods) {
            chatRoleFilter = ChatRoleFilter.VIP_OR_MOD;
        } else if (roleFilterSubscribers) {
            chatRoleFilter = ChatRoleFilter.SUBSCRIBERS;
        } else if (roleFilterVip) {
            chatRoleFilter = ChatRoleFilter.VIP;
        } else {
            chatRoleFilter = ChatRoleFilter.MODS;
        }
    }
    
    // Методы для обратной совместимости
    public boolean isMessagesInFrontOfPlayerOnly() { 
        return messageSpawnMode == MessageSpawnMode.FRONT_OF_PLAYER; 
    }
    
    public void setMessagesInFrontOfPlayerOnly(boolean messagesInFrontOfPlayerOnly) { 
        this.messageSpawnMode = messagesInFrontOfPlayerOnly ? 
            MessageSpawnMode.FRONT_OF_PLAYER : MessageSpawnMode.AROUND_PLAYER; 
    }
} 