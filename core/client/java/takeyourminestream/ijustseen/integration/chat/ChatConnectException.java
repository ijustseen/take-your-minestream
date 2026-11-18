package takeyourminestream.ijustseen.integration.chat;

import java.io.IOException;

/** Ошибка подключения к платформе с локализуемой причиной для вывода игроку. */
public class ChatConnectException extends IOException {
    private final String translationKey;
    private final Object[] args;

    public ChatConnectException(String translationKey, String logMessage, Object... args) {
        super(logMessage);
        this.translationKey = translationKey;
        this.args = args;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
