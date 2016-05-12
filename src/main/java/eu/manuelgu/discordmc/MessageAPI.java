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
    private static IChannel channel;

    public MessageAPI(DiscordMC main) {
        MessageAPI.plugin = main;
    }

    /**
     * Send a chat message to the minecraft server
     *
     * @param username player who sent the message
     * @param message  the actual message that gets sent
     */
    public static void sendToMinecraft(String username, String message) {
        String formattedMessage = DiscordMC.get().getConfig().getString("settings.templates.chat_message_minecraft")
                .replaceAll("%u", username)
                .replaceAll("%m", message);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', EmojiParser.parseToAliases(formattedMessage)));
    }

    /**
     * Send a raw message to the minecraft server
     *
     * @param message raw message that gets sent
     */
    public static void sendRawToMinecraft(String message) {
        Bukkit.broadcastMessage(EmojiParser.parseToAliases(message));
    }

    /**
     * Send a chat message to the discord server
     *
     * @param message the message that gets sent
     */
    public static void sendToDiscord(String message) {
        if (!DiscordMC.getClient().isReady()) {
            return;
        }
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            channel = DiscordMC.getChannel();

            if (channel == null) {
                plugin.getLogger().severe("Could not send message, channel was null");
                return;
            }

            try {
                new MessageBuilder(DiscordMC.getClient()).appendContent(message).withChannel(channel).build();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (HTTP429Exception ignored) {
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
                plugin.getLogger().severe("Your Bot is missing required permission to perform this action!");
            }
        });
    }
}