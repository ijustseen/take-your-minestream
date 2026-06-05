package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;

/** Экран управления чёрным списком ников. */
public class BlockedUsernameConfigScreen extends AbstractStringListScreen {
    private final BlockedUsernameManager blockedUsernameManager = BlockedUsernameManager.getInstance();

    public BlockedUsernameConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourminestream.blocked_users.title"), parent);
    }

    @Override
    protected Text inputPlaceholder() {
        return Text.translatable("takeyourminestream.blocked_users.input");
    }

    @Override
    protected Text addButtonLabel() {
        return Text.translatable("takeyourminestream.blocked_users.add");
    }

    @Override
    protected void reloadEntries() {
        entries.clear();
        entries.addAll(blockedUsernameManager.getBlockedUsernames());
        entries.sort(String::compareTo);
    }

    @Override
    protected void onAdd(String entry) {
        blockedUsernameManager.addBlockedUsername(entry);
    }

    @Override
    protected void onRemove(String entry) {
        blockedUsernameManager.removeBlockedUsername(entry);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var hiddenButtons = takeyourminestream.ijustseen.ui.gui.ScreenUiHelper.hideButtons(this);
        super.render(context, mouseX, mouseY, delta);
        takeyourminestream.ijustseen.ui.gui.ScreenUiHelper.restoreButtons(hiddenButtons);
        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width);
        renderDefaultList(context, mouseX, mouseY, (ctx, entry, textX, y, mx, my) -> {
            ctx.drawText(this.textRenderer, Text.literal(entry), textX, y, ModUiTheme.TEXT_PRIMARY, true);
            drawRemoveButton(ctx, y, mx, my);
        });
        renderInputChrome(context);
        renderThemedChrome(context, mouseX, mouseY);
    }
}
