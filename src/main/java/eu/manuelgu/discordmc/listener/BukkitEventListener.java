package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import eu.manuelgu.discordmc.update.Updater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import lombok.Getter;

public class BukkitEventListener implements Listener {
    @Getter
    private final DiscordMC plugin;

    public BukkitEventListener(DiscordMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.send_game_chat")) {
            return;
        }
        if (!canChat(event.getPlayer())) {
            return;
        }
        final String username = event.getPlayer().getName();
        final String message = event.getMessage();

        final String formattedMessage = getPlugin().getConfig().getString("settings.templates.chat_message_discord")
                .replaceAll("%u", username)
                .replaceAll("%m", message);

        MessageAPI.sendToDiscord(formattedMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.send_game_login")) {
            return;
        }
        if (!canChat(event.getPlayer())) {
            return;
        } else {
            // Add to cache
            DiscordMC.getCachedHasChatPermission().add(event.getPlayer().getUniqueId());
            // Add player as a permissive player
            DiscordMC.getPermissivePlayers().add(event.getPlayer().getUniqueId());
        }
        final String username = event.getPlayer().getName();
        final String formattedMessage = getPlugin().getConfig().getString("settings.templates.player_join_minecraft")
                .replaceAll("%u", username);

        MessageAPI.sendToDiscord(formattedMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.send_game_logout")) {
            return;
        }
        if (!DiscordMC.getCachedHasChatPermission().contains(event.getPlayer().getUniqueId())) {
            return;
        }
        DiscordMC.getCachedHasChatPermission().remove(event.getPlayer().getUniqueId());
        final String username = event.getPlayer().getName();
        final String formattedMessage = getPlugin().getConfig().getString("settings.templates.player_leave_minecraft")
                .replaceAll("%u", username);

        MessageAPI.sendToDiscord(formattedMessage);
        // Remove player as a permissive player
        DiscordMC.getPermissivePlayers().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.send_death_message")) {
            return;
        }
        if (!canChat(event.getEntity())) {
            return;
        }
        final String deathMessage = event.getDeathMessage();

        MessageAPI.sendToDiscord(deathMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdminPlayerJoin(PlayerJoinEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.check_for_updates")) {
            return;
        }
        if (!event.getPlayer().hasPermission("discordmc.admin")) {
            return;
        }
        Updater.sendUpdateMessage(event.getPlayer().getUniqueId(), getPlugin());
    }

    private boolean canChat(Player player) {
        return player.hasPermission("discordmc.chat") && DiscordMC.getPermissivePlayers().contains(player.getUniqueId());
    }
}
