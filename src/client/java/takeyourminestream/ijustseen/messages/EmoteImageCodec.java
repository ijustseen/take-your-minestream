package takeyourminestream.ijustseen.messages;

import net.minecraft.client.texture.NativeImage;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * CPU-декодирование эмоутов/стикеров. Вызывать только с фоновых потоков:
 * NativeImage создаётся здесь, GPU-регистрация — на клиентском тике.
 */
final class EmoteImageCodec {
    static final int MAX_EDGE = 128;
    static final int MAX_GIF_FRAMES = 24;

    record DecodedFrame(NativeImage image, int delayMs) {}

    record DecodedEmote(NativeImage image, List<DecodedFrame> frames) {
        boolean animated() {
            return frames != null && frames.size() > 1;
        }

        int textureCount() {
            if (animated()) {
                return frames.size();
            }
            return image != null ? 1 : 0;
        }

        void close() {
            if (image != null) {
                image.close();
            }
            if (frames != null) {
                for (DecodedFrame frame : frames) {
                    if (frame.image() != null) {
                        frame.image().close();
                    }
                }
            }
        }
    }

    static {
        ImageIO.setUseCache(false);
    }

    private EmoteImageCodec() {}

    static DecodedEmote decode(byte[] imageBytes, String provider) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        if ("7tv".equals(provider) && isGif(imageBytes)) {
            DecodedEmote animated = decodeGif(imageBytes);
            if (animated != null) {
                return animated;
            }
        }

