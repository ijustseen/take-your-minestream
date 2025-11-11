# Руководство по обновлению мода на Minecraft 1.21.10

## 📋 Содержание

1. [Быстрый старт](#быстрый-старт)
2. [Что было исправлено](#что-было-исправлено)
3. [Изменения в API](#изменения-в-api)
4. [Кастомный рендеринг](#кастомный-рендеринг)
5. [Примеры использования](#примеры-использования)
6. [Решение проблем](#решение-проблем)

---

## Быстрый старт

### ✅ Статус: Проект готов к запуску на Minecraft 1.21.10

```bash
# Сборка
./gradlew build

# Запуск
./gradlew runClient
```

### Что было сделано

1. ✅ Обновлены версии зависимостей (1.21.7 → 1.21.10)
2. ✅ Исправлены импорты рендеринга (добавлен `.world`)
3. ✅ Обновлены API методы (WorldRenderContext, Player, GUI)
4. ✅ Добавлена поддержка нового типа `Click` в GUI
5. ✅ Создан кастомный рендер-пайплайн с примерами

---

## Что было исправлено

### 1. Версии зависимостей (gradle.properties)

```properties
minecraft_version=1.21.10      # было: 1.21.7
yarn_mappings=1.21.10+build.2  # было: 1.21.7+build.6
loader_version=0.17.3          # было: 0.16.14
fabric_version=0.137.0+1.21.10 # было: 0.129.0+1.21.7
```

### 2. Импорты рендеринга

**Файлы:** CustomRenderPipeline.java, WaypointRenderer.java, MessageRenderer.java

```java
// Было:
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

// Стало:
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
```

### 3. API WorldRenderContext

```java
// Было:
MatrixStack matrices = context.matrixStack();
Vec3d camera = context.camera().getPos();

// Стало:
MatrixStack matrices = context.matrices();
MinecraftClient client = MinecraftClient.getInstance();
Vec3d camera = client.gameRenderer.getCamera().getPos();
```

### 4. Player API

```java
// Было:
Vec3d playerPos = client.player.getPos();

// Стало:
Vec3d playerPos = client.player.getEyePos();
```

### 5. KeyBinding Category

```java
// Было:
new KeyBinding(..., "category.takeyourminestream.general")

// Стало:
new KeyBinding(..., KeyBinding.Category.MISC)
```

### 6. GUI Click API

В Minecraft 1.21.10 введен новый record `net.minecraft.client.gui.Click`:

```java
// Было:
@Override
public void onClick(double mouseX, double mouseY) { ... }

@Override
public boolean mouseDragged(double mouseX, double mouseY, int button,
                           double deltaX, double deltaY) { ... }

@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) { ... }

// Стало:
@Override
public void onClick(net.minecraft.client.gui.Click click, boolean rightClick) {
    double mouseX = click.x();
    double mouseY = click.y();
    int button = click.button();
    ...
}

@Override
protected void onDrag(net.minecraft.client.gui.Click click, double deltaX, double deltaY) { ... }

@Override
public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean rightClick) { ... }
```

---

## Изменения в API

### Сводная таблица

| Компонент          | Было                                | Стало                                      | Файлы |
| ------------------ | ----------------------------------- | ------------------------------------------ | ----- |
| WorldRenderEvents  | `v1.WorldRenderEvents`              | `v1.world.WorldRenderEvents`               | 3     |
| WorldRenderContext | `matrixStack()`                     | `matrices()`                               | 3     |
| Camera             | `context.camera().getPos()`         | `client.gameRenderer.getCamera().getPos()` | 3     |
| Player             | `getPos()`                          | `getEyePos()`                              | 1     |
| KeyBinding         | String category                     | `Category.MISC` enum                       | 1     |
| SliderWidget       | `onClick(double, double)`           | `onClick(Click, boolean)`                  | 1     |
| SliderWidget       | `mouseDragged(...)`                 | `onDrag(Click, double, double)`            | 1     |
| Screen             | `mouseClicked(double, double, int)` | `mouseClicked(Click, boolean)`             | 1     |

### Новый тип Click

`net.minecraft.client.gui.Click` - это record с методами:

- `double x()` - координата X мыши
- `double y()` - координата Y мыши
- `int button()` - номер кнопки мыши
- `MouseInput buttonInfo()` - дополнительная информация

---

## Кастомный рендеринг

### Обзор новой системы рендеринга (1.21.1+)

В Minecraft 1.21.1+ рендеринг разделен на две фазы:

1. **Extraction (Извлечение)** - сбор данных для рендеринга
2. **Drawing (Отрисовка)** - фактическая отрисовка на GPU

Это позволяет рендерить предыдущий кадр параллельно с подготовкой следующего.

### Созданные файлы

#### 1. CustomRenderPipeline.java

Базовый пример из документации Fabric:

- Рендерит зеленый куб на координатах (0, 100, 0)
- Куб виден через стены (NO_DEPTH_TEST)
- Демонстрирует двухфазную систему рендеринга

**Тестирование:**

```bash
./gradlew runClient
# В игре: /tp 0 100 0
```

#### 2. WaypointRenderer.java

Расширенный рендерер с подробными комментариями:

- Поддержка множественных waypoint'ов
- Разные цвета и размеры
- Удобные методы добавления/удаления
- Готов к использованию в проекте

### Создание кастомного пайплайна

```java
private static final RenderPipeline MY_PIPELINE = RenderPipelines.register(
    RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation(Identifier.of("mod-id", "pipeline/my_pipeline"))
        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST) // рендерит через стены
        .build()
);
```

### Фаза Extraction

```java
private void extract(WorldRenderContext context) {
    MatrixStack matrices = context.matrices();
    MinecraftClient client = MinecraftClient.getInstance();
    Vec3d camera = client.gameRenderer.getCamera().getPos();

    matrices.push();
    matrices.translate(-camera.x, -camera.y, -camera.z);

    if (buffer == null) {
        buffer = new BufferBuilder(allocator, mode, format);
    }

    // Добавляем вершины
    VertexRendering.drawFilledBox(matrices, buffer,
        x1, y1, z1, x2, y2, z2, r, g, b, a);

    matrices.pop();
}
```

### Фаза Drawing

```java
private void draw(MinecraftClient client, RenderPipeline pipeline) {
    // Строим буфер
    BuiltBuffer builtBuffer = buffer.end();
    BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
    VertexFormat format = drawParameters.format();

    // Загружаем в GPU
    GpuBuffer vertices = uploadToGpu(drawParameters, format, builtBuffer);

    // Выполняем отрисовку
    executeDrawCall(client, pipeline, builtBuffer, drawParameters, vertices, format);

    // Ротируем буфер
    vertexBuffer.rotate();
    buffer = null;
}
```

### Очистка ресурсов

**ВАЖНО:** Обязательно очищайте ресурсы!

```java
// В вашем рендерере
public void close() {
    allocator.close();
    if (vertexBuffer != null) {
        vertexBuffer.close();
        vertexBuffer = null;
    }
}

// Mixin для GameRenderer
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "close", at = @At("TAIL"))
    private void onClose(CallbackInfo ci) {
        YourRenderer.getInstance().close();
    }
}
```

---

## Примеры использования

### Базовый пример - один waypoint

```java
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.modid.rendering.WaypointRenderer;
import takeyourminestream.modid.rendering.WaypointRenderer.Waypoint;

public class MyModClient implements ClientModInitializer {
    private WaypointRenderer waypointRenderer;

    @Override
    public void onInitializeClient() {
        waypointRenderer = new WaypointRenderer();

        // Добавляем зеленый waypoint
        waypointRenderer.addWaypoint(
            Waypoint.green(new Vec3d(0, 100, 0))
        );

        // Регистрируем рендеринг
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            waypointRenderer.render(context);
        });
    }
}
```

### Множественные waypoint'ы

```java
@Override
public void onInitializeClient() {
    waypointRenderer = new WaypointRenderer();

    // Разные цвета
    waypointRenderer.addWaypoint(Waypoint.green(new Vec3d(0, 100, 0)));
    waypointRenderer.addWaypoint(Waypoint.red(new Vec3d(10, 100, 10)));
    waypointRenderer.addWaypoint(Waypoint.blue(new Vec3d(-10, 100, -10)));

    // Кастомный waypoint
    waypointRenderer.addWaypoint(new Waypoint(
        new Vec3d(20, 100, 20),  // позиция
        1f, 1f, 0f,              // желтый цвет (RGB)
        0.7f,                    // прозрачность
        2f                       // размер (в блоках)
    ));

    WorldRenderEvents.AFTER_ENTITIES.register(context -> {
        waypointRenderer.render(context);
    });
}
```

### Динамическое управление

```java
// Добавление waypoint'а
waypointRenderer.addWaypoint(Waypoint.green(position));

// Очистка всех waypoint'ов
waypointRenderer.clearWaypoints();

// Обновление waypoint'ов каждый кадр
WorldRenderEvents.AFTER_ENTITIES.register(context -> {
    waypointRenderer.clearWaypoints();

    // Добавляем waypoint'ы для активных сообщений
    for (Message msg : activeMessages) {
        waypointRenderer.addWaypoint(
            Waypoint.green(msg.getPosition())
        );
    }

    waypointRenderer.render(context);
});
```

### Интеграция с MessageRenderer

```java
public class MessageRenderer {
    private final WaypointRenderer waypointRenderer;

    public MessageRenderer(...) {
        this.waypointRenderer = new WaypointRenderer();

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            // Рендерим сообщения
            renderMessages(context);

            // Добавляем waypoint'ы над сообщениями
            waypointRenderer.clearWaypoints();
            for (Message msg : activeMessages) {
                Vec3d pos = msg.getPosition().add(0, 2, 0);
                waypointRenderer.addWaypoint(new Waypoint(
                    pos,
                    1f, 1f, 0f,  // желтый
                    0.3f,        // прозрачный
                    0.3f         // маленький
                ));
            }
            waypointRenderer.render(context);
        });
    }
}
```

### Отключение тестового waypoint'а

Если не нужен тестовый waypoint из CustomRenderPipeline, в `fabric.mod.json`:

```json
"client": [
  "takeyourminestream.modid.TakeYourMineStreamClient"
  // "takeyourminestream.modid.rendering.CustomRenderPipeline"
]
```

---

## Решение проблем

### Проблема: Ошибки компиляции после обновления

**Решение:** Очистите кэш Gradle:

```bash
./gradlew --stop
rm -rf .gradle/loom-cache build
./gradlew build
```

### Проблема: "cannot find symbol: WorldRenderEvents"

**Решение:** Проверьте импорты - должен быть `.world`:

```java
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
```

### Проблема: "cannot find symbol: matrixStack()"

**Решение:** Используйте `matrices()`:

```java
MatrixStack matrices = context.matrices();
```

### Проблема: "cannot find symbol: camera()"

**Решение:** Используйте `client.gameRenderer.getCamera()`:

```java
MinecraftClient client = MinecraftClient.getInstance();
Vec3d camera = client.gameRenderer.getCamera().getPos();
```

### Проблема: Ошибки с Click в GUI методах

**Решение:** Обновите сигнатуры методов:

```java
// Для SliderWidget
@Override
public void onClick(net.minecraft.client.gui.Click click, boolean rightClick) {
    double x = click.x();
    double y = click.y();
    ...
}

// Для Screen
@Override
public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean rightClick) {
    int button = click.button();
    ...
}
```

### Проблема: Утечка памяти GPU

**Решение:** Обязательно вызывайте `close()`:

```java
public void close() {
    allocator.close();
    if (vertexBuffer != null) {
        vertexBuffer.close();
        vertexBuffer = null;
    }
}
```

И добавьте mixin для GameRenderer (см. раздел "Очистка ресурсов").

### Проблема: Низкая производительность

**Решение:**

1. Переиспользуйте BufferBuilder (не создавайте новый каждый кадр)
2. Используйте MappableRingBuffer для автоматического управления
3. Группируйте объекты по пайплайну
4. Очищайте неиспользуемые waypoint'ы

---

## Дополнительная информация

### Совместимость

- ✅ Minecraft 1.21.10
- ✅ Fabric Loader 0.17.3+
- ✅ Fabric API 0.137.0+
- ✅ Java 21

Код также должен работать на других версиях 1.21.x с минимальными изменениями.

### Полезные ссылки

- [Fabric Rendering Documentation](https://docs.fabricmc.net/develop/rendering/world)
- [Rendering Concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts)
- [Fabric API Changelog](https://github.com/FabricMC/fabric/blob/1.21/CHANGELOG.md)

### Структура проекта

```
src/client/java/takeyourminestream/modid/
├── rendering/
│   ├── CustomRenderPipeline.java    # Базовый пример
│   └── WaypointRenderer.java        # Расширенный рендерер
├── mixin/client/
│   └── GameRendererMixin.java       # Очистка ресурсов
├── messages/
│   ├── MessageRenderer.java         # Обновлен для 1.21.10
│   └── MessagePosition.java         # Обновлен для 1.21.10
├── input/
│   └── KeyBindingManager.java       # Обновлен для 1.21.10
└── widget/
    └── MessageScaleSliderWidget.java # Обновлен для 1.21.10
```

---

## Итог

Проект полностью адаптирован для Minecraft 1.21.10:

- ✅ Все зависимости обновлены
- ✅ Все API изменения учтены
- ✅ Добавлена поддержка новой системы рендеринга
- ✅ Созданы примеры использования
- ✅ 0 ошибок компиляции

**Готово к запуску!** 🚀

```bash
./gradlew runClient
```
