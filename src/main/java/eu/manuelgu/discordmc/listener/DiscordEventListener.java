package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscordEventListener {
    private final DiscordMC plugin;
    private final boolean relayChat;
    private final boolean sendToConsole;
    private final boolean commands;
    private final boolean useNickname;
    private final String commandPrefix;

    public DiscordEventListener(DiscordMC plugin) {
        this.plugin = plugin;
        this.relayChat = plugin.getConfig().getBoolean("settings.send_discord_chat", true);
        this.commands = plugin.getConfig().getBoolean("settings.discord_commands.enabled", true);
        this.useNickname = plugin.getConfig().getBoolean("settings.use_nicknames", true);
        this.commandPrefix = plugin.getConfig().getString("settings.discord_commands.command_prefix", "?");
        this.sendToConsole = plugin.getConfig().getBoolean("settings.send_game_chat_also_to_console", true);
    }

    @EventSubscriber
    public void userChat(MessageReceivedEvent event) {
        if (commands && event.getMessage().getContent().startsWith(commandPrefix) && event.getMessage().getContent().length() > 1) {
            // Commands enabled and it is a valid command
            switch (event.getMessage().getContent().substring(1)) {
                case "help":
                    MessageAPI.sendToDiscord("Available commands: list");
                    break;
                case "list":
                    List<String> online = Bukkit.getOnlinePlayers().stream().map((Function<Player, String>) Player::getName).collect(Collectors.toList());

                    MessageAPI.sendToDiscord("There are " + online.size() + "/"
                            + Bukkit.getMaxPlayers() + " players online:"
                            + "\n" + StringUtils.join(online.toArray(), ", "));
                    break;
                default:
                    break;
            }
        } else {
            if (relayChat) {
                if (!DiscordMC.getDiscordToMinecraft().contains(event.getMessage().getChannel())) {
                    return;
                }
                
                String content = event.getMessage().getContent();
                List<IUser> mentions = event.getMessage().getMentions();
                List<IRole> roleMentions = event.getMessage().getRoleMentions();

                for (IUser u : mentions) {
                    String name = u.getNicknameForGuild(event.getMessage().getGuild());
                    String id = u.getStringID();

                    // User name
                    content = content.replaceAll("<@" + id + ">", "@" + name);
                    // Nick name
                    content = content.replaceAll("<@!" + id + ">", "@" + name);
                }

                for (IRole r : roleMentions) {
                    String roleName = r.getName();
                    String roleId = r.getStringID();

                    content = content.replaceAll("<@&" + roleId + ">", "@" + roleName);
                }

                final String finalContent = content;
                final String nickname;
                if (useNickname) {
                    nickname = event.getMessage().getAuthor().getNicknameForGuild(event.getMessage().getGuild());
                } else {
                    nickname = event.getMessage().getAuthor().getName();
                }
                MessageAPI.sendToMinecraft(event.getMessage().getChannel(), nickname, finalContent);

                if (sendToConsole) {
                    MessageAPI.sendToMinecraftConsole(event.getMessage().getChannel(), nickname, finalContent);
                }
            }
        }
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        // Set game to Minecraft
        DiscordMC.getClient().changePlayingText("Minecraft");

        // Check for file encoder (emoji-related)
        switch (System.getProperty("file.encoding")) {
            case "UTF8":
            case "UTF-8":
                break;
            default:
                plugin.getLogger().warning("WARNING ::: Your file encoder is set to something else than UTF-8. This might break emoji encoding");
                break;
        }

        if (event.getClient().getGuilds().size() == 0) {
            plugin.getLogger().warning("Your bot is not joined to any guild. Please follow the instructions on the Spigot page");
            DiscordMC.get().getServer().getPluginManager().disablePlugin(DiscordMC.get());
        }
    }
}
