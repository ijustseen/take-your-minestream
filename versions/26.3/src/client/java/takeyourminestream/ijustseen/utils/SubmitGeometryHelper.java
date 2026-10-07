package takeyourminestream.ijustseen.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import takeyourminestream.ijustseen.core.render.MessagePanel9Slice;
import takeyourminestream.ijustseen.core.text.LegacySectionText;

/** Отправка кастомной геометрии и текста через SubmitNodeCollector (Minecraft 26.2). */
public final class SubmitGeometryHelper {
    private static final int LIGHT = 0xF000F0;
    private static final int OVERLAY = OverlayTexture.NO_OVERLAY;

    private SubmitGeometryHelper() {}

    public static void submitText(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        FormattedCharSequence text,
        float x,
        float y,
        int color
    ) {
        collector.submitText(
            poseStack,
            x,
            y,
            text,
            true,
            Font.DisplayMode.POLYGON_OFFSET,
            LIGHT,
            color,
            0,
            0
        );
    }

    public static void submitText(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        String text,
        float x,
        float y,
        int color
    ) {
        submitText(collector, poseStack, LegacySectionText.toOrderedText(text), x, y, color);
    }

    public static void submitTexturedQuad(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        Identifier texture,
        boolean textLayer,
        float x0,
        float y0,
        float x1,
        float y1,
        float z,
        float u0,
        float v0,
        float u1,
        float v1,
        float r,
        float g,
        float b,
        float a
    ) {
        RenderType renderType = textLayer
            ? RenderTypes.text(texture)
            : RenderTypes.entityTranslucent(texture);
        submitTexturedQuad(collector, poseStack, renderType, x0, y0, x1, y1, z, u0, v0, u1, v1, r, g, b, a);
    }

    public static void submitTexturedQuad(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        RenderType renderType,
        float x0,
        float y0,
        float x1,
        float y1,
        float z,
        float u0,
        float v0,
        float u1,
        float v1,
        float r,
        float g,
        float b,
        float a
    ) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            addVertex(consumer, pose, x0, y0, z, u0, v0, r, g, b, a);
            addVertex(consumer, pose, x0, y1, z, u0, v1, r, g, b, a);
            addVertex(consumer, pose, x1, y1, z, u1, v1, r, g, b, a);
            addVertex(consumer, pose, x1, y0, z, u1, v0, r, g, b, a);
        });
    }

    public static void submitPanel(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        Identifier texture,
        int x,
        int y,
        int width,
        int height,
        float z,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityTranslucent(texture),
            (pose, consumer) -> writePanelSlices(consumer, pose, x, y, width, height, z, alpha, red, green, blue)
        );
    }

    private static void writePanelSlices(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        int x,
        int y,
        int width,
        int height,
        float z,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        for (MessagePanel9Slice.WorldSlice slice : MessagePanel9Slice.worldSlices(x, y, width, height)) {
            addVertex(consumer, pose, slice.x0(), slice.y0(), z, slice.u0(), slice.v0(), red, green, blue, alpha);
            addVertex(consumer, pose, slice.x0(), slice.y1(), z, slice.u0(), slice.v1(), red, green, blue, alpha);
            addVertex(consumer, pose, slice.x1(), slice.y1(), z, slice.u1(), slice.v1(), red, green, blue, alpha);
            addVertex(consumer, pose, slice.x1(), slice.y0(), z, slice.u1(), slice.v0(), red, green, blue, alpha);
        }
    }

    private static void addVertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        float r,
        float g,
        float b,
        float a
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(OVERLAY)
            .setLight(LIGHT)
            .setNormal(pose, 0, 0, -1);
    }
}
