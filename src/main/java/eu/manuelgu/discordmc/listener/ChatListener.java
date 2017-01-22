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

public class ChatListener implements Listener {
    @Getter
    private final DiscordMC plugin;

    public ChatListener(DiscordMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.send_game_chat")) {
            return;
        }
        if (!isSubscribed(event.getPlayer()) || !hasChatPermission(event.getPlayer())) {
            return;
        }
        final String username = event.getPlayer().getName();
        final String message = event.getMessage();

        String formattedMessage = getPlugin().getConfig().getString("settings.templates.chat_message_discord")
                .replaceAll("%user", username)
                .replaceAll("%message", message);

        if (getPlugin().getConfig().getBoolean("settings.use_mentions", true)) {
            List<String> mentionNames = new ArrayList<>();
            Arrays.stream(formattedMessage.split(" ")).filter(s -> s.startsWith("@")).forEach(s -> mentionNames.add(s.substring(1)));

            if (!mentionNames.isEmpty()) {
                for (String s : mentionNames) {
                    List<IUser> users = DiscordMC.getClient().getGuilds().get(0).getUsersByName(s, true);

                    if (!users.isEmpty()) {
                        formattedMessage = formattedMessage.replaceAll("@" + s, "<@" + users.get(0).getID() + ">");
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
