package eu.manuelgu.discordmc;

import eu.manuelgu.discordmc.util.DiscordUtil;
import eu.manuelgu.discordmc.util.HastebinUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Presences;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscordCommand implements CommandExecutor {
    private final String USAGE = "Usage: /discord <logout|login|lookup|send|debug|toggle>";
    private final String LACKING_PERMISSION = "You are lacking the required permission to execute this command";

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (!(args.length > 0)) {
            cs.sendMessage(ChatColor.RED + USAGE);
            return true;
        }
        switch (args[0]) {
            // Log off the client from Discord
            case "logout":
            case "logoff":
                if (!cs.hasPermission("discordmc.command.logout")) {
                    cs.sendMessage(ChatColor.RED + LACKING_PERMISSION);
                    break;
                }
                if (!DiscordMC.getClient().isReady()) {
                    cs.sendMessage(ChatColor.RED + "Your bot is already disconnected");
                    break;
                }
                cs.sendMessage(ChatColor.GREEN + "Logging out..");
                if (!DiscordUtil.logout(DiscordMC.getClient())) {
                    cs.sendMessage(ChatColor.RED + "Error while logging off the bot. See console.");
                } else {
                    cs.sendMessage(ChatColor.GREEN + "Done.");
                }
                break;
            // Log in the client to Discord
            case "login":
                if (!cs.hasPermission("discordmc.command.login")) {
                    cs.sendMessage(ChatColor.RED + LACKING_PERMISSION);
                    break;
                }
                if (DiscordMC.getClient().isReady()) {
                    cs.sendMessage(ChatColor.RED + "Your bot is already connected");
                    break;
                }
                cs.sendMessage(ChatColor.GREEN + "Logging in..");
                if (!DiscordUtil.login(DiscordMC.getClient())) {
                    cs.sendMessage(ChatColor.RED + "Error while logging in the bot. See console.");
                } else {
                    cs.sendMessage(ChatColor.GREEN + "Done.");
                }
                break;
            case "lookup":
                if (!cs.hasPermission("discordmc.command.lookup")) {
                    cs.sendMessage(ChatColor.RED + LACKING_PERMISSION);
                    break;
                }
                if (args.length != 2) {
                    cs.sendMessage(ChatColor.RED + "Usage: /discord lookup <user>");
                    break;
                }
                String lookupUserName = args[1];
                final IUser[] lookupUser = new IUser[1];
                DiscordMC.getClient().getGuilds().get(0).getUsers().stream().filter(
                        u -> lookupUserName.equalsIgnoreCase(u.getName()))
                        .forEach(u -> lookupUser[0] = u);

                if (lookupUser[0] == null) {
                    cs.sendMessage(ChatColor.RED + "Unknown user");
                    break;
                }

                String name = lookupUser[0].getName();
                Optional<String> game = Optional.ofNullable(lookupUser[0].getStatus().getStatusMessage());
                boolean isBot = lookupUser[0].isBot();
                String id = lookupUser[0].getID();
                List<IRole> roles = lookupUser[0].getRolesForGuild(DiscordMC.getClient().getGuilds().get(0));
                String discriminator = lookupUser[0].getDiscriminator();
                Presences presences = lookupUser[0].getPresence();

                cs.sendMessage(ChatColor.BLUE + "Stats for user " + ChatColor.AQUA + name);
                cs.sendMessage(ChatColor.BLUE + "> Roles: " + ChatColor.AQUA + StringUtils.join(roles, ", "));
                cs.sendMessage(ChatColor.BLUE + "> Current game: " + ChatColor.AQUA + (game.isPresent() ? game.get() : "None"));
                cs.sendMessage(ChatColor.BLUE + "> ID: " + ChatColor.AQUA + id);
                cs.sendMessage(ChatColor.BLUE + "> Presence: " + ChatColor.AQUA + StringUtils.capitalize(presences.name().toLowerCase()));
                cs.sendMessage(ChatColor.BLUE + "> Is Bot: " + ChatColor.AQUA + StringUtils.capitalize(String.valueOf(isBot)));
                cs.sendMessage(ChatColor.BLUE + "> Discriminator: " + ChatColor.AQUA + "#" + discriminator);
                break;
            case "debug":
                if (!cs.hasPermission("discordmc.admin")) {
                    cs.sendMessage(ChatColor.RED + LACKING_PERMISSION);
                    break;
                }
                if (args.length == 3) {
                    String channelName = args[2];
                    if (args[1].equalsIgnoreCase("sendtest")) {
                        for (IChannel cha : DiscordMC.getClient().getGuilds().get(0).getChannelsByName(channelName)) {
                            MessageAPI.sendToDiscord(cha, "This is a test payload!");
                        }
                        cs.sendMessage(ChatColor.GREEN + "Sent a test payload");
                        break;
                    } else {
                        break;
                    }
                }
                String debugInfo = getDebugInfo();
                try {
                    cs.sendMessage(ChatColor.GOLD + HastebinUtility.upload(debugInfo));
                } catch (IOException e) {
                    cs.sendMessage(ChatColor.RED + "Unable to upload data.. Copy & paste console output");
                    DiscordMC.get().getLogger().info(debugInfo);
                }
                break;
            case "send":
                if (!cs.hasPermission("discordmc.command.send")) {
                    cs.sendMessage(ChatColor.RED + LACKING_PERMISSION);
                    break;
                }
                if (args.length < 3) {
                    cs.sendMessage(ChatColor.RED + "Usage: /discord send <channel> <message>");
                    break;
                }

                String channel = args[1];
                String message = "";
                // Starting from second argument
                for (int i = 2; i < args.length; i++) {
                    message += args[i] + " ";
                }
                String finalMessage = message;
                List<IChannel> ch = DiscordMC.getClient().getGuilds().get(0).getChannelsByName(channel);

                if (ch == null) {
                    cs.sendMessage(ChatColor.RED + "Channel not found");
                    break;
                }
                MessageAPI.sendToDiscord(ch, finalMessage);
                cs.sendMessage(ChatColor.GREEN + "Message sent");
                break;
            case "toggle":
                if (!cs.hasPermission("discordmc.command.toggle")) {
                    cs.sendMessage(ChatColor.RED + LACKING_PERMISSION);
                    break;
                }
                if (!(cs instanceof Player)) {
                    cs.sendMessage(ChatColor.RED + "Only players can execute this command");
                    break;
                }
                Player player = (Player) cs;
                if (DiscordMC.getSubscribedPlayers().contains(player.getUniqueId())) {
                    DiscordMC.getSubscribedPlayers().remove(player.getUniqueId());
                    // toggled off
                    player.sendMessage(ChatColor.BLUE + "You have disabled the Discord module for yourself.");
                } else {
                    DiscordMC.getSubscribedPlayers().add(player.getUniqueId());
                    // toggled on
                    player.sendMessage(ChatColor.BLUE + "You have enabled the Discord module for yourself.");
                }
            default:
                cs.sendMessage(ChatColor.RED + USAGE);
                break;
        }
        return true;
    }

    /**
     * Get debug information about current setup
     *
     * @return debug info in one long string with line breaks
     */
    private String getDebugInfo() {
        String configFileString = null;

        try {
            File config = new File(DiscordMC.get().getDataFolder(), "config.yml");
            List<String> lines = FileUtils.readLines(config);
            List<String> updatedLines = lines.stream().filter(s -> !s.contains("token")).collect(Collectors.toList());
            for (String s : updatedLines) {
                configFileString += s + System.lineSeparator();
            }
            configFileString = HastebinUtility.upload(configFileString);
        } catch (IOException ignored) {
            configFileString = "Couldn't paste config.";
        }

        StringBuilder b = new StringBuilder();
        // General
        b.append("# Some general stuff\n");
        b.append("versionServer: ").append(Bukkit.getVersion()).append(" (").append(Bukkit.getBukkitVersion()).append(")").append('\n');
        b.append("plugins:");
        for (Plugin plugin : DiscordMC.get().getServer().getPluginManager().getPlugins()) {
            String plEnabled = plugin.isEnabled() ? "true" : "false";
            String plName = plugin.getName();
            String plVersion = plugin.getDescription().getVersion();
            b.append("\n  ").append(plName).append(":\n    ").append("version: '").append(plVersion).append('\'').append("\n    enabled: ")
                    .append(plEnabled);
        }

        // JVM
        b.append("\n\n# Now some jvm related information\n");
        Runtime runtime = Runtime.getRuntime();
        b.append("memory.free: ").append(runtime.freeMemory()).append('\n');
        b.append("memory.max: ").append(runtime.maxMemory()).append('\n');
        b.append("java.specification.version: '").append(System.getProperty("java.specification.version")).append("'\n");
        b.append("java.vendor: '").append(System.getProperty("java.vendor")).append("'\n");
        b.append("java.version: '").append(System.getProperty("java.version")).append("'\n");
        b.append("os.arch: '").append(System.getProperty("os.arch")).append("'\n");
        b.append("os.name: '").append(System.getProperty("os.name")).append("'\n");
        b.append("os.version: '").append(System.getProperty("os.version")).append("'\n\n");

        // DiscordMC
        b.append("# DiscordMC related stuff\n");
        b.append("configFile: ").append(configFileString).append('\n');
        b.append("isReady: ").append(String.valueOf(DiscordMC.getClient().isReady())).append('\n');
        b.append("guilds:");
        for (IGuild guild : DiscordMC.getClient().getGuilds()) {
            String guildName = guild.getName();
            b.append("\n  ").append("\'" + guildName + "\'");
        }
        b.append("\nbotName: ").append(DiscordMC.getClient().getOurUser().getName()).append('\n');
        b.append("channels:\n");
        b.append("  minecraftToDiscord: ").append(StringUtils.join(
                DiscordMC.getMinecraftToDiscord().stream().map(IChannel::getName).collect(Collectors.toList()), ", ")).append('\n');
        b.append("  discordToMinecraft: ").append(StringUtils.join(
                DiscordMC.getMinecraftToDiscord().stream().map(IChannel::getName).collect(Collectors.toList()), ", ")).append('\n');

        return b.toString();
    }
}
