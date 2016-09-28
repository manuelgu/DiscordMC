package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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

        final String formattedMessage = getPlugin().getConfig().getString("settings.templates.chat_message_discord")
                .replaceAll("%user", username)
                .replaceAll("%message", message);

        MessageAPI.sendToDiscord(formattedMessage);
    }

    private boolean isSubscribed(Player player) {
        return DiscordMC.getSubscribedPlayers().contains(player.getUniqueId());
    }

    private boolean hasChatPermission(Player player) {
        return player.hasPermission("discordmc.chat");
    }


}
