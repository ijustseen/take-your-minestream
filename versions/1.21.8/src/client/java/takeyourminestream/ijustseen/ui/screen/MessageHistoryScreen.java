package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
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
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.HistoryMessageActionPopup;
import takeyourminestream.ijustseen.ui.gui.MessageCardLayout;
import takeyourminestream.ijustseen.ui.gui.MessageCardRenderer;
import takeyourminestream.ijustseen.ui.gui.TwitchToggleHelper;

import java.util.ArrayList;
import java.util.List;

/** Экран истории сообщений с контекстным меню действий. */
public class MessageHistoryScreen extends Screen {
    private static final int SIDE_PADDING = 16;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 36;
    private static final int CARD_SPACING = 6;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 4;

    private final @Nullable Screen parent;
    private final MessageLifecycleManager lifecycleManager;
    private final BlockedUsernameManager blockedUsernameManager = BlockedUsernameManager.getInstance();
    private final HistoryMessageActionPopup actionPopup = new HistoryMessageActionPopup();

    private int scrollOffset = 0;
    private int contentMaxWidth = 0;
    private int contentLeft = 0;
    private int contentTop = 0;
    private int contentBottom = 0;

    private HistoryCard selectedCard;
    private HistoryCard hoveredCard;
    private String feedbackKey;
    private Object[] feedbackArgs;
    private long feedbackUntilMs;

    private int cachedHistorySize = -1;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private final List<HistoryCard> historyCards = new ArrayList<>();

