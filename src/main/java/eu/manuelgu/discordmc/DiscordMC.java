package eu.manuelgu.discordmc;

import eu.manuelgu.discordmc.listener.BukkitEventListener;
import eu.manuelgu.discordmc.listener.DiscordEventListener;
import eu.manuelgu.discordmc.update.Updater;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
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
    public static DiscordMC instance;
    @Getter
    public static IDiscordClient client;

    /**
     * Channels that send messages to minecraft
     */
    @Getter
    public static List<IChannel> discordToMinecraft;

    /**
     * Channels that receive messages from minecraft
     */
    @Getter
    public static List<IChannel> minecraftToDiscord;


    @Override
    public void onEnable() {
        instance = this;
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Disable all modules
        Configuration.LOAD_EXTERNAL_MODULES = false;
        Configuration.AUTOMATICALLY_ENABLE_MODULES = false;

        new MessageAPI(this);

        String token = getConfig().getString("settings.token");

        // Token not entered, probably initial start
        if (token.equalsIgnoreCase("TOKEN_HERE") || token.equalsIgnoreCase("")) {
            getLogger().warning("You haven't entered a valid token for your bot. See Spigot page for a tutorial.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Client builder and login
        try {
            client = new ClientBuilder().withToken(token).login();
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

        // Check plugin updates
        if (getConfig().getBoolean("settings.check_for_updates")) {
            Updater.sendUpdateMessage(this);
        }

    }

    @EventSubscriber
    public void onGuildCreate(GuildCreateEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        discordToMinecraft = new ArrayList<>();
        minecraftToDiscord = new ArrayList<>();

        List<String> dTm = getConfig().getStringList("settings.channels.discord_to_minecraft");
        List<String> mTd = getConfig().getStringList("settings.channels.minecraft_to_discord");

        for (IChannel channel : getClient().getChannels(false)) {
            if (dTm.contains(channel.getName())) {
                discordToMinecraft.add(channel);
            }
            if (mTd.contains(channel.getName())) {
                minecraftToDiscord.add(channel);
            }
        }
    }

    @Override
    public void onDisable() {
        if (!DiscordMC.getClient().isReady()) {
            return;
        }
        try {
            client.logout();
        } catch (DiscordException | HTTP429Exception ignored) {
            getLogger().severe("Failed to logout");
        }
    }

    /**
     * Get instance of DiscordMC main class
     *
     * @return instance of main class
     */
    public static DiscordMC get() {
        return instance;
    }
}
