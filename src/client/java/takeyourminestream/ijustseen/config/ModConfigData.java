package takeyourminestream.ijustseen.config;

/**
 * Модель данных конфигурации мода
 */
public class ModConfigData {
    private String twitchChannelName = "ijustseen";
    // Старые поля для обратной совместимости (тики)
    private int messageLifetimeTicks = 80;
    private int messageFallTicks = 20;
    // Новые поля: секунды (предпочтительное хранение для UI)
    private double messageLifetimeSeconds = 4.0; // 80 тиков @20 TPS
    private double messageFallSeconds = 1.0;     // 20 тиков @20 TPS
    private boolean enableFreezingOnView = true;
    private double maxFreezeDistance = 15.0;
    private MessageSpawnMode messageSpawnMode = MessageSpawnMode.FRONT_OF_PLAYER;
    private boolean enableAutomoderation = false;
    private int particleMinCount = 10;
    private int particleMaxCount = 20;
    private int particleLifetimeTicks = 20;
    private String[] nickColors = {"§c", "§9", "§a", "§5"};
    private MessageScale messageScale = MessageScale.NORMAL;
    private boolean showMessageBackground = true;
    private boolean followPlayer = false;
    private boolean enableClickToRemove = true;
    private boolean enableMessageSound = true;
    private double messageSoundVolume = 0.45;
    private boolean autoConnectIrcOnJoin = false;
    private int chanceForSpawn = 100;

    // Геттеры
    public int getChanceForSpawn() { return chanceForSpawn; }
    public String getTwitchChannelName() { return twitchChannelName; }
    public int getMessageLifetimeTicks() { return messageLifetimeTicks; }
    public int getMessageFallTicks() { return messageFallTicks; }
    public double getMessageLifetimeSeconds() { return messageLifetimeSeconds; }
    public double getMessageFallSeconds() { return messageFallSeconds; }
    public boolean isEnableFreezingOnView() { return enableFreezingOnView; }
    public double getMaxFreezeDistance() { return maxFreezeDistance; }
    public MessageSpawnMode getMessageSpawnMode() { return messageSpawnMode; }
    public boolean isEnableAutomoderation() { return enableAutomoderation; }
    public int getParticleMinCount() { return particleMinCount; }
    public int getParticleMaxCount() { return particleMaxCount; }
    public int getParticleLifetimeTicks() { return particleLifetimeTicks; }
    public String[] getNickColors() { return nickColors; }
    public MessageScale getMessageScale() { return messageScale; }
    public boolean isShowMessageBackground() { return showMessageBackground; }
    public boolean isFollowPlayer() { return followPlayer; }
    public boolean isEnableClickToRemove() { return enableClickToRemove; }
    public boolean isEnableMessageSound() { return enableMessageSound; }
    public double getMessageSoundVolume() { return messageSoundVolume; }
    public boolean isAutoConnectIrcOnJoin() { return autoConnectIrcOnJoin; }

    // Сеттеры
    public void setChanceForSpawn(int chanceForSpawn) { this.chanceForSpawn = chanceForSpawn; }
    public void setTwitchChannelName(String twitchChannelName) { this.twitchChannelName = twitchChannelName; }
    public void setMessageLifetimeTicks(int messageLifetimeTicks) { this.messageLifetimeTicks = messageLifetimeTicks; }
    public void setMessageFallTicks(int messageFallTicks) { this.messageFallTicks = messageFallTicks; }
    public void setMessageLifetimeSeconds(double messageLifetimeSeconds) { this.messageLifetimeSeconds = messageLifetimeSeconds; }
    public void setMessageFallSeconds(double messageFallSeconds) { this.messageFallSeconds = messageFallSeconds; }
    public void setEnableFreezingOnView(boolean enableFreezingOnView) { this.enableFreezingOnView = enableFreezingOnView; }
    public void setMaxFreezeDistance(double maxFreezeDistance) { this.maxFreezeDistance = maxFreezeDistance; }
    public void setMessageSpawnMode(MessageSpawnMode messageSpawnMode) { this.messageSpawnMode = messageSpawnMode; }
    public void setEnableAutomoderation(boolean enableAutomoderation) { this.enableAutomoderation = enableAutomoderation; }
    public void setParticleMinCount(int particleMinCount) { this.particleMinCount = particleMinCount; }
    public void setParticleMaxCount(int particleMaxCount) { this.particleMaxCount = particleMaxCount; }
    public void setParticleLifetimeTicks(int particleLifetimeTicks) { this.particleLifetimeTicks = particleLifetimeTicks; }
    public void setNickColors(String[] nickColors) { this.nickColors = nickColors; }
    public void setMessageScale(MessageScale messageScale) { this.messageScale = messageScale; }
    public void setShowMessageBackground(boolean showMessageBackground) { this.showMessageBackground = showMessageBackground; }
    public void setFollowPlayer(boolean followPlayer) { this.followPlayer = followPlayer; }
    public void setEnableClickToRemove(boolean enableClickToRemove) { this.enableClickToRemove = enableClickToRemove; }
    public void setEnableMessageSound(boolean enableMessageSound) { this.enableMessageSound = enableMessageSound; }
    public void setMessageSoundVolume(double messageSoundVolume) { this.messageSoundVolume = messageSoundVolume; }
    public void setAutoConnectIrcOnJoin(boolean autoConnectIrcOnJoin) { this.autoConnectIrcOnJoin = autoConnectIrcOnJoin; }
    
    // Методы для обратной совместимости
    public boolean isMessagesInFrontOfPlayerOnly() { 
        return messageSpawnMode == MessageSpawnMode.FRONT_OF_PLAYER; 
    }
    
    public void setMessagesInFrontOfPlayerOnly(boolean messagesInFrontOfPlayerOnly) { 
        this.messageSpawnMode = messagesInFrontOfPlayerOnly ? 
            MessageSpawnMode.FRONT_OF_PLAYER : MessageSpawnMode.AROUND_PLAYER; 
    }
} 