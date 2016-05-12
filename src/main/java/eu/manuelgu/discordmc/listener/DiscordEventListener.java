package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import sx.blah.discord.api.EventSubscriber;
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;

public class DiscordEventListener {
    private final DiscordMC plugin;
    private final long RECONNECT_DELAY = TimeUnit.SECONDS.toMillis(15);
    private Timer reconnectTimer;

    public DiscordEventListener(DiscordMC plugin) {
        this.plugin = plugin;
    }

    @EventSubscriber
    public void userChat(final MessageReceivedEvent event) {
        final String channelName = event.getMessage().getChannel().getName();
        if (!channelName.equalsIgnoreCase(plugin.getConfig().getString("settings.channel"))) {
            return;
        }

        boolean relayChat = plugin.getConfig().getBoolean("settings.send_discord_chat");
        boolean commands = plugin.getConfig().getBoolean("settings.enable_discord_commands");

        if (commands && event.getMessage().getContent().startsWith(plugin.getConfig().getString("settings.command_prefix")) && event.getMessage().getContent().length() > 1) {
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
                String content = event.getMessage().getContent();
                List<IUser> mentions = event.getMessage().getMentions();

                for (IUser u : mentions) {
                    String name = u.getName();
                    String id = u.getID();

                    content = content.replaceAll("<@" + id + ">", "@" + name);
                }

                String[] trimmedContent = content.split("\\s+");
                int i = 0;
                for (String tmp : trimmedContent) {
                    if (tmp.startsWith("<@&") && tmp.endsWith(">")) {
                        // Is role mention
                        String id = tmp.substring(3, tmp.length() - 1);
                        String roleName = event.getMessage().getGuild().getRoleByID(id).getName();

                        trimmedContent[i] = "@" + roleName;
                    }
                    i++;
                }

                MessageAPI.sendToMinecraft(event.getMessage().getAuthor().getName(), StringUtils.join(Arrays.asList(trimmedContent), " "));
            }
        }
    }

    @EventSubscriber
    public void onDisconnect(final DiscordDisconnectedEvent event) {
        plugin.getLogger().info("Bot got disconnected with reason " + event.getReason().name());
        if (event.getReason() == DiscordDisconnectedEvent.Reason.LOGGED_OUT) {
            return;
        }

        try {
            reconnectTimer = new Timer();
            reconnectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!event.getClient().isReady()) {
                        try {
                            event.getClient().login();
                            plugin.getLogger().info("Logged in again after a timeout");
                        } catch (DiscordException e) {
                            plugin.getLogger().severe("Failed to relog to the Discord servers..");
                            e.printStackTrace();
                        }
                    } else {
                        reconnectTimer.cancel();
                    }
                }
            }, (long) 0, RECONNECT_DELAY);
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("There was a problem with reconnecting.. Please report the above stacktrace to the developer");
        }
    }

    @EventSubscriber
    public void onReady(final ReadyEvent event) {
        plugin.getLogger().info("Successfully logged in with '" + event.getClient().getOurUser().getName() + "'");

        // Check for file encoder (emoji-related)
        switch (System.getProperty("file.encoding")) {
            case "UTF8":
            case "UTF-8":
                break;
            default:
                plugin.getLogger().warning("WARNING ::: Your file encoder is set to something else than UTF-8. This might break emoji encoding");
                break;
        }
    }
}