package takeyourminestream.ijustseen.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Экран поверх мира без затемнения и размытия; клики и перетаскивание мимо виджетов
 * приходят в версионно-независимые {@code onPointer*}.
 */
public abstract class PreviewPointerScreen extends Screen {
    protected static final int POINTER_PRIMARY = 0;
    protected static final int POINTER_SECONDARY = 1;
    protected static final int POINTER_OTHER = 2;

    protected PreviewPointerScreen(Component title) {
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
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (onPointerDragged(click.x(), click.y(), toPointer(click.button()), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        onPointerReleased(click.x(), click.y(), toPointer(click.button()));
        return super.mouseReleased(click);
    }

    private static int toPointer(int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            return POINTER_PRIMARY;
        }
        return button == InputConstants.MOUSE_BUTTON_RIGHT ? POINTER_SECONDARY : POINTER_OTHER;
    }
}
