package takeyourminestream.modid.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Waypoint-рендер временно отключён для совместимости с 1.21.11.
 */
public class WaypointRenderer {
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

    public void render(WorldRenderContext context) {
        if (context == null) {
            return;
        }
    }

    /**
     * Очистка ресурсов
     * ВАЖНО: Должна вызываться при закрытии GameRenderer
     */
    public void close() {
        waypoints.clear();
    }
}