        NativeImage image = decodeStatic(imageBytes);
        return image == null ? null : new DecodedEmote(image, null);
    }

    static boolean isGif(byte[] bytes) {
        return bytes != null
            && bytes.length >= 6
            && bytes[0] == 'G'
            && bytes[1] == 'I'
            && bytes[2] == 'F'
            && bytes[3] == '8'
            && (bytes[4] == '7' || bytes[4] == '9')
            && bytes[5] == 'a';
    }

    private static NativeImage decodeStatic(byte[] imageBytes) {
        NativeImage stb = readStb(imageBytes);
        if (stb != null && stb.getWidth() <= MAX_EDGE && stb.getHeight() <= MAX_EDGE) {
            return stb;
        }
        if (stb != null) {
            stb.close();
        }

        BufferedImage buffered = readAwt(imageBytes);
        if (buffered == null) {
            return null;
        }
        return toNative(downscale(buffered));
    }

    private static NativeImage readStb(byte[] imageBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            return NativeImage.read(in);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage readAwt(byte[] imageBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            return ImageIO.read(in);
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("AWT image decode failed: {}", e.getMessage());
            return null;
        }
    }

    private static DecodedEmote decodeGif(byte[] imageBytes) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader gifReader = readers.next();
            try {
                gifReader.setInput(imageInputStream, false, false);
                int frameCount = gifReader.getNumImages(true);
                if (frameCount <= 0) {
                    return null;
                }

                int[] canvasSize = getGifCanvasSize(gifReader);
                int canvasWidth = Math.max(1, canvasSize[0]);
                int canvasHeight = Math.max(1, canvasSize[1]);
                BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                BufferedImage previousCanvas = null;

                int step = frameCount <= MAX_GIF_FRAMES ? 1 : (int) Math.ceil(frameCount / (double) MAX_GIF_FRAMES);
                List<DecodedFrame> frames = new ArrayList<>(Math.min(frameCount, MAX_GIF_FRAMES));

                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    BufferedImage frame = gifReader.read(frameIndex);
                    if (frame == null) {
                        continue;
                    }

                    GifFrameMeta meta = readGifFrameMeta(gifReader.getImageMetadata(frameIndex));
                    if ("restoreToPrevious".equals(meta.disposalMethod)) {
                        previousCanvas = copyBufferedImage(canvas);
                    }

                    Graphics2D drawGraphics = canvas.createGraphics();
                    drawGraphics.drawImage(frame, meta.left, meta.top, null);
                    drawGraphics.dispose();

                    if (frameIndex % step == 0 || frameIndex == frameCount - 1) {
                        NativeImage nativeFrame = toNative(downscale(copyBufferedImage(canvas)));
                        if (nativeFrame != null) {
                            int delay = meta.delayMs * step;
                            frames.add(new DecodedFrame(nativeFrame, delay));
                        }
                    }

                    if ("restoreToBackgroundColor".equals(meta.disposalMethod)) {
                        Graphics2D clearGraphics = canvas.createGraphics();
                        clearGraphics.setComposite(java.awt.AlphaComposite.Clear);
                        clearGraphics.fillRect(meta.left, meta.top, frame.getWidth(), frame.getHeight());
                        clearGraphics.dispose();
                    } else if ("restoreToPrevious".equals(meta.disposalMethod) && previousCanvas != null) {
                        canvas = previousCanvas;
                        previousCanvas = null;
                    }
                }

                if (frames.isEmpty()) {
                    return null;
                }
                if (frames.size() == 1) {
                    return new DecodedEmote(frames.get(0).image(), null);
                }
                return new DecodedEmote(null, List.copyOf(frames));
            } finally {
                gifReader.dispose();
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Error parsing GIF: {}", e.getMessage());
            return null;
        }
    }

    private static NativeImage toNative(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(bufferedImage, "png", baos)) {
                return null;
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                return NativeImage.read(bais);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage downscale(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        if (width <= MAX_EDGE && height <= MAX_EDGE) {
            return src;
        }
        float scale = Math.min(MAX_EDGE / (float) width, MAX_EDGE / (float) height);
        int newWidth = Math.max(1, Math.round(width * scale));
        int newHeight = Math.max(1, Math.round(height * scale));
        BufferedImage dst = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dst.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(src, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return dst;
    }

    private static BufferedImage copyBufferedImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(src, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static int[] getGifCanvasSize(ImageReader reader) {
        try {
            IIOMetadata streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                String format = streamMeta.getNativeMetadataFormatName();
                if (format != null) {
                    Node root = streamMeta.getAsTree(format);
                    Node child = root.getFirstChild();
                    while (child != null) {
                        if ("LogicalScreenDescriptor".equals(child.getNodeName())) {
                            NamedNodeMap attrs = child.getAttributes();
                            if (attrs != null) {
                                Node width = attrs.getNamedItem("logicalScreenWidth");
                                Node height = attrs.getNamedItem("logicalScreenHeight");
                                if (width != null && height != null) {
                                    return new int[] {
                                        Integer.parseInt(width.getNodeValue()),
                                        Integer.parseInt(height.getNodeValue())
                                    };
                                }
                            }
                            break;
                        }
                        child = child.getNextSibling();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new int[] {28, 28};
    }

    private record GifFrameMeta(int left, int top, int delayMs, String disposalMethod) {}

    private static GifFrameMeta readGifFrameMeta(IIOMetadata metadata) {
        int left = 0;
        int top = 0;
        int delayMs = 100;
        String disposalMethod = "none";

        if (metadata == null) {
            return new GifFrameMeta(left, top, delayMs, disposalMethod);
        }

        try {
            String formatName = metadata.getNativeMetadataFormatName();
            if (formatName == null) {
                return new GifFrameMeta(left, top, delayMs, disposalMethod);
            }

            Node root = metadata.getAsTree(formatName);
            Node child = root.getFirstChild();
            while (child != null) {
                if ("GraphicControlExtension".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    if (attrs != null) {
                        Node delayNode = attrs.getNamedItem("delayTime");
                        if (delayNode != null) {
                            int centiseconds = Integer.parseInt(delayNode.getNodeValue());
                            delayMs = Math.max(20, centiseconds * 10);
                        }
                        Node disposalNode = attrs.getNamedItem("disposalMethod");
                        if (disposalNode != null) {
                            disposalMethod = disposalNode.getNodeValue();
                        }
                    }
                } else if ("ImageDescriptor".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    if (attrs != null) {
                        Node leftNode = attrs.getNamedItem("imageLeftPosition");
                        Node topNode = attrs.getNamedItem("imageTopPosition");
                        if (leftNode != null) {
                            left = Integer.parseInt(leftNode.getNodeValue());
                        }
                        if (topNode != null) {
                            top = Integer.parseInt(topNode.getNodeValue());
                        }
                    }
                }
                child = child.getNextSibling();
            }
        } catch (Exception ignored) {
        }

        return new GifFrameMeta(left, top, delayMs, disposalMethod);
    }
}
