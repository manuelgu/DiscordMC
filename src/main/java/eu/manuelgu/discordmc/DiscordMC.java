package eu.manuelgu.discordmc;

import eu.manuelgu.discordmc.listener.BukkitEventListener;
import eu.manuelgu.discordmc.listener.DiscordEventListener;

import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.EventSubscriber;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.modules.Configuration;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;

public class DiscordMC extends JavaPlugin {
    @Getter
	public static IDiscordClient client;
    @Getter
    public static IChannel channel;
	public static DiscordMC instance;

	@Override
	public void onEnable() {
		instance = this;
		getConfig().options().copyDefaults(true);
		saveConfig();

        // Disable all modules
        Configuration.LOAD_EXTERNAL_MODULES = false;
        Configuration.AUTOMATICALLY_ENABLE_MODULES = false;

        new MessageAPI(this);

        // Client builder and login
        try {
            client = new ClientBuilder().withToken(getConfig().getString("settings.token")).login();
        } catch (DiscordException e) {
            e.printStackTrace();
            getLogger().severe("Failed to login");
        }

        // Register listeners
        client.getDispatcher().registerListener(new DiscordEventListener(this));
        client.getDispatcher().registerListener(this);

        // Register bukkit listeners and commands
		getServer().getPluginManager().registerEvents(new BukkitEventListener(this), this);
		getCommand("discord").setExecutor(new DiscordCommand());

    }

    @EventSubscriber
    public void onGuildCreate(GuildCreateEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        channel = null;
        final String channelName = getConfig().getString("settings.channel");

        getClient().getChannels(false).stream().filter(c -> c.getName().equalsIgnoreCase(channelName)).forEach(c -> channel = c);
    }

    @Override
    public void onDisable() {
        try {
            client.logout();
        } catch (DiscordException | HTTP429Exception e) {
            getLogger().severe("Failed to logout");
        }
    }

    /**
     * Get instance of DiscordMC main class
     * @return instance of main class
     */
    public static DiscordMC get() {
		return instance;
	}
}