package takeyourminestream.ijustseen.core;

import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.integration.chat.ChatEventType;

/** Пользовательские цвета панели сообщения (заливка и бортик). */
public final class MessagePanelColors {
    private static final ThreadLocal<ChatEventType> ACTIVE_EVENT = new ThreadLocal<>();

    private MessagePanelColors() {}

    public static void bindEvent(ChatEventType eventType) {
        ACTIVE_EVENT.set(eventType);
    }

    public static void unbindEvent() {
        ACTIVE_EVENT.remove();
    }

    /** Тон заливки {@code message_panel.png}; 0x000000 — чёрная панель по умолчанию. */
    public static int baseRgb() {
        ChatEventType active = ACTIVE_EVENT.get();
        return baseRgb(active != null ? active : ChatEventType.CHAT);
    }

    public static int baseRgb(ChatEventType eventType) {
        int base = ModConfig.getPANEL_BASE_COLOR_RGB();
        if (eventType == null || !eventType.isEvent()) {
            return base;
        }
        return mix(base, eventType.accentRgb(), 0.28f);
    }

    /** Цвет бортика: фирменный цвет платформы или заданный вручную. */
    public static int borderRgb(int platformRgb) {
        ChatEventType active = ACTIVE_EVENT.get();
        return borderRgb(platformRgb, active != null ? active : ChatEventType.CHAT);
    }

    public static int borderRgb(int platformRgb, ChatEventType eventType) {
        if (eventType != null && eventType.isEvent()) {
            return eventType.accentRgb();
        }
        return ModConfig.isPANEL_BORDER_FROM_PLATFORM()
            ? platformRgb & 0xFFFFFF
            : ModConfig.getPANEL_BORDER_COLOR_RGB();
    }

    private static int mix(int fromRgb, int toRgb, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int fr = (fromRgb >> 16) & 0xFF;
        int fg = (fromRgb >> 8) & 0xFF;
        int fb = fromRgb & 0xFF;
        int tr = (toRgb >> 16) & 0xFF;
        int tg = (toRgb >> 8) & 0xFF;
        int tb = toRgb & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }
}
