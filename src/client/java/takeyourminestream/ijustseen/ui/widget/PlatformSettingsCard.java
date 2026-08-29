package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.ButtonWidget;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;

/** Карточка платформы на главной странице: тумблер и кнопка перехода к её настройкам. */
public final class PlatformSettingsCard {
    public final ChatPlatform platform;
    public final ButtonWidget toggle;
    public final ButtonWidget settings;

    public PlatformSettingsCard(ChatPlatform platform, ButtonWidget toggle, ButtonWidget settings) {
        this.platform = platform;
        this.toggle = toggle;
        this.settings = settings;
    }
}
