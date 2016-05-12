package eu.manuelgu.discordmc.listener;

import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitEventListener implements Listener {
	private final DiscordMC plugin;
	
	public BukkitEventListener(DiscordMC plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChat(AsyncPlayerChatEvent event) {
        plugin.getLogger().info(event.getEventName() + " fired");
		if (event.isCancelled()) {
            return;
		}
		if (!plugin.getConfig().getBoolean("settings.send_game_chat")) {
			return;
		}
		if (!event.getPlayer().hasPermission("discordmc.chat")) {
			return;
		}
		
		final String username = event.getPlayer().getName();
		final String message = event.getMessage();

		final String formattedMessage = plugin.getConfig().getString("settings.templates.chat_message_discord")
				.replaceAll("%u", username)
				.replaceAll("%m", message);

        MessageAPI.sendToDiscord(formattedMessage);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (!plugin.getConfig().getBoolean("settings.send_game_login")) {
			return;
		}
		
		final String username = event.getPlayer().getName();
		final String formattedMessage = plugin.getConfig().getString("settings.templates.player_join_minecraft")
                .replaceAll("%u", username);

        MessageAPI.sendToDiscord(formattedMessage);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (!plugin.getConfig().getBoolean("settings.send_game_logout")) {
			return;
		}
		final String username = event.getPlayer().getName();
		final String formattedMessage = plugin.getConfig().getString("settings.templates.player_leave_minecraft")
                .replaceAll("%u", username);

        MessageAPI.sendToDiscord(formattedMessage);
	}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("settings.send_death_message")) {
            return;
        }
        final String deathMessage = event.getDeathMessage();

        MessageAPI.sendToDiscord(deathMessage);
    }
}