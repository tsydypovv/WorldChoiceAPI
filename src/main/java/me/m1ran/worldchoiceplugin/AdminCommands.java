package me.m1ran.worldchoiceplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class AdminCommands implements CommandExecutor {

    private final WorldChoicePlugin plugin;

    public AdminCommands(WorldChoicePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права администратора
        if (!sender.hasPermission("worldchoice.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setspawn")) {
            return handleSetSpawnCommand(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("changeworldfor")) {
            return handleChangeWorldForCommand(sender, args);
        }

        return false;
    }

    private boolean handleSetSpawnCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда может быть выполнена только игроками.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Укажите номер мира (1 или 2).");
            return false;
        }

        try {
            int worldNum = Integer.parseInt(args[0]);
            if (worldNum != 1 && worldNum != 2) {
                player.sendMessage(ChatColor.RED + "Укажите 1 или 2 для выбора мира.");
                return false;
            }

            // Сохраняем текущую локацию игрока как точку спавна для указанного мира
            Location playerLoc = player.getLocation();
            String worldPrefix = (worldNum == 1) ? "world1" : "world2";

            plugin.getConfig().set(worldPrefix + ".spawn.x", playerLoc.getX());
            plugin.getConfig().set(worldPrefix + ".spawn.y", playerLoc.getY());
            plugin.getConfig().set(worldPrefix + ".spawn.z", playerLoc.getZ());
            plugin.getConfig().set(worldPrefix + ".spawn.yaw", playerLoc.getYaw());
            plugin.getConfig().set(worldPrefix + ".spawn.pitch", playerLoc.getPitch());
            plugin.saveConfig();

            player.sendMessage(ChatColor.GREEN + "Точка спавна для мира " + worldNum + " установлена.");

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Укажите 1 или 2 для выбора мира.");
            return false;
        }

        return true;
    }

    private boolean handleChangeWorldForCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда может быть выполнена только игроками.");
            return true;
        }

        Player admin = (Player) sender;

        if (args.length < 2) {
            admin.sendMessage(ChatColor.RED + "Использование: /changeworldfor <имя_игрока> <номер_мира>");
            return false;
        }

        String playerName = args[0];

        try {
            int worldNum = Integer.parseInt(args[1]);
            if (worldNum != 1 && worldNum != 2) {
                admin.sendMessage(ChatColor.RED + "Укажите 1 или 2 для выбора мира.");
                return false;
            }

            // Получаем команды
            Team firstTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("first");
            Team secondTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("second");


            // Удаляем игрока из обеих команд (на всякий случай)
            if (firstTeam != null) firstTeam.removeEntry(playerName);
            plugin.getTeamManager().updateTeamInFile("first", firstTeam);

            if (secondTeam != null) secondTeam.removeEntry(playerName);
            plugin.getTeamManager().updateTeamInFile("second", secondTeam);

            // Добавляем игрока в выбранную команду и обновляем teams.yml
            if (worldNum == 1 && firstTeam != null) {
                firstTeam.addEntry(playerName);
                plugin.getTeamManager().updateTeamInFile("first", firstTeam);
            } else if (worldNum == 2 && secondTeam != null) {
                secondTeam.addEntry(playerName);
                plugin.getTeamManager().updateTeamInFile("second", secondTeam);
            }

            // Меняем мир игрока в конфиге
            return plugin.changeWorldFor(admin, playerName, worldNum);

        } catch (NumberFormatException e) {
            admin.sendMessage(ChatColor.RED + "Укажите 1 или 2 для выбора мира.");
            return false;
        }
    }
}