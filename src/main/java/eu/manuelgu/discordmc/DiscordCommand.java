package eu.manuelgu.discordmc;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

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
                    DiscordMC.getClient().logout();
                    cs.sendMessage(ChatColor.GREEN + "Successfully logged off the Discord Bot");
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
                    DiscordMC.getClient().login();
                    cs.sendMessage(ChatColor.GREEN + "Successfully logged in the Discord Bot");
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
                DiscordMC.getClient().getGuilds().get(0).getUsers().stream().filter(u -> lookupUserName.equalsIgnoreCase(u.getName())).forEach(u -> lookupUser[0] = u);

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
            default:
                cs.sendMessage(ChatColor.RED + USAGE);
                break;
        }
		return true;
	}
}