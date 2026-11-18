package takeyourminestream.ijustseen.config;

/** Угол экрана, от которого строится стек HUD-виджета. */
public enum HudAnchor {
    TOP_RIGHT("top_right"),
    TOP_LEFT("top_left"),
    BOTTOM_RIGHT("bottom_right"),
    BOTTOM_LEFT("bottom_left");

    private final String key;

    HudAnchor(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean isRight() {
        return this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    public boolean isBottom() {
        return this == BOTTOM_RIGHT || this == BOTTOM_LEFT;
    }

    public static HudAnchor fromKey(String key) {
        if (key == null) {
            return TOP_RIGHT;
        }
        for (HudAnchor anchor : values()) {
            if (anchor.key.equals(key)) {
                return anchor;
            }
        }
        return TOP_RIGHT;
    }

    public HudAnchor next() {
        HudAnchor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
