package takeyourminestream.ijustseen.integration.chat;

import takeyourminestream.ijustseen.TakeYourMineStreamClient;

public final class SimpleWebSocket {
    private final String url;
    private final MessageHandler messageHandler;
    private final Runnable onOpen;
    private volatile java.net.http.WebSocket webSocket;
    private volatile boolean running;

    public interface MessageHandler {
        void onMessage(String message);
    }

    public SimpleWebSocket(String url, Runnable onOpen, MessageHandler messageHandler) {
        this.url = url;
        this.onOpen = onOpen;
        this.messageHandler = messageHandler;
    }

    public void connect() {
        running = true;
        java.net.http.HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .buildAsync(java.net.URI.create(url), new java.net.http.WebSocket.Listener() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void onOpen(java.net.http.WebSocket ws) {
                    webSocket = ws;
                    if (onOpen != null) {
                        onOpen.run();
                    }
                    ws.request(1);
                }

                @Override
                public java.util.concurrent.CompletionStage<?> onText(
                    java.net.http.WebSocket ws,
                    CharSequence data,
                    boolean last
                ) {
                    buffer.append(data);
                    if (last) {
                        String message = buffer.toString();
                        buffer.setLength(0);
                        try {
                            messageHandler.onMessage(message);
                        } catch (Exception e) {
                            TakeYourMineStreamClient.LOGGER.warn("WebSocket message handler failed", e);
                        }
                    }
                    ws.request(1);
                    return null;
                }

                @Override
                public java.util.concurrent.CompletionStage<?> onClose(
                    java.net.http.WebSocket ws,
                    int statusCode,
                    String reason
                ) {
                    webSocket = null;
                    return null;
                }

                @Override
                public void onError(java.net.http.WebSocket ws, Throwable error) {
                    TakeYourMineStreamClient.LOGGER.warn("WebSocket error: {}", error.getMessage());
                }
            });
    }

    public void send(String text) {
        java.net.http.WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendText(text, true);
        }
    }

    public void disconnect() {
        running = false;
        java.net.http.WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    public boolean isRunning() {
        return running;
    }
}
