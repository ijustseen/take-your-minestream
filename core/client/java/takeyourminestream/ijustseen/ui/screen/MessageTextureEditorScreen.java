package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.render.MessagePanelTextures;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageEditorPreview;
import takeyourminestream.ijustseen.ui.gui.ColorPickerPopup;
import takeyourminestream.ijustseen.ui.gui.MessageCardLayout;
import takeyourminestream.ijustseen.ui.gui.MessageCardRenderer;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import takeyourminestream.ijustseen.utils.ScreenNavigationCompat;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Полноэкранное рисование заливки и бортика. Оригиналы в jar не трогаются.
 * Линии на холсте — разрезы nine-slice: внешняя четверть остаётся углом, середина тянется.
 */
public class MessageTextureEditorScreen extends PreviewPointerScreen {
    private static final int GUIDE = 0xFF00E5FF;
    private static final int SCREEN_BG = 0xFF0C0A14;
    private static final int BAR_BG = 0xFF161222;
    private static final int UNDO_LIMIT = 50;

    private int headerH = 36;
    private int footerH = 36;
    private int margin = 14;
    private int cardH = 58;
    private int thumbSize = 32;

    private final @Nullable Screen parent;
    private final ColorPickerPopup colorPopup = new ColorPickerPopup();

    private ButtonWidget sizeButton;
    private ButtonWidget colorButton;
    private ButtonWidget eraserButton;
    private ButtonWidget undoButton;
    private ButtonWidget redoButton;
    private ButtonWidget zoomOutButton;
    private ButtonWidget zoomInButton;
    private ButtonWidget restoreButton;
    private ButtonWidget doneButton;
    private TextFieldWidget hexField;

    private boolean borderLayer;
    private int paintColor = 0xFFFFFFFF;
    private boolean eraserMode;
    private boolean painting;
    private boolean erasing;
    private boolean strokeChanged;
    private @Nullable LayerSnapshot strokeBefore;
    private final ArrayDeque<LayerSnapshot> undo = new ArrayDeque<>();
    private final ArrayDeque<LayerSnapshot> redo = new ArrayDeque<>();
    private @Nullable Message sampleMessage;

    private record LayerSnapshot(boolean border, int[] pixels) {}

    private int canvasX;
    private int canvasY;
    private int cell = 4;
    private int canvasPixels;
    private int fillCardX;
    private int fillCardY;
    private int fillCardW;
    private int borderCardX;
    private int borderCardY;
    private int borderCardW;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
    private int zoomCell = 1;
    private int zoomForSize = -1;
    private int panX;
    private int panY;
    private boolean zoomAnchored;
    private double anchorX;
    private double anchorY;
    private boolean panning;

