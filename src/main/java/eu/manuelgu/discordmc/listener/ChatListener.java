package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ChatListener implements Listener {
    @Getter
    private final DiscordMC plugin;
    private final Boolean useIngameFormat;
    private final String adminChatPrefix;

    public ChatListener(DiscordMC plugin) {
        this.plugin = plugin;
        this.useIngameFormat = getPlugin().getConfig().getBoolean("settings.use_ingame_format", false);
        this.adminChatPrefix = getPlugin().getConfig().getString("settings.admin_chat_prefix", "§cAdminChat>");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!getPlugin().getConfig().getBoolean("settings.send_game_chat")) {
            return;
        }
        if (!isSubscribed(event.getPlayer()) || !hasChatPermission(event.getPlayer())) {
            return;
        }
        final String username = event.getPlayer().getName();
        final String message = event.getMessage();

        if (event.getFormat().startsWith(adminChatPrefix)) {
            return;
        }

        String formattedMessage;

        if (useIngameFormat && !Objects.equals(DiscordMC.getUserFormats().get(event.getPlayer().getDisplayName()), event.getFormat())) {
            DiscordMC.getUserFormats().set(event.getPlayer().getDisplayName(), event.getFormat());
            DiscordMC.saveUserFormats();
        }
        String format = DiscordMC.getUserFormats().getString(username, "-");
        if (!useIngameFormat || Objects.equals(format, "-")) {
            formattedMessage = getPlugin().getConfig().getString("settings.templates.chat_message_discord")
                    .replaceAll("%user", username)
                    .replaceAll("%message", message);
        } else {
            formattedMessage = format.replace("%1$s", "**" + username + "**").replace("%2$s", message);
        }


        if (getPlugin().getConfig().getBoolean("settings.use_mentions", true)) {
            List<String> mentionNames = new ArrayList<>();
            Arrays.stream(formattedMessage.split(" ")).filter(s -> s.startsWith("@")).forEach(s -> mentionNames.add(s.substring(1)));

            if (!mentionNames.isEmpty()) {
                for (String s : mentionNames) {
                    List<IUser> users = DiscordMC.getClient().getGuilds().get(0).getUsersByName(s, true);

                    if (!users.isEmpty()) {
                        formattedMessage = formattedMessage.replaceAll("@" + s, "<@" + users.get(0).getStringID() + ">");
                    }
                }
            }
        }

        MessageAPI.sendToDiscord(formattedMessage);
    }

    private boolean isSubscribed(Player player) {
        return DiscordMC.getSubscribedPlayers().contains(player.getUniqueId());
    }

    private boolean hasChatPermission(Player player) {
        return player.hasPermission("discordmc.chat");
    }

}
