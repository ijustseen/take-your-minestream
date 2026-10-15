package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.config.HudAnchor;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.config.ModConfigData;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageEditorPreview;
import takeyourminestream.ijustseen.messages.MessageLifecycleManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.ui.gui.ConfigUiHelper;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;
import takeyourminestream.ijustseen.ui.widget.ConfigIntSliderWidget;
import takeyourminestream.ijustseen.utils.EditorCameraCompat;
import takeyourminestream.ijustseen.utils.ScreenNavigationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Визуальный редактор сообщений: панель настроек поверх мира и живой предпросмотр.
 * Пробные сообщения рисуются обычным рендером мода, реальные на это время скрыты.
 */
public class MessageEditorScreen extends PreviewPointerScreen {
    public enum ViewMode { MESSAGE, AROUND_PLAYER }

    private static final int PANEL_WIDTH = 170;
    private static final int PANEL_MARGIN = 6;
    private static final int PANEL_PADDING = 8;
    private static final int CONTROL_HEIGHT = 20;
    private static final int LABEL_GAP = 11;
    private static final int ROW_HEIGHT = 36;
    private static final int TITLE_HEIGHT = 22;
    private static final int SWATCH_SIZE = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLL_STEP = 18;

    private static final double MIN_DISTANCE = 1.2;
    private static final double MAX_DISTANCE = 8.0;
    private static final double DEFAULT_DISTANCE = 2.6;
    private static final float DEFAULT_ORBIT_PITCH = 25.0f;
    private static final float DRAG_DEGREES_PER_PIXEL = 0.6f;

    private static ViewMode lastViewMode = ViewMode.MESSAGE;
    private static int lastSampleIndex = 0;
    private static MessageSpawnMode lastWorldSpawnMode = MessageSpawnMode.FRONT_OF_PLAYER;

    private record Row(String labelKey, Object widget, BooleanSupplier visible, BooleanSupplier enabled) {}

    private final @Nullable Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private final List<Runnable> labelRefreshers = new ArrayList<>();
    private ButtonWidget doneButton;

    private ViewMode viewMode = lastViewMode;
    private int sampleIndex = lastSampleIndex;
    private double distance = DEFAULT_DISTANCE;
    private float extraYaw;
    private float extraPitch;

    private boolean engaged;
    private float baseYaw;
    private float basePitch;
    private float orbitYaw;
    private float orbitPitch = DEFAULT_ORBIT_PITCH;
    private boolean orbitInitialized;
    private Object savedPerspective;

    private String previewSignature = "";
    private Message focusedMessage;
    private boolean dragging;
    private int scrollOffset;

    public MessageEditorScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourstreamchat.editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();
        rowButtons.clear();
        labelRefreshers.clear();
        colorRows.clear();

        addCycleRow("takeyourstreamchat.config.display_mode", this::displayModeText, this::toggleDisplayKind, () -> true);
        addCycleRow("takeyourstreamchat.editor.view_mode", this::viewModeText, this::toggleViewMode, this::isWorldMode);
        addCycleRow("takeyourstreamchat.editor.sample", () -> currentSample().label(), this::nextSample,
            () -> isWorldMode() && viewMode == ViewMode.MESSAGE);

        addSliderRow("takeyourstreamchat.config.message_scale", "messageSizePercent",
            ModConfigData.MIN_SIZE_PERCENT, ModConfigData.MAX_SIZE_PERCENT, ModConfigData.SIZE_PERCENT_STEP, 100,
            value -> Text.literal(value + "%"), this::isWorldMode, () -> true);
        addSliderRow("takeyourstreamchat.config.hud_scale", "hudSizePercent",
            ModConfigData.MIN_SIZE_PERCENT, ModConfigData.MAX_SIZE_PERCENT, ModConfigData.SIZE_PERCENT_STEP, 100,
            value -> Text.literal(value + "%"), this::isHudMode, () -> true);
        addSliderRow("takeyourstreamchat.config.message_wrap_width", "messageWrapWidth",
            ModConfigData.MIN_WRAP_WIDTH, ModConfigData.MAX_WRAP_WIDTH, ModConfigData.WRAP_WIDTH_STEP, ModConfigData.DEFAULT_WRAP_WIDTH,
            value -> Text.translatable("takeyourstreamchat.config.message_wrap_width.value", value), () -> true, () -> true);

