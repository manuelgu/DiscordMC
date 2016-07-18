package eu.manuelgu.discordmc;

import com.vdurmont.emoji.EmojiParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.util.List;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class MessageAPI {
    private static DiscordMC plugin;

    public MessageAPI(DiscordMC main) {
        MessageAPI.plugin = main;
    }

    /**
     * Send a chat message to the Minecraft server
     *
     * @param origin {@link IChannel} the message came from
     * @param username player who sent the message
     * @param message  the actual message which gets sent
     */
    public static void sendToMinecraft(IChannel origin, String username, String message) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', DiscordMC.get().getConfig().getString("settings.templates.chat_message_minecraft")
                .replaceAll("%u", username)
                .replaceAll("%c", origin.getName()));

        DiscordMC.getPermissivePlayers().stream()
                .forEach(uuid -> Bukkit.getPlayer(uuid).sendMessage(
                        EmojiParser.parseToAliases(formattedMessage
                        .replaceAll("%m", ChatColor.stripColor(message)))));
    }

    /**
     * Send a raw message to the Minecraft server
     *
     * @param message raw message which gets sent
     */
    public static void sendRawToMinecraft(String message) {
        Bukkit.broadcastMessage(EmojiParser.parseToAliases(message));
    }

    /**
     * Send a chat message to the Discord server
     *
     * @param message the message which gets sent
     */
    public static void sendToDiscord(String message) {
        if (!DiscordMC.getClient().isReady()) {
            return;
        }
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            DiscordMC.minecraftToDiscord.forEach(channel -> sendToDiscord(channel, message));
        });
    }

    /**
     * Send a message to a specific channel
     * @param channel channel to receive message
     * @param message message to send
     */
    public static void sendToDiscord(IChannel channel, String message) {
        RequestBuffer.request(() -> {
            try {
                new MessageBuilder(DiscordMC.getClient()).appendContent(message).withChannel(channel).build();
            } catch (DiscordException ignored) {
            } catch (MissingPermissionsException e) {
                plugin.getLogger().severe("Your Bot is missing required permissions to perform this action! "
                    + e.getErrorMessage());
            }
            return null;
        });
    }

    /**
     * Send a message to a list of specific channels
     * @param channels channels that receive message
     * @param message message to send
     */
    public static void sendToDiscord(List<IChannel> channels, String message) {
        channels.stream().forEach(channel -> sendToDiscord(channel, message));
    }
}
