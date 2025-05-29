package me.m1ran.worldchoiceplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

public class PlayerEventListener implements Listener {

    private final WorldChoicePlugin plugin;

    public PlayerEventListener(WorldChoicePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String world = plugin.getPlayerWorld(player);

        if (!plugin.getConfig().contains("players." + player.getUniqueId() + ".world")) {
            plugin.showWorldChoice(player);

            // Если задача уже есть — отменяем её (на всякий случай)
            if (plugin.repeatTasks.containsKey(player.getUniqueId())) {
                plugin.repeatTasks.get(player.getUniqueId()).cancel();
            }

            BukkitTask task = new BukkitRunnable() {
                private int count = 0;
                private final int maxRepeats = 15;

                @Override
                public void run() {
                    if (!player.isOnline() || count >= maxRepeats || plugin.hasChosenWorld(player)) {
                        this.cancel();
                        plugin.repeatTasks.remove(player.getUniqueId());
                        return;
                    }
                    plugin.showWorldChoice(player);
                    count++;
                }
            }.runTaskTimer(plugin, 0L, 20L * 5);

            plugin.repeatTasks.put(player.getUniqueId(), task);
        }

        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(world);
        if (team != null) {
            team.addEntry(player.getName());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
            Player player = event.getPlayer();
            String chosenWorld = plugin.getPlayerWorld(player);

            if (chosenWorld != null) {
                Location respawnLoc = null;
                if ("world1".equals(chosenWorld)) {
                    respawnLoc = plugin.getWorld1Spawn();
                } else if ("world2".equals(chosenWorld)) {
                    respawnLoc = plugin.getWorld2Spawn();
                }

                if (respawnLoc != null) {
                    event.setRespawnLocation(respawnLoc);
                }
            }
        }
    }
}
