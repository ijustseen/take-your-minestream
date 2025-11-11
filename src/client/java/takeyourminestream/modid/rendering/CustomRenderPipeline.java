package takeyourminestream.modid.rendering;

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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Пример кастомного рендер-пайплайна для Minecraft 1.21.7
 * Рендерит waypoint через стены
 */
public class CustomRenderPipeline implements ClientModInitializer {
    private static CustomRenderPipeline instance;
    
    // Определяем кастомный рендер-пайплайн
    private static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("take-your-minestream", "pipeline/debug_filled_box_through_walls"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    
    // Фаза extraction
    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.CUTOUT_BUFFER_SIZE);
    private BufferBuilder buffer;
    
    // Фаза drawing
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private MappableRingBuffer vertexBuffer;

    public static CustomRenderPipeline getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        WorldRenderEvents.AFTER_ENTITIES.register(this::extractAndDrawWaypoint);
    }

    private void extractAndDrawWaypoint(WorldRenderContext context) {
        renderWaypoint(context);
        drawFilledThroughWalls(MinecraftClient.getInstance(), FILLED_THROUGH_WALLS);
    }

    /**
     * Фаза extraction - добавляем waypoint в буфер
     */
    private void renderWaypoint(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getPos();

        assert matrices != null;
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, FILLED_THROUGH_WALLS.getVertexFormatMode(), FILLED_THROUGH_WALLS.getVertexFormat());
        }

        // Рендерим зеленый куб на координатах (0, 100, 0)
        VertexRendering.drawFilledBox(matrices, buffer, 0f, 100f, 0f, 1f, 101f, 1f, 0f, 1f, 0f, 0.5f);

        matrices.pop();
    }

    /**
     * Фаза drawing - отрисовываем буфер на экран
     */
    private void drawFilledThroughWalls(MinecraftClient client, RenderPipeline pipeline) {
        // Строим буфер
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

        draw(client, pipeline, builtBuffer, drawParameters, vertices, format);

        // Ротируем vertex buffer чтобы избежать конфликтов с GPU
        vertexBuffer.rotate();
        buffer = null;
    }

    private GpuBuffer upload(BuiltBuffer.DrawParameters drawParameters, VertexFormat format, BuiltBuffer builtBuffer) {
        // Вычисляем размер vertex buffer
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Инициализируем или изменяем размер vertex buffer
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            vertexBuffer = new MappableRingBuffer(
                () -> "take-your-minestream example render pipeline", 
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, 
                vertexBufferSize
            );
        }

        // Копируем данные вершин в vertex buffer
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    private static void draw(MinecraftClient client, RenderPipeline pipeline, BuiltBuffer builtBuffer, 
                           BuiltBuffer.DrawParameters drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
            // Сортируем квады если есть прозрачность
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().getVertexSorter());
            // Загружаем index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
            indexType = builtBuffer.getDrawParameters().indexType();
        } else {
            // Используем общий shape index buffer для не-quad режимов
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.getIndexType();
        }

        // Выполняем отрисовку
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, new Vector3f(), RenderSystem.getTextureMatrix(), 1f);
        
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> "take-your-minestream example render pipeline rendering", 
                    client.getFramebuffer().getColorAttachmentView(), 
                    OptionalInt.empty(), 
                    client.getFramebuffer().getDepthAttachmentView(), 
                    OptionalDouble.empty()
                )) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            // Привязываем текстуру если нужно:
            // Sampler0 используется для текстурных входов в вершинах
            // renderPass.bindSampler("Sampler0", textureView);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // Base vertex - это начальный индекс когда мы копировали данные в vertex buffer деленный на размер вершины
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    /**
     * Очистка ресурсов - должна вызываться при закрытии GameRenderer
     */
    public void close() {
        allocator.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
