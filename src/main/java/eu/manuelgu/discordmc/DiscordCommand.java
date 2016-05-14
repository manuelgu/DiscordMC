package eu.manuelgu.discordmc;

import eu.manuelgu.discordmc.util.HastebinUtility;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Presences;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;

public class DiscordCommand implements CommandExecutor {
    private final String USAGE = "Usage: /discord <logout|login|lookup>";
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
                try {
                    cs.sendMessage(ChatColor.GREEN + "Logging out..");
                    DiscordMC.getClient().logout();
                    cs.sendMessage(ChatColor.GREEN + "Done.");
                } catch (DiscordException | HTTP429Exception e) {
                    cs.sendMessage(ChatColor.RED + "Error while logging off the bot. See console.");
                    e.printStackTrace();
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
                try {
                    cs.sendMessage(ChatColor.GREEN + "Logging in..");
                    DiscordMC.getClient().login();
                    cs.sendMessage(ChatColor.GREEN + "Done.");
                } catch (DiscordException e) {
                    cs.sendMessage(ChatColor.RED + "Error while logging in the bot. See console.");
                    e.printStackTrace();
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
                Optional<String> game = lookupUser[0].getGame();
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
                StringBuilder b = new StringBuilder();
                // General
                b.append("# Some general stuff\n");
                b.append("version.server: ").append(Bukkit.getVersion() + " (" + Bukkit.getBukkitVersion() + ")").append('\n');
                b.append("plugins:");
                for (Plugin plugin : DiscordMC.get().getServer().getPluginManager().getPlugins()) {
                    String plEnabled = plugin.isEnabled() ? "true" : "false";
                    String plName = plugin.getName();
                    String plVersion = plugin.getDescription().getVersion();
                    b.append("\n  ").append(plName).append(":\n    ").append("version: '").append(plVersion).append('\'').append("\n    enabled: ")
                            .append(plEnabled);
                }

                // JVM
                b.append("\n# Now some jvm related information\n");
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
                b.append("\n\n# DiscordMC related stuff\n");
                b.append("isReady: ").append(String.valueOf(DiscordMC.getClient().isReady())).append('\n');
                b.append("botName: ").append(DiscordMC.getClient().getOurUser().getName()).append('\n');
                b.append("channels.minecraftToDiscord: ").append(StringUtils.join(
                        DiscordMC.minecraftToDiscord.stream().map(IChannel::getName).collect(Collectors.toList()), ", ")).append('\n');
                b.append("channels.discordToMinecraft: ").append(StringUtils.join(
                        DiscordMC.discordToMinecraft.stream().map(IChannel::getName).collect(Collectors.toList()), ", ")).append('\n');

                try {
                    cs.sendMessage(ChatColor.GOLD + HastebinUtility.upload(b.toString()));
                } catch (IOException e) {
                    cs.sendMessage(ChatColor.RED + "Unable to upload data.. Copy & paste console output");
                    System.out.println(b.toString());
                }
                break;
            default:
                cs.sendMessage(ChatColor.RED + USAGE);
                break;
        }
        return true;
    }
}