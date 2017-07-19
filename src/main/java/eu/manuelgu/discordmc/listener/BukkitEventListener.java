package eu.manuelgu.discordmc.listener;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.mojang.authlib.GameProfile;
import eu.manuelgu.discordmc.DiscordMC;
import eu.manuelgu.discordmc.MessageAPI;
import eu.manuelgu.discordmc.update.Updater;
import lombok.Getter;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_12_R1.PlayerConnection;
import net.minecraft.server.v1_12_R1.PlayerInteractManager;
import net.minecraft.server.v1_12_R1.WorldServer;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.StatusType;

public class BukkitEventListener implements Listener {
    @Getter
    private final DiscordMC plugin;
    private final String tablistPlayerPrefix;
    private final String tablistAdminPrefix;
    private final String discordAdminRole;
    private final boolean showDiscordUsersInTablist;
    private final List<String> roles;

    public BukkitEventListener(DiscordMC plugin) {
        this.plugin = plugin;
        this.discordAdminRole = plugin.getConfig().getString("settings.discord_admin_role", "Admin");
        this.tablistAdminPrefix = plugin.getConfig().getString("settings.templates.tablist_player_prefix", "§6D ");
        this.tablistPlayerPrefix = plugin.getConfig().getString("settings.templates.tablist_admin_prefix", "§7D ");
        this.roles = (List<String>) plugin.getConfig().getList("settings.show_in_tablist_discord_roles");
        this.showDiscordUsersInTablist = plugin.getConfig().getBoolean("settings.show_discord_users_in_tablist", false);
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

        if (!showDiscordUsersInTablist) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    DiscordMC.getGuild().getUsers().forEach(user -> {
                        if (user.getPresence().getStatus() != StatusType.OFFLINE) {
                            final Set<String> userRoles = user
                                    .getRolesForGuild(DiscordMC.getGuild())
                                    .stream()
                                    .map(IRole::getName)
                                    .collect(Collectors.toSet());
                            if (!Collections.disjoint(roles, userRoles)) {
                                MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                                WorldServer world = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();

                                final String prefix;

                                if (userRoles.contains(discordAdminRole)) {
                                    prefix = tablistAdminPrefix;
                                } else {
                                    prefix = tablistPlayerPrefix;
                                }

                                String s = prefix + user.getNicknameForGuild(DiscordMC.getGuild()).orElseGet(user::getName);
                                s = s.substring(0, Math.min(s.length(), 15));
                                GameProfile profile = new GameProfile(UUID.randomUUID(), s);
                                PlayerInteractManager manager = new PlayerInteractManager(world);

                                EntityPlayer npc = new EntityPlayer(server, world, profile, manager);

                                final PacketPlayOutPlayerInfo info =
                                        new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc);

                                PlayerConnection connection = ((CraftPlayer) event.getPlayer()).getHandle().playerConnection;
                                connection.sendPacket(info);
                            }
                        }
                    });
                }
            }.runTaskAsynchronously(plugin);
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
