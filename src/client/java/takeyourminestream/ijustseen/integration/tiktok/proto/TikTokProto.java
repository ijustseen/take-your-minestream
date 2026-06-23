package takeyourminestream.ijustseen.integration.tiktok.proto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class TikTokProto {
    private static final int MAX_NEST_DEPTH = 64;
    private static final int MAX_FIELD_BYTES = 16 * 1024 * 1024;

    private TikTokProto() {}

    public static byte[] encode(Consumer<Writer> builder) {
        Writer writer = new Writer();
        builder.accept(writer);
        return writer.toByteArray();
    }

    public static ProtoMap decode(byte[] data) {
        return decode(data, 0);
    }

    private static ProtoMap decode(byte[] data, int depth) {
        if (depth > MAX_NEST_DEPTH) {
            throw new IllegalArgumentException("protobuf nesting too deep");
        }
        return new Reader(data, 0, data.length, depth).readMessage();
    }

    public static final class Writer {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        public void writeUint64(int field, long value) {
            writeVarint((long) field << 3);
            writeVarint(value);
        }

        public void writeInt64(int field, long value) {
            writeUint64(field, value);
        }

        public void writeInt32(int field, int value) {
            writeUint64(field, value & 0xFFFFFFFFL);
        }

        public void writeString(int field, String value) {
            writeByteArray(field, value.getBytes(StandardCharsets.UTF_8));
        }

        public void writeBytes(int field, byte[] value) {
            writeByteArray(field, value);
        }

        private void writeVarint(long value) {
            while ((value & ~0x7FL) != 0) {
                buf.write((int) (value & 0x7F) | 0x80);
                value >>>= 7;
            }
            buf.write((int) value);
        }

        private void writeByteArray(int field, byte[] value) {
            writeVarint(((long) field << 3) | 2);
            writeVarint(value.length);
            buf.write(value, 0, value.length);
        }

        public byte[] toByteArray() {
            return buf.toByteArray();
        }
    }

    private static final class Reader {
        private static final int MAX_VARINT_BYTES = 10;

        private final byte[] data;
        private int pos;
        private final int end;
        private final int depth;

        Reader(byte[] data, int offset, int length, int depth) {
            this.data = data;
            this.pos = offset;
            this.end = offset + length;
            this.depth = depth;
        }

        long readVarint() {
            long result = 0;
            int shift = 0;
            int iterations = 0;
            while (pos < end) {
                if (++iterations > MAX_VARINT_BYTES) {
                    throw new IllegalArgumentException("varint too long");
                }
                int b = data[pos++] & 0xFF;
                result |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return result;
                }
                shift += 7;
            }
            throw new IllegalArgumentException("truncated varint");
        }

        byte[] readLenDelim() {
            long lenLong = readVarint();
            if (lenLong < 0 || lenLong > MAX_FIELD_BYTES) {
                throw new IllegalArgumentException("length-delimited field too large");
            }
            int len = (int) lenLong;
            if (pos + len > end) {
                throw new IllegalArgumentException("truncated length-delimited field");
            }
            byte[] result = Arrays.copyOfRange(data, pos, pos + len);
            pos += len;
            return result;
        }

        ProtoMap readMessage() {
            Map<Integer, List<Object>> fields = new HashMap<>();
            while (pos < end) {
                long tag = readVarint();
                int fieldNum = (int) (tag >>> 3);
                int wireType = (int) (tag & 7);
                if (fieldNum == 0) {
                    break;
                }

                Object value = switch (wireType) {
                    case 0 -> readVarint();
                    case 1 -> {
                        pos += 8;
                        yield 0L;
                    }
                    case 2 -> readLenDelim();
                    case 3 -> {
                        skipGroup();
                        yield null;
                    }
                    case 4 -> null;
                    case 5 -> {
                        pos += 4;
                        yield 0L;
                    }
                    default -> {
                        skipField(wireType);
                        yield null;
                    }
                };
                if (value != null) {
                    fields.computeIfAbsent(fieldNum, k -> new ArrayList<>()).add(value);
                }
            }
            return new ProtoMap(fields, depth);
        }

        private void skipGroup() {
            while (pos < end) {
                long tag = readVarint();
                int wireType = (int) (tag & 7);
                if (wireType == 4) {
                    return;
                }
                skipField(wireType);
            }
        }

        private void skipField(int wireType) {
            switch (wireType) {
                case 0 -> readVarint();
                case 1 -> pos += 8;
                case 2 -> readLenDelim();
                case 3 -> skipGroup();
                case 4 -> { /* end group */ }
                case 5 -> pos += 4;
                default -> pos = end;
            }
        }
    }

    public static final class ProtoMap {
        private final Map<Integer, List<Object>> fields;
        private final int depth;

        ProtoMap(Map<Integer, List<Object>> fields, int depth) {
            this.fields = fields;
            this.depth = depth;
        }

        public long getVarint(int field) {
            List<Object> values = fields.get(field);
            if (values == null || values.isEmpty()) {
                return 0;
            }
            Object first = values.getFirst();
            return first instanceof Long l ? l : 0;
        }

        public int getInt(int field) {
            return (int) getVarint(field);
        }

        public boolean getBool(int field) {
            return getVarint(field) != 0;
        }

        public byte[] getRawBytes(int field) {
            List<Object> values = fields.get(field);
            if (values == null || values.isEmpty()) {
                return new byte[0];
            }
            Object first = values.getFirst();
            return first instanceof byte[] b ? b : new byte[0];
        }

        public String getString(int field) {
            return new String(getRawBytes(field), StandardCharsets.UTF_8);
        }

        public ProtoMap getMessage(int field) {
            byte[] raw = getRawBytes(field);
            return raw.length > 0 ? TikTokProto.decode(raw, depth + 1) : new ProtoMap(Map.of(), depth);
        }

        public List<ProtoMap> getRepeatedMessages(int field) {
            List<Object> values = fields.get(field);
            if (values == null) {
                return List.of();
            }
            List<ProtoMap> result = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof byte[] bytes && bytes.length > 0) {
                    result.add(TikTokProto.decode(bytes, depth + 1));
                }
            }
            return result;
        }

        public List<String> getRepeatedStrings(int field) {
            List<Object> values = fields.get(field);
            if (values == null) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof byte[] bytes) {
                    result.add(new String(bytes, StandardCharsets.UTF_8));
                }
            }
            return result;
        }

        public Map<String, Object> toFlatMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<Object>> entry : fields.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
    }
}
