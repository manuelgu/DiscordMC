package eu.manuelgu.discordmc;

import com.vdurmont.emoji.EmojiParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;

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
        String formattedMessage = DiscordMC.get().getConfig().getString("settings.templates.chat_message_minecraft")
                .replaceAll("%u", username)
                .replaceAll("%m", message)
                .replaceAll("%c", origin.getName());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', EmojiParser.parseToAliases(formattedMessage)));
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
        if (DiscordMC.getClient().getGuilds().isEmpty()) {
            return;
        }
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            for (IChannel channel : DiscordMC.minecraftToDiscord) {
                try {
                    new MessageBuilder(DiscordMC.getClient()).appendContent(message).withChannel(channel).build();
                } catch (DiscordException e) {
                    plugin.getLogger().severe("Discord threw an exception while sending the message. " + e.getErrorMessage());
                } catch (HTTP429Exception ignored) {
                } catch (MissingPermissionsException e) {
                    plugin.getLogger().severe("Your Bot is missing required permission to perform this action! "
                            + e.getErrorMessage());
                }
            }
        });
    }
}