        addToggleRow("takeyourstreamchat.config.show_message_bg", ModConfig::isSHOW_MESSAGE_BACKGROUND,
            ModConfig::setSHOW_MESSAGE_BACKGROUND, () -> true);
        addColorRow("takeyourstreamchat.config.panel_base_color", ModConfig::getPANEL_BASE_COLOR_RGB,
            ModConfig::setPANEL_BASE_COLOR_RGB, ModConfig::isSHOW_MESSAGE_BACKGROUND);
        addToggleRow("takeyourstreamchat.config.panel_border_from_platform", ModConfig::isPANEL_BORDER_FROM_PLATFORM,
            ModConfig::setPANEL_BORDER_FROM_PLATFORM, ModConfig::isSHOW_MESSAGE_BACKGROUND);
        addColorRow("takeyourstreamchat.config.panel_border_color", ModConfig::getPANEL_BORDER_COLOR_RGB,
            ModConfig::setPANEL_BORDER_COLOR_RGB,
            () -> ModConfig.isSHOW_MESSAGE_BACKGROUND() && !ModConfig.isPANEL_BORDER_FROM_PLATFORM());
        addToggleRow("takeyourstreamchat.config.show_role_badges", ModConfig::isSHOW_ROLE_BADGES,
            ModConfig::setSHOW_ROLE_BADGES, () -> true);

        addCycleRow("takeyourstreamchat.config.world_placement", this::worldPlacementText, this::toggleWorldPlacement,
            this::isAroundView);
        addSliderRow("takeyourstreamchat.config.spawn_min_distance", "messageSpawnMinDistance", 1, 64, 1, 2,
            value -> Text.translatable("takeyourstreamchat.editor.blocks", value), this::isAroundView, () -> true);
        addSliderRow("takeyourstreamchat.config.spawn_max_distance", "messageSpawnMaxDistance", 1, 64, 1, 5,
            value -> Text.translatable("takeyourstreamchat.editor.blocks", value), this::isAroundView, () -> true);
        addActionRow("takeyourstreamchat.editor.regenerate", Text.translatable("takeyourstreamchat.editor.regenerate.button"),
            this::rebuildPreview, this::isAroundView);

        addCycleRow("takeyourstreamchat.config.hud_anchor", this::hudAnchorText,
            () -> ModConfig.setHUD_ANCHOR(ModConfig.getHUD_ANCHOR().next()), this::isHudMode);

