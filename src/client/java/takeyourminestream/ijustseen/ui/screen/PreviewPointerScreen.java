package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Экран поверх мира без затемнения и размытия; клики и перетаскивание мимо виджетов
 * приходят в версионно-независимые {@code onPointer*}.
 */
public abstract class PreviewPointerScreen extends Screen {
    protected static final int POINTER_PRIMARY = 0;
    protected static final int POINTER_SECONDARY = 1;
    protected static final int POINTER_OTHER = 2;

    protected PreviewPointerScreen(Text title) {
        super(title);
    }

    protected abstract boolean onPointerPressed(double mouseX, double mouseY, int pointer);

    protected abstract boolean onPointerDragged(double mouseX, double mouseY, int pointer, double deltaX, double deltaY);

    protected abstract void onPointerReleased(double mouseX, double mouseY, int pointer);

    /** Клик в этой точке съедается до виджетов (например, по обрезанной прокруткой части панели). */
    protected boolean isPointerBlocked(double mouseX, double mouseY) {
        return false;
    }

    /** Клик обрабатывается раньше дочерних виджетов (закреплённая кнопка поверх прокрутки). */
    protected boolean onPriorityClicked(double mouseX, double mouseY, int pointer) {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (onPriorityClicked(click.x(), click.y(), toPointer(click.button()))) {
            return true;
        }
        if (isPointerBlocked(click.x(), click.y())) {
            return true;
        }
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        return onPointerPressed(click.x(), click.y(), toPointer(click.button()));
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (onPointerDragged(click.x(), click.y(), toPointer(click.button()), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        onPointerReleased(click.x(), click.y(), toPointer(click.button()));
        return super.mouseReleased(click);
    }

    private static int toPointer(int button) {
        if (button == 0) {
            return POINTER_PRIMARY;
        }
        return button == 1 ? POINTER_SECONDARY : POINTER_OTHER;
    }
}
