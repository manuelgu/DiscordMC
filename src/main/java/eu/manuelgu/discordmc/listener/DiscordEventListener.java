package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Optional;
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
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Status;
import sx.blah.discord.util.DiscordException;

public class DiscordEventListener {
    private final DiscordMC plugin;
    private final long RECONNECT_DELAY = TimeUnit.SECONDS.toMillis(15);
    private Timer reconnectTimer;
    private boolean relayChat;
    private boolean commands;
    private boolean useNickname;
    private List<String> names;

    public DiscordEventListener(DiscordMC plugin) {
        this.plugin = plugin;
        relayChat = plugin.getConfig().getBoolean("settings.send_discord_chat");
        commands = plugin.getConfig().getBoolean("settings.discord_commands.enabled");
        useNickname = plugin.getConfig().getBoolean("settings.use_nicknames");
        names = DiscordMC.discordToMinecraft.stream().map(IChannel::getName).collect(Collectors.toList());
    }

    @EventSubscriber
    public void userChat(final MessageReceivedEvent event) {
        final String channelName = event.getMessage().getChannel().getName();
        if (!names.contains(channelName)) {
            return;
        }

        if (commands && event.getMessage().getContent().startsWith(plugin.getConfig().getString("settings.discord_commands.command_prefix")) && event.getMessage().getContent().length() > 1) {
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
                List<IRole> roleMentions = event.getMessage().getRoleMentions();

                for (IUser u : mentions) {
                    String name = u.getNicknameForGuild(event.getMessage().getGuild()).get();
                    String id = u.getID();

                    // User name
                    content = content.replaceAll("<@" + id + ">", "@" + name);
                    // Nick name
                    content = content.replaceAll("<@!" + id + ">", "@" + name);
                }

                for (IRole r : roleMentions) {
                    String roleName = r.getName();
                    String roleId = r.getID();

                    content = content.replaceAll("<@&" + roleId + ">", "@" + roleName);
                }

                String nickname = null;
                if (useNickname && event.getMessage().getAuthor().getNicknameForGuild(event.getMessage().getGuild()).isPresent()) {
                    Optional<String> nick = event.getMessage().getAuthor().getNicknameForGuild(event.getMessage().getGuild());
                    if (nick.isPresent()) {
                        nickname = nick.get();
                    }
                } else {
                    useNickname = false;
                }

                MessageAPI.sendToMinecraft(event.getMessage().getChannel(),
                        useNickname ? nickname : event.getMessage().getAuthor().getName(),
                        content);
            }
        }
    }

    @EventSubscriber
    public void onDisconnect(final DiscordDisconnectedEvent event) {
        plugin.getLogger().info("Bot got disconnected with reason " + event.getReason().name());
        if (event.getReason() == DiscordDisconnectedEvent.Reason.LOGGED_OUT) {
            return;
        }

        // Auto reconnect
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
            }, (long) 2000, RECONNECT_DELAY);
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("There was a problem with reconnecting.. Please report the above stacktrace to the developer");
        }
    }

    @EventSubscriber
    public void onReady(final ReadyEvent event) {
        // Set game to Minecraft
        DiscordMC.getClient().changeStatus(Status.game("Minecraft"));

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
