package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * Всплывающая HSV-палитра рядом с кнопкой цвета.
 * Не экран: позицию и мышь задаёт хозяин, HEX-поле остаётся его виджетом.
 */
public final class ColorPickerPopup {
    private static final int SV_SIZE = 104;
    private static final int HUE_WIDTH = 14;
    private static final int GAP = 8;
    private static final int PREVIEW_HEIGHT = 16;
    private static final int HEX_HEIGHT = 16;
    private static final int PADDING = 8;
    private static final int PREVIEW_GAP = 6;
    private static final int HEX_GAP = 6;

    public static final int HEX_WIDTH = SV_SIZE + GAP + HUE_WIDTH;
    public static final int HEX_FIELD_HEIGHT = HEX_HEIGHT;

    private @Nullable ButtonWidget anchor;
    private @Nullable IntConsumer onChange;
    private @Nullable TextFieldWidget field;

    private float hue;
    private float saturation;
    private float value;
    private int lastPublished = -1;
    private boolean open;
    private boolean draggingSv;
    private boolean draggingHue;
    private boolean syncingHex;

    private int x;
    private int y;
    private int svLeft;
    private int svTop;
    private int hueLeft;

    public void attach(TextFieldWidget field) {
        this.field = field;
        field.setMaxLength(7);
        field.setChangedListener(this::onHexTyped);
    }

    public boolean isOpen() {
        return open;
    }

    public @Nullable ButtonWidget anchor() {
        return anchor;
    }

    public void show(ButtonWidget anchor, int rgb, IntConsumer onChange) {
        this.anchor = anchor;
        this.onChange = onChange;
        float[] hsv = ColorHsv.fromRgb(rgb & 0xFFFFFF);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        this.lastPublished = rgb & 0xFFFFFF;
        this.open = true;
        this.draggingSv = false;
        this.draggingHue = false;
        if (field != null) {
            field.visible = true;
            field.active = true;
            syncHexField();
        }
    }

    public void dismiss() {
        open = false;
        draggingSv = false;
        draggingHue = false;
        anchor = null;
        onChange = null;
        if (field != null) {
            field.visible = false;
            field.active = false;
            field.setFocused(false);
        }
    }

    public void place(
        int panelX,
        int panelWidth,
        int anchorY,
        int anchorHeight,
        boolean panelOnRight,
        int screenWidth,
        int screenHeight
    ) {
        if (!open) {
            return;
        }
        int popupW = width();
        int popupH = height();
        int gap = 4;
        int px = panelOnRight ? panelX - gap - popupW : panelX + panelWidth + gap;
        if (px < 2) {
            px = panelX + panelWidth + gap;
        }
        if (px + popupW > screenWidth - 2) {
            px = panelX - gap - popupW;
        }
        px = Math.max(2, Math.min(px, Math.max(2, screenWidth - popupW - 2)));
        int py = anchorY + anchorHeight / 2 - popupH / 2;
        py = Math.max(2, Math.min(py, Math.max(2, screenHeight - popupH - 2)));
        this.x = px;
        this.y = py;
        this.svLeft = px + PADDING;
        this.svTop = py + PADDING;
        this.hueLeft = svLeft + SV_SIZE + GAP;
        if (field != null) {
            field.setPosition(svLeft, hexTop());
        }
    }

    public boolean contains(double mouseX, double mouseY) {
        return open && mouseX >= x && mouseX < x + width() && mouseY >= y && mouseY < y + height();
    }

    public boolean isOverHex(double mouseX, double mouseY) {
        if (field == null || !open) {
            return false;
        }
        return mouseX >= field.getX() && mouseX < field.getX() + field.getWidth()
            && mouseY >= field.getY() && mouseY < field.getY() + field.getHeight();
    }

    /** Начинает перетаскивание квадрата или полосы оттенка. Пустое место палитры не двигает камеру. */
    public void beginDrag(double mouseX, double mouseY) {
        draggingSv = isInSv(mouseX, mouseY);
        draggingHue = !draggingSv && isInHue(mouseX, mouseY);
        if (draggingSv || draggingHue) {
            applyPointer(mouseX, mouseY);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!open || (!draggingSv && !draggingHue)) {
            return false;
        }
        applyPointer(mouseX, mouseY);
        return true;
    }

    public void mouseReleased() {
        draggingSv = false;
        draggingHue = false;
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!open) {
            return;
        }
        ModUiTheme.drawBorderedPanel(context, x, y, width(), height());
        drawSvSquare(context);
        drawHueBar(context);
        drawSvCursor(context);
        drawHueCursor(context);

        int previewTop = svTop + SV_SIZE + PREVIEW_GAP;
        int previewRight = hueLeft + HUE_WIDTH;
        context.fill(svLeft, previewTop, previewRight, previewTop + PREVIEW_HEIGHT, 0xFF000000 | currentRgb());
        context.fill(svLeft, previewTop, previewRight, previewTop + 1, ModUiTheme.PANEL_BORDER);
        context.fill(svLeft, previewTop + PREVIEW_HEIGHT - 1, previewRight, previewTop + PREVIEW_HEIGHT, ModUiTheme.PANEL_BORDER);

