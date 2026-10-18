package takeyourminestream.ijustseen.core.render;

import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.core.MessagePanelConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Процедурная 3D-рамка сообщения: закруглённые углы, выпуклый бортик и задняя стенка.
 */
public final class MessageFrame3DModel {
    private static final MessageFrame3DModel INSTANCE = new MessageFrame3DModel();

    private List<FrameQuad> cachedQuads;
    private int cachedX;
    private int cachedY;
    private int cachedW;
    private int cachedH;
    private int cachedAlphaBits;
    private int cachedBaseRgb;
    private int cachedBorderRgb;

    public record FrameQuad(
        Identifier textureId,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float u0, float v0,
        float u1, float v1,
        float u2, float v2,
        float u3, float v3,
        float r, float g, float b, float a
    ) {}

    private MessageFrame3DModel() {}

    public static MessageFrame3DModel getInstance() {
        return INSTANCE;
    }

    /** Формирует список 3D-квадов для рамки заданного размера. */
    public List<FrameQuad> buildQuads(
        float targetX,
        float targetY,
        float targetW,
        float targetH,
        float alpha,
        int baseRgb,
        int borderRgb
    ) {
        int ix = Math.round(targetX);
        int iy = Math.round(targetY);
        int iw = Math.round(targetW);
        int ih = Math.round(targetH);
        int alphaBits = Math.round(alpha * 255f);
        if (cachedQuads != null
            && cachedX == ix && cachedY == iy && cachedW == iw && cachedH == ih
            && cachedAlphaBits == alphaBits
            && cachedBaseRgb == baseRgb
            && cachedBorderRgb == borderRgb
        ) {
            return cachedQuads;
        }

        List<FrameQuad> quads = new ArrayList<>();

        float baseR = ((baseRgb >> 16) & 0xFF) / 255.0f;
        float baseG = ((baseRgb >> 8) & 0xFF) / 255.0f;
        float baseB = (baseRgb & 0xFF) / 255.0f;

        float borderR = ((borderRgb >> 16) & 0xFF) / 255.0f;
        float borderG = ((borderRgb >> 8) & 0xFF) / 255.0f;
        float borderB = (borderRgb & 0xFF) / 255.0f;

        float zFrontCanvas = 0.0f;
        float zFrontBorder = 0.6f;
        float zBack = -1.4f;

        MessagePanel9Slice.WorldSlice[] slices = MessagePanel9Slice.worldSlices(ix, iy, iw, ih);

        // 1. Лицевой холст сообщения (9-slice с закруглёнными углами)
        for (MessagePanel9Slice.WorldSlice s : slices) {
            quads.add(new FrameQuad(
                MessagePanelConstants.PANEL_TEXTURE,
                s.x0(), s.y0(), zFrontCanvas,
                s.x0(), s.y1(), zFrontCanvas,
                s.x1(), s.y1(), zFrontCanvas,
                s.x1(), s.y0(), zFrontCanvas,
                s.u0(), s.v0(),
                s.u0(), s.v1(),
                s.u1(), s.v1(),
                s.u1(), s.v0(),
                baseR, baseG, baseB, alpha
            ));
        }

        // 2. Лицевая выпуклая рамка (9-slice с закруглёнными углами, z = +0.6f)
        for (MessagePanel9Slice.WorldSlice s : slices) {
            quads.add(new FrameQuad(
                MessagePanelConstants.PANEL_BORDER_TEXTURE,
                s.x0(), s.y0(), zFrontBorder,
                s.x0(), s.y1(), zFrontBorder,
                s.x1(), s.y1(), zFrontBorder,
                s.x1(), s.y0(), zFrontBorder,
                s.u0(), s.v0(),
                s.u0(), s.v1(),
                s.u1(), s.v1(),
                s.u1(), s.v0(),
                borderR, borderG, borderB, alpha
            ));
        }

        float clampedW = MessagePanel9Slice.clampSize(iw);
        float clampedH = MessagePanel9Slice.clampSize(ih);
        float x0 = targetX;
        float x1 = targetX + MessagePanelConstants.CORNER;
        float x2 = targetX + clampedW - MessagePanelConstants.CORNER;
        float x3 = targetX + clampedW;
        float y0 = targetY;
        float y1 = targetY + MessagePanelConstants.CORNER;
        float y2 = targetY + clampedH - MessagePanelConstants.CORNER;
        float y3 = targetY + clampedH;

        // 3. Задняя стенка картины (1:1 масштаб текстуры дерева без растяжения в центре)
        float backShade = 0.85f;
        float bR = backShade;
        float bG = backShade;
        float bB = backShade;

        // Угловые 8x8 квады (1:1 масштаб)
        // Верхний левый угол
        quads.add(new FrameQuad(
            MessagePanelConstants.FRAME_BACK_TEXTURE,
            x1, y0, zBack,
            x1, y1, zBack,
            x0, y1, zBack,
            x0, y0, zBack,
            8f / 32f, 0f,
            8f / 32f, 8f / 32f,
            0f, 8f / 32f,
            0f, 0f,
            bR, bG, bB, alpha
        ));
        // Верхний правый угол
        quads.add(new FrameQuad(
            MessagePanelConstants.FRAME_BACK_TEXTURE,
            x3, y0, zBack,
            x3, y1, zBack,
            x2, y1, zBack,
            x2, y0, zBack,
            1f, 0f,
            1f, 8f / 32f,
            24f / 32f, 8f / 32f,
            24f / 32f, 0f,
            bR, bG, bB, alpha
        ));
        // Нижний левый угол
        quads.add(new FrameQuad(
            MessagePanelConstants.FRAME_BACK_TEXTURE,
            x1, y2, zBack,
            x1, y3, zBack,
            x0, y3, zBack,
            x0, y2, zBack,
            8f / 32f, 24f / 32f,
            8f / 32f, 1f,
            0f, 1f,
            0f, 24f / 32f,
            bR, bG, bB, alpha
        ));
        // Нижний правый угол
        quads.add(new FrameQuad(
            MessagePanelConstants.FRAME_BACK_TEXTURE,
            x3, y2, zBack,
            x3, y3, zBack,
            x2, y3, zBack,
            x2, y2, zBack,
            1f, 24f / 32f,
            1f, 1f,
            24f / 32f, 1f,
            24f / 32f, 24f / 32f,
            bR, bG, bB, alpha
        ));

        // Верхняя грань (замощение по 16 пикселей с масштабом 1:1)
        for (float cx = x1; cx < x2; cx += 16f) {
            float step = Math.min(16f, x2 - cx);
            quads.add(new FrameQuad(
                MessagePanelConstants.FRAME_BACK_TEXTURE,
                cx + step, y0, zBack,
                cx + step, y1, zBack,
                cx, y1, zBack,
                cx, y0, zBack,
                (8f + step) / 32f, 0f,
                (8f + step) / 32f, 8f / 32f,
                8f / 32f, 8f / 32f,
                8f / 32f, 0f,
                bR, bG, bB, alpha
            ));
        }

        // Нижняя грань (замощение по 16 пикселей с масштабом 1:1)
        for (float cx = x1; cx < x2; cx += 16f) {
            float step = Math.min(16f, x2 - cx);
            quads.add(new FrameQuad(
                MessagePanelConstants.FRAME_BACK_TEXTURE,
                cx + step, y2, zBack,
                cx + step, y3, zBack,
                cx, y3, zBack,
                cx, y2, zBack,
                (8f + step) / 32f, 24f / 32f,
                (8f + step) / 32f, 1f,
                8f / 32f, 1f,
                8f / 32f, 24f / 32f,
                bR, bG, bB, alpha
            ));
        }

        // Левая грань (замощение по 16 пикселей с масштабом 1:1)
        for (float cy = y1; cy < y2; cy += 16f) {
            float step = Math.min(16f, y2 - cy);
            quads.add(new FrameQuad(
                MessagePanelConstants.FRAME_BACK_TEXTURE,
                x1, cy, zBack,
                x1, cy + step, zBack,
                x0, cy + step, zBack,
                x0, cy, zBack,
                8f / 32f, 8f / 32f,
                8f / 32f, (8f + step) / 32f,
                0f, (8f + step) / 32f,
                0f, 8f / 32f,
                bR, bG, bB, alpha
            ));
        }

        // Правая грань (замощение по 16 пикселей с масштабом 1:1)
        for (float cy = y1; cy < y2; cy += 16f) {
            float step = Math.min(16f, y2 - cy);
            quads.add(new FrameQuad(
                MessagePanelConstants.FRAME_BACK_TEXTURE,
                x3, cy, zBack,
                x3, cy + step, zBack,
                x2, cy + step, zBack,
                x2, cy, zBack,
                1f, 8f / 32f,
                1f, (8f + step) / 32f,
                24f / 32f, (8f + step) / 32f,
                24f / 32f, 8f / 32f,
                bR, bG, bB, alpha
            ));
        }

        // Центр задней стенки (сетка 16x16 без растяжения, шаг 1:1)
        for (float cy = y1; cy < y2; cy += 16f) {
            float stepY = Math.min(16f, y2 - cy);
            for (float cx = x1; cx < x2; cx += 16f) {
                float stepX = Math.min(16f, x2 - cx);
                quads.add(new FrameQuad(
                    MessagePanelConstants.FRAME_BACK_TEXTURE,
                    cx + stepX, cy, zBack,
                    cx + stepX, cy + stepY, zBack,
                    cx, cy + stepY, zBack,
                    cx, cy, zBack,
                    (8f + stepX) / 32f, 8f / 32f,
                    (8f + stepX) / 32f, (8f + stepY) / 32f,
                    8f / 32f, (8f + stepY) / 32f,
                    8f / 32f, 8f / 32f,
                    bR, bG, bB, alpha
                ));
            }
        }

        // 4. Боковые внешние 3D-стенки (точный 28-точечный пиксельный контур, идеально совпадающий с 9-slice)
        float[][] perimeter = {
            // Верхняя грань
            {x0 + 4f, y0, 0.95f},
            {x3 - 4f, y0, 0.95f},

            // Верхний правый закруглённый угол (пиксельные ступени текстуры)
            {x3 - 4f, y0 + 1f, 0.85f},
            {x3 - 2f, y0 + 1f, 0.95f},
            {x3 - 2f, y0 + 2f, 0.85f},
            {x3 - 1f, y0 + 2f, 0.95f},
            {x3 - 1f, y0 + 4f, 0.80f},
            {x3,      y0 + 4f, 0.95f},

            // Правая грань
            {x3,      y3 - 4f, 0.80f},

            // Нижний правый закруглённый угол
            {x3 - 1f, y3 - 4f, 0.65f},
            {x3 - 1f, y3 - 2f, 0.80f},
            {x3 - 2f, y3 - 2f, 0.65f},
            {x3 - 2f, y3 - 1f, 0.80f},
            {x3 - 4f, y3 - 1f, 0.65f},
            {x3 - 4f, y3,      0.80f},

            // Нижняя грань
            {x0 + 4f, y3,      0.65f},

            // Нижний левый закруглённый угол
            {x0 + 4f, y3 - 1f, 0.80f},
            {x0 + 2f, y3 - 1f, 0.65f},
            {x0 + 2f, y3 - 2f, 0.80f},
            {x0 + 1f, y3 - 2f, 0.65f},
            {x0 + 1f, y3 - 4f, 0.80f},
            {x0,      y3 - 4f, 0.65f},

            // Левая грань
            {x0,      y0 + 4f, 0.80f},

            // Верхний левый закруглённый угол
            {x0 + 1f, y0 + 4f, 0.95f},
            {x0 + 1f, y0 + 2f, 0.80f},
            {x0 + 2f, y0 + 2f, 0.95f},
            {x0 + 2f, y0 + 1f, 0.80f},
            {x0 + 4f, y0 + 1f, 0.95f}
        };

        float uBorder = 16f / 32f;
        float vBorder = 0.5f / 32f;

        for (int i = 0; i < perimeter.length; i++) {
            float[] pA = perimeter[i];
            float[] pB = perimeter[(i + 1) % perimeter.length];
            float shade = (pA[2] + pB[2]) * 0.5f;

            float r = borderR * shade;
            float g = borderG * shade;
            float b = borderB * shade;

            quads.add(new FrameQuad(
                MessagePanelConstants.PANEL_BORDER_TEXTURE,
                pA[0], pA[1], zFrontBorder,
                pA[0], pA[1], zBack,
                pB[0], pB[1], zBack,
                pB[0], pB[1], zFrontBorder,
                uBorder, vBorder,
                uBorder, vBorder,
                uBorder, vBorder,
                uBorder, vBorder,
                r, g, b, alpha
            ));
        }

        // 5. Внутренний бортик выпуклой рамки (опускается с zFrontBorder=0.6f на холст zFrontCanvas=0.0f)
        float innerShade = 0.70f;
        float inR = borderR * innerShade;
        float inG = borderG * innerShade;
        float inB = borderB * innerShade;

        // Верхний внутренний край
        quads.add(new FrameQuad(
            MessagePanelConstants.PANEL_BORDER_TEXTURE,
            x3 - 4f, y0 + 1f, zFrontBorder,
            x3 - 4f, y0 + 1f, zFrontCanvas,
            x0 + 4f, y0 + 1f, zFrontCanvas,
            x0 + 4f, y0 + 1f, zFrontBorder,
            uBorder, vBorder, uBorder, vBorder, uBorder, vBorder, uBorder, vBorder,
            inR, inG, inB, alpha
        ));
        // Нижний внутренний край
        quads.add(new FrameQuad(
            MessagePanelConstants.PANEL_BORDER_TEXTURE,
            x0 + 4f, y3 - 1f, zFrontBorder,
            x0 + 4f, y3 - 1f, zFrontCanvas,
            x3 - 4f, y3 - 1f, zFrontCanvas,
            x3 - 4f, y3 - 1f, zFrontBorder,
            uBorder, vBorder, uBorder, vBorder, uBorder, vBorder, uBorder, vBorder,
            inR * 0.9f, inG * 0.9f, inB * 0.9f, alpha
        ));
        // Левый внутренний край
        quads.add(new FrameQuad(
            MessagePanelConstants.PANEL_BORDER_TEXTURE,
            x0 + 1f, y0 + 4f, zFrontBorder,
            x0 + 1f, y0 + 4f, zFrontCanvas,
            x0 + 1f, y3 - 4f, zFrontCanvas,
            x0 + 1f, y3 - 4f, zFrontBorder,
            uBorder, vBorder, uBorder, vBorder, uBorder, vBorder, uBorder, vBorder,
            inR, inG, inB, alpha
        ));
        // Правый внутренний край
        quads.add(new FrameQuad(
            MessagePanelConstants.PANEL_BORDER_TEXTURE,
            x3 - 1f, y3 - 4f, zFrontBorder,
            x3 - 1f, y3 - 4f, zFrontCanvas,
            x3 - 1f, y0 + 4f, zFrontCanvas,
            x3 - 1f, y0 + 4f, zFrontBorder,
            uBorder, vBorder, uBorder, vBorder, uBorder, vBorder, uBorder, vBorder,
            inR, inG, inB, alpha
        ));

        cachedQuads = List.copyOf(quads);
        cachedX = ix;
        cachedY = iy;
        cachedW = iw;
        cachedH = ih;
        cachedAlphaBits = alphaBits;
        cachedBaseRgb = baseRgb;
        cachedBorderRgb = borderRgb;
        return cachedQuads;
    }
}
