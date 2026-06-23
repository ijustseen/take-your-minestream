package takeyourminestream.ijustseen.integration.chat;

import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.Collections;
import java.util.List;

public record IncomingChatMessage(
    ChatPlatform platform,
    String authorLogin,
    String displayName,
    String text,
    Integer authorColorRgb,
    List<MessageEmote> emotes,
    ChatAuthorRoles roles,
    String twitchRoomId,
    String twitchChannelName,
    String twitchEmotesTag,
    /** Unix-время сообщения в микросекундах (YouTube {@code timestampUsec}); null — показать сразу. */
    Long sourceTimestampMicros
) {
    public IncomingChatMessage {
        emotes = emotes != null ? emotes : Collections.emptyList();
        roles = roles != null ? roles : ChatAuthorRoles.NONE;
    }

    public static Builder builder(ChatPlatform platform) {
        return new Builder(platform);
    }

    public static final class Builder {
        private final ChatPlatform platform;
        private String authorLogin = "";
        private String displayName = "";
        private String text = "";
        private Integer authorColorRgb;
        private List<MessageEmote> emotes = Collections.emptyList();
        private ChatAuthorRoles roles = ChatAuthorRoles.NONE;
        private String twitchRoomId;
        private String twitchChannelName;
        private String twitchEmotesTag;
        private Long sourceTimestampMicros;

        private Builder(ChatPlatform platform) {
            this.platform = platform;
        }

        public Builder authorLogin(String authorLogin) {
            this.authorLogin = authorLogin;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder authorColorRgb(Integer authorColorRgb) {
            this.authorColorRgb = authorColorRgb;
            return this;
        }

        public Builder emotes(List<MessageEmote> emotes) {
            this.emotes = emotes;
            return this;
        }

        public Builder roles(ChatAuthorRoles roles) {
            this.roles = roles;
            return this;
        }

        public Builder twitchRoomId(String twitchRoomId) {
            this.twitchRoomId = twitchRoomId;
            return this;
        }

        public Builder twitchChannelName(String twitchChannelName) {
            this.twitchChannelName = twitchChannelName;
            return this;
        }

        public Builder twitchEmotesTag(String twitchEmotesTag) {
            this.twitchEmotesTag = twitchEmotesTag;
            return this;
        }

        public Builder sourceTimestampMicros(Long sourceTimestampMicros) {
            this.sourceTimestampMicros = sourceTimestampMicros;
            return this;
        }

        public IncomingChatMessage build() {
            return new IncomingChatMessage(
                platform,
                authorLogin,
                displayName,
                text,
                authorColorRgb,
                emotes,
                roles,
                twitchRoomId,
                twitchChannelName,
                twitchEmotesTag,
                sourceTimestampMicros
            );
        }
    }
}
