package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.EditorCameraCompat;

import java.util.ArrayList;
import java.util.List;

/** Пробные сообщения визуального редактора и их расстановка в мире. */
public final class MessageEditorPreview {
    private static final int AROUND_MESSAGE_COUNT = 6;

    public record Sample(
        String platformKey,
        String colorCode,
        int colorRgb,
        String name,
        List<String> badges,
        String textKey,
        takeyourminestream.ijustseen.integration.chat.ChatEventType eventType
    ) {
        public Text label() {
            return Text.translatable("takeyourstreamchat.editor.sample." + platformKey);
        }
    }

    public static final List<Sample> SAMPLES = List.of(
        new Sample("twitch", "§d", 0xFF55FF, "StreamFan", List.of("sub"), "takeyourstreamchat.editor.sample_text.twitch", takeyourminestream.ijustseen.integration.chat.ChatEventType.CHAT),
        new Sample("youtube", "§c", 0xFF5555, "Lena_Plays", List.of(), "takeyourstreamchat.editor.sample_text.youtube", takeyourminestream.ijustseen.integration.chat.ChatEventType.CHAT),
        new Sample("kick", "§a", 0x55FF55, "kick_mod", List.of("mod", "vip"), "takeyourstreamchat.editor.sample_text.kick", takeyourminestream.ijustseen.integration.chat.ChatEventType.CHAT),
        new Sample("tiktok", "§b", 0x55FFFF, "tiktoker42", List.of(), "takeyourstreamchat.editor.sample_text.tiktok", takeyourminestream.ijustseen.integration.chat.ChatEventType.CHAT),
        new Sample("twitch", "§d", 0xFF55FF, "StreamFan", List.of(), "takeyourstreamchat.event.twitch.sub", takeyourminestream.ijustseen.integration.chat.ChatEventType.SUBSCRIBE),
        new Sample("kick", "§a", 0x55FF55, "kick_mod", List.of(), "takeyourstreamchat.event.kick.follow", takeyourminestream.ijustseen.integration.chat.ChatEventType.FOLLOW)
    );

    private MessageEditorPreview() {}

    public static Message create(MinecraftClient client, Sample sample, Vec3d position, float yaw, float pitch, Vec3d offsetFromEye) {
        boolean event = sample.eventType() != null && sample.eventType().isEvent();
        String text = event
            ? sample.colorCode() + sample.name() + "§r " + Text.translatable(sample.textKey()).getString()
            : sample.colorCode() + sample.name() + ":§r " + Text.translatable(sample.textKey()).getString();
        List<MessageEmote> markers = new ArrayList<>();
        markers.add(new MessageEmote("platform", sample.platformKey(), "", -1, -1));
        if (event) {
            markers.add(new MessageEmote(RoleBadges.PROVIDER, sample.eventType().badgeKey(), "", -1, -1));
        }
        if (ModConfig.isSHOW_ROLE_BADGES()) {
            for (String badge : sample.badges()) {
                markers.add(new MessageEmote(RoleBadges.PROVIDER, badge, "", -1, -1));
            }
        }
        Message message = new Message(text, position, 0, yaw, pitch, sample.colorRgb(), offsetFromEye, markers);
        message.setEventType(sample.eventType());
        message.setPreview(true);
        PinnedMessageStore.tagWithCurrentDimension(message, client);
        return message;
    }

    /** Все примеры стопкой — для HUD-виджета. */
    public static List<Message> createHudStack(MinecraftClient client) {
        List<Message> messages = new ArrayList<>();
        for (Sample sample : SAMPLES) {
            messages.add(create(client, sample, Vec3d.ZERO, 0, 0, Vec3d.ZERO));
        }
        return messages;
    }

    /** Сообщения там, где их поставил бы спавнер при текущих настройках расположения и дистанции. */
    public static List<Message> createAroundPlayer(MinecraftClient client) {
        List<Message> messages = new ArrayList<>();
        if (client.player == null || client.world == null) {
            return messages;
        }
        boolean frontOnly = ModConfig.getMESSAGE_SPAWN_MODE() == MessageSpawnMode.FRONT_OF_PLAYER;
        Vec3d eye = client.player.getEyePos();
        for (int i = 0; i < AROUND_MESSAGE_COUNT; i++) {
            Vec3d position = frontOnly
                ? MessagePosition.generatePositionInFrontOfPlayer(client)
                : MessagePosition.generateRandomPosition(client);
            float[] facing = facing(position, eye);
            messages.add(create(client, SAMPLES.get(i % SAMPLES.size()), position, facing[0], facing[1], position.subtract(eye)));
        }
        return messages;
    }

    /**
     * Ставит сообщение перед камерой на {@code distance} блоков, со сдвигом по горизонтали
     * {@code screenOffset} (-1…1 от центра экрана к краю), развёрнутым к камере и повёрнутым на заданные углы.
     */
    public static void placeInFrontOfCamera(
        MinecraftClient client, Message message, double distance, double screenOffset, float extraYaw, float extraPitch
    ) {
        if (client.player == null) {
            return;
        }
        Vec3d camera = CameraPositionCompat.getCameraPos(client);
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        double yawRad = Math.toRadians(EditorCameraCompat.yaw(client.player));
        Vec3d localDir = new Vec3d(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        double lateral = distance * screenOffset * Math.tan(horizontalHalfFovRad(client));
        Vec3d position = camera.add(lookVec.multiply(distance)).add(localDir.multiply(lateral));
        float[] facing = facing(position, camera);
        message.setPosition(position);
        message.setYaw(facing[0] + extraYaw);
        message.setPitch(MathHelper.clamp(facing[1] + extraPitch, -89.0f, 89.0f));
    }

    private static float[] facing(Vec3d position, Vec3d target) {
        double dx = target.x - position.x;
        double dy = target.y - position.y;
        double dz = target.z - position.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
        return new float[] {yaw, pitch};
    }

    private static double horizontalHalfFovRad(MinecraftClient client) {
        double aspect = (double) client.getWindow().getFramebufferWidth()
            / (double) Math.max(1, client.getWindow().getFramebufferHeight());
        double vertFov = client.options.getFov().getValue();
        return Math.atan(Math.tan(Math.toRadians(vertFov / 2.0)) * aspect);
    }
}
