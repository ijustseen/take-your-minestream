package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import takeyourminestream.ijustseen.ui.widget.ConfigIntTextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageHistoryActions;
import takeyourminestream.ijustseen.messages.MessageLifecycleManager;
import takeyourminestream.ijustseen.messages.PinnedMessageStore;
import takeyourminestream.ijustseen.ui.gui.ConfirmResetButton;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.GuiLayerFlush;
import takeyourminestream.ijustseen.ui.gui.HistoryMessageActionPopup;
import takeyourminestream.ijustseen.ui.gui.MessageCardLayout;
import takeyourminestream.ijustseen.ui.gui.MessageCardRenderer;

import java.util.ArrayList;
import java.util.List;

/** Экран истории сообщений с контекстным меню действий. */
public class MessageHistoryScreen extends DimScreen {
    private static final int LIST_PADDING = 10;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 58;
    private static final int HISTORY_SIZE_FIELD_WIDTH = 48;
    private static final int SCROLL_NONE = 0;
    private static final int SCROLL_HISTORY = 1;
    private static final int SCROLL_PINNED = 2;
    private static final int CARD_SPACING = 6;
    private static final int COLUMN_GAP = 12;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 4;

    private final @Nullable Screen parent;
    private final MessageLifecycleManager lifecycleManager;
    private final BlockedUsernameManager blockedUsernameManager = BlockedUsernameManager.getInstance();
    private final HistoryMessageActionPopup actionPopup = new HistoryMessageActionPopup();

    // Открываем историю прокрученной вниз — к самым свежим сообщениям
    private int historyScrollOffset = Integer.MAX_VALUE;
    private int pinnedScrollOffset = 0;
    private int contentMaxWidth = 0;
    private int panelWidth = 0;
    private int groupLeft = 0;
    private int groupWidth = 0;
    private int historyPanelLeft = 0;
    private int pinnedPanelLeft = 0;
    private int contentTop = 0;
    private int contentBottom = 0;

    private HistoryCard selectedCard;
    private HistoryCard hoveredCard;
    private ConfigIntTextFieldWidget historySizeField;
    private int historySizeLabelX;
    private int historySizeLabelY;
    private int draggingColumn = SCROLL_NONE;
    private int dragThumbGrab;
    private String feedbackKey;
    private Object[] feedbackArgs;
    private long feedbackUntilMs;

    private long cachedFirstMessageId = -1L;
    private long cachedLastMessageId = -1L;
    private long cachedPinnedSignature = -1L;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private final List<HistoryCard> historyCards = new ArrayList<>();
    private final List<HistoryCard> pinnedCards = new ArrayList<>();

    public MessageHistoryScreen(@Nullable Screen parent, MessageLifecycleManager lifecycleManager) {
        super(Text.translatable("takeyourstreamchat.history.title"));
        this.parent = parent;
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    protected void init() {
        updateLayout();

        int centerX = this.width / 2;
        int buttonY = this.height - 28;
        int buttonWidth = 88;
        int buttonSpacing = 6;
        int rowWidth = buttonWidth * 3 + buttonSpacing * 2;
        int rowLeft = centerX - rowWidth / 2;

        historySizeField = new ConfigIntTextFieldWidget(
            this.textRenderer,
            0,
            0,
            HISTORY_SIZE_FIELD_WIDTH,
            20,
            Text.translatable("takeyourstreamchat.config.message_history_max"),
            "messageHistoryMaxSize",
            10,
            500
        );
        this.addDrawableChild(historySizeField);
        placeHistorySizeField();

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("takeyourstreamchat.history.clear"),
            btn -> {
                lifecycleManager.clearMessageHistory();
                actionPopup.dismiss();
                updateLayout();
            }
        ).dimensions(rowLeft, buttonY, buttonWidth, 20).build());

