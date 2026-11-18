package takeyourminestream.ijustseen.integration.chat;

public enum ChatPlatform {
    TWITCH("Twitch", "§5", takeyourminestream.ijustseen.core.MessagePanelConstants.DEFAULT_BORDER_RGB),
    YOUTUBE("YouTube", "§c", 0xFF0000),
    KICK("Kick", "§a", 0x53FC18),
    TIKTOK("TikTok", "§b", 0x25F4EE);

    private final String displayName;
    private final String prefixColor;
    private final int accentColorRgb;

    ChatPlatform(String displayName, String prefixColor, int accentColorRgb) {
        this.displayName = displayName;
        this.prefixColor = prefixColor;
        this.accentColorRgb = accentColorRgb;
    }

    /** Фирменный цвет платформы (бортик панели сообщения). */
    public int getAccentColorRgb() {
        return accentColorRgb;
    }

    /** Цвет бортика по ключу иконки платформы; дефолтный фиолетовый, если ключ неизвестен. */
    public static int accentColorForIconKey(String iconKey) {
        if (iconKey != null) {
            for (ChatPlatform platform : values()) {
                if (platform.getIconKey().equals(iconKey)) {
                    return platform.accentColorRgb;
                }
            }
        }
        return takeyourminestream.ijustseen.core.MessagePanelConstants.DEFAULT_BORDER_RGB;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefixColor() {
        return prefixColor;
    }

    /** Ключ пиксельной иконки платформы: assets/take-your-stream-chat/textures/platform/<key>.png */
    public String getIconKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
