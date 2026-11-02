package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.MessageEmoteGuiRenderer;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.ui.gui.PlatformBanner;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import takeyourminestream.ijustseen.utils.ScreenNavigationCompat;

import java.util.List;

/** Пошаговая инструкция, как скопировать cookie TikTok для более полного чата. */
public class TikTokCookieHelpScreen extends DimScreen {
    private static final Identifier ICON_BACK =
        Identifier.of("take-your-stream-chat", "textures/gui/icon_back.png");

    private static final int TITLE_Y = 6;
    private static final int BANNER_Y = 22;
    private static final int BANNER_HEIGHT = 34;
    private static final int BANNER_TO_PANEL_GAP = 6;
    private static final int CONTENT_PADDING = 10;
    private static final int PANEL_BOTTOM_MARGIN = 12;
    private static final int FOOTER_BUTTON_HEIGHT = 20;
    private static final int FOOTER_BUTTON_BOTTOM_PADDING = 10;
    private static final int FOOTER_ZONE_HEIGHT = FOOTER_BUTTON_HEIGHT + FOOTER_BUTTON_BOTTOM_PADDING;
    private static final int FOOTER_BUTTON_PADDING = 12;
    private static final int BUTTON_ICON_SIZE = 10;
    private static final int BUTTON_ICON_GAP = 3;
    private static final int CARD_GAP = 8;
    private static final int CARD_PAD = 8;
    private static final int BADGE_SIZE = 16;
    private static final int BADGE_TEXT_GAP = 8;
    private static final int IMAGE_GAP = 8;
    private static final int FRAME_PAD = 3;
    private static final int IMAGE_MAX_WIDTH = 360;
    private static final int TIKTOK_ACCENT = 0xFF000000 | ChatPlatform.TIKTOK.getAccentColorRgb();
    private static final int INTRO_BG = 0x4025F4EE;
    private static final int STEP_BG = 0x402A2438;
    private static final int STEP_BORDER = 0xFF453465;

    private static final Step[] STEPS = {
        new Step("takeyourstreamchat.config.tiktok_cookie_help.step1", "cookie_step_1", 486, 172),
        new Step("takeyourstreamchat.config.tiktok_cookie_help.step2", "cookie_step_2", 2160, 1210),
        new Step("takeyourstreamchat.config.tiktok_cookie_help.step3", "cookie_step_3", 1608, 948),
        new Step("takeyourstreamchat.config.tiktok_cookie_help.step4", "cookie_step_4", 1140, 846),
        new Step("takeyourstreamchat.config.tiktok_cookie_help.step5", null, 0, 0)
    };

    private final @Nullable Screen parent;
    private ButtonWidget backButton;
    private int scrollOffset;

