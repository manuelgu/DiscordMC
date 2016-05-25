package eu.manuelgu.discordmc.util;

import eu.manuelgu.discordmc.DiscordMC;
import java.util.List;
import java.util.stream.Collectors;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;

public class DiscordUtil {

    /**
     * Logout the client
     * @param client client to disconnect
     * @return True when disconnect was successful, False if otherwise
     */
    public static boolean logout(IDiscordClient client) {
        try {
            client.logout();
            return true;
        } catch (DiscordException | HTTP429Exception ignored) {
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
}
