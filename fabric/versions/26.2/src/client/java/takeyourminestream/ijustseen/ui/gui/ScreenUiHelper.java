package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.HashSet;
import java.util.Set;

/** Отрисовка кнопок экрана в стиле ModUiTheme. */
public final class ScreenUiHelper {
    private ScreenUiHelper() {}

    public static Set<Button> hideButtons(Screen screen) {
        Set<Button> hidden = new HashSet<>();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && button.visible) {
                button.visible = false;
                hidden.add(button);
            }
        }
        return hidden;
    }

    public static void restoreButtons(Set<Button> hidden) {
        for (Button button : hidden) {
            button.visible = true;
        }
    }

    public static void renderButtons(
        GuiGraphicsExtractor context,
        int mouseX,
        int mouseY,
        Iterable<Button> buttons,
        Button selectedTab
    ) {
        var textRenderer = Minecraft.getInstance().font;
        for (Button button : buttons) {
            if (!button.visible) {
                continue;
            }
            boolean hovered = button.active && ModUiTheme.isHovered(
                mouseX,
                mouseY,
                button.getX(),
                button.getY(),
                button.getWidth(),
                button.getHeight()
            );
            ModUiTheme.drawButton(
                context,
                textRenderer,
                button.getX(),
                button.getY(),
                button.getWidth(),
                button.getHeight(),
                button.getMessage(),
                hovered,
                button.active,
                button == selectedTab,
                true
            );
        }
    }

    public static void renderAllButtons(GuiGraphicsExtractor context, int mouseX, int mouseY, Screen screen) {
        renderButtons(context, mouseX, mouseY, screen.children().stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .toList(), null);
    }
}
