package takeyourminestream.ijustseen.integration.tiktok;

import takeyourminestream.ijustseen.integration.tiktok.proto.TikTokProto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

final class TikTokFrames {
    private static final int GZIP_MAGIC_BYTE1 = 0x1F;
    private static final int GZIP_MAGIC_BYTE2 = 0x8B;
    private static final long ENTER_ROOM_CLIENT_TYPE = 12L;
    private static final int MAX_GZIP_BYTES = 16 * 1024 * 1024;

    private TikTokFrames() {}

    static byte[] buildHeartbeat(String roomId) {
        byte[] hb = TikTokProto.encode(w -> w.writeUint64(1, Long.parseLong(roomId)));
        return TikTokProto.encode(w -> {
            w.writeString(6, "pb");
            w.writeString(7, "hb");
            w.writeBytes(8, hb);
        });
    }

    static byte[] buildEnterRoom(String roomId) {
        byte[] enter = TikTokProto.encode(w -> {
            w.writeInt64(1, Long.parseLong(roomId));
            w.writeInt64(4, ENTER_ROOM_CLIENT_TYPE);
            w.writeString(5, "audience");
            w.writeString(9, "0");
        });
        return TikTokProto.encode(w -> {
            w.writeString(6, "pb");
            w.writeString(7, "im_enter_room");
            w.writeBytes(8, enter);
        });
    }

    static byte[] buildAck(long logId, byte[] internalExt) {
        return TikTokProto.encode(w -> {
            w.writeString(6, "pb");
            w.writeString(7, "ack");
            w.writeUint64(2, logId);
            w.writeBytes(8, internalExt);
        });
    }

    static byte[] decompressIfGzipped(byte[] data) throws IOException {
        if (data.length >= 2
            && (data[0] & 0xFF) == GZIP_MAGIC_BYTE1
            && (data[1] & 0xFF) == GZIP_MAGIC_BYTE2) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data))) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = gis.read(buffer)) >= 0) {
                    if (out.size() + read > MAX_GZIP_BYTES) {
                        throw new IOException("gzip payload too large");
                    }
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            }
        }
        return data;
    }
}
