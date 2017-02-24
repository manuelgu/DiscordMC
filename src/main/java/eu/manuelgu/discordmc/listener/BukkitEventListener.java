package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import eu.manuelgu.discordmc.update.Updater;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitEventListener implements Listener {
    @Getter
    private final DiscordMC plugin;

    public BukkitEventListener(DiscordMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!hasChatPermission(event.getPlayer())) {
            return;
        } else {
            // Add to cache
            DiscordMC.getCachedHasChatPermission().add(event.getPlayer().getUniqueId());

            // Add player as a permissive player
            DiscordMC.getSubscribedPlayers().add(event.getPlayer().getUniqueId());
        }
        if (!getPlugin().getConfig().getBoolean("settings.send_game_login")) {
            return;
        }
        final String username = event.getPlayer().getName();
        final String formattedMessage = getPlugin().getConfig().getString("settings.templates.player_join_minecraft")
                .replaceAll("%user", username);

        MessageAPI.sendToDiscord(formattedMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove player as a permissive player
        DiscordMC.getSubscribedPlayers().remove(event.getPlayer().getUniqueId());
        if (!DiscordMC.getCachedHasChatPermission().contains(event.getPlayer().getUniqueId())) {
            return;
        }
        DiscordMC.getCachedHasChatPermission().remove(event.getPlayer().getUniqueId());
        if (!getPlugin().getConfig().getBoolean("settings.send_game_logout")) {
            return;
        }
        final String username = event.getPlayer().getName();
        final String formattedMessage = getPlugin().getConfig().getString("settings.templates.player_leave_minecraft")
                .replaceAll("%user", username);

        MessageAPI.sendToDiscord(formattedMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!getPlugin().getConfig().getBoolean("settings.send_death_message")) {
            return;
        }
        if (!isSubscribed(event.getEntity()) || !hasChatPermission(event.getEntity())) {
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

    private boolean isSubscribed(Player player) {
        return DiscordMC.getSubscribedPlayers().contains(player.getUniqueId());
    }

    private boolean hasChatPermission(Player player) {
        return player.hasPermission("discordmc.chat");
    }
}
