package club.mineplex.bot.chat;

import club.mineplex.bot.common.cache.GlobalCacheRepository;
import club.mineplex.bot.common.mineplex.community.TrackedCommunityCache;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SystemMessage {

    COMMUNITY_JOIN(Pattern.compile("Communities> ([^\\s]+) has joined ([^\\s]+)!"), (s, matcher) -> {
        GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                             .handleJoin(matcher.group(2), matcher.group(1));
    }),

    COMMUNITY_LEAVE(Pattern.compile("Communities> ([^\\s]+) has left ([^\\s]+)!"), (s, matcher) -> {
        GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                             .handleLeave(matcher.group(2), matcher.group(1));
    }),

    COMMUNITY_KICK(Pattern.compile("Communities> ([^\\s]+) has kicked ([^\\s]+) from ([^\\s]+)!"), (s, matcher) -> {
        GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                             .handleKick(matcher.group(3), matcher.group(1), matcher.group(2));
    }),


    COMMUNITY_UNBAN(Pattern.compile("Communities> ([^\\s]+) has unbanned ([^\\s]+) from ([^\\s]+)"), (s, matcher) -> {
        GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                             .handleUnban(matcher.group(3), matcher.group(1), matcher.group(2));
    }),

    COMMUNITY_BAN(Pattern.compile("Communities> ([^\\s]+) has banned ([^\\s]+) from ([^\\s]+)"), (s, matcher) -> {
        GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                             .handleBan(matcher.group(3), matcher.group(1), matcher.group(2));
    }),

    COMMUNITY_INVITE(Pattern.compile("Communities> ([^\\s]+) has invited ([^\\s]+) to ([^\\s]+)!"), (s, matcher) -> {
        GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                             .handleInvite(matcher.group(3), matcher.group(1), matcher.group(2));
    }),

    COMMUNITY_UNINVITE(
            Pattern.compile("Communities> ([^\\s]+)'s invitation to join ([^\\s]+) has been revoked by ([^\\s]+)!"),
            (s, matcher) -> {
                GlobalCacheRepository.getCache(TrackedCommunityCache.class)
                                     .handleUninvite(matcher.group(2), matcher.group(3), matcher.group(1));
            }
    );

    private final Pattern pattern;
    private final BiConsumer<String, Matcher> consumer;

    SystemMessage(final Pattern pattern, final BiConsumer<String, Matcher> consumer) {
        this.pattern = pattern;
        this.consumer = consumer;
    }

    public boolean processMessage(final String text) {
        final Matcher matcher = this.pattern.matcher(text);
        if (!matcher.matches()) {
            return false;
        }
        this.consumer.accept(text, matcher);
        return true;
    }

}