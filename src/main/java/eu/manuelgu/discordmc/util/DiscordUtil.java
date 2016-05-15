package eu.manuelgu.discordmc.util;

import eu.manuelgu.discordmc.DiscordMC;
import java.util.List;
import java.util.stream.Collectors;
import sx.blah.discord.handle.obj.IChannel;

public class DiscordUtil {

    /**
     * Get a list of channels by matching a name
     *
     * @param name name of the channel
     * @return channel matching name
     */
    public static List<IChannel> getChannelMatchingName(String name) {
        return DiscordMC.getClient().getChannels(false).stream()
                .filter((channel) -> channel.getName().equals(name))
                .collect(Collectors.toList());
    }
}