package club.mineplex.bot.common.mineplex.community;

import club.mineplex.bot.UtilReference;
import club.mineplex.bot.common.discord.Embed;
import club.mineplex.bot.common.discord.WebhookMessage;
import club.mineplex.bot.common.mineplex.MineplexRank;
import club.mineplex.bot.common.player.PlayerData;
import club.mineplex.bot.database.Database;
import club.mineplex.bot.util.UtilText;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Value
public class Community {

    String name;
    @Getter(AccessLevel.PACKAGE)
    String[] webhookLinks;

    @JsonIgnore
    Set<CommunityPlayerData> punishData = new HashSet<>();

    public Community(@JsonProperty("name") final String name,
                     @JsonProperty("webhookLinks") final String[] webhookLinks) {
        this.name = name;
        this.webhookLinks = webhookLinks;
    }

    public Collection<CommunityPlayerData> getPlayerData() {
        return this.punishData;
    }

    public CommunityPlayerData getPlayerData(final UUID uuid) {
        for (final CommunityPlayerData punishData : this.punishData) {
            if (punishData.getUuid().equals(uuid)) {
                return punishData;
            }
        }

        final CommunityPlayerData defaultData = new CommunityPlayerData(uuid.toString(), 0, 0, 0);
        final AtomicReference<CommunityPlayerData> data = new AtomicReference<>(defaultData);
        Database.getInstance().getJdbi().useExtension(CommunitiesDatabase.class, extension -> {
            final CommunityPlayerData foundData = extension.getPunishData(this.getName(), uuid.toString());
            if (foundData != null) {
                data.set(foundData);
            }
        });

        this.punishData.add(data.get());
        return data.get();
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Message {

        Community community;
        PlayerData player;
        String text;

        public void postMessage() {


            final MineplexRank rank = this.player.getMineplexRank();
            final CommunityPlayerData data = this.community.getPlayerData()
                                                           .stream()
                                                           .filter(data_ -> data_
                                                                   .getUuid()
                                                                   .equals(this.player.getUuid()))
                                                           .findAny()
                                                           .orElse(new CommunityPlayerData(
                                                                   this.player.getUuid().toString(), 0, 0, 0
                                                           ));

            final int kicks = data.getKicks();
            final int bans = data.getBans();
            final long messages = data.getMessages();
            final String footer = String.format("Kicks: %s   |   Bans: %s   |   Messages: %s", kicks, bans, messages);
            final String prefix = UtilText.getRawTextComponent(((TextComponent) rank.getPrefix())) + " ";

            final Color color = Color.decode(rank.getPrefixColor().orElse(NamedTextColor.AQUA).asHexString());

            final Embed embed = Embed.builder()
                                     .color(color)
                                     .footer(new Embed.Footer(footer, null))
                                     .thumbnail(new Embed.Thumbnail(this.player.getAvatarUrl(), 100, 100))
                                     .description(UtilText.getDiscordCompatibleText(this.text))
                                     .title((rank.equals(MineplexRank.PLAYER) ? "" : prefix) + this.player.getName())
                                     .build();


            for (final String webhookLink : this.community.getWebhookLinks()) {
                WebhookMessage.builder()
                              .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                              .username(this.community.getName())
                              .url(webhookLink)
                              .embeds(new Embed[]{embed})
                              .build()
                              .post();
            }
        }

    }

    @Data
    public static class CommunityPlayerData {

        private final String uuid;
        private long messages;
        private int kicks;
        private int bans;

        public CommunityPlayerData(@ColumnName("uuid") final String uuid,
                                   @ColumnName("messages") final long messages,
                                   @ColumnName("kicks") final int kicks,
                                   @ColumnName("bans") final int bans) {
            this.messages = messages;
            this.uuid = uuid;
            this.kicks = kicks;
            this.bans = bans;
        }

        public UUID getUuid() {
            return UUID.fromString(this.uuid);
        }
    }

}
