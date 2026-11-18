package takeyourminestream.ijustseen.config;

/** Способ открепления закреплённого сообщения в мире (ПКМ). */
public enum UnpinMode {
    /** Открепить только при клике по иконке пина. */
    PIN_ICON("pin_icon"),

    /** Открепить при клике в любую точку панели сообщения. */
    WHOLE_MESSAGE("whole_message");

    private final String key;

    UnpinMode(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static UnpinMode fromKey(String key) {
        if (key == null) {
            return PIN_ICON;
        }
        for (UnpinMode mode : values()) {
            if (mode.key.equals(key)) {
                return mode;
            }
        }
        return PIN_ICON;
    }

    public UnpinMode next() {
        UnpinMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
