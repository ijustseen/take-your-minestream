package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.ui.gui.ColorHsv;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.utils.ScreenNavigationCompat;

import java.util.function.IntConsumer;

/** HSV color picker: квадрат насыщенности/яркости, полоса оттенка, HEX и предпросмотр. */
public class ColorPickerScreen extends Screen {
    private static final int SV_SIZE = 140;
    private static final int HUE_WIDTH = 16;
    private static final int GAP = 10;
    private static final int PREVIEW_HEIGHT = 22;
    private static final int CONTENT_PADDING = 12;
    private static final int PANEL_WIDTH = CONTENT_PADDING * 2 + SV_SIZE + GAP + HUE_WIDTH;

    private final @Nullable Screen parent;
    private final IntConsumer onConfirm;

    private float hue;
    private float saturation;
    private float value;
    private boolean draggingSv;
    private boolean draggingHue;
    private boolean syncingHex;

    private TextFieldWidget field;
    private int svLeft;
    private int svTop;
    private int hueLeft;

    public ColorPickerScreen(@Nullable Screen parent, Text title, int initialRgb, IntConsumer onConfirm) {
        super(title);
        this.parent = parent;
        this.onConfirm = onConfirm;
        float[] hsv = ColorHsv.fromRgb(initialRgb & 0xFFFFFF);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    @Override
    protected void init() {
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = getPanelTop();
        svLeft = panelLeft + CONTENT_PADDING;
        svTop = panelTop + CONTENT_PADDING;
        hueLeft = svLeft + SV_SIZE + GAP;

        field = new TextFieldWidget(
            this.textRenderer,
            svLeft,
            getHexTop(),
            SV_SIZE + GAP + HUE_WIDTH,
            18,
            Text.translatable("takeyourstreamchat.config.color_picker.hex")
        );
        field.setMaxLength(7);
        field.setChangedListener(this::onHexTyped);
        this.addDrawableChild(field);
        syncHexField();
    }

    private void onHexTyped(String raw) {
        if (syncingHex) {
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hexVisible = field != null && field.visible;
        if (hexVisible) {
            field.visible = false;
        }
        super.render(context, mouseX, mouseY, delta);
        if (hexVisible) {
            field.visible = true;
        }

        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width, 8);
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = getPanelTop();
        ModUiTheme.drawBorderedPanel(context, panelLeft, panelTop, PANEL_WIDTH, getPanelHeight());

        drawSvSquare(context);
        drawHueBar(context);
        drawSvCursor(context);
        drawHueCursor(context);

        int previewTop = svTop + SV_SIZE + 8;
        int previewRight = hueLeft + HUE_WIDTH;
        context.fill(svLeft, previewTop, previewRight, previewTop + PREVIEW_HEIGHT, 0xFF000000 | currentRgb());
        context.fill(svLeft, previewTop, previewRight, previewTop + 1, ModUiTheme.PANEL_BORDER);
        context.fill(svLeft, previewTop + PREVIEW_HEIGHT - 1, previewRight, previewTop + PREVIEW_HEIGHT, ModUiTheme.PANEL_BORDER);

        if (field != null) {
            ModUiTheme.drawInputFrame(
                context,
                field.getX(),
                field.getY(),
                field.getWidth(),
                field.getHeight(),
                field.isFocused()
            );
            TextFieldWidget widget = field;
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    private void drawSvSquare(DrawContext context) {
        int cells = 35;
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
        context.fill(svLeft, svTop, svLeft + SV_SIZE, svTop + 1, ModUiTheme.PANEL_BORDER);
        context.fill(svLeft, svTop + SV_SIZE - 1, svLeft + SV_SIZE, svTop + SV_SIZE, ModUiTheme.PANEL_BORDER);
        context.fill(svLeft, svTop, svLeft + 1, svTop + SV_SIZE, ModUiTheme.PANEL_BORDER);
        context.fill(svLeft + SV_SIZE - 1, svTop, svLeft + SV_SIZE, svTop + SV_SIZE, ModUiTheme.PANEL_BORDER);
    }

    private void drawHueBar(DrawContext context) {
        int steps = 70;
        for (int i = 0; i < steps; i++) {
            float h = 360f * i / (float) (steps - 1);
            int y0 = svTop + i * SV_SIZE / steps;
            int y1 = svTop + (i + 1) * SV_SIZE / steps;
            context.fill(hueLeft, y0, hueLeft + HUE_WIDTH, y1, 0xFF000000 | ColorHsv.toRgb(h, 1f, 1f));
        }
        context.fill(hueLeft, svTop, hueLeft + HUE_WIDTH, svTop + 1, ModUiTheme.PANEL_BORDER);
        context.fill(hueLeft, svTop + SV_SIZE - 1, hueLeft + HUE_WIDTH, svTop + SV_SIZE, ModUiTheme.PANEL_BORDER);
        context.fill(hueLeft, svTop, hueLeft + 1, svTop + SV_SIZE, ModUiTheme.PANEL_BORDER);
        context.fill(hueLeft + HUE_WIDTH - 1, svTop, hueLeft + HUE_WIDTH, svTop + SV_SIZE, ModUiTheme.PANEL_BORDER);
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

    private boolean handlePointer(double mouseX, double mouseY, boolean startDrag) {
        if (startDrag) {
            draggingSv = isInSv(mouseX, mouseY);
            draggingHue = isInHue(mouseX, mouseY);
        }
        if (draggingSv) {
            saturation = (float) Math.max(0.0, Math.min(1.0, (mouseX - svLeft) / (double) (SV_SIZE - 1)));
            value = (float) Math.max(0.0, Math.min(1.0, 1.0 - (mouseY - svTop) / (double) (SV_SIZE - 1)));
            syncHexField();
            return true;
        }
        if (draggingHue) {
            hue = (float) Math.max(0.0, Math.min(360.0, 360.0 * (mouseY - svTop) / (double) (SV_SIZE - 1)));
            syncHexField();
            return true;
        }
        return false;
    }

    private boolean isInSv(double mouseX, double mouseY) {
        return mouseX >= svLeft && mouseX < svLeft + SV_SIZE && mouseY >= svTop && mouseY < svTop + SV_SIZE;
    }

    private boolean isInHue(double mouseX, double mouseY) {
        return mouseX >= hueLeft && mouseX < hueLeft + HUE_WIDTH && mouseY >= svTop && mouseY < svTop + SV_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handlePointer(mouseX, mouseY, true)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && (draggingSv || draggingHue) && handlePointer(mouseX, mouseY, false)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSv = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getPanelTop() {
        return 28;
    }

    private int getHexTop() {
        return svTop + SV_SIZE + 8 + PREVIEW_HEIGHT + 8;
    }

    private int getPanelHeight() {
        return CONTENT_PADDING + SV_SIZE + 8 + PREVIEW_HEIGHT + 8 + 18 + CONTENT_PADDING;
    }

    private void applyAndClose() {
        onConfirm.accept(currentRgb());
        ScreenNavigationCompat.open(this.client, this.parent);
    }

    @Override
    public void close() {
        applyAndClose();
    }
}
