package club.mineplex.bot.common.mineplex.community;

import club.mineplex.bot.MineplexBot;
import club.mineplex.bot.UtilReference;
import club.mineplex.bot.common.cache.Cache;
import club.mineplex.bot.common.cache.GlobalCacheRepository;
import club.mineplex.bot.common.discord.Embed;
import club.mineplex.bot.common.discord.WebhookMessage;
import club.mineplex.bot.common.player.PlayerCache;
import club.mineplex.bot.common.player.PlayerData;
import club.mineplex.bot.database.Database;
import club.mineplex.bot.util.UtilFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class TrackedCommunityCache extends Cache<List<Community>> {

    private static final File COMMUNITIES_FILE = UtilFile.getAppResource("communities.json");

    private final List<Community> communities = new ArrayList<>();

    public TrackedCommunityCache() {
        super(5L);
        try {
            final JsonMapper mapper = new JsonMapper();
            this.communities.addAll(mapper.readValue(
                    COMMUNITIES_FILE,
                    new TypeReference<Collection<Community>>() {
                    }
            ));

            for (final Community community : this.communities) {
                Database.getInstance().getJdbi().useExtension(CommunitiesDatabase.class, extension -> {
                    extension.createTable(community.getName());
                });
            }
        } catch (final IOException e) {
            MineplexBot.getLogger().error("There was an error reading '{}'", COMMUNITIES_FILE.getName());
            e.printStackTrace();
        }
    }

    @Override
    public List<Community> get() {
        return this.communities;
    }

    @Override
    protected void updateCache() {
        this.saveData();
    }

    private void saveData() {
        for (final Community community : this.communities) {
            Database.getInstance().getJdbi().useExtension(CommunitiesDatabase.class, extension -> {
                community.getPlayerData().forEach(data -> {
                    final UUID playerUuid = data.getUuid();
                    final int kicks = data.getKicks();
                    final int bans = data.getBans();
                    final long messages = data.getMessages();
                    final String name = community.getName().toLowerCase();
                    try {
                        extension.insertRow(name, playerUuid.toString(), messages, kicks, bans);
                    } catch (final UnableToExecuteStatementException e) {
                        if (e.getMessage().contains("Duplicate entry")) {
                            extension.updateRow(name, playerUuid.toString(), messages, kicks, bans);
                        }
                    }
                });
            });
        }
    }

    public void handleKick(final String communityName, final String staff, final String kicked) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> kickedData = GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(kicked);
        if (!kickedData.isPresent()) {
            return;
        }

        final PlayerData kickedPlayer = kickedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(kickedPlayer.getUuid());
        punish.setKicks(punish.getKicks() + 1);
        this.saveData();

        final String kickedPlayerFormat = kickedPlayer.getName() + " (" + punish.getKicks() + ")";
        final String title = String.format("%s has been kicked by %s.", kickedPlayerFormat, staff);
        final Embed embed = Embed.builder().title(title).color(Color.RED).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    public void handleBan(final String communityName, final String staff, final String banned) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> bannedData = GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(banned);
        if (!bannedData.isPresent()) {
            return;
        }

        final PlayerData bannedPlayer = bannedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(bannedPlayer.getUuid());
        punish.setBans(punish.getBans() + 1);
        this.saveData();

        final String bannedPlayerFormat = bannedPlayer.getName() + " (" + punish.getBans() + ")";
        final String title = String.format("%s has been banned by %s.", bannedPlayerFormat, staff);
        final Embed embed = Embed.builder().title(title).color(Color.RED).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    public void handleUnban(final String communityName, final String staff, final String unbanned) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> unbannedData =
                GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(unbanned);
        if (!unbannedData.isPresent()) {
            return;
        }

        final PlayerData unbannedPlayer = unbannedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(unbannedPlayer.getUuid());

        final String bannedPlayerFormat = unbannedPlayer.getName() + " (" + punish.getBans() + ")";
        final String title = String.format("%s has been unbanned by %s.", bannedPlayerFormat, staff);
        final Embed embed = Embed.builder().title(title).color(Color.GREEN).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    public void handleInvite(final String communityName, final String staff, final String invited) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> invitedData = GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(invited);
        if (!invitedData.isPresent()) {
            return;
        }

        final PlayerData invitedPlayer = invitedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(invitedPlayer.getUuid());

        final String title = String.format("%s has been invited by %s.", invitedPlayer.getName(), staff);
        final Embed.Footer footer = new Embed.Footer(
                String.format("Kicks: %s   |   Bans: %s", punish.getKicks(), punish.getBans()), null
        );

        final Embed embed = Embed.builder().title(title).color(Color.GREEN).footer(footer).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    public void handleUninvite(final String communityName, final String staff, final String invited) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> invitedData = GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(invited);
        if (!invitedData.isPresent()) {
            return;
        }

        final PlayerData invitedPlayer = invitedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(invitedPlayer.getUuid());

        final String title = String.format("%s has been uninvited by %s.", invitedPlayer.getName(), staff);
        final Embed.Footer footer = new Embed.Footer(
                String.format("Kicks: %s   |   Bans: %s", punish.getKicks(), punish.getBans()), null
        );

        final Embed embed = Embed.builder().title(title).color(Color.RED).footer(footer).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    public void handleJoin(final String communityName, final String invited) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> invitedData = GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(invited);
        if (!invitedData.isPresent()) {
            return;
        }

        final PlayerData invitedPlayer = invitedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(invitedPlayer.getUuid());

        final String title = String.format("%s has joined %s.", invitedPlayer.getName(), community.getName());
        final Embed.Footer footer = new Embed.Footer(
                String.format("Kicks: %s   |   Bans: %s", punish.getKicks(), punish.getBans()), null
        );

        final Embed embed = Embed.builder().title(title).color(Color.GREEN).footer(footer).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    public void handleLeave(final String communityName, final String invited) {
        final Optional<Community> communityOpt = this.getTrackedCommunity(communityName);
        if (!communityOpt.isPresent()) {
            return;
        }

        final Community community = communityOpt.get();
        final Optional<PlayerData> invitedData = GlobalCacheRepository.getCache(PlayerCache.class).savePlayer(invited);
        if (!invitedData.isPresent()) {
            return;
        }

        final PlayerData invitedPlayer = invitedData.get();
        final Community.CommunityPlayerData punish = community.getPlayerData(invitedPlayer.getUuid());

        final String title = String.format("%s has left %s.", invitedPlayer.getName(), community.getName());
        final Embed.Footer footer = new Embed.Footer(
                String.format("Kicks: %s   |   Bans: %s", punish.getKicks(), punish.getBans()), null
        );

        final Embed embed = Embed.builder().title(title).color(Color.RED).footer(footer).build();
        for (final String webhookLink : community.getWebhookLinks()) {
            WebhookMessage.builder()
                          .url(webhookLink)
                          .avatar_url(UtilReference.MINEPLEX_SMALL_LOGO_URL)
                          .username(community.getName())
                          .embeds(new Embed[]{embed}).build()
                          .post();
        }
    }

    private Optional<Community> getTrackedCommunity(final String name) {
        final TrackedCommunityCache communityCache = GlobalCacheRepository.getCache(TrackedCommunityCache.class);
        final List<Community> communities = communityCache.get();
        return communities
                .stream()
                .filter(tracked -> tracked.getName().equals(name))
                .findAny();
    }

}