    public MessageTextureEditorScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourstreamchat.texture_editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        colorPopup.dismiss();
        sizeButton = ButtonWidget.builder(Text.empty(), btn -> {
            remember();
            MessagePanelTextures.resize(borderLayer, MessagePanelTextures.nextSize(MessagePanelTextures.size(borderLayer)));
            refreshLabels();
        }).dimensions(0, 0, 120, 20).build();
        colorButton = ButtonWidget.builder(Text.empty(), btn -> openColorPopup(btn)).dimensions(0, 0, 120, 20).build();
        eraserButton = ButtonWidget.builder(Text.translatable("takeyourstreamchat.texture_editor.eraser"), btn -> {
            eraserMode = !eraserMode;
            if (eraserMode) {
                dismissColorPopup();
            }
        }).dimensions(0, 0, 120, 20).build();
        restoreButton = ButtonWidget.builder(Text.translatable("takeyourstreamchat.texture_editor.restore"), btn -> {
            remember();
            MessagePanelTextures.restore(borderLayer);
            refreshLabels();
        }).dimensions(0, 0, 120, 20).build();
        undoButton = ButtonWidget.builder(Text.literal("\u2190"), btn -> undo()).dimensions(0, 0, 22, 20).build();
        redoButton = ButtonWidget.builder(Text.literal("\u2192"), btn -> redo()).dimensions(0, 0, 22, 20).build();
        zoomOutButton = ButtonWidget.builder(Text.literal("-"), btn -> changeZoom(-1, viewX + viewW / 2.0, viewY + viewH / 2.0)).dimensions(0, 0, 22, 20).build();
        zoomInButton = ButtonWidget.builder(Text.literal("+"), btn -> changeZoom(1, viewX + viewW / 2.0, viewY + viewH / 2.0)).dimensions(0, 0, 22, 20).build();
        doneButton = ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close()).dimensions(0, 0, 90, 20).build();
        this.addDrawableChild(sizeButton);
        this.addDrawableChild(colorButton);
        this.addDrawableChild(eraserButton);
        this.addDrawableChild(undoButton);
        this.addDrawableChild(redoButton);
        this.addDrawableChild(zoomOutButton);
        this.addDrawableChild(zoomInButton);
        this.addDrawableChild(restoreButton);
        this.addDrawableChild(doneButton);

        hexField = new TextFieldWidget(
            this.textRenderer,
            0,
            0,
            ColorPickerPopup.HEX_WIDTH,
            ColorPickerPopup.HEX_FIELD_HEIGHT,
            Text.translatable("takeyourstreamchat.config.color_picker.hex")
        );
        hexField.visible = false;
        hexField.active = false;
        colorPopup.attach(hexField);
        this.addDrawableChild(hexField);
        refreshLabels();
    }

    private void refreshLabels() {
        sizeButton.setMessage(Text.translatable("takeyourstreamchat.texture_editor.size", MessagePanelTextures.size(borderLayer)));
        colorButton.setMessage(Text.literal(String.format("#%06X", paintColor & 0xFFFFFF)));
    }

    private void layout() {
        boolean compact = this.height < 340 || this.width < 640;
        margin = this.width < 520 ? 6 : 14;
        headerH = compact ? 24 : 36;
        footerH = this.height < 230 ? 0 : (compact ? 20 : 36);
        cardH = this.height < 300 ? 36 : 58;
        thumbSize = cardH >= 50 ? 32 : 16;

        int contentTop = headerH + (compact ? 4 : 10);
        int contentBottom = this.height - footerH - (footerH == 0 ? 4 : 8);
        if (contentBottom < contentTop + 72) {
            footerH = 0;
            contentBottom = this.height - 4;
        }
        int innerW = Math.max(40, this.width - margin * 2);
        int minLeft = 148;
        int minPreview = this.width >= 900 ? 220 : 156;
        boolean columns = innerW >= minLeft + 8 + minPreview;

        int leftX = margin;
        int leftW;
        if (columns) {
            int previewCol = Math.min(300, Math.max(minPreview, this.width / 3));
            previewCol = Math.min(previewCol, innerW - 8 - minLeft);
            leftW = innerW - 8 - previewCol;
            previewX = leftX + leftW + 8;
            previewY = contentTop;
            previewW = Math.max(40, this.width - margin - previewX);
            previewH = Math.max(40, contentBottom - contentTop);
        } else {
            int dock = this.height >= 260 ? Math.min(72, Math.max(0, (contentBottom - contentTop) / 5)) : 0;
            leftW = innerW;
            previewX = margin;
            previewW = innerW;
            previewH = dock;
            previewY = contentBottom - dock;
            if (dock > 0) {
                contentBottom -= dock + 4;
            }
        }

        int gap = 6;
        fillCardW = Math.max(1, (leftW - gap) / 2);
        borderCardW = Math.max(1, leftW - gap - fillCardW);
        fillCardX = leftX;
        borderCardX = leftX + fillCardW + gap;
        fillCardY = contentTop;
        borderCardY = contentTop;

        int labelRow = compact ? 14 : 20;
        int canvasTop = contentTop + cardH + labelRow;
        int toolbarY = contentBottom - 20;
        if (toolbarY < canvasTop) {
            toolbarY = canvasTop;
        }
        viewX = leftX;
        viewY = canvasTop;
        viewW = Math.max(16, leftW);
        viewH = Math.max(16, toolbarY - 4 - canvasTop);

        int tex = MessagePanelTextures.size(borderLayer);
        int fit = Math.max(1, Math.min(48, Math.min(viewW, viewH) / Math.max(1, tex)));
        if (zoomForSize != tex || zoomCell < 1) {
            zoomForSize = tex;
            zoomCell = fit;
            panX = 0;
            panY = 0;
            zoomAnchored = false;
        }
        cell = zoomCell;
        canvasPixels = cell * tex;
        if (zoomAnchored) {
            canvasX = (int) Math.round(anchorX);
            canvasY = (int) Math.round(anchorY);
            zoomAnchored = false;
        } else {
            canvasX = viewX + (viewW - canvasPixels) / 2 + panX;
            canvasY = viewY + (viewH - canvasPixels) / 2 + panY;
        }
        if (canvasPixels <= viewW) {
            canvasX = viewX + (viewW - canvasPixels) / 2;
        } else {
            canvasX = Math.max(viewX + viewW - canvasPixels, Math.min(viewX, canvasX));
        }
        if (canvasPixels <= viewH) {
            canvasY = viewY + (viewH - canvasPixels) / 2;
        } else {
            canvasY = Math.max(viewY + viewH - canvasPixels, Math.min(viewY, canvasY));
        }
        panX = canvasX - (viewX + (viewW - canvasPixels) / 2);
        panY = canvasY - (viewY + (viewH - canvasPixels) / 2);

        int buttonGap = 4;
        int buttonW = Math.max(20, (leftW - buttonGap * 3) / 4);
        if (buttonW * 4 + buttonGap * 3 > leftW) {
            buttonW = Math.max(16, (leftW - buttonGap * 3) / 4);
        }
        sizeButton.setWidth(buttonW);
        colorButton.setWidth(buttonW);
        eraserButton.setWidth(buttonW);
        restoreButton.setWidth(buttonW);
        sizeButton.setPosition(leftX, toolbarY);
        colorButton.setPosition(leftX + (buttonW + buttonGap), toolbarY);
        eraserButton.setPosition(leftX + (buttonW + buttonGap) * 2, toolbarY);
        restoreButton.setPosition(leftX + (buttonW + buttonGap) * 3, toolbarY);

        int doneW = Math.min(90, Math.max(44, Math.min(this.textRenderer.getWidth(doneButton.getMessage()) + 12, this.width / 4)));
        doneButton.setWidth(doneW);
        int doneX = Math.max(margin, this.width - margin - doneW);
        doneButton.setPosition(doneX, Math.max(2, (headerH - 20) / 2));

        int arrow = compact ? 18 : 22;
        undoButton.setWidth(arrow);
        redoButton.setWidth(arrow);
        zoomOutButton.setWidth(arrow);
        zoomInButton.setWidth(arrow);
        int cluster = arrow * 4 + 4 * 3;
        Text canvasLabel = Text.translatable(borderLayer
            ? "takeyourstreamchat.texture_editor.canvas.border"
            : "takeyourstreamchat.texture_editor.canvas.fill");
        boolean clusterInHeader = compact || this.textRenderer.getWidth(canvasLabel) + 8 + cluster > leftW;
        int arrowY = clusterInHeader ? doneButton.getY() : fillCardY + cardH;
        int arrowRight = clusterInHeader ? doneX - 6 - arrow : leftX + leftW - arrow;
        arrowRight = Math.max(margin + cluster - arrow, arrowRight);
        redoButton.setPosition(arrowRight, arrowY);
        undoButton.setPosition(arrowRight - (arrow + 4), arrowY);
        zoomInButton.setPosition(arrowRight - (arrow + 4) * 2, arrowY);
        zoomOutButton.setPosition(arrowRight - (arrow + 4) * 3, arrowY);
        undoButton.active = !undo.isEmpty() || strokeBefore != null;
        redoButton.active = !redo.isEmpty();

        if (colorPopup.isOpen() && colorPopup.anchor() == colorButton) {
            colorPopup.place(colorButton.getX(), colorButton.getWidth(), colorButton.getY(), colorButton.getHeight(), false, this.width, this.height);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        java.util.Set<ButtonWidget> hidden = ScreenUiHelper.hideButtons(this);
        boolean hexVisible = hexField != null && hexField.visible;
        if (hexVisible) {
            hexField.visible = false;
        }
        super.render(context, mouseX, mouseY, delta);
        if (hexVisible) {
            hexField.visible = true;
        }
        ScreenUiHelper.restoreButtons(hidden);

        context.fill(0, 0, this.width, this.height, SCREEN_BG);
        context.fill(0, 0, this.width, headerH, BAR_BG);
        context.fill(0, headerH, this.width, headerH + 1, ModUiTheme.ACCENT);
        if (footerH > 0) {
            context.fill(0, this.height - footerH, this.width, this.height, BAR_BG);
            context.fill(0, this.height - footerH, this.width, this.height - footerH + 1, 0xFF2A2438);
        }
        int titleStop = Math.min(doneButton.getX(), zoomOutButton.getX()) - margin - 4;
        String title = ModUiTheme.fit(this.textRenderer, this.title.getString(), Math.max(8, titleStop - margin));
        context.drawTextWithShadow(this.textRenderer, title, margin, Math.max(4, (headerH - this.textRenderer.fontHeight) / 2), ModUiTheme.TEXT_PRIMARY);

        drawLayerCard(
            context,
            mouseX,
            mouseY,
            fillCardX,
            fillCardY,
            fillCardW,
            false,
            "takeyourstreamchat.texture_editor.layer.fill",
            "takeyourstreamchat.texture_editor.layer.fill.desc"
        );
        drawLayerCard(
            context,
            mouseX,
            mouseY,
            borderCardX,
            borderCardY,
            borderCardW,
            true,
            "takeyourstreamchat.texture_editor.layer.border",
            "takeyourstreamchat.texture_editor.layer.border.desc"
        );

        boolean zoomOnLabelRow = Math.abs(zoomOutButton.getY() - (fillCardY + cardH)) <= 4;
        int labelLimit = zoomOnLabelRow ? zoomOutButton.getX() - margin - 6 : viewW;
        String canvasLabel = ModUiTheme.fit(
            this.textRenderer,
            Text.translatable(borderLayer
                ? "takeyourstreamchat.texture_editor.canvas.border"
                : "takeyourstreamchat.texture_editor.canvas.fill").getString(),
            Math.max(8, labelLimit)
        );
        if (labelLimit > 8) {
            context.drawTextWithShadow(
                this.textRenderer,
                canvasLabel,
                margin,
                fillCardY + cardH + 4,
                ModUiTheme.TEXT_SECONDARY
            );
        }
        context.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);
        drawCanvas(context);
        drawGuides(context);
        outline(context, canvasX - 1, canvasY - 1, canvasPixels + 2, canvasPixels + 2, ModUiTheme.ACCENT);
        context.disableScissor();

        drawPreview(context);
        ScreenUiHelper.renderButtons(
            context,
            mouseX,
            mouseY,
            List.of(sizeButton, colorButton, eraserButton, zoomOutButton, zoomInButton, undoButton, redoButton, restoreButton, doneButton),
            eraserMode ? eraserButton : null
        );
        if (colorButton.getWidth() >= 28) {
            int sx = colorButton.getX() + colorButton.getWidth() - 16;
            int sy = colorButton.getY() + 5;
            context.fill(sx - 1, sy - 1, sx + 11, sy + 11, ModUiTheme.PANEL_BORDER);
            context.fill(sx, sy, sx + 10, sy + 10, 0xFF000000 | paintColor);
        }
        drawHint(context);
        colorPopup.draw(context, mouseX, mouseY, delta);
    }

    private void drawLayerCard(
        DrawContext context,
        int mouseX,
        int mouseY,
        int x,
        int y,
        int width,
        boolean border,
        String titleKey,
        String descKey
    ) {
        boolean active = borderLayer == border;
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + cardH;
        int bg = active ? 0xFF221C33 : (hovered ? 0xFF1A1626 : 0xFF14111C);
        context.fill(x, y, x + width, y + cardH, bg);
        outline(context, x, y, width, cardH, active ? ModUiTheme.ACCENT : 0xFF3A3450);
        int thumb = Math.min(thumbSize, Math.max(8, Math.min(cardH - 8, width - 12)));
        int thumbX = x + 4;
        int thumbY = y + (cardH - thumb) / 2;
        drawThumb(context, thumbX, thumbY, thumb, border);
        outline(context, thumbX - 1, thumbY - 1, thumb + 2, thumb + 2, 0xFF000000);
        int textX = thumbX + thumb + 6;
        int textW = width - (textX - x) - 4;
        if (textW < 12) {
            return;
        }
        String title = ModUiTheme.fit(this.textRenderer, Text.translatable(titleKey).getString(), textW);
        context.drawTextWithShadow(
            this.textRenderer,
            title,
            textX,
            y + Math.max(4, (cardH - this.textRenderer.fontHeight * 2 - 2) / 2),
            active ? ModUiTheme.TEXT_PRIMARY : ModUiTheme.TEXT_SECONDARY
        );
        if (cardH >= 48) {
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable(descKey), textW)) {
                context.drawTextWithShadow(this.textRenderer, line, textX, y + 28, ModUiTheme.TEXT_MUTED);
                break;
            }
        }
    }

    private void drawThumb(DrawContext context, int x, int y, int box, boolean border) {
        int tex = MessagePanelTextures.size(border);
        for (int py = 0; py < box; py++) {
            for (int px = 0; px < box; px++) {
                int checker = ((px + py) & 1) == 0 ? 0xFF2A2438 : 0xFF3A3450;
                context.fill(x + px, y + py, x + px + 1, y + py + 1, checker);
            }
        }
        int scale = Math.max(1, box / tex);
        if (tex * scale <= box) {
            int shown = tex * scale;
            int ox = x + (box - shown) / 2;
            int oy = y + (box - shown) / 2;
            for (int sy = 0; sy < tex; sy++) {
                for (int sx = 0; sx < tex; sx++) {
                    int argb = MessagePanelTextures.argb(border, sx, sy);
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                    context.fill(ox + sx * scale, oy + sy * scale, ox + sx * scale + scale, oy + sy * scale + scale, argb);
                }
            }
            return;
        }
        for (int py = 0; py < box; py++) {
            int sy = py * (tex - 1) / (box - 1);
            for (int px = 0; px < box; px++) {
                int sx = px * (tex - 1) / (box - 1);
                int argb = MessagePanelTextures.argb(border, sx, sy);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                context.fill(x + px, y + py, x + px + 1, y + py + 1, argb);
            }
        }
    }

    private void drawPreview(DrawContext context) {
        ModUiTheme.drawBorderedPanel(context, previewX, previewY, previewW, previewH);
        if (previewW < 48 || previewH < 36) {
            return;
        }
        int pad = previewW < 180 ? 6 : 10;
        int innerX = previewX + pad;
        int innerW = Math.max(16, previewW - pad * 2);
        boolean showCaption = previewH >= 160 && previewW >= 180;
        boolean showLegend = previewH >= 120;
        int y = previewY + 8;
        String previewTitle = ModUiTheme.fit(
            this.textRenderer,
            Text.translatable("takeyourstreamchat.texture_editor.preview").getString(),
            innerW
        );
        context.drawTextWithShadow(this.textRenderer, previewTitle, innerX, y, ModUiTheme.TEXT_PRIMARY);
        y += this.textRenderer.fontHeight + 4;
        if (showCaption) {
            for (OrderedText line : this.textRenderer.wrapLines(
                Text.translatable("takeyourstreamchat.texture_editor.preview.caption"),
                innerW
            )) {
                context.drawTextWithShadow(this.textRenderer, line, innerX, y, ModUiTheme.TEXT_MUTED);
                y += this.textRenderer.fontHeight + 1;
            }
            y += 6;
        }

        Message message = sample();
        int legendBlock = showLegend ? 46 : 4;
        if (message != null && this.client != null) {
            ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(message.getText());
            MessageCardLayout.Layout layout = MessageCardLayout.compute(this.textRenderer, message, innerW);
            int cardX = innerX + Math.max(0, (innerW - layout.width()) / 2);
            int cardBottom = previewY + previewH - legendBlock - 6;
            if (y + layout.height() > cardBottom) {
                y = Math.max(previewY + 8, cardBottom - layout.height());
            }
            int scissorBottom = Math.max(previewY + 6, previewY + previewH - legendBlock);
            if (scissorBottom > previewY + 4) {
                context.enableScissor(previewX + 2, previewY + 2, previewX + previewW - 2, scissorBottom);
                MessageCardRenderer.draw(
                    context,
                    this.textRenderer,
                    BlockedUsernameManager.getInstance(),
                    message,
                    parsed,
                    layout,
                    cardX,
                    y,
                    1.0f,
                    false,
                    true,
                    false
                );
                context.disableScissor();
            }
        }

        if (showLegend) {
            int legendY = previewY + previewH - 44;
            drawLegendRow(context, innerX, legendY, innerW, false);
            drawLegendRow(context, innerX, legendY + 20, innerW, true);
        }
    }

    private void drawLegendRow(DrawContext context, int x, int y, int width, boolean border) {
        boolean active = borderLayer == border;
        if (active) {
            context.fill(x - 4, y - 3, x + width, y + 18, 0x339146FF);
        }
        drawThumb(context, x, y, 16, border);
        Text label = Text.translatable(border
            ? "takeyourstreamchat.texture_editor.legend.border"
            : "takeyourstreamchat.texture_editor.legend.fill");
        String fitted = ModUiTheme.fit(this.textRenderer, label.getString(), Math.max(8, width - 22));
        context.drawTextWithShadow(
            this.textRenderer,
            fitted,
            x + 20,
            y + 3,
            active ? ModUiTheme.TEXT_PRIMARY : ModUiTheme.TEXT_SECONDARY
        );
    }

    private @Nullable Message sample() {
        if (sampleMessage == null && this.client != null && !MessageEditorPreview.SAMPLES.isEmpty()) {
            sampleMessage = MessageEditorPreview.create(
                this.client,
                MessageEditorPreview.SAMPLES.get(0),
                Vec3d.ZERO,
                0,
                0,
                Vec3d.ZERO
            );
        }
        return sampleMessage;
    }

    private static void outline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawCanvas(DrawContext context) {
        int tex = MessagePanelTextures.size(borderLayer);
        for (int y = 0; y < tex; y++) {
            for (int x = 0; x < tex; x++) {
                int px = canvasX + x * cell;
                int py = canvasY + y * cell;
                int checker = ((x + y) & 1) == 0 ? 0xFF2A2438 : 0xFF3A3450;
                context.fill(px, py, px + cell, py + cell, checker);
                int argb = MessagePanelTextures.argb(borderLayer, x, y);
                if ((argb >>> 24) != 0) {
                    context.fill(px, py, px + cell, py + cell, argb);
                }
            }
        }
    }

    private void drawGuides(DrawContext context) {
        int tex = MessagePanelTextures.size(borderLayer);
        int cut = tex / 4;
        int x1 = canvasX + cut * cell;
        int x2 = canvasX + (tex - cut) * cell;
        int y1 = canvasY + cut * cell;
        int y2 = canvasY + (tex - cut) * cell;
        int right = canvasX + canvasPixels;
        int bottom = canvasY + canvasPixels;
        context.fill(x1, canvasY, x1 + 1, bottom, GUIDE);
        context.fill(x2, canvasY, x2 + 1, bottom, GUIDE);
        context.fill(canvasX, y1, right, y1 + 1, GUIDE);
        context.fill(canvasX, y2, right, y2 + 1, GUIDE);
    }

    private void drawHint(DrawContext context) {
        if (footerH < 12) {
            return;
        }
        int width = Math.max(40, this.width - margin * 2);
        int maxLines = footerH >= 32 ? 2 : 1;
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.translatable("takeyourstreamchat.texture_editor.hint"), width);
        int y = this.height - footerH + Math.max(2, (footerH - maxLines * (this.textRenderer.fontHeight + 1)) / 2);
        int shown = 0;
        for (OrderedText line : lines) {
            if (shown >= maxLines || y + this.textRenderer.fontHeight > this.height - 1) {
                break;
            }
            context.drawTextWithShadow(this.textRenderer, line, margin, y, ModUiTheme.TEXT_MUTED);
            y += this.textRenderer.fontHeight + 1;
            shown++;
        }
    }

    private boolean overCanvas(double mouseX, double mouseY) {
        return mouseX >= canvasX && mouseX < canvasX + canvasPixels
            && mouseY >= canvasY && mouseY < canvasY + canvasPixels
            && overView(mouseX, mouseY);
    }

    private boolean overView(double mouseX, double mouseY) {
        return mouseX >= viewX && mouseX < viewX + viewW
            && mouseY >= viewY && mouseY < viewY + viewH;
    }

    private void changeZoom(int delta, double anchorMouseX, double anchorMouseY) {
        int next = Math.max(1, Math.min(48, zoomCell + delta));
        if (next == zoomCell) {
            return;
        }
        double u = (anchorMouseX - canvasX) / Math.max(1, zoomCell);
        double v = (anchorMouseY - canvasY) / Math.max(1, zoomCell);
        zoomCell = next;
        anchorX = anchorMouseX - u * next;
        anchorY = anchorMouseY - v * next;
        zoomAnchored = true;
    }

    private boolean overCard(double mouseX, double mouseY, int x, int y, int width) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + cardH;
    }

    private void paintAt(double mouseX, double mouseY) {
        int tex = MessagePanelTextures.size(borderLayer);
        int x = (int) ((mouseX - canvasX) / cell);
        int y = (int) ((mouseY - canvasY) / cell);
        x = Math.max(0, Math.min(tex - 1, x));
        y = Math.max(0, Math.min(tex - 1, y));
        strokeChanged |= MessagePanelTextures.paint(borderLayer, x, y, erasing ? 0 : (0xFF000000 | paintColor));
    }

    private void openColorPopup(ButtonWidget button) {
        if (colorPopup.isOpen() && colorPopup.anchor() == button) {
            dismissColorPopup();
            return;
        }
        colorPopup.show(button, paintColor, rgb -> {
            paintColor = 0xFF000000 | rgb;
            refreshLabels();
        });
        colorPopup.place(button.getX(), button.getWidth(), button.getY(), button.getHeight(), false, this.width, this.height);
    }

    private void dismissColorPopup() {
        colorPopup.dismiss();
        if (hexField != null && this.getFocused() == hexField) {
            this.setFocused(null);
        }
    }

    @Override
    protected boolean onPriorityClicked(double mouseX, double mouseY, int pointer) {
        if (pointer == POINTER_PRIMARY && colorPopup.isOpen() && colorPopup.contains(mouseX, mouseY)) {
            if (colorPopup.isOverHex(mouseX, mouseY)) {
                return false;
            }
            if (hexField != null && this.getFocused() == hexField) {
                hexField.setFocused(false);
                this.setFocused(null);
            }
            colorPopup.beginDrag(mouseX, mouseY);
            return true;
        }
        if (pointer == POINTER_PRIMARY && colorPopup.isOpen() && !isOnColorButton(mouseX, mouseY)) {
            dismissColorPopup();
        }
        if (pointer == POINTER_PRIMARY && overCard(mouseX, mouseY, fillCardX, fillCardY, fillCardW)) {
            borderLayer = false;
            refreshLabels();
            return true;
        }
        if (pointer == POINTER_PRIMARY && overCard(mouseX, mouseY, borderCardX, borderCardY, borderCardW)) {
            borderLayer = true;
            refreshLabels();
            return true;
        }
        return false;
    }

    private boolean isOnColorButton(double mouseX, double mouseY) {
        return mouseX >= colorButton.getX() && mouseX < colorButton.getX() + colorButton.getWidth()
            && mouseY >= colorButton.getY() && mouseY < colorButton.getY() + colorButton.getHeight();
    }

    @Override
    protected boolean isPointerBlocked(double mouseX, double mouseY) {
        return false;
    }

    @Override
    protected boolean onPointerPressed(double mouseX, double mouseY, int pointer) {
        if (pointer == POINTER_OTHER && overView(mouseX, mouseY)) {
            panning = true;
            return true;
        }
        if (!overCanvas(mouseX, mouseY)) {
            return false;
        }
        if (pointer != POINTER_PRIMARY && pointer != POINTER_SECONDARY) {
            return false;
        }
        painting = true;
        erasing = eraserMode || pointer == POINTER_SECONDARY;
        beginStroke();
        paintAt(mouseX, mouseY);
        return true;
    }

    @Override
    protected boolean onPointerDragged(double mouseX, double mouseY, int pointer, double deltaX, double deltaY) {
        if (pointer == POINTER_PRIMARY && colorPopup.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        if (panning) {
            panX += (int) Math.round(deltaX);
            panY += (int) Math.round(deltaY);
            return true;
        }
        if (!painting) {
            return false;
        }
        paintAt(mouseX, mouseY);
        return true;
    }

    @Override
    protected void onPointerReleased(double mouseX, double mouseY, int pointer) {
        colorPopup.mouseReleased();
        panning = false;
        if (painting) {
            painting = false;
            commitStroke();
        }
    }

    private LayerSnapshot capture(boolean border) {
        return new LayerSnapshot(border, MessagePanelTextures.copyPixels(border));
    }

    private void beginStroke() {
        if (strokeBefore == null) {
            strokeBefore = capture(borderLayer);
            strokeChanged = false;
        }
    }

    private void commitStroke() {
        if (strokeBefore != null && strokeChanged) {
            pushUndo(strokeBefore);
            redo.clear();
            MessagePanelTextures.save(strokeBefore.border());
        }
        strokeBefore = null;
        strokeChanged = false;
    }

    private void remember() {
        pushUndo(capture(borderLayer));
        redo.clear();
    }

    private void pushUndo(LayerSnapshot snapshot) {
        undo.addLast(snapshot);
        while (undo.size() > UNDO_LIMIT) {
            undo.removeFirst();
        }
    }

    private void undo() {
        if (strokeBefore != null) {
            MessagePanelTextures.replace(strokeBefore.border(), strokeBefore.pixels());
            painting = false;
            strokeBefore = null;
            strokeChanged = false;
            return;
        }
        if (undo.isEmpty()) {
            return;
        }
        LayerSnapshot previous = undo.removeLast();
        redo.addLast(capture(previous.border()));
        MessagePanelTextures.replace(previous.border(), previous.pixels());
        borderLayer = previous.border();
        refreshLabels();
    }

    private void redo() {
        if (redo.isEmpty()) {
            return;
        }
        LayerSnapshot next = redo.removeLast();
        undo.addLast(capture(next.border()));
        MessagePanelTextures.replace(next.border(), next.pixels());
        borderLayer = next.border();
        refreshLabels();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (overView(mouseX, mouseY) && verticalAmount != 0) {
            changeZoom(verticalAmount > 0 ? 1 : -1, mouseX, mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected boolean onKeyShortcut(int keyCode, int modifiers) {
        if (hexField != null && this.getFocused() == hexField) {
            return false;
        }
        return handleShortcut(keyCode, modifiers);
    }

    private boolean handleShortcut(int keyCode, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (!ctrl) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_Z && !shift) {
            undo();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Y || (keyCode == GLFW.GLFW_KEY_Z && shift)) {
            redo();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        MessagePanelTextures.save(false);
        MessagePanelTextures.save(true);
        ScreenNavigationCompat.open(this.client, this.parent);
    }
}
