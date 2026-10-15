package takeyourminestream.ijustseen.ui.screen;

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onPriorityClicked(mouseX, mouseY, toPointer(button))) {
            return true;
        }
        if (isPointerBlocked(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return onPointerPressed(mouseX, mouseY, toPointer(button));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (onPointerDragged(mouseX, mouseY, toPointer(button), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        onPointerReleased(mouseX, mouseY, toPointer(button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private static int toPointer(int button) {
        if (button == 0) {
            return POINTER_PRIMARY;
        }
        return button == 1 ? POINTER_SECONDARY : POINTER_OTHER;
    }
}
