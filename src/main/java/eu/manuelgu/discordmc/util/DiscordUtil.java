package eu.manuelgu.discordmc.util;

import eu.manuelgu.discordmc.DiscordMC;
import java.util.List;
import java.util.stream.Collectors;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

/**
 * Util class for utility methods
 */
public class DiscordUtil {
    public static List<String> names;

    /**
     * Logout the client
     * @param client client to disconnect
     * @return True when disconnect was successful, False if otherwise
     */
    public static boolean logout(IDiscordClient client) {
        try {
            client.logout();
            return true;
        } catch (DiscordException | RateLimitException ignored) {
            return false;
        }
    }

    /**
     * Login the client
     * @param client client to connect
     * @return True when connect was successful, False if otherwise
     */
    public static boolean login(IDiscordClient client) {
        try {
            client.login();
            return true;
        } catch (DiscordException ignored) {
            return false;
        }
    }

    /**
     * Check whether a channel is valid to receive or send messages
     *
     * @param channel The channel to test
     * @return True when channel is valid, False if otherwise
     */
    public static boolean isValidChannel(IChannel channel) {
        return names.contains(channel.getName());
    }

    static {
        names = DiscordMC.discordToMinecraft.stream().map(IChannel::getName).collect(Collectors.toList());
    }
}
