package takeyourminestream.ijustseen.core.render;

import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.core.MessagePanelConstants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Процедурная 3D-рамка сообщения: закруглённые углы, выпуклый бортик и задняя стенка.
 */
public final class MessageFrame3DModel {
    private static final MessageFrame3DModel INSTANCE = new MessageFrame3DModel();
    /** Передняя грань бортика и задняя стенка, в пикселях панели до мирового масштаба. */
    public static final float Z_FRONT = 0.6f;
    public static final float Z_BACK = -1.4f;

    /** Сообщения разного размера/цвета на экране одновременно — одной ячейки кэша мало. */
    private static final int MAX_CACHED_SHAPES = 512;

    private record CacheKey(
        int x, int y, int w, int h,
        int alphaBits, int baseRgb, int borderRgb,
        int revision, int shape, int borderSize, int fillSize
    ) {}

    private final java.util.LinkedHashMap<CacheKey, List<FrameQuad>> cache =
        new java.util.LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<CacheKey, List<FrameQuad>> eldest) {
                return size() > MAX_CACHED_SHAPES;
            }
        };

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
        int textureRevision = MessagePanelTextures.revision();
        int shape = MessagePanelTextures.shapeStamp();
        int borderSize = MessagePanelTextures.size(true);
        int fillSize = MessagePanelTextures.size(false);
        CacheKey key = new CacheKey(
            ix, iy, iw, ih, alphaBits, baseRgb, borderRgb,
            textureRevision, shape, borderSize, fillSize
        );
        List<FrameQuad> hit = cache.get(key);
        if (hit != null) {
            return hit;
        }

        List<FrameQuad> quads = new ArrayList<>();

        float baseR = ((baseRgb >> 16) & 0xFF) / 255.0f;
        float baseG = ((baseRgb >> 8) & 0xFF) / 255.0f;
        float baseB = (baseRgb & 0xFF) / 255.0f;

        float borderR = ((borderRgb >> 16) & 0xFF) / 255.0f;
        float borderG = ((borderRgb >> 8) & 0xFF) / 255.0f;
        float borderB = (borderRgb & 0xFF) / 255.0f;

        float zFrontCanvas = 0.0f;
        float zFrontBorder = Z_FRONT;
        float zBack = Z_BACK;

        MessagePanel9Slice.WorldSlice[] slices = MessagePanel9Slice.worldSlices(ix, iy, iw, ih);

        // 1. Лицевой холст сообщения (9-slice с закруглёнными углами)
        for (MessagePanel9Slice.WorldSlice s : slices) {
            quads.add(new FrameQuad(
                MessagePanelTextures.panel(),
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

        // 2. Лицевая рамка — тот же nine-slice, что и в плоском виде.
        for (MessagePanel9Slice.WorldSlice s : slices) {
            quads.add(new FrameQuad(
                MessagePanelTextures.border(),
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

        appendTextureDepth(
            quads,
            ix,
            iy,
            iw,
            ih,
            zFrontBorder,
            zFrontCanvas,
            zBack,
            borderR,
            borderG,
            borderB,
            baseR,
            baseG,
            baseB,
            alpha
        );

        // Подряд по текстуре: рендер берёт буфер слоя один раз на группу, а не на каждый квад.
        quads.sort(java.util.Comparator.comparing(q -> q.textureId().toString()));
        List<FrameQuad> built = List.copyOf(quads);
        cache.put(key, built);
        return built;
    }

    /**
     * Боковые стенки повторяют непрозрачный контур заливки и бортика после nine-slice,
     * а не зашитый скруглённый прямоугольник на 32 пикселя.
     */
    private static void appendTextureDepth(
        List<FrameQuad> quads,
        int originX,
        int originY,
        int width,
        int height,
        float zFront,
        float zCanvas,
        float zBack,
        float borderR,
        float borderG,
        float borderB,
        float baseR,
        float baseG,
        float baseB,
        float alpha
    ) {
        width = MessagePanel9Slice.clampSize(width);
        height = MessagePanel9Slice.clampSize(height);
        int step = width > 280 || height > 200 ? 2 : 1;
        int cols = (width + step - 1) / step;
        int rows = (height + step - 1) / step;
        byte[] kind = new byte[cols * rows];
        for (int row = 0; row < rows; row++) {
            int sy = Math.min(height - 1, row * step + step / 2);
            for (int col = 0; col < cols; col++) {
                int sx = Math.min(width - 1, col * step + step / 2);
                if (sampleOpaque(true, sx, sy, width, height)) {
                    kind[row * cols + col] = RAISED;
                } else if (sampleOpaque(false, sx, sy, width, height)) {
                    kind[row * cols + col] = CANVAS;
                }
            }
        }
        floodOutside(kind, cols, rows);
        appendBack(quads, kind, cols, rows, originX, originY, width, height, step, zBack, alpha);

        Grid grid = new Grid(kind, cols, rows, originX, originY, width, height, step);
        float[] border = {borderR, borderG, borderB};
        float[] base = {baseR, baseG, baseB};
        // Стенка по пикселю края — тысячи квадов на сообщение. Соседние рёбра с тем же
        // направлением, типом и цветом текселя склеиваются в один квад: картинка та же.
        for (int dir = 0; dir < 4; dir++) {
            boolean horizontal = dir < 2;
            int lines = horizontal ? rows : cols;
            int span = horizontal ? cols : rows;
            for (int line = 0; line < lines; line++) {
                int runStart = -1;
                int runKind = 0;
                boolean runOnto = false;
                int runArgb = 0;
                float[] runUv = null;
                for (int i = 0; i <= span; i++) {
                    int edgeKind = 0;
                    boolean onto = false;
                    int argb = 0;
                    if (i < span) {
                        int col = horizontal ? i : line;
                        int row = horizontal ? line : i;
                        int here = kind[row * cols + col];
                        if (here == RAISED || here == CANVAS) {
                            int nx = col + DIR_X[dir];
                            int ny = row + DIR_Y[dir];
                            int neighbor = nx < 0 || ny < 0 || nx >= cols || ny >= rows
                                ? OUTSIDE
                                : kind[ny * cols + nx];
                            boolean raised = here == RAISED;
                            if (neighbor != here && (raised || neighbor != RAISED)) {
                                edgeKind = here;
                                onto = raised && neighbor == CANVAS;
                                argb = texel(raised, grid.sampleX(col), grid.sampleY(row), width, height);
                            }
                        }
                    }
                    boolean continues = runStart >= 0 && edgeKind == runKind && onto == runOnto && argb == runArgb;
                    if (continues) {
                        continue;
                    }
                    if (runStart >= 0) {
                        emitWall(quads, grid, dir, line, runStart, i - 1, runKind == RAISED, runOnto, runUv,
                            zFront, zCanvas, zBack, runKind == RAISED ? border : base, alpha);
                        runStart = -1;
                    }
                    if (edgeKind != 0) {
                        int col = horizontal ? i : line;
                        int row = horizontal ? line : i;
                        runStart = i;
                        runKind = edgeKind;
                        runOnto = onto;
                        runArgb = argb;
                        runUv = sampleUv(edgeKind == RAISED, grid.sampleX(col), grid.sampleY(row), width, height);
                    }
                }
            }
        }
    }

    private static final int[] DIR_X = {0, 0, -1, 1};
    private static final int[] DIR_Y = {-1, 1, 0, 0};

    private record Grid(byte[] kind, int cols, int rows, int originX, int originY, int width, int height, int step) {
        int sampleX(int col) {
            return Math.min(width - 1, col * step + step / 2);
        }

        int sampleY(int row) {
            return Math.min(height - 1, row * step + step / 2);
        }

        float x0(int col) {
            return originX + col * step;
        }

        float x1(int col) {
            return originX + Math.min(width, (col + 1) * step);
        }

        float y0(int row) {
            return originY + row * step;
        }

        float y1(int row) {
            return originY + Math.min(height, (row + 1) * step);
        }
    }

    /** Стенка вдоль ячеек [from..to] одной строки (dir 0/1) или колонки (dir 2/3). */
    private static void emitWall(
        List<FrameQuad> quads,
        Grid grid,
        int dir,
        int line,
        int from,
        int to,
        boolean raised,
        boolean ontoCanvas,
        float[] uv,
        float zFront,
        float zCanvas,
        float zBack,
        float[] rgb,
        float alpha
    ) {
        float ex0;
        float ey0;
        float ex1;
        float ey1;
        if (dir == 0) {
            ex0 = grid.x0(from);
            ey0 = grid.y0(line);
            ex1 = grid.x1(to);
            ey1 = ey0;
        } else if (dir == 1) {
            ex0 = grid.x1(to);
            ey0 = grid.y1(line);
            ex1 = grid.x0(from);
            ey1 = ey0;
        } else if (dir == 2) {
            ex0 = grid.x0(line);
            ey0 = grid.y1(to);
            ex1 = ex0;
            ey1 = grid.y0(from);
        } else {
            ex0 = grid.x1(line);
            ey0 = grid.y0(from);
            ex1 = ex0;
            ey1 = grid.y1(to);
        }
        if (ontoCanvas) {
            float swapX = ex0;
            float swapY = ey0;
            ex0 = ex1;
            ey0 = ey1;
            ex1 = swapX;
            ey1 = swapY;
        }
        float zNear = raised ? zFront : zCanvas;
        float zFar = ontoCanvas ? zCanvas : zBack;
        float shade = dir == 0 ? (ontoCanvas ? 0.72f : 0.95f) : dir == 1 ? (ontoCanvas ? 0.55f : 0.62f) : 0.8f;
        quads.add(new FrameQuad(
            raised ? MessagePanelTextures.border() : MessagePanelTextures.panel(),
            ex0, ey0, zNear,
            ex0, ey0, zFar,
            ex1, ey1, zFar,
            ex1, ey1, zNear,
            uv[0], uv[1], uv[0], uv[1], uv[0], uv[1], uv[0], uv[1],
            rgb[0] * shade, rgb[1] * shade, rgb[2] * shade, alpha
        ));
    }

    private static final byte RAISED = 1;
    private static final byte CANVAS = 2;
    private static final byte OUTSIDE = 3;

    private static void floodOutside(byte[] kind, int cols, int rows) {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int col = 0; col < cols; col++) {
            seedOutside(queue, kind, col, 0, cols);
            seedOutside(queue, kind, col, rows - 1, cols);
        }
        for (int row = 0; row < rows; row++) {
            seedOutside(queue, kind, 0, row, cols);
            seedOutside(queue, kind, cols - 1, row, cols);
        }
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int col = index % cols;
            int row = index / cols;
            for (int dir = 0; dir < 4; dir++) {
                int nx = col + dx[dir];
                int ny = row + dy[dir];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) {
                    continue;
                }
                int next = ny * cols + nx;
                if (kind[next] == 0) {
                    kind[next] = OUTSIDE;
                    queue.addLast(next);
                }
            }
        }
    }

    private static void seedOutside(ArrayDeque<Integer> queue, byte[] kind, int col, int row, int cols) {
        int index = row * cols + col;
        if (kind[index] == 0) {
            kind[index] = OUTSIDE;
            queue.addLast(index);
        }
    }

    /** Задник повторяет тот же контур, что лицевая сторона, без собственного скругления дерева. */
    private static void appendBack(
        List<FrameQuad> quads,
        byte[] kind,
        int cols,
        int rows,
        int originX,
        int originY,
        int width,
        int height,
        int step,
        float zBack,
        float alpha
    ) {
        float shade = 0.85f;
        // Строки внутри одного 16px тайла с одинаковыми отрезками склеиваются по вертикали:
        // UV в тайле линейный, поэтому один высокий квад совпадает со стопкой строк.
        int[] prevSegments = null;
        int bandStart = 0;
        for (int row = 0; row <= rows; row++) {
            int[] segments = row < rows ? backSegments(kind, cols, row, step) : null;
            boolean sameBand = prevSegments != null
                && segments != null
                && (row * step) / 16 == (bandStart * step) / 16
                && java.util.Arrays.equals(prevSegments, segments);
            if (sameBand) {
                continue;
            }
            if (prevSegments != null) {
                float y0 = originY + bandStart * step;
                float y1 = originY + Math.min(height, row * step);
                float v0 = (8f + (bandStart * step) % 16) / 32f;
                float v1 = v0 + (y1 - y0) / 32f;
                for (int s = 0; s < prevSegments.length; s += 2) {
                    int start = prevSegments[s];
                    int end = prevSegments[s + 1];
                    float x0 = originX + start * step;
                    float x1 = originX + Math.min(width, end * step);
                    float u0 = (8f + (start * step) % 16) / 32f;
                    float u1 = u0 + (x1 - x0) / 32f;
                    quads.add(new FrameQuad(
                        MessagePanelConstants.FRAME_BACK_TEXTURE,
                        x1, y0, zBack,
                        x1, y1, zBack,
                        x0, y1, zBack,
                        x0, y0, zBack,
                        u1, v0,
                        u1, v1,
                        u0, v1,
                        u0, v0,
                        shade, shade, shade, alpha
                    ));
                }
            }
            prevSegments = segments;
            bandStart = row;
        }
    }

    /** Отрезки [start, end) заполненных ячеек строки, разрезанные по границам 16px тайла. */
    private static int[] backSegments(byte[] kind, int cols, int row, int step) {
        int[] out = new int[8];
        int n = 0;
        int col = 0;
        while (col < cols) {
            int here = kind[row * cols + col];
            if (here != RAISED && here != CANVAS) {
                col++;
                continue;
            }
            int start = col;
            int tile = (start * step) / 16;
            col++;
            while (col < cols && (col * step) / 16 == tile) {
                int next = kind[row * cols + col];
                if (next != RAISED && next != CANVAS) {
                    break;
                }
                col++;
            }
            if (n + 2 > out.length) {
                out = java.util.Arrays.copyOf(out, out.length * 2);
            }
            out[n++] = start;
            out[n++] = col;
        }
        return java.util.Arrays.copyOf(out, n);
    }

    private static boolean sampleOpaque(boolean border, int x, int y, int width, int height) {
        int tex = MessagePanelTextures.size(border);
        int tx = mapToTex(x, width, tex);
        int ty = mapToTex(y, height, tex);
        return (MessagePanelTextures.argb(border, tx, ty) >>> 24) > 16;
    }

    private static int texel(boolean border, int x, int y, int width, int height) {
        int tex = MessagePanelTextures.size(border);
        return MessagePanelTextures.argb(border, mapToTex(x, width, tex), mapToTex(y, height, tex));
    }

    private static float[] sampleUv(boolean border, int x, int y, int width, int height) {
        int tex = MessagePanelTextures.size(border);
        float u = (mapToTex(x, width, tex) + 0.5f) / tex;
        float v = (mapToTex(y, height, tex) + 0.5f) / tex;
        return new float[] {u, v};
    }

    /** Экранный пиксель панели → пиксель текстуры с теми же разрезами, что у nine-slice. */
    private static int mapToTex(int screen, int panelSize, int tex) {
        int corner = MessagePanelConstants.CORNER;
        int srcCorner = Math.max(1, tex / 4);
        if (panelSize <= corner * 2) {
            return Math.min(tex - 1, screen * tex / Math.max(1, panelSize));
        }
        if (screen < corner) {
            return Math.min(srcCorner - 1, screen * srcCorner / corner);
        }
        if (screen >= panelSize - corner) {
            int local = screen - (panelSize - corner);
            return Math.min(tex - 1, tex - srcCorner + local * srcCorner / corner);
        }
        int middle = panelSize - corner * 2;
        int srcMiddle = tex - srcCorner * 2;
        if (middle <= 0 || srcMiddle <= 0) {
            return Math.min(tex - 1, srcCorner);
        }
        return Math.min(tex - 1, srcCorner + (screen - corner) * srcMiddle / middle);
    }
}
