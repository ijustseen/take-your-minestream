package takeyourminestream.modid.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

/**
 * Пример рендерера waypoint'ов с кастомным пайплайном для Minecraft 1.21.1+
 * 
 * Этот класс демонстрирует:
 * - Создание кастомного рендер-пайплайна
 * - Рендеринг через стены (без depth test)
 * - Управление множественными waypoint'ами
 * - Правильное управление GPU ресурсами
 */
public class WaypointRenderer {
    
    /**
     * Кастомный рендер-пайплайн для рендеринга через стены
     * 
     * Параметры:
     * - POSITION_COLOR_SNIPPET: использует позицию и цвет вершин
     * - TRIANGLE_STRIP: режим отрисовки треугольников
     * - NO_DEPTH_TEST: отключает depth test (рендерит через стены)
     */
    private static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("take-your-minestream", "pipeline/waypoint_through_walls"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    
    // Ресурсы для фазы extraction
    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.CUTOUT_BUFFER_SIZE);
    private BufferBuilder buffer;
    
    // Ресурсы для фазы drawing
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private MappableRingBuffer vertexBuffer;
    
    // Список waypoint'ов для рендеринга
    private final List<Waypoint> waypoints = new ArrayList<>();

    /**
     * Класс для хранения данных waypoint'а
     */
    public static class Waypoint {
        public final Vec3d position;
        public final float red, green, blue, alpha;
        public final float size;
        
        public Waypoint(Vec3d position, float red, float green, float blue, float alpha, float size) {
            this.position = position;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.size = size;
        }
        
        /**
         * Создает зеленый waypoint стандартного размера
         */
        public static Waypoint green(Vec3d position) {
            return new Waypoint(position, 0f, 1f, 0f, 0.5f, 1f);
        }
        
        /**
         * Создает красный waypoint стандартного размера
         */
        public static Waypoint red(Vec3d position) {
            return new Waypoint(position, 1f, 0f, 0f, 0.5f, 1f);
        }
        
        /**
         * Создает синий waypoint стандартного размера
         */
        public static Waypoint blue(Vec3d position) {
            return new Waypoint(position, 0f, 0f, 1f, 0.5f, 1f);
        }
    }

    /**
     * Добавляет waypoint в список для рендеринга
     */
    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
    }
    
    /**
     * Очищает все waypoint'ы
     */
    public void clearWaypoints() {
        waypoints.clear();
    }

    /**
     * Основной метод рендеринга - вызывается из WorldRenderEvents
     * 
     * Выполняет обе фазы:
     * 1. Extraction - добавляет все waypoint'ы в буфер
     * 2. Drawing - отрисовывает буфер на экран
     */
    public void render(WorldRenderContext context) {
        if (waypoints.isEmpty()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Фаза 1: Extraction - собираем данные для рендеринга
        extractWaypoints(context);
        
        // Фаза 2: Drawing - отрисовываем на экран
        drawWaypoints(client, FILLED_THROUGH_WALLS);
    }

    /**
     * ФАЗА EXTRACTION
     * 
     * Добавляет все waypoint'ы в BufferBuilder.
     * Эта фаза собирает все данные, необходимые для рендеринга.
     */
    private void extractWaypoints(WorldRenderContext context) {
        MatrixStack matrices = context.matrices();
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getPos();

        assert matrices != null;
        matrices.push();
        
        // Смещаем относительно камеры
        matrices.translate(-camera.x, -camera.y, -camera.z);

        // Инициализируем буфер если нужно
        if (buffer == null) {
            buffer = new BufferBuilder(
                allocator, 
                FILLED_THROUGH_WALLS.getVertexFormatMode(), 
                FILLED_THROUGH_WALLS.getVertexFormat()
            );
        }

        // Добавляем все waypoint'ы в буфер
        for (Waypoint waypoint : waypoints) {
            Vec3d pos = waypoint.position;
            float halfSize = waypoint.size / 2f;
            
            // Рендерим куб для каждого waypoint'а
            VertexRendering.drawFilledBox(
                matrices, 
                buffer,
                (float)(pos.x - halfSize), (float)(pos.y - halfSize), (float)(pos.z - halfSize),
                (float)(pos.x + halfSize), (float)(pos.y + halfSize), (float)(pos.z + halfSize),
                waypoint.red, waypoint.green, waypoint.blue, waypoint.alpha
            );
        }

        matrices.pop();
    }

    /**
     * ФАЗА DRAWING
     * 
     * Отрисовывает BufferBuilder на экран.
     * Эта фаза выполняет фактическую отрисовку на GPU.
     */
    private void drawWaypoints(MinecraftClient client, RenderPipeline pipeline) {
        // Строим буфер (завершаем фазу extraction)
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();

        // Загружаем данные в GPU
        GpuBuffer vertices = uploadToGpu(drawParameters, format, builtBuffer);

        // Выполняем отрисовку
        executeDrawCall(client, pipeline, builtBuffer, drawParameters, vertices, format);

        // Ротируем vertex buffer для избежания конфликтов с GPU
        vertexBuffer.rotate();
        buffer = null;
    }

    /**
     * Загружает данные вершин в GPU буфер
     */
    private GpuBuffer uploadToGpu(BuiltBuffer.DrawParameters drawParameters, VertexFormat format, BuiltBuffer builtBuffer) {
        // Вычисляем необходимый размер буфера
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Создаем или изменяем размер vertex buffer
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            vertexBuffer = new MappableRingBuffer(
                () -> "take-your-minestream waypoint renderer", 
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, 
                vertexBufferSize
            );
        }

        // Копируем данные в GPU буфер
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), 
                false, 
                true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    /**
     * Выполняет draw call на GPU
     */
    private static void executeDrawCall(MinecraftClient client, RenderPipeline pipeline, 
                                       BuiltBuffer builtBuffer, BuiltBuffer.DrawParameters drawParameters, 
                                       GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        // Подготавливаем index buffer в зависимости от режима отрисовки
        if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
            // Для квадов: сортируем и загружаем индексы
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().getVertexSorter());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
            indexType = builtBuffer.getDrawParameters().indexType();
        } else {
            // Для других режимов: используем sequential buffer
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(
                pipeline.getVertexFormatMode()
            );
            indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.getIndexType();
        }

        // Подготавливаем uniform'ы для шейдера
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(
                    RenderSystem.getModelViewMatrix(), 
                    COLOR_MODULATOR, 
                    new Vector3f(), 
                    RenderSystem.getTextureMatrix(), 
                    1f
                );
        
        // Создаем render pass и выполняем отрисовку
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> "take-your-minestream waypoint rendering", 
                    client.getFramebuffer().getColorAttachmentView(), 
                    OptionalInt.empty(), 
                    client.getFramebuffer().getDepthAttachmentView(), 
                    OptionalDouble.empty()
                )) {
            
            // Устанавливаем пайплайн
            renderPass.setPipeline(pipeline);

            // Привязываем uniform'ы
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            // Привязываем буферы
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // Выполняем отрисовку
            renderPass.drawIndexed(
                0 / format.getVertexSize(),  // base vertex
                0,                            // base index
                drawParameters.indexCount(),  // index count
                1                             // instance count
            );
        }

        // Освобождаем built buffer
        builtBuffer.close();
    }

    /**
     * Очистка ресурсов
     * ВАЖНО: Должна вызываться при закрытии GameRenderer
     */
    public void close() {
        allocator.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