    public MessageHistoryScreen(@Nullable Screen parent, MessageLifecycleManager lifecycleManager) {
        super(Text.translatable("takeyourminestream.history.title"));
        this.parent = parent;
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    protected void init() {
        updateLayout();

        int centerX = this.width / 2;
        int buttonY = this.height - FOOTER_HEIGHT + 8;
        int buttonWidth = 100;
        int buttonSpacing = 10;

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("takeyourminestream.history.clear"),
            btn -> {
                lifecycleManager.clearMessageHistory();
                actionPopup.close();
                updateLayout();
            }
        ).dimensions(centerX - buttonWidth * 3 / 2 - buttonSpacing, buttonY, buttonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            TwitchToggleHelper.buttonLabel(),
            btn -> {
                TwitchToggleHelper.toggle();
                btn.setMessage(TwitchToggleHelper.buttonLabel());
            }
        ).dimensions(centerX - buttonWidth / 2, buttonY, buttonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("takeyourminestream.history.close"),
            btn -> this.close()
        ).dimensions(centerX + buttonWidth / 2 + buttonSpacing, buttonY, buttonWidth, 20).build());
    }

    private boolean needsLayoutRefresh() {
        int historySize = lifecycleManager.getMessageHistorySize();
        if (historySize != cachedHistorySize || this.width != cachedWidth || this.height != cachedHeight) {
            return true;
        }
        return historyCards.isEmpty() && historySize > 0;
    }

    private void updateLayout() {
        cachedHistorySize = lifecycleManager.getMessageHistorySize();
        cachedWidth = this.width;
        cachedHeight = this.height;
        historyCards.clear();

        if (this.textRenderer == null) {
            return;
        }

        contentLeft = SIDE_PADDING;
        contentTop = HEADER_HEIGHT;
        contentBottom = this.height - FOOTER_HEIGHT;
        contentMaxWidth = this.width - SIDE_PADDING * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;

        List<Message> allMessages = lifecycleManager.getAllMessages();
        int y = contentTop + MessageCardLayout.USERNAME_ROW_HEIGHT / 2;

        for (int i = 0; i < allMessages.size(); i++) {
            Message message = allMessages.get(i);
            ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(message.getText());
            MessageCardLayout.Layout layout = MessageCardLayout.compute(textRenderer, message.getText(), contentMaxWidth);
            if (lifecycleManager.isPinnedInWorld(message)) {
                y += MessagePanelConstants.PIN_ICON_OVERFLOW;
            }
            historyCards.add(new HistoryCard(message, parsed, layout, contentLeft, y));
            y += layout.height() + CARD_SPACING;
        }

        scrollOffset = GuiScrollbar.clampScroll(scrollOffset, getTotalContentHeight(), contentBottom - contentTop);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (needsLayoutRefresh()) {
            updateLayout();
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        context.drawText(
            this.textRenderer,
            Text.translatable("takeyourminestream.history.count", lifecycleManager.getMessageHistorySize()),
            SIDE_PADDING,
            22,
            0xFFAAAAAA,
            false
        );

        context.enableScissor(contentLeft, contentTop, this.width - SIDE_PADDING, contentBottom);
        renderHistoryCards(context, mouseX, mouseY);
        context.disableScissor();

        if (getTotalContentHeight() > contentBottom - contentTop) {
            GuiScrollbar.draw(
                context,
                this.width - SIDE_PADDING - SCROLLBAR_WIDTH,
                contentTop,
                contentBottom,
                scrollOffset,
                getTotalContentHeight()
            );
        }

        renderFooterHint(context);
        updateHoverState(mouseX, mouseY);
        renderTooltip(context, mouseX, mouseY);

        if (actionPopup.isOpen()) {
            context.fill(0, 0, this.width, this.height, 0x40000000);
            actionPopup.render(context, this.textRenderer, mouseX, mouseY);
        }

        renderFeedback(context);
    }

    private void renderHistoryCards(DrawContext context, int mouseX, int mouseY) {
        if (historyCards.isEmpty()) {
            Text emptyText = Text.translatable("takeyourminestream.history.empty").formatted(Formatting.GRAY);
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                emptyText,
                this.width / 2,
                contentTop + (contentBottom - contentTop) / 2 - this.textRenderer.fontHeight / 2,
                0xFF888888
            );
            return;
        }

        for (HistoryCard card : historyCards) {
            int drawY = card.y - scrollOffset;
            int pinOverflow = lifecycleManager.isPinnedInWorld(card.message) ? MessagePanelConstants.PIN_ICON_OVERFLOW : 0;
            if (drawY + card.layout.height() < contentTop || drawY - pinOverflow > contentBottom) {
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

    private void updateHoverState(int mouseX, int mouseY) {
        hoveredCard = null;
        if (actionPopup.isOpen()) {
            return;
        }

        for (HistoryCard card : historyCards) {
            int drawY = card.y - scrollOffset;
            if (mouseX >= card.x && mouseX <= card.x + card.layout.width()
                && mouseY >= drawY && mouseY <= drawY + card.layout.height()) {
                hoveredCard = card;
                return;
            }
        }
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        if (actionPopup.isOpen() || hoveredCard == null) {
            return;
        }
        context.drawTooltip(
            this.textRenderer,
            Text.translatable("takeyourminestream.history.click_for_actions"),
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
            this.width / 2 - feedbackWidth / 2,
            contentBottom + 2,
            0xFF55FF55
        );
    }

    private void renderFooterHint(DrawContext context) {
        Text helpText = Text.translatable("takeyourminestream.history.scroll_hint").formatted(Formatting.DARK_GRAY);
        int textWidth = this.textRenderer.getWidth(helpText);
        context.drawText(
            this.textRenderer,
            helpText,
            this.width / 2 - textWidth / 2,
            this.height - FOOTER_HEIGHT - 12,
            0xFF666666,
            false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (actionPopup.isOpen()) {
            HistoryMessageActionPopup.Entry entry = actionPopup.hitTest(mx, my);
            if (entry != null && selectedCard != null) {
                handleAction(entry, selectedCard);
                actionPopup.close();
                selectedCard = null;
                return true;
            }
            if (!actionPopup.contains(mx, my)) {
                actionPopup.close();
                selectedCard = null;
            }
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

        return super.mouseClicked(mouseX, mouseY, button);
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
                    showFeedback("takeyourminestream.history.message_pinned", null);
                } else if (result == MessageHistoryActions.PinToggleResult.UNPINNED) {
                    showFeedback("takeyourminestream.history.message_unpinned", null);
                }
            }
            case BLOCK -> {
                String username = card.parsed.username();
                if (username == null) {
                    return;
                }
                if (MessageHistoryActions.blockUser(username, blockedUsernameManager)) {
                    showFeedback("takeyourminestream.history.user_blocked", username);
                } else {
                    showFeedback("takeyourminestream.history.user_already_blocked", username);
                }
            }
            case REPLAY -> {
                if (MessageHistoryActions.replay(card.message, lifecycleManager, this.client)) {
                    showFeedback("takeyourminestream.history.message_replayed", null);
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
            actionPopup.close();
            selectedCard = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (actionPopup.isOpen()) {
            return true;
        }
        scrollOffset = GuiScrollbar.clampScroll(
            scrollOffset - (int) (verticalAmount * 24),
            getTotalContentHeight(),
            contentBottom - contentTop
        );
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int getTotalContentHeight() {
        if (historyCards.isEmpty()) {
            return 0;
        }
        HistoryCard last = historyCards.get(historyCards.size() - 1);
        return last.y + last.layout.height() - contentTop;
    }

    @Override
    public void close() {
        actionPopup.close();
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
