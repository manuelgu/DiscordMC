package eu.manuelgu.discordmc.listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mojang.authlib.GameProfile;
import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_12_R1.PlayerConnection;
import net.minecraft.server.v1_12_R1.PlayerInteractManager;
import net.minecraft.server.v1_12_R1.WorldServer;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.StatusType;

public class DiscordEventListener {
    private final DiscordMC plugin;
    private final boolean relayChat;
    private final boolean sendToConsole;
    private final boolean commands;
    private final boolean useNickname;
    private final boolean showDiscordUsersInTablist;
    private final String tablistPlayerPrefix;
    private final String tablistAdminPrefix;
    private final String discordAdminRole;
    private final String commandPrefix;
    private final List<String> roles;

    public DiscordEventListener(DiscordMC plugin) {
        this.plugin = plugin;
        this.relayChat = plugin.getConfig().getBoolean("settings.send_discord_chat", true);
        this.commands = plugin.getConfig().getBoolean("settings.discord_commands.enabled", true);
        this.useNickname = plugin.getConfig().getBoolean("settings.use_nicknames", true);
        this.commandPrefix = plugin.getConfig().getString("settings.discord_commands.command_prefix", "?");
        this.sendToConsole = plugin.getConfig().getBoolean("settings.send_game_chat_also_to_console", true);
        this.discordAdminRole = plugin.getConfig().getString("settings.discord_admin_role", "Admin");
        this.tablistAdminPrefix = plugin.getConfig().getString("settings.templates.tablist_player_prefix", "§6D ");
        this.tablistPlayerPrefix = plugin.getConfig().getString("settings.templates.tablist_admin_prefix", "§7D ");
        this.roles = (List<String>) plugin.getConfig().getList("settings.show_in_tablist_discord_roles");
        this.showDiscordUsersInTablist = plugin.getConfig().getBoolean("settings.show_discord_users_in_tablist", false);
    }

    @EventSubscriber
    public void userChat(MessageReceivedEvent event) {
        if (commands && event.getMessage().getContent().startsWith(commandPrefix) && event.getMessage().getContent().length() > 1) {
            // Commands enabled and it is a valid command
            switch (event.getMessage().getContent().substring(1)) {
                case "help":
                    MessageAPI.sendToDiscord("Available commands: list");
                    break;
                case "list":
                    List<String> online = Bukkit
                            .getOnlinePlayers()
                            .stream()
                            .map((Function<Player, String>) Player::getName)
                            .collect(Collectors.toList());

                    MessageAPI.sendToDiscord("There are " + online.size() + "/"
                            + Bukkit.getMaxPlayers() + " players online:"
                            + "\n" + StringUtils.join(online.toArray(), ", "));
                    break;
                default:
                    break;
            }
        } else {
            if (relayChat && (DiscordMC.getDiscordToMinecraft().contains(event.getMessage().getChannel())
                    || DiscordMC.getNews().contains(event.getMessage().getChannel()))) {
                String content = event.getMessage().getContent();
                List<IUser> mentions = event.getMessage().getMentions();
                List<IRole> roleMentions = event.getMessage().getRoleMentions();

                for (IUser u : mentions) {
                    String name = u.getNicknameForGuild(event.getMessage().getGuild());
                    String id = u.getStringID();

                    // User name
                    content = content.replaceAll("<@" + id + ">", "@" + name);
                    // Nick name
                    content = content.replaceAll("<@!" + id + ">", "@" + name);
                }

                for (IRole r : roleMentions) {
                    String roleName = r.getName();
                    String roleId = r.getStringID();

                    content = content.replaceAll("<@&" + roleId + ">", "@" + roleName);
                }

                final String finalContent = content;
                final String nickname;
                if (useNickname) {
                    nickname = event.getMessage().getAuthor().getNicknameForGuild(event.getMessage().getGuild());
                } else {
                    nickname = event.getMessage().getAuthor().getName();
                }
                if (DiscordMC.getDiscordToMinecraft().contains(event.getMessage().getChannel())) {
                    MessageAPI.sendToMinecraft(event.getMessage().getChannel(), nickname, finalContent);

                    if (sendToConsole) {
                        MessageAPI.sendToMinecraftConsole(event.getMessage().getChannel(), nickname, finalContent);
                    }
                }
                if (DiscordMC.getNews().contains(event.getMessage().getChannel())) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try (Connection connection = DriverManager.getConnection("jdbc:mysql://"
                                            + DiscordMC.get().getConfig().getString("settings.mysql.ip") + ":"
                                            + DiscordMC.get().getConfig().getString("settings.mysql.port") + "/"
                                            + DiscordMC.get().getConfig().getString("settings.mysql.database")
                                            + "?characterEncoding=UTF-8&autoReconnect=true",
                                    DiscordMC.get().getConfig().getString("settings.mysql.username"),
                                    DiscordMC
                                            .get()
                                            .getConfig()
                                            .getString("settings.mysql.password", null));
                                 Statement statement = connection.createStatement()) {
                                DiscordMC.get().getLogger().info("Connection created");
                                String query = "INSERT INTO `DiscordMC_news`(`message`, `user`) " +
                                        "VALUES ('" + finalContent + "','" + nickname + "')";
                                statement.execute(query);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }.runTaskAsynchronously(plugin);
                }
            }
        }
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        // Set game to Minecraft
        DiscordMC.getClient().changePlayingText("Minecraft");

        // Check for file encoder (emoji-related)
        switch (System.getProperty("file.encoding")) {
            case "UTF8":
            case "UTF-8":
                break;
            default:
                plugin.getLogger().warning("WARNING ::: Your file encoder is set to something else than UTF-8. This might break emoji encoding");
                break;
        }

        if (event.getClient().getGuilds().size() == 0) {
            plugin.getLogger().warning("Your bot is not joined to any guild. Please follow the instructions on the Spigot page");
            DiscordMC.get().getServer().getPluginManager().disablePlugin(DiscordMC.get());
        }
    }

    @EventSubscriber
    public void onPresenceUpdate(PresenceUpdateEvent event) {
        if (showDiscordUsersInTablist) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Set<String> eventRoles = event
                            .getUser()
                            .getRolesForGuild(DiscordMC.getGuild())
                            .stream()
                            .map(IRole::getName)
                            .collect(Collectors.toSet());
                    if (Collections.disjoint(roles, eventRoles)) {
                        return;
                    }

                    MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                    WorldServer world = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();

                    final String prefix;

                    if (eventRoles.contains(discordAdminRole)) {
                        prefix = tablistAdminPrefix;
                    } else {
                        prefix = tablistPlayerPrefix;
                    }

                    String s = prefix + event.getUser().getNicknameForGuild(DiscordMC.getGuild());
                    s = s.substring(0, Math.min(s.length(), 18));
                    GameProfile profile = new GameProfile(UUID.randomUUID(), s);

                    PlayerInteractManager manager = new PlayerInteractManager(world);

                    EntityPlayer npc = new EntityPlayer(server, world, profile, manager);

                    final PacketPlayOutPlayerInfo info;
                    if (event.getNewPresence().getStatus() == StatusType.OFFLINE) {
                        info = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc);
                    } else if (event.getOldPresence().getStatus() == StatusType.OFFLINE) {
                        info = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc);
                    } else {
                        return;
                    }

                    Bukkit.getOnlinePlayers().forEach(player -> {
                        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
                        connection.sendPacket(info);
                    });
                }
            }.runTaskAsynchronously(plugin);
        }
    }
}