        if (field != null) {
            ModUiTheme.drawInputFrame(context, field.getX(), field.getY(), field.getWidth(), field.getHeight(), field.isFocused());
            TextFieldWidget widget = field;
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    private void onHexTyped(String raw) {
        if (!open || syncingHex) {
            return;
        }
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (!hex.matches("(?i)[0-9a-f]{6}")) {
            return;
        }
        float[] hsv = ColorHsv.fromRgb(Integer.parseInt(hex, 16));
        if (hsv[1] > 1e-5f) {
            hue = hsv[0];
        }
        saturation = hsv[1];
        value = hsv[2];
        publish(true);
    }

    private void applyPointer(double mouseX, double mouseY) {
        if (draggingSv) {
            saturation = (float) Math.max(0.0, Math.min(1.0, (mouseX - svLeft) / (double) (SV_SIZE - 1)));
            value = (float) Math.max(0.0, Math.min(1.0, 1.0 - (mouseY - svTop) / (double) (SV_SIZE - 1)));
        } else if (draggingHue) {
            hue = (float) Math.max(0.0, Math.min(360.0, 360.0 * (mouseY - svTop) / (double) (SV_SIZE - 1)));
        }
        publish(false);
    }

    private void publish(boolean fromHex) {
        int rgb = currentRgb();
        if (!fromHex) {
            syncHexField();
        }
        if (rgb == lastPublished || onChange == null) {
            lastPublished = rgb;
            return;
        }
        lastPublished = rgb;
        onChange.accept(rgb);
    }

    private void syncHexField() {
        if (field == null) {
            return;
        }
        syncingHex = true;
        field.setText(String.format("%06X", currentRgb()));
        syncingHex = false;
    }

    private int currentRgb() {
        return ColorHsv.toRgb(hue, saturation, value);
    }

    private void drawSvSquare(DrawContext context) {
        int cells = 26;
        for (int iy = 0; iy < cells; iy++) {
            float v = 1f - iy / (float) (cells - 1);
            for (int ix = 0; ix < cells; ix++) {
                float s = ix / (float) (cells - 1);
                int x0 = svLeft + ix * SV_SIZE / cells;
                int x1 = svLeft + (ix + 1) * SV_SIZE / cells;
                int y0 = svTop + iy * SV_SIZE / cells;
                int y1 = svTop + (iy + 1) * SV_SIZE / cells;
                context.fill(x0, y0, x1, y1, 0xFF000000 | ColorHsv.toRgb(hue, s, v));
            }
        }
        outline(context, svLeft, svTop, SV_SIZE, SV_SIZE);
    }

    private void drawHueBar(DrawContext context) {
        int steps = 52;
        for (int i = 0; i < steps; i++) {
            float h = 360f * i / (float) (steps - 1);
            int y0 = svTop + i * SV_SIZE / steps;
            int y1 = svTop + (i + 1) * SV_SIZE / steps;
            context.fill(hueLeft, y0, hueLeft + HUE_WIDTH, y1, 0xFF000000 | ColorHsv.toRgb(h, 1f, 1f));
        }
        outline(context, hueLeft, svTop, HUE_WIDTH, SV_SIZE);
    }

    private void outline(DrawContext context, int left, int top, int w, int h) {
        context.fill(left, top, left + w, top + 1, ModUiTheme.PANEL_BORDER);
        context.fill(left, top + h - 1, left + w, top + h, ModUiTheme.PANEL_BORDER);
        context.fill(left, top, left + 1, top + h, ModUiTheme.PANEL_BORDER);
        context.fill(left + w - 1, top, left + w, top + h, ModUiTheme.PANEL_BORDER);
    }

    private void drawSvCursor(DrawContext context) {
        int cx = svLeft + Math.round(saturation * (SV_SIZE - 1));
        int cy = svTop + Math.round((1f - value) * (SV_SIZE - 1));
        context.fill(cx - 3, cy - 1, cx + 4, cy + 2, 0xFFFFFFFF);
        context.fill(cx - 1, cy - 3, cx + 2, cy + 4, 0xFFFFFFFF);
        context.fill(cx - 2, cy, cx + 3, cy + 1, 0xFF000000);
        context.fill(cx, cy - 2, cx + 1, cy + 3, 0xFF000000);
    }

    private void drawHueCursor(DrawContext context) {
        int cy = svTop + Math.round((hue / 360f) * (SV_SIZE - 1));
        context.fill(hueLeft - 2, cy - 2, hueLeft + HUE_WIDTH + 2, cy + 3, 0xFFFFFFFF);
        context.fill(hueLeft, cy - 1, hueLeft + HUE_WIDTH, cy + 2, 0xFF000000);
    }

    private boolean isInSv(double mouseX, double mouseY) {
        return mouseX >= svLeft && mouseX < svLeft + SV_SIZE && mouseY >= svTop && mouseY < svTop + SV_SIZE;
    }

    private boolean isInHue(double mouseX, double mouseY) {
        return mouseX >= hueLeft && mouseX < hueLeft + HUE_WIDTH && mouseY >= svTop && mouseY < svTop + SV_SIZE;
    }

    private int hexTop() {
        return svTop + SV_SIZE + PREVIEW_GAP + PREVIEW_HEIGHT + HEX_GAP;
    }

    private int width() {
        return PADDING * 2 + SV_SIZE + GAP + HUE_WIDTH;
    }

    private int height() {
        return PADDING + SV_SIZE + PREVIEW_GAP + PREVIEW_HEIGHT + HEX_GAP + HEX_HEIGHT + PADDING;
    }
}
