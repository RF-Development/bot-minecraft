package club.mineplex.bot.common.discord;

import club.mineplex.bot.MineplexBot;
import club.mineplex.bot.util.HttpClientUtilities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

@Value
@Builder
public class WebhookMessage {

    @NonNull
    @JsonIgnore
    String url;

    String avatar_url;
    String content;
    String username;
    Embed[] embeds;

    @SneakyThrows
    public void post() {
        final ObjectMapper mapper = new ObjectMapper();
        final String payload = mapper.writeValueAsString(this);

        final RequestBody body = RequestBody.create(MediaType.parse("application/json"), payload);
        final Request request = new Request.Builder().url(this.url).post(body).build();
        try {
            final Response response = HttpClientUtilities.getOkHttpClient().newCall(request).execute();
            if (response.isSuccessful()) {
                return;
            }
            response.close();
        } catch (final IOException e) {
            MineplexBot.getLogger().error("There was an error sending a webhook with '{}'", this.url);
            e.printStackTrace();
        }
    }

}
