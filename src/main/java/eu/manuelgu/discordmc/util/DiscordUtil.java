package eu.manuelgu.discordmc.util;

import java.util.logging.Level;

import eu.manuelgu.discordmc.DiscordMC;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

/**
 * Util class for utility methods
 */
public class DiscordUtil {

    /**
     * Logout the client
     *
     * @param client client to disconnect
     * @return True when disconnect was successful, False if otherwise
     */
    public static boolean logout(IDiscordClient client) {
        try {
            client.logout();
            return true;
        } catch (DiscordException ignored) {
            DiscordMC.get().getLogger().log(Level.WARNING, "Failed to logout:", ignored);
            return false;
        }
    }

    /**
     * Login the client
     *
     * @param client client to connect
     * @return True when connect was successful, False if otherwise
     */
    public static boolean login(IDiscordClient client) {
        try {
            client.login();
            return true;
        } catch (DiscordException | RateLimitException ignored) {
            DiscordMC.get().getLogger().log(Level.WARNING, "Failed to login:", ignored);
            return false;
        }
    }
}
