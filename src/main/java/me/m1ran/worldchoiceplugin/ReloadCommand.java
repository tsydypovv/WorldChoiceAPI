package me.m1ran.worldchoiceplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {

    private final WorldChoicePlugin plugin;

    public ReloadCommand(WorldChoicePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Проверка прав (только для администраторов или консоли)
        if (!sender.hasPermission("worldchoice.admin") && !(sender instanceof Player)) {
            sender.sendMessage("§cОшибка: У вас нет прав на эту команду!");
            return true;
        }

        // Перезагрузка конфигов и команд
        plugin.reloadConfig();
        plugin.getTeamManager().reloadTeams();

        sender.sendMessage("§aКонфигурация и команды успешно перезагружены!");
        return true;
    }
}
