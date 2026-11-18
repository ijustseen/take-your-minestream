package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;

/** Экран настроек: в мире статичное затемнение, без ванильного blur. */
public abstract class DimScreen extends Screen {
    protected DimScreen(Text title) {
        super(title);
    }

    @Override
    public void renderBackground(DrawContext context) {
        var client = this.client;
        if (client != null && client.world != null) {
            ModUiTheme.drawWorldDim(context, this.width, this.height);
        } else {
            super.renderBackground(context);
        }
    }
}