        doneButton = ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close())
            .dimensions(0, 0, PANEL_WIDTH - PANEL_PADDING * 2, CONTROL_HEIGHT).build();
        this.addDrawableChild(doneButton);

        engage();
    }

    // --- Строки панели ---

    private void addCycleRow(String labelKey, Supplier<Text> text, Runnable action, BooleanSupplier visible) {
        ButtonWidget button = ButtonWidget.builder(text.get(), btn -> {
            action.run();
            btn.setMessage(text.get());
        }).dimensions(0, 0, controlWidth(), CONTROL_HEIGHT).build();
        labelRefreshers.add(() -> button.setMessage(text.get()));
        addButtonRow(labelKey, button, visible, () -> true);
    }

    private void addToggleRow(String labelKey, BooleanSupplier getter, java.util.function.Consumer<Boolean> setter, BooleanSupplier enabled) {
        ButtonWidget button = ButtonWidget.builder(ConfigUiHelper.onOffText(getter.getAsBoolean()), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(ConfigUiHelper.onOffText(getter.getAsBoolean()));
        }).dimensions(0, 0, controlWidth(), CONTROL_HEIGHT).build();
        labelRefreshers.add(() -> button.setMessage(ConfigUiHelper.onOffText(getter.getAsBoolean())));
        addButtonRow(labelKey, button, () -> true, enabled);
    }

    private void addColorRow(String labelKey, IntSupplier getter, IntConsumer setter, BooleanSupplier enabled) {
        ButtonWidget button = ButtonWidget.builder(colorText(getter.getAsInt()), btn -> ScreenNavigationCompat.open(
            this.client,
            new ColorPickerScreen(
                this,
                Text.translatable("takeyourstreamchat.config.color_picker.title"),
                getter.getAsInt(),
                setter
            )
        )).dimensions(0, 0, controlWidth(), CONTROL_HEIGHT).build();
        labelRefreshers.add(() -> button.setMessage(colorText(getter.getAsInt())));
        addButtonRow(labelKey, button, () -> true, enabled);
        colorRows.add(new ColorSwatch(button, getter));
    }

    private record ColorSwatch(ButtonWidget button, IntSupplier rgb) {}

    private final List<ColorSwatch> colorRows = new ArrayList<>();

    private void addActionRow(String labelKey, Text text, Runnable action, BooleanSupplier visible) {
        ButtonWidget button = ButtonWidget.builder(text, btn -> action.run())
            .dimensions(0, 0, controlWidth(), CONTROL_HEIGHT).build();
        addButtonRow(labelKey, button, visible, () -> true);
    }

    private void addButtonRow(String labelKey, ButtonWidget button, BooleanSupplier visible, BooleanSupplier enabled) {
        this.addDrawableChild(button);
        rowButtons.add(button);
        rows.add(new Row(labelKey, button, visible, enabled));
    }

    private void addSliderRow(
        String labelKey, String configKey, int min, int max, int step, int fallback,
        java.util.function.IntFunction<Text> label, BooleanSupplier visible, BooleanSupplier enabled
    ) {
        ConfigIntSliderWidget slider = new ConfigIntSliderWidget(0, 0, controlWidth(), CONTROL_HEIGHT, configKey, min, max, step, fallback, label);
        this.addDrawableChild(slider);
        rows.add(new Row(labelKey, slider, visible, enabled));
    }

    private static Text colorText(int rgb) {
        return Text.literal(String.format("#%06X", rgb & 0xFFFFFF));
    }

    private static int controlWidth() {
        return PANEL_WIDTH - PANEL_PADDING * 2 - SCROLLBAR_WIDTH - 3;
    }

    // --- Режимы ---

    private boolean isHudMode() {
        MessageSpawnMode mode = ModConfig.getMESSAGE_SPAWN_MODE();
        return mode != null && mode.isHud();
    }

    private boolean isWorldMode() {
        return !isHudMode();
    }

    private boolean isAroundView() {
        return isWorldMode() && viewMode == ViewMode.AROUND_PLAYER;
    }

    private void toggleDisplayKind() {
        MessageSpawnMode current = ModConfig.getMESSAGE_SPAWN_MODE();
        if (current != null && !current.isHud()) {
            lastWorldSpawnMode = current;
        }
        ModConfig.setMESSAGE_SPAWN_MODE(current == null
            ? MessageSpawnMode.FRONT_OF_PLAYER
            : current.toggleDisplayKind(lastWorldSpawnMode));
        scrollOffset = 0;
    }

    private void toggleWorldPlacement() {
        MessageSpawnMode current = ModConfig.getMESSAGE_SPAWN_MODE();
        if (current != null && !current.isHud()) {
            ModConfig.setMESSAGE_SPAWN_MODE(current.nextWorldPlacement());
        }
    }

    private void toggleViewMode() {
        viewMode = viewMode == ViewMode.MESSAGE ? ViewMode.AROUND_PLAYER : ViewMode.MESSAGE;
        lastViewMode = viewMode;
        scrollOffset = 0;
    }

    private void nextSample() {
        sampleIndex = (sampleIndex + 1) % MessageEditorPreview.SAMPLES.size();
        lastSampleIndex = sampleIndex;
    }

    private MessageEditorPreview.Sample currentSample() {
        return MessageEditorPreview.SAMPLES.get(Math.floorMod(sampleIndex, MessageEditorPreview.SAMPLES.size()));
    }

    private Text displayModeText() {
        return isHudMode()
            ? Text.translatable("takeyourstreamchat.config.display_mode.hud")
            : Text.translatable("takeyourstreamchat.config.display_mode.world");
    }

    private Text viewModeText() {
        return viewMode == ViewMode.MESSAGE
            ? Text.translatable("takeyourstreamchat.editor.view_mode.message")
            : Text.translatable("takeyourstreamchat.editor.view_mode.around");
    }

    private Text worldPlacementText() {
        return ModConfig.getMESSAGE_SPAWN_MODE() == MessageSpawnMode.AROUND_PLAYER
            ? Text.translatable("takeyourstreamchat.config.around_player")
            : Text.translatable("takeyourstreamchat.config.fop_only");
    }

    private Text hudAnchorText() {
        return Text.translatable("takeyourstreamchat.config.hud_anchor." + ModConfig.getHUD_ANCHOR().getKey());
    }

    // --- Предпросмотр ---

    private @Nullable MessageLifecycleManager lifecycle() {
        MessageSpawner spawner = TakeYourMineStreamClient.getStaticMessageSpawner();
        return spawner != null ? spawner.getLifecycleManager() : null;
    }

    private boolean hasWorld() {
        var client = this.client;
        return client != null && client.player != null && client.world != null;
    }

    private void engage() {
        if (engaged || !hasWorld() || lifecycle() == null) {
            return;
        }
        engaged = true;
        baseYaw = EditorCameraCompat.yaw(this.client.player);
        basePitch = EditorCameraCompat.pitch(this.client.player);
        if (!orbitInitialized) {
            orbitYaw = baseYaw;
            orbitPitch = DEFAULT_ORBIT_PITCH;
            orbitInitialized = true;
        }
        previewSignature = "";
        syncPreview();
    }

    private void disengage() {
        if (!engaged) {
            return;
        }
        engaged = false;
        MessageLifecycleManager lifecycle = lifecycle();
        if (lifecycle != null) {
            lifecycle.stopPreview();
        }
        focusedMessage = null;
        if (this.client != null) {
            if (savedPerspective != null) {
                EditorCameraCompat.restorePerspective(this.client, savedPerspective);
                savedPerspective = null;
            }
            if (this.client.player != null) {
                EditorCameraCompat.setRotation(this.client.player, baseYaw, basePitch);
            }
        }
    }

    @Override
    public void removed() {
        disengage();
        super.removed();
    }

    /** Пересобирает пробные сообщения, когда меняется то, что влияет на их состав или расстановку. */
    private void syncPreview() {
        if (!engaged) {
            return;
        }
        String signature = ModConfig.getMESSAGE_SPAWN_MODE() + "|" + viewMode + "|" + sampleIndex
            + "|" + ModConfig.isSHOW_ROLE_BADGES()
            + "|" + ModConfig.getMESSAGE_SPAWN_MIN_DISTANCE() + "|" + ModConfig.getMESSAGE_SPAWN_MAX_DISTANCE();
        if (!signature.equals(previewSignature)) {
            previewSignature = signature;
            applyCamera();
            rebuildPreview();
        }
        if (focusedMessage != null) {
            MessageEditorPreview.placeInFrontOfCamera(this.client, focusedMessage, distance, previewScreenOffset(), extraYaw, extraPitch);
        } else if (isAroundView()) {
            EditorCameraCompat.setRotation(this.client.player, orbitYaw, orbitPitch);
        }
    }

    private void applyCamera() {
        if (isAroundView()) {
            if (savedPerspective == null) {
                savedPerspective = EditorCameraCompat.capturePerspective(this.client);
            }
            EditorCameraCompat.setThirdPersonBack(this.client);
            EditorCameraCompat.setRotation(this.client.player, orbitYaw, orbitPitch);
        } else {
            if (savedPerspective != null) {
                EditorCameraCompat.restorePerspective(this.client, savedPerspective);
                savedPerspective = null;
            }
            EditorCameraCompat.setRotation(this.client.player, baseYaw, basePitch);
        }
    }

    private void rebuildPreview() {
        MessageLifecycleManager lifecycle = lifecycle();
        if (!engaged || lifecycle == null) {
            return;
        }
        focusedMessage = null;
        List<Message> messages;
        if (isHudMode()) {
            messages = MessageEditorPreview.createHudStack(this.client);
        } else if (viewMode == ViewMode.AROUND_PLAYER) {
            // Позиции «перед игроком» считаются от исходного взгляда, а не от текущего облёта камеры
            EditorCameraCompat.setRotation(this.client.player, baseYaw, basePitch);
            messages = MessageEditorPreview.createAroundPlayer(this.client);
            EditorCameraCompat.setRotation(this.client.player, orbitYaw, orbitPitch);
        } else {
            focusedMessage = MessageEditorPreview.create(this.client, currentSample(),
                net.minecraft.util.math.Vec3d.ZERO, 0, 0, net.minecraft.util.math.Vec3d.ZERO);
            focusedMessage.setBlockCube(true);
            MessageEditorPreview.placeInFrontOfCamera(this.client, focusedMessage, distance, previewScreenOffset(), extraYaw, extraPitch);
            messages = List.of(focusedMessage);
        }
        lifecycle.startPreview(messages);
    }

    /** Центр свободной от панели области в долях полуширины экрана (для сдвига сообщения вбок). */
    private double previewScreenOffset() {
        int areaLeft = panelX() + PANEL_WIDTH;
        int areaRight = this.width;
        if (isPanelOnRight()) {
            areaLeft = 0;
            areaRight = panelX();
        }
        double center = (areaLeft + areaRight) / 2.0;
        double half = this.width / 2.0;
        return half <= 0 ? 0.0 : (center - half) / half;
    }

    private void resetView() {
        distance = DEFAULT_DISTANCE;
        extraYaw = 0;
        extraPitch = 0;
        orbitYaw = baseYaw;
        orbitPitch = DEFAULT_ORBIT_PITCH;
    }

    // --- Ввод ---

    private boolean isOverPanel(double mouseX, double mouseY) {
        int x = panelX();
        return mouseX >= x && mouseX <= x + PANEL_WIDTH
            && mouseY >= PANEL_MARGIN && mouseY <= this.height - PANEL_MARGIN;
    }

    @Override
    protected boolean onPriorityClicked(double mouseX, double mouseY, int pointer) {
        if (pointer != POINTER_PRIMARY || doneButton == null) {
            return false;
        }
        int x = doneButton.getX();
        int y = doneButton.getY();
        if (mouseX >= x && mouseX < x + doneButton.getWidth()
            && mouseY >= y && mouseY < y + doneButton.getHeight()) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isPointerBlocked(double mouseX, double mouseY) {
        if (!isOverPanel(mouseX, mouseY)) {
            return false;
        }
        boolean inContent = mouseY >= contentTop() && mouseY < contentBottom();
        boolean onDone = mouseY >= doneButtonY() && mouseY < doneButtonY() + CONTROL_HEIGHT;
        return !inContent && !onDone;
    }

    @Override
    protected boolean onPointerPressed(double mouseX, double mouseY, int pointer) {
        if (!engaged || isOverPanel(mouseX, mouseY) || isHudMode()) {
            return false;
        }
        if (pointer == POINTER_SECONDARY) {
            resetView();
            return true;
        }
        if (pointer == POINTER_PRIMARY) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onPointerDragged(double mouseX, double mouseY, int pointer, double deltaX, double deltaY) {
        if (!dragging || pointer != POINTER_PRIMARY) {
            return false;
        }
        float dYaw = (float) deltaX * DRAG_DEGREES_PER_PIXEL;
        float dPitch = (float) deltaY * DRAG_DEGREES_PER_PIXEL;
        if (isAroundView()) {
            orbitYaw += dYaw;
            orbitPitch = Math.max(-30.0f, Math.min(80.0f, orbitPitch + dPitch));
        } else {
            extraYaw -= dYaw;
            extraPitch = Math.max(-80.0f, Math.min(80.0f, extraPitch + dPitch));
        }
        return true;
    }

    @Override
    protected void onPointerReleased(double mouseX, double mouseY, int pointer) {
        if (pointer == POINTER_PRIMARY) {
            dragging = false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOverPanel(mouseX, mouseY)) {
            scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) (verticalAmount * SCROLL_STEP)));
            return true;
        }
        if (engaged && focusedMessage != null) {
            distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance - verticalAmount * 0.25));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // --- Раскладка и отрисовка ---

    private boolean isPanelOnRight() {
        HudAnchor anchor = ModConfig.getHUD_ANCHOR();
        return isHudMode() && !anchor.isRight();
    }

    private int panelX() {
        return isPanelOnRight() ? this.width - PANEL_WIDTH - PANEL_MARGIN : PANEL_MARGIN;
    }

    private int contentTop() {
        return PANEL_MARGIN + TITLE_HEIGHT;
    }

    private int doneButtonY() {
        return this.height - PANEL_MARGIN - PANEL_PADDING - CONTROL_HEIGHT;
    }

    private int contentBottom() {
        return doneButtonY() - 4;
    }

    private boolean controlIntersectsContent(int controlY) {
        return controlY + CONTROL_HEIGHT > contentTop() && controlY < contentBottom();
    }

    private int visibleRowCount() {
        int count = 0;
        for (Row row : rows) {
            if (row.visible().getAsBoolean()) {
                count++;
            }
        }
        return count;
    }

    private int totalContentHeight() {
        int count = visibleRowCount();
        return count == 0 ? 0 : count * ROW_HEIGHT - (ROW_HEIGHT - LABEL_GAP - CONTROL_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, totalContentHeight() - (contentBottom() - contentTop()));
    }

    private void layoutRows() {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        int x = panelX() + PANEL_PADDING;
        int y = contentTop() - scrollOffset;
        for (Row row : rows) {
            boolean shown = row.visible().getAsBoolean();
            int controlY = y + LABEL_GAP;
            // Кликабельна только кнопка, целиком лежащая в области прокрутки —
            // иначе хитбокс заходит на «Done» и клик проваливается в строку под ней.
            boolean clickable = shown && controlY >= contentTop() && controlY + CONTROL_HEIGHT <= contentBottom();
            setWidgetState(row.widget(), x, controlY, clickable, row.enabled().getAsBoolean());
            if (shown) {
                y += ROW_HEIGHT;
            }
        }
        doneButton.setPosition(x, doneButtonY());
    }

    private static void setWidgetState(Object widget, int x, int y, boolean visible, boolean enabled) {
        if (widget instanceof ButtonWidget button) {
            button.setPosition(x, y);
            button.visible = visible;
            button.active = enabled;
        } else if (widget instanceof ConfigIntSliderWidget slider) {
            slider.setPosition(x, y);
            slider.visible = visible;
            slider.active = enabled;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (Runnable refresher : labelRefreshers) {
            refresher.run();
        }
        syncPreview();
        layoutRows();

        java.util.Set<ButtonWidget> hiddenButtons = ScreenUiHelper.hideButtons(this);
        List<ConfigIntSliderWidget> hiddenSliders = new ArrayList<>();
        for (Row row : rows) {
            if (row.widget() instanceof ConfigIntSliderWidget slider && slider.visible) {
                slider.visible = false;
                hiddenSliders.add(slider);
            }
        }
        super.render(context, mouseX, mouseY, delta);
        ScreenUiHelper.restoreButtons(hiddenButtons);
        for (ConfigIntSliderWidget slider : hiddenSliders) {
            slider.visible = true;
        }

        int panelX = panelX();
        ModUiTheme.drawBorderedPanel(context, panelX, PANEL_MARGIN, PANEL_WIDTH, this.height - PANEL_MARGIN * 2);
        context.drawTextWithShadow(this.textRenderer, this.title, panelX + PANEL_PADDING, PANEL_MARGIN + 8, ModUiTheme.TEXT_PRIMARY);
        context.fill(panelX + PANEL_PADDING, PANEL_MARGIN + 19, panelX + PANEL_WIDTH - PANEL_PADDING, PANEL_MARGIN + 20, ModUiTheme.ACCENT);

        if (!engaged) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable("takeyourstreamchat.editor.no_world"),
                panelX + PANEL_PADDING, contentTop() + 4, ModUiTheme.TEXT_MUTED);
        }

        context.enableScissor(panelX + 1, contentTop(), panelX + PANEL_WIDTH - 1, contentBottom());
        List<ButtonWidget> visibleButtons = new ArrayList<>();
        boolean pointerInContent = mouseY >= contentTop() && mouseY < contentBottom();
        for (Row row : rows) {
            if (!row.visible().getAsBoolean()) {
                continue;
            }
            if (row.widget() instanceof ButtonWidget button) {
                if (!controlIntersectsContent(button.getY())) {
                    continue;
                }
                drawRowLabel(context, row, button.getX(), button.getY());
                visibleButtons.add(button);
            } else if (row.widget() instanceof ConfigIntSliderWidget widget) {
                if (!controlIntersectsContent(widget.getY())) {
                    continue;
                }
                drawRowLabel(context, row, widget.getX(), widget.getY());
                ModUiTheme.drawInputFrame(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), false);
                widget.render(context, mouseX, mouseY, delta);
            }
        }
        ScreenUiHelper.renderButtons(context, pointerInContent ? mouseX : -1, pointerInContent ? mouseY : -1, visibleButtons, null);

        for (ColorSwatch swatch : colorRows) {
            ButtonWidget button = swatch.button();
            if (!button.visible) {
                continue;
            }
            int sx = button.getX() + button.getWidth() - SWATCH_SIZE - 5;
            int sy = button.getY() + (button.getHeight() - SWATCH_SIZE) / 2;
            context.fill(sx - 1, sy - 1, sx + SWATCH_SIZE + 1, sy + SWATCH_SIZE + 1, ModUiTheme.PANEL_BORDER);
            int alpha = button.active ? 0xFF000000 : 0x60000000;
            context.fill(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, alpha | (swatch.rgb().getAsInt() & 0xFFFFFF));
        }
        context.disableScissor();

        if (maxScroll() > 0) {
            GuiScrollbar.draw(context, panelX + PANEL_WIDTH - PANEL_PADDING - SCROLLBAR_WIDTH,
                contentTop(), contentBottom(), scrollOffset, totalContentHeight());
        }
        ScreenUiHelper.renderButtons(context, mouseX, mouseY, List.of(doneButton), null);

        if (engaged) {
            drawHint(context);
        }
    }

    private void drawRowLabel(DrawContext context, Row row, int controlX, int controlY) {
        int color = row.enabled().getAsBoolean() ? ModUiTheme.TEXT_SECONDARY : ModUiTheme.TEXT_DIM;
        context.drawTextWithShadow(this.textRenderer, Text.translatable(row.labelKey()), controlX, controlY - LABEL_GAP, color);
    }

    private void drawHint(DrawContext context) {
        String key;
        if (isHudMode()) {
            key = "takeyourstreamchat.editor.hint.hud";
        } else if (viewMode == ViewMode.AROUND_PLAYER) {
            key = "takeyourstreamchat.editor.hint.around";
        } else {
            key = "takeyourstreamchat.editor.hint.message";
        }
        Text hint = Text.translatable(key);
        int areaLeft = isPanelOnRight() ? 0 : panelX() + PANEL_WIDTH;
        int areaRight = isPanelOnRight() ? panelX() : this.width;
        int textWidth = this.textRenderer.getWidth(hint);
        int x = (areaLeft + areaRight - textWidth) / 2;
        int y = this.height - PANEL_MARGIN - 14;
        context.fill(x - 4, y - 3, x + textWidth + 4, y + 11, ModUiTheme.OVERLAY_DIM);
        context.drawTextWithShadow(this.textRenderer, hint, x, y, ModUiTheme.TEXT_SECONDARY);
    }

    @Override
    public void close() {
        ScreenNavigationCompat.open(this.client, this.parent);
    }
}
