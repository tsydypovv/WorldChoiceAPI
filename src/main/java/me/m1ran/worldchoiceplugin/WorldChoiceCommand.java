package me.m1ran.worldchoiceplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class WorldChoiceCommand implements CommandExecutor {

    private final WorldChoicePlugin plugin;
    private Team team;

    public WorldChoiceCommand(WorldChoicePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда может быть выполнена только игроками.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            plugin.showWorldChoice(player);
            return true;
        }

        try {
            int worldNum = Integer.parseInt(args[0]);
            if (worldNum != 1 && worldNum != 2) {
                player.sendMessage(ChatColor.RED + "Укажите 1 или 2 для выбора мира.");
                return false;
            }

            if (worldNum == 1) {
                Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("first");
                team.addEntry(player.getName());
                plugin.getTeamManager().updateTeamInFile("first", team);
            } else {
                Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("second");
                team.addEntry(player.getName());
                plugin.getTeamManager().updateTeamInFile("second", team);
            }
            plugin.getTeamManager().reloadTeams();
            plugin.chooseWorld(player, worldNum);

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Укажите 1 или 2 для выбора мира.");
            return false;
        }

        return true;
    }

}