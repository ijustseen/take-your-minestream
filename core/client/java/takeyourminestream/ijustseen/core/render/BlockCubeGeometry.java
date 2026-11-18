package takeyourminestream.ijustseen.core.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Эталонный куб 1×1×1 блок вокруг сообщения в редакторе.
 * Рисуются только рёбра: сплошные грани на 26.2/26.3 перекрывают рамку панели по глубине.
 */
public final class BlockCubeGeometry {
    public static final float HALF = 0.5f;
    public static final float FACE_ALPHA = 0.2f;
    public static final float EDGE_ALPHA = 0.4f;
    private static final float EDGE_WIDTH = 0.025f;
    private static final float EDGE_LIFT = 0.002f;

    /** Четыре вершины (x, y, z подряд) в блоках относительно центра куба. */
    public record Quad(float[] xyz, boolean edge) {}

    private static final List<Quad> QUADS = build();

    private BlockCubeGeometry() {}

    public static List<Quad> quads() {
        return QUADS;
    }

    private static List<Quad> build() {
        List<Quad> quads = new ArrayList<>();
        float h = HALF;
        float w = EDGE_WIDTH;
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                float edgePlane = sign * (h + EDGE_LIFT);
                quads.add(face(axis, edgePlane, -h, -h, -h + w, h, true));
                quads.add(face(axis, edgePlane, h - w, -h, h, h, true));
                quads.add(face(axis, edgePlane, -h + w, -h, h - w, -h + w, true));
                quads.add(face(axis, edgePlane, -h + w, h - w, h - w, h, true));
            }
        }
        return List.copyOf(quads);
    }

    /** Прямоугольник в плоскости {@code axis = plane}, по двум другим осям от (u0, v0) до (u1, v1). */
    private static Quad face(int axis, float plane, float u0, float v0, float u1, float v1, boolean edge) {
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        float[] xyz = new float[12];
        for (int i = 0; i < 4; i++) {
            float[] point = new float[3];
            point[axis] = plane;
            point[(axis + 1) % 3] = uv[i][0];
            point[(axis + 2) % 3] = uv[i][1];
            System.arraycopy(point, 0, xyz, i * 3, 3);
        }
        return new Quad(xyz, edge);
    }
}