    public TikTokCookieHelpScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourstreamchat.config.tiktok_cookie_help.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        createBackButton();
    }

    private void createBackButton() {
        Text label = Text.translatable("takeyourstreamchat.config.back");
        int width = BUTTON_ICON_SIZE + BUTTON_ICON_GAP
            + this.textRenderer.getWidth(label) + FOOTER_BUTTON_PADDING * 2;
        backButton = ButtonWidget.builder(label, btn -> this.close())
            .dimensions((this.width - width) / 2, getFooterZoneTop(), width, FOOTER_BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(backButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        clampScrollOffset();
        java.util.Set<ButtonWidget> hiddenButtons = ScreenUiHelper.hideButtons(this);
        super.render(context, mouseX, mouseY, delta);
        ScreenUiHelper.restoreButtons(hiddenButtons);

        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width, TITLE_Y);
        PlatformBanner.draw(
            context,
            this.textRenderer,
            ChatPlatform.TIKTOK,
            CONTENT_PADDING,
            BANNER_Y,
            this.width - CONTENT_PADDING * 2,
            BANNER_HEIGHT,
            false
        );

        int panelTop = getMainPanelTop();
        int panelBottom = getMainPanelBottom();
        int scrollBottom = getFooterZoneTop() - 6;
        ModUiTheme.drawBorderedPanel(
            context,
            CONTENT_PADDING,
            panelTop,
            this.width - CONTENT_PADDING * 2,
            panelBottom - panelTop
        );

        int innerLeft = CONTENT_PADDING + 8;
        int innerWidth = this.width - CONTENT_PADDING * 2 - 24;
        context.enableScissor(CONTENT_PADDING, panelTop, this.width - CONTENT_PADDING, scrollBottom);
        renderContent(context, innerLeft, panelTop + CONTENT_PADDING - scrollOffset, innerWidth);
        context.disableScissor();
        GuiScrollbar.draw(
            context,
            this.width - 16,
            panelTop,
            scrollBottom,
            scrollOffset,
            getTotalContentHeight(innerWidth)
        );

        context.fill(
            CONTENT_PADDING + 1,
            scrollBottom,
            this.width - CONTENT_PADDING - 1,
            panelBottom - 1,
            ModUiTheme.PANEL_BG
        );

        if (backButton != null) {
            backButton.setPosition(backButton.getX(), getFooterZoneTop());
            boolean hovered = ModUiTheme.isHovered(
                mouseX,
                mouseY,
                backButton.getX(),
                backButton.getY(),
                backButton.getWidth(),
                backButton.getHeight()
            );
            ModUiTheme.drawButton(
                context,
                this.textRenderer,
                backButton.getX(),
                backButton.getY(),
                backButton.getWidth(),
                backButton.getHeight(),
                Text.empty(),
                hovered,
                true,
                false,
                true
            );
            Text label = Text.translatable("takeyourstreamchat.config.back");
            int textWidth = this.textRenderer.getWidth(label);
            int totalWidth = BUTTON_ICON_SIZE + BUTTON_ICON_GAP + textWidth;
            int startX = backButton.getX() + (backButton.getWidth() - totalWidth) / 2;
            MessageEmoteGuiRenderer.drawGuiIcon(
                context,
                ICON_BACK,
                startX,
                backButton.getY() + (backButton.getHeight() - BUTTON_ICON_SIZE) / 2,
                BUTTON_ICON_SIZE
            );
            context.drawTextWithShadow(
                this.textRenderer,
                label,
                startX + BUTTON_ICON_SIZE + BUTTON_ICON_GAP,
                ModUiTheme.centeredTextY(backButton.getY(), backButton.getHeight(), this.textRenderer.fontHeight),
                hovered ? ModUiTheme.TEXT_PRIMARY : ModUiTheme.TEXT_SECONDARY
            );
        }
    }

    private void renderContent(DrawContext context, int x, int y, int width) {
        y = drawCallout(
            context,
            x,
            y,
            width,
            Text.translatable("takeyourstreamchat.config.tiktok_cookie_help.intro"),
            INTRO_BG,
            TIKTOK_ACCENT,
            ModUiTheme.TEXT_SECONDARY
        ) + CARD_GAP;

        for (int i = 0; i < STEPS.length; i++) {
            y = drawStepCard(context, x, y, width, i + 1, STEPS[i]) + CARD_GAP;
        }

        drawCallout(
            context,
            x,
            y,
            width,
            Text.translatable("takeyourstreamchat.config.tiktok_cookie_help.warning"),
            ModUiTheme.DANGER_BG,
            ModUiTheme.CONNECTION_OFF_BORDER,
            ModUiTheme.TEXT_SECONDARY
        );
    }

    private int drawStepCard(DrawContext context, int x, int y, int width, int number, Step step) {
        int height = measureStepCard(width, step);
        context.fill(x, y, x + width, y + height, STEP_BG);
        context.fill(x, y, x + 2, y + height, TIKTOK_ACCENT);
        context.fill(x, y, x + width, y + 1, STEP_BORDER);
        context.fill(x, y + height - 1, x + width, y + height, STEP_BORDER);
        context.fill(x + width - 1, y, x + width, y + height, STEP_BORDER);

        int innerX = x + CARD_PAD + 2;
        int innerWidth = width - CARD_PAD * 2 - 2;
        int cursorY = y + CARD_PAD;
        drawBadge(context, innerX, cursorY, number);
        int textX = innerX + BADGE_SIZE + BADGE_TEXT_GAP;
        int textWidth = innerWidth - BADGE_SIZE - BADGE_TEXT_GAP;
        int textBottom = drawWrapped(
            context,
            Text.translatable(step.textKey()),
            textX,
            cursorY + Math.max(0, (BADGE_SIZE - this.textRenderer.fontHeight) / 2),
            textWidth,
            ModUiTheme.TEXT_PRIMARY
        );
        cursorY = Math.max(cursorY + BADGE_SIZE, textBottom);

        if (step.textureName() != null) {
            cursorY += IMAGE_GAP;
            int imageWidth = Math.min(innerWidth, IMAGE_MAX_WIDTH);
            int imageHeight = screenshotHeight(imageWidth, step);
            int framedWidth = imageWidth + FRAME_PAD * 2;
            int imageX = innerX + Math.max(0, (innerWidth - framedWidth) / 2);
            drawScreenshot(context, imageX, cursorY, imageWidth, imageHeight, step.textureName());
        }
        return y + height;
    }

    private void drawScreenshot(DrawContext context, int x, int y, int width, int height, String textureName) {
        int frameRight = x + width + FRAME_PAD * 2;
        int frameBottom = y + height + FRAME_PAD * 2;
        context.fill(x, y, frameRight, frameBottom, TIKTOK_ACCENT);
        context.fill(x + 1, y + 1, frameRight - 1, frameBottom - 1, 0xFF1A1624);
        MessageEmoteGuiRenderer.drawGuiImage(
            context,
            Identifier.of("take-your-stream-chat", "textures/gui/tiktok/" + textureName + ".png"),
            x + FRAME_PAD,
            y + FRAME_PAD,
            width,
            height
        );
    }

    private void drawBadge(DrawContext context, int x, int y, int number) {
        context.fill(x, y, x + BADGE_SIZE, y + BADGE_SIZE, TIKTOK_ACCENT);
        String label = Integer.toString(number);
        int labelWidth = this.textRenderer.getWidth(label);
        context.drawTextWithShadow(
            this.textRenderer,
            label,
            x + (BADGE_SIZE - labelWidth) / 2,
            ModUiTheme.centeredTextY(y, BADGE_SIZE, this.textRenderer.fontHeight),
            0xFF101018
        );
    }

    private int drawCallout(DrawContext context, int x, int y, int width, Text text, int bg, int accent, int color) {
        int innerWidth = width - CARD_PAD * 2 - 2;
        int textHeight = wrappedHeight(text, innerWidth);
        int height = textHeight + CARD_PAD * 2;
        context.fill(x, y, x + width, y + height, bg);
        context.fill(x, y, x + 2, y + height, accent);
        drawWrapped(context, text, x + CARD_PAD + 2, y + CARD_PAD, innerWidth, color);
        return y + height;
    }

    private int drawWrapped(DrawContext context, Text text, int x, int y, int width, int color) {
        List<OrderedText> lines = this.textRenderer.wrapLines(text, width);
        for (OrderedText line : lines) {
            context.drawTextWithShadow(this.textRenderer, line, x, y, color);
            y += this.textRenderer.fontHeight + 2;
        }
        return y;
    }

    private int measureStepCard(int width, Step step) {
        int innerWidth = width - CARD_PAD * 2 - 2;
        int textWidth = innerWidth - BADGE_SIZE - BADGE_TEXT_GAP;
        int textHeight = wrappedHeight(Text.translatable(step.textKey()), textWidth);
        int header = Math.max(BADGE_SIZE, textHeight);
        int extra = 0;
        if (step.textureName() != null) {
            int imageWidth = Math.min(innerWidth, IMAGE_MAX_WIDTH);
            extra += IMAGE_GAP + FRAME_PAD * 2 + screenshotHeight(imageWidth, step);
        }
        return CARD_PAD * 2 + header + extra;
    }

    private int getTotalContentHeight(int width) {
        int innerWidth = width - CARD_PAD * 2 - 2;
        int y = wrappedHeight(Text.translatable("takeyourstreamchat.config.tiktok_cookie_help.intro"), innerWidth)
            + CARD_PAD * 2 + CARD_GAP;
        for (int i = 0; i < STEPS.length; i++) {
            y += measureStepCard(width, STEPS[i]) + CARD_GAP;
        }
        return y + wrappedHeight(Text.translatable("takeyourstreamchat.config.tiktok_cookie_help.warning"), innerWidth)
            + CARD_PAD * 2 + CONTENT_PADDING * 2;
    }

    private static int screenshotHeight(int imageWidth, Step step) {
        if (step.textureWidth() <= 0 || step.textureHeight() <= 0) {
            return 1;
        }
        return Math.max(1, imageWidth * step.textureHeight() / step.textureWidth());
    }

    private int wrappedHeight(Text text, int width) {
        return Math.max(this.textRenderer.fontHeight + 2, this.textRenderer.wrapLines(text, width).size() * (this.textRenderer.fontHeight + 2));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelTop = getMainPanelTop();
        int scrollBottom = getFooterZoneTop() - 6;
        if (mouseY < panelTop || mouseY > scrollBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int innerWidth = this.width - CONTENT_PADDING * 2 - 24;
        int maxScroll = Math.max(0, getTotalContentHeight(innerWidth) - (scrollBottom - panelTop));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 24)));
        return true;
    }

    private void clampScrollOffset() {
        int innerWidth = this.width - CONTENT_PADDING * 2 - 24;
        int visible = getFooterZoneTop() - 6 - getMainPanelTop();
        scrollOffset = GuiScrollbar.clampScroll(scrollOffset, getTotalContentHeight(innerWidth), visible);
    }

    private int getMainPanelTop() {
        return BANNER_Y + BANNER_HEIGHT + BANNER_TO_PANEL_GAP;
    }

    private int getMainPanelBottom() {
        return this.height - PANEL_BOTTOM_MARGIN;
    }

    private int getFooterZoneTop() {
        return getMainPanelBottom() - FOOTER_ZONE_HEIGHT;
    }

    @Override
    public void close() {
        ScreenNavigationCompat.open(this.client, this.parent);
    }

    private record Step(
        String textKey,
        @Nullable String textureName,
        int textureWidth,
        int textureHeight
    ) {}
}
