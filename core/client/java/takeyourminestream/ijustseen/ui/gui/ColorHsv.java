package takeyourminestream.ijustseen.ui.gui;

/** RGB ↔ HSV для color picker. H — 0..360, S/V — 0..1. */
public final class ColorHsv {
    private ColorHsv() {}

    public static int toRgb(float hue, float saturation, float value) {
        float h = ((hue % 360f) + 360f) % 360f;
        float s = clamp01(saturation);
        float v = clamp01(value);
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r = 0f;
        float g = 0f;
        float b = 0f;
        if (h < 60f) {
            r = c;
            g = x;
        } else if (h < 120f) {
            r = x;
            g = c;
        } else if (h < 180f) {
            g = c;
            b = x;
        } else if (h < 240f) {
            g = x;
            b = c;
        } else if (h < 300f) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return (roundByte(r + m) << 16) | (roundByte(g + m) << 8) | roundByte(b + m);
    }

    /** {@code [hue, saturation, value]}. */
    public static float[] fromRgb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0f;
        if (delta > 1e-5f) {
            if (max == r) {
                hue = 60f * (((g - b) / delta) % 6f);
            } else if (max == g) {
                hue = 60f * (((b - r) / delta) + 2f);
            } else {
                hue = 60f * (((r - g) / delta) + 4f);
            }
        }
        if (hue < 0f) {
            hue += 360f;
        }
        float saturation = max <= 1e-5f ? 0f : delta / max;
        return new float[] { hue, saturation, max };
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static int roundByte(float channel) {
        return Math.max(0, Math.min(255, Math.round(channel * 255f)));
    }
}