        this.addDrawableChild(ConfirmResetButton.create(
            rowLeft + buttonWidth + buttonSpacing,
            buttonY,
            buttonWidth,
            20,
            Text.translatable("takeyourstreamchat.history.clear_pinned"),
            () -> {
                PinnedMessageStore.clearCurrentWorld(lifecycleManager);
                actionPopup.dismiss();
                updateLayout();
            }
        ));

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("takeyourstreamchat.history.close"),
            btn -> this.close()
        ).dimensions(rowLeft + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, 20).build());
    }

    private static int computeColumnPanelWidth() {
        return LIST_PADDING * 2
            + SCROLLBAR_WIDTH
            + SCROLLBAR_MARGIN
            + MessagePanelConstants.MESSAGE_WRAP_WIDTH
            + MessagePanelConstants.PADDING_X * 2;
    }

    private int getListContentLeft(int panelLeft) {
        return panelLeft + LIST_PADDING;
    }

    private int getListContentWidth() {
        return panelWidth - LIST_PADDING * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
    }

    private long computePinnedSignature() {
        long signature = 0L;
        for (Message message : lifecycleManager.getAllPinnedMessages()) {
            signature = signature * 31L + message.getId();
        }
        return signature;
    }

    private boolean needsLayoutRefresh() {
        if (this.width != cachedWidth || this.height != cachedHeight) {
            return true;
        }
        long firstId = lifecycleManager.peekHistoryFirstId();
        long lastId = lifecycleManager.peekHistoryLastId();
        if (firstId != cachedFirstMessageId || lastId != cachedLastMessageId) {
            return true;
        }
        return computePinnedSignature() != cachedPinnedSignature;
    }

    private void updateLayout() {
        int previousHistoryHeight = getTotalContentHeight(historyCards);
        int previousPinnedHeight = getTotalContentHeight(pinnedCards);
        boolean firstHistoryOpen = historyCards.isEmpty() && historyScrollOffset == Integer.MAX_VALUE;

        cachedWidth = this.width;
        cachedHeight = this.height;
        historyCards.clear();
        pinnedCards.clear();

        if (this.textRenderer == null) {
            return;
        }

        panelWidth = Math.min(
            computeColumnPanelWidth(),
            Math.max(computeColumnPanelWidth(), (this.width - COLUMN_GAP - 16) / 2)
        );
        groupWidth = panelWidth * 2 + COLUMN_GAP;
        groupLeft = Math.max(8, (this.width - groupWidth) / 2);
        historyPanelLeft = groupLeft;
        pinnedPanelLeft = groupLeft + panelWidth + COLUMN_GAP;
        contentTop = HEADER_HEIGHT;
        contentBottom = this.height - FOOTER_HEIGHT;
        contentMaxWidth = MessageCardLayout.getWrapWidth(getListContentWidth());
        int visibleHeight = Math.max(0, getVisibleListHeight());
        boolean stickHistoryToBottom = firstHistoryOpen
            || historyScrollOffset == Integer.MAX_VALUE
            || (previousHistoryHeight > visibleHeight
                && historyScrollOffset >= previousHistoryHeight - visibleHeight - 4);
        boolean stickPinnedToBottom = previousPinnedHeight > visibleHeight
            && pinnedScrollOffset >= previousPinnedHeight - visibleHeight - 4;

        List<Message> allMessages = lifecycleManager.getAllMessages();
        cachedFirstMessageId = allMessages.isEmpty() ? -1L : allMessages.get(0).getId();
        cachedLastMessageId = allMessages.isEmpty() ? -1L : allMessages.get(allMessages.size() - 1).getId();
        cachedPinnedSignature = computePinnedSignature();

        buildCardList(allMessages, getListContentLeft(historyPanelLeft), historyCards);
        buildCardList(lifecycleManager.getAllPinnedMessages(), getListContentLeft(pinnedPanelLeft), pinnedCards);

        int historyHeight = getTotalContentHeight(historyCards);
        int pinnedHeight = getTotalContentHeight(pinnedCards);
        if (stickHistoryToBottom) {
            historyScrollOffset = GuiScrollbar.clampScroll(Integer.MAX_VALUE, historyHeight, visibleHeight);
        } else {
            historyScrollOffset = GuiScrollbar.clampScroll(historyScrollOffset, historyHeight, visibleHeight);
        }

        if (stickPinnedToBottom) {
            pinnedScrollOffset = GuiScrollbar.clampScroll(Integer.MAX_VALUE, pinnedHeight, visibleHeight);
        } else {
            pinnedScrollOffset = GuiScrollbar.clampScroll(pinnedScrollOffset, pinnedHeight, visibleHeight);
        }
        placeHistorySizeField();
    }

    private void buildCardList(List<Message> messages, int listLeft, List<HistoryCard> target) {
        int y = contentTop + LIST_PADDING;
        for (Message message : messages) {
            ChatMessageParser.ParsedMessage parsed = message.getParsed();
            MessageCardLayout.Layout layout = message.getHistoryLayout(this.textRenderer, contentMaxWidth);
            if (lifecycleManager.isPinnedInWorld(message)) {
                y += MessagePanelConstants.PIN_ICON_OVERFLOW;
            }
            target.add(new HistoryCard(message, parsed, layout, listLeft, y));
            y += layout.height() + CARD_SPACING;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (needsLayoutRefresh()) {
            updateLayout();
        }

        var hiddenButtons = ScreenUiHelper.hideButtons(this);
        if (historySizeField != null) {
            historySizeField.visible = false;
        }
        super.render(context, mouseX, mouseY, delta);
        ScreenUiHelper.restoreButtons(hiddenButtons);
        if (historySizeField != null) {
            historySizeField.visible = true;
        }

        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width);

        Text historyCountText = Text.translatable("takeyourstreamchat.history.count", lifecycleManager.getMessageHistorySize());
        int historyCountWidth = this.textRenderer.getWidth(historyCountText);
        context.drawText(
            this.textRenderer,
            historyCountText,
            historyPanelLeft + (panelWidth - historyCountWidth) / 2,
            22,
            ModUiTheme.TEXT_MUTED,
            false
        );

        Text pinnedTitleText = Text.translatable(
            "takeyourstreamchat.history.pinned_count",
            lifecycleManager.getAllPinnedMessages().size()
        );
        int pinnedTitleWidth = this.textRenderer.getWidth(pinnedTitleText);
        context.drawText(
            this.textRenderer,
            pinnedTitleText,
            pinnedPanelLeft + (panelWidth - pinnedTitleWidth) / 2,
            22,
            ModUiTheme.TEXT_MUTED,
            false
        );

        renderColumn(context, mouseX, mouseY, historyPanelLeft, historyCards, historyScrollOffset);
        renderColumn(context, mouseX, mouseY, pinnedPanelLeft, pinnedCards, pinnedScrollOffset);

        updateHoverState(mouseX, mouseY);
        renderTooltip(context, mouseX, mouseY);

        renderFeedback(context);
        renderHistorySizeField(context, mouseX, mouseY, delta);
        ScreenUiHelper.renderAllButtons(context, mouseX, mouseY, this);

        if (actionPopup.isOpen()) {
            GuiLayerFlush.renderOverlay(
                context,
                () -> actionPopup.render(context, this.textRenderer, mouseX, mouseY),
                mouseX,
                mouseY,
                delta
            );
        }
    }

    private void renderColumn(
        DrawContext context,
        int mouseX,
        int mouseY,
        int panelLeft,
        List<HistoryCard> cards,
        int scrollOffset
    ) {
        ModUiTheme.drawBorderedPanel(
            context,
            panelLeft,
            contentTop,
            panelWidth,
            contentBottom - contentTop
        );

        int contentLeft = getListContentLeft(panelLeft);
        int contentRight = panelLeft + panelWidth - LIST_PADDING;
        context.enableScissor(
            contentLeft,
            contentTop + LIST_PADDING,
            contentRight,
            contentBottom - LIST_PADDING
        );
        renderCards(context, cards, scrollOffset, contentLeft, panelLeft == pinnedPanelLeft);
        context.disableScissor();

        if (getTotalContentHeight(cards) > getVisibleListHeight()) {
            int scrollTop = contentTop + LIST_PADDING;
            int scrollBottom = contentBottom - LIST_PADDING;
            GuiScrollbar.draw(
                context,
                contentRight - SCROLLBAR_WIDTH,
                scrollTop,
                scrollBottom,
                scrollOffset,
                getTotalContentHeight(cards)
            );
        }
    }

    private void renderCards(
        DrawContext context,
        List<HistoryCard> cards,
        int scrollOffset,
        int contentLeft,
        boolean pinnedColumn
    ) {
        if (cards.isEmpty()) {
            Text emptyText = pinnedColumn
                ? Text.translatable("takeyourstreamchat.history.pinned_empty").formatted(Formatting.GRAY)
                : Text.translatable("takeyourstreamchat.history.empty").formatted(Formatting.GRAY);
            renderWrappedEmptyHint(context, emptyText, contentLeft);
            return;
        }

        for (HistoryCard card : cards) {
            int drawY = card.y - scrollOffset;
            int pinOverflow = lifecycleManager.isPinnedInWorld(card.message) ? MessagePanelConstants.PIN_ICON_OVERFLOW : 0;
            int listTop = contentTop + LIST_PADDING;
            int listBottom = contentBottom - LIST_PADDING;
            if (drawY + card.layout.height() < listTop || drawY - pinOverflow > listBottom) {
                continue;
            }

            boolean visibleInWorld = lifecycleManager.isVisibleInWorld(card.message);
            float alpha = visibleInWorld ? 1.0f : 0.75f;
            boolean hovered = card == hoveredCard && !actionPopup.isOpen();

            MessageCardRenderer.draw(
                context,
                this.textRenderer,
                blockedUsernameManager,
                card.message,
                card.parsed,
                card.layout,
                card.x,
                drawY,
                alpha,
                hovered,
                visibleInWorld,
                lifecycleManager.isPinnedInWorld(card.message)
            );
        }
    }

    private void renderWrappedEmptyHint(DrawContext context, Text emptyText, int contentLeft) {
        int contentWidth = getListContentWidth();
        List<OrderedText> lines = this.textRenderer.wrapLines(emptyText, contentWidth);
        int blockHeight = lines.size() * this.textRenderer.fontHeight;
        int startY = contentTop + (contentBottom - contentTop) / 2 - blockHeight / 2;
        for (int i = 0; i < lines.size(); i++) {
            OrderedText line = lines.get(i);
            int lineWidth = this.textRenderer.getWidth(line);
            context.drawTextWithShadow(
                this.textRenderer,
                line,
                contentLeft + (contentWidth - lineWidth) / 2,
                startY + i * this.textRenderer.fontHeight,
                0xFF888888
            );
        }
    }

    private void updateHoverState(int mouseX, int mouseY) {
        hoveredCard = null;
        if (actionPopup.isOpen() || draggingColumn != SCROLL_NONE) {
            return;
        }

        HistoryCard card = findCardAt(historyCards, historyScrollOffset, mouseX, mouseY);
        if (card != null) {
            hoveredCard = card;
            return;
        }
        hoveredCard = findCardAt(pinnedCards, pinnedScrollOffset, mouseX, mouseY);
    }

    private HistoryCard findCardAt(List<HistoryCard> cards, int scrollOffset, int mouseX, int mouseY) {
        int listTop = contentTop + LIST_PADDING;
        int listBottom = contentBottom - LIST_PADDING;
        if (mouseY < listTop || mouseY > listBottom) {
            return null;
        }

        for (HistoryCard card : cards) {
            int drawY = card.y - scrollOffset;
            if (mouseX >= card.x && mouseX <= card.x + card.layout.width()
                && mouseY >= drawY && mouseY <= drawY + card.layout.height()) {
                return card;
            }
        }
        return null;
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        if (actionPopup.isOpen() || hoveredCard == null) {
            return;
        }
        context.drawTooltip(
            this.textRenderer,
            Text.translatable("takeyourstreamchat.history.click_for_actions"),
            mouseX,
            mouseY
        );
    }

    private void renderFeedback(DrawContext context) {
        if (feedbackKey == null || System.currentTimeMillis() >= feedbackUntilMs) {
            return;
        }
        Text feedback = Text.translatable(feedbackKey, feedbackArgs).formatted(Formatting.GREEN);
        int feedbackWidth = this.textRenderer.getWidth(feedback);
        context.drawTextWithShadow(
            this.textRenderer,
            feedback,
            groupLeft + (groupWidth - feedbackWidth) / 2,
            contentBottom + 2,
            0xFF55FF55
        );
    }

    private boolean handleHistoryMouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (actionPopup.isOpen()) {
            HistoryMessageActionPopup.Entry entry = actionPopup.hitTest(mx, my);
            if (entry != null && selectedCard != null) {
                handleAction(entry, selectedCard);
                actionPopup.dismiss();
                selectedCard = null;
                updateLayout();
                return true;
            }
            if (!actionPopup.contains(mx, my)) {
                actionPopup.dismiss();
                selectedCard = null;
            }
            return true;
        }

        if (beginScrollbarDrag(mouseX, mouseY, button)) {
            return true;
        }

        updateHoverState(mx, my);
        if (hoveredCard != null) {
            selectedCard = hoveredCard;
            actionPopup.open(
                mx,
                my,
                this.width,
                this.height,
                lifecycleManager.isPinnedInWorld(hoveredCard.message),
                hoveredCard.parsed.username() != null
            );
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        blurHistorySizeFieldIfOutside(mouseX, mouseY);
        if (handleHistoryMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragScrollbar(mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingColumn != SCROLL_NONE && button == 0) {
            draggingColumn = SCROLL_NONE;
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleAction(HistoryMessageActionPopup.Entry entry, HistoryCard card) {
        switch (entry) {
            case PIN, UNPIN -> {
                MessageHistoryActions.PinToggleResult result = MessageHistoryActions.togglePin(
                    card.message,
                    lifecycleManager,
                    this.client
                );
                if (result == MessageHistoryActions.PinToggleResult.PINNED) {
                    showFeedback("takeyourstreamchat.history.message_pinned", null);
                } else if (result == MessageHistoryActions.PinToggleResult.UNPINNED) {
                    showFeedback("takeyourstreamchat.history.message_unpinned", null);
                }
            }
            case BLOCK -> {
                String username = card.parsed.username();
                if (username == null) {
                    return;
                }
                if (MessageHistoryActions.blockUser(username, blockedUsernameManager)) {
                    showFeedback("takeyourstreamchat.history.user_blocked", username);
                } else {
                    showFeedback("takeyourstreamchat.history.user_already_blocked", username);
                }
            }
            case REPLAY -> {
                if (MessageHistoryActions.replay(card.message, lifecycleManager, this.client)) {
                    showFeedback("takeyourstreamchat.history.message_replayed", null);
                }
            }
        }
    }

    private void showFeedback(String key, Object arg) {
        feedbackKey = key;
        feedbackArgs = arg != null ? new Object[] { arg } : new Object[0];
        feedbackUntilMs = System.currentTimeMillis() + 2500L;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (actionPopup.isOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            actionPopup.dismiss();
            selectedCard = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (actionPopup.isOpen()) {
            return true;
        }

        int scrollDelta = (int) (verticalAmount * 24);
        if (mouseX >= pinnedPanelLeft && mouseX < pinnedPanelLeft + panelWidth) {
            pinnedScrollOffset = GuiScrollbar.clampScroll(
                pinnedScrollOffset - scrollDelta,
                getTotalContentHeight(pinnedCards),
                getVisibleListHeight()
            );
            return true;
        }
        if (mouseX >= historyPanelLeft && mouseX < historyPanelLeft + panelWidth) {
            historyScrollOffset = GuiScrollbar.clampScroll(
                historyScrollOffset - scrollDelta,
                getTotalContentHeight(historyCards),
                getVisibleListHeight()
            );
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void placeHistorySizeField() {
        if (historySizeField == null || this.textRenderer == null) {
            return;
        }
        Text label = Text.translatable("takeyourstreamchat.config.message_history_max");
        int labelWidth = this.textRenderer.getWidth(label);
        int total = labelWidth + 6 + HISTORY_SIZE_FIELD_WIDTH;
        int x = groupLeft + Math.max(0, (groupWidth - total) / 2);
        int fieldY = this.height - 52;
        historySizeLabelX = x;
        historySizeLabelY = fieldY + (20 - this.textRenderer.fontHeight) / 2;
        historySizeField.setX(x + labelWidth + 6);
        historySizeField.setY(fieldY);
    }

    private void renderHistorySizeField(DrawContext context, int mouseX, int mouseY, float delta) {
        if (historySizeField == null) {
            return;
        }
        placeHistorySizeField();
        Text label = Text.translatable("takeyourstreamchat.config.message_history_max");
        context.drawText(
            this.textRenderer,
            label,
            historySizeLabelX,
            historySizeLabelY,
            ModUiTheme.TEXT_SECONDARY,
            false
        );
        ModUiTheme.drawInputFrame(
            context,
            historySizeField.getX(),
            historySizeField.getY(),
            historySizeField.getWidth(),
            historySizeField.getHeight(),
            historySizeField.isFocused()
        );
        TextFieldWidget widget = historySizeField;
        widget.render(context, mouseX, mouseY, delta);
    }

    private boolean beginScrollbarDrag(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int column = scrollbarColumnAt(mouseX, mouseY);
        if (column == SCROLL_NONE) {
            return false;
        }
        draggingColumn = column;
        int top = contentTop + LIST_PADDING;
        int bottom = contentBottom - LIST_PADDING;
        List<HistoryCard> cards = column == SCROLL_HISTORY ? historyCards : pinnedCards;
        int offset = column == SCROLL_HISTORY ? historyScrollOffset : pinnedScrollOffset;
        int total = getTotalContentHeight(cards);
        int thumbY = GuiScrollbar.thumbTop(top, bottom, offset, total);
        int thumb = GuiScrollbar.thumbHeight(bottom - top, total);
        int my = (int) mouseY;
        dragThumbGrab = my >= thumbY && my < thumbY + thumb ? my - thumbY : thumb / 2;
        applyScrollbarDrag(mouseY);
        setDragging(true);
        return true;
    }

    private boolean dragScrollbar(double mouseY, int button) {
        if (draggingColumn == SCROLL_NONE) {
            return false;
        }
        if (button != 0) {
            return false;
        }
        applyScrollbarDrag(mouseY);
        return true;
    }

    private void blurHistorySizeFieldIfOutside(double mouseX, double mouseY) {
        if (historySizeField == null || !historySizeField.isFocused()) {
            return;
        }
        if (mouseX >= historySizeField.getX()
            && mouseX < historySizeField.getX() + historySizeField.getWidth()
            && mouseY >= historySizeField.getY()
            && mouseY < historySizeField.getY() + historySizeField.getHeight()) {
            return;
        }
        setFocused(null);
    }

    private void applyScrollbarDrag(double mouseY) {
        int top = contentTop + LIST_PADDING;
        int bottom = contentBottom - LIST_PADDING;
        List<HistoryCard> cards = draggingColumn == SCROLL_HISTORY ? historyCards : pinnedCards;
        int total = getTotalContentHeight(cards);
        int scroll = GuiScrollbar.scrollForThumbTop(top, bottom, (int) mouseY - dragThumbGrab, total);
        scroll = GuiScrollbar.clampScroll(scroll, total, Math.max(0, bottom - top));
        if (draggingColumn == SCROLL_HISTORY) {
            historyScrollOffset = scroll;
        } else {
            pinnedScrollOffset = scroll;
        }
    }

    private int scrollbarColumnAt(double mouseX, double mouseY) {
        if (hitColumnScrollbar(historyPanelLeft, historyCards, mouseX, mouseY)) {
            return SCROLL_HISTORY;
        }
        if (hitColumnScrollbar(pinnedPanelLeft, pinnedCards, mouseX, mouseY)) {
            return SCROLL_PINNED;
        }
        return SCROLL_NONE;
    }

    private boolean hitColumnScrollbar(int panelLeft, List<HistoryCard> cards, double mouseX, double mouseY) {
        int top = contentTop + LIST_PADDING;
        int bottom = contentBottom - LIST_PADDING;
        int track = bottom - top;
        if (track <= 0 || getTotalContentHeight(cards) <= track) {
            return false;
        }
        int x = panelLeft + panelWidth - LIST_PADDING - SCROLLBAR_WIDTH;
        return mouseX >= x - 4 && mouseX < x + SCROLLBAR_WIDTH + 4
            && mouseY >= top && mouseY < bottom;
    }

    private int getVisibleListHeight() {
        return contentBottom - contentTop - LIST_PADDING * 2;
    }

    private int getTotalContentHeight(List<HistoryCard> cards) {
        if (cards.isEmpty()) {
            return 0;
        }
        HistoryCard last = cards.get(cards.size() - 1);
        int listTop = contentTop + LIST_PADDING;
        return last.y + last.layout.height() - listTop + LIST_PADDING;
    }

    @Override
    public void close() {
        actionPopup.dismiss();
        if (this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            this.client.setScreen(null);
        }
    }

    private static final class HistoryCard {
        final Message message;
        final ChatMessageParser.ParsedMessage parsed;
        final MessageCardLayout.Layout layout;
        final int x;
        final int y;

        HistoryCard(Message message, ChatMessageParser.ParsedMessage parsed, MessageCardLayout.Layout layout, int x, int y) {
            this.message = message;
            this.parsed = parsed;
            this.layout = layout;
            this.x = x;
            this.y = y;
        }
    }
}
