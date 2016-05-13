package eu.manuelgu.discordmc.update;


import eu.manuelgu.discordmc.DiscordMC;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class Updater {
    public final static String PREFIX = ChatColor.GREEN + "" + ChatColor.BOLD + "[DiscordMC] " + ChatColor.GREEN;
    private final static String URL = "http://api.spiget.org/v1/resources/";
    private final static int PLUGIN = 17067;

    public static void sendUpdateMessage(final UUID uuid, final Plugin plugin) {
        if (DiscordMC.get().getDescription().getVersion().endsWith("-SNAPSHOT")) {
            return;
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                final String message = getUpdateMessage(false);
                if (message != null) {
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                p.sendMessage(PREFIX + message);
                            }
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public static void sendUpdateMessage(final Plugin plugin) {
        if (DiscordMC.get().getDescription().getVersion().endsWith("-SNAPSHOT")) {
            return;
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                final String message = getUpdateMessage(true);
                if (message != null) {
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            plugin.getLogger().warning(message);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private static String getUpdateMessage(boolean console) {
        String newestString = getNewestVersion();
        if (newestString == null) {
            if (console) {
                return "Could not check for updates, check your connection.";
            } else {
                return null;
            }
        }
        Version current;
        try {
            current = new Version(DiscordMC.get().getDescription().getVersion());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return "You are using a debug/custom version, consider updating.";
        }
        Version newest = new Version(newestString);
        if (current.compareTo(newest) < 0)
            return "There is a newer version available: "
                    + newest.toString()
                    + "\n"
                    + "https://www.spigotmc.org/resources/17067/";
        else if (console && current.compareTo(newest) != 0) {
            return "You are running a newer version than is released!";
        }
        return null;
    }

    public static String getNewestVersion() {
        try {
            URL url = new URL(URL + PLUGIN + "?" + System.currentTimeMillis());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "DiscordMC " + DiscordMC.get().getDescription().getVersion());
            connection.setDoOutput(true);
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String input;
            String content = "";
            while ((input = br.readLine()) != null) {
                content = content + input;
            }
            br.close();
            JSONParser parser = new JSONParser();
            JSONObject statistics;
            try {
                statistics = (JSONObject) parser.parse(content);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            return (String) statistics.get("version");
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
