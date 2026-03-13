package takeyourminestream.modid.messages;

import net.minecraft.util.math.Vec3d;

/**
 * Модель данных для отображаемого сообщения
 */
public class Message {
    private final String text;
    private final java.util.List<MessageEmote> emotes;
    private Vec3d position;
    private Vec3d previousPosition;
    private final long spawnTick;
    private int frozenTicks;
    private float yaw;
    private float pitch;
    private float previousYaw;
    private float previousPitch;
    private final Integer authorColorRgb; // null если неизвестен
    // Смещение цели относительно глаз игрока на момент спавна (для Follow Player)
    private final Vec3d targetOffsetFromEye;
    // Текущая скорость для плавного следования
    private Vec3d velocity = Vec3d.ZERO;
    // Отстающая базовая ориентация (yaw), вокруг которой вращается локальное смещение
    private float followBasisYaw;
    private boolean pinned;
    
    public Message(String text, Vec3d position, long spawnTick, float yaw, float pitch) {
        this(text, position, spawnTick, yaw, pitch, null, Vec3d.ZERO, java.util.Collections.emptyList());
    }

    public Message(String text, Vec3d position, long spawnTick, float yaw, float pitch, Integer authorColorRgb) {
        this(text, position, spawnTick, yaw, pitch, authorColorRgb, Vec3d.ZERO, java.util.Collections.emptyList());
    }

    public Message(String text, Vec3d position, long spawnTick, float yaw, float pitch, Integer authorColorRgb, Vec3d targetOffsetFromEye) {
        this(text, position, spawnTick, yaw, pitch, authorColorRgb, targetOffsetFromEye, java.util.Collections.emptyList());
    }

    public Message(String text, Vec3d position, long spawnTick, float yaw, float pitch, Integer authorColorRgb, Vec3d targetOffsetFromEye, java.util.List<MessageEmote> emotes) {
        this.text = text;
        this.emotes = emotes == null ? java.util.Collections.emptyList() : java.util.List.copyOf(emotes);
        this.position = position;
        this.spawnTick = spawnTick;
        this.frozenTicks = 0;
        this.yaw = yaw;
        this.pitch = pitch;
        this.previousYaw = yaw;
        this.previousPitch = pitch;
        this.authorColorRgb = authorColorRgb;
        this.targetOffsetFromEye = targetOffsetFromEye;
        this.followBasisYaw = 0.0f;
        this.previousPosition = position;
        this.pinned = false;
    }
    
    public String getText() { 
        return text; 
    }

    public java.util.List<MessageEmote> getEmotes() {
        return emotes;
    }
    
    public Vec3d getPosition() { 
        return position; 
    }
    public void setPosition(Vec3d position) {
        this.previousPosition = this.position;
        this.position = position;
    }
    
    public long getSpawnTick() { 
        return spawnTick; 
    }
    
    public int getFrozenTicks() { 
        return frozenTicks; 
    }
    
    public void incrementFrozenTicks() {
        this.frozenTicks++;
    }
    
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.previousYaw = this.yaw; this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.previousPitch = this.pitch; this.pitch = pitch; }
    public Integer getAuthorColorRgb() { return authorColorRgb; }
    public Vec3d getTargetOffsetFromEye() { return targetOffsetFromEye; }
    public Vec3d getVelocity() { return velocity; }
    public void setVelocity(Vec3d velocity) { this.velocity = velocity; }
    public float getFollowBasisYaw() { return followBasisYaw; }
    public void setFollowBasisYaw(float followBasisYaw) { this.followBasisYaw = followBasisYaw; }
    public Vec3d getPreviousPosition() { return previousPosition; }
    public float getPreviousYaw() { return previousYaw; }
    public float getPreviousPitch() { return previousPitch; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    
    /**
     * Вычисляет эффективный возраст сообщения с учетом замороженного времени
     * @param currentTick текущий тик
     * @return эффективный возраст сообщения в тиках
     */
    public int getEffectiveAge(int currentTick) {
        int baseAge = currentTick - (int)spawnTick;
        return Math.max(0, baseAge - frozenTicks);
    }
} 