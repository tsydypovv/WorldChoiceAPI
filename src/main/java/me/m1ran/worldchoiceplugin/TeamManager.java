package me.m1ran.worldchoiceplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class TeamManager {

    private final WorldChoicePlugin plugin;
    private Scoreboard scoreboard;
    private FileConfiguration teamsConfig;

    public TeamManager(WorldChoicePlugin plugin) {
        this.plugin = plugin;
        loadTeams();
    }

    // Загружает или создаёт файл teams.yml
    public void loadTeams() {
        File file = new File(plugin.getDataFolder(), "teams.yml");
        if (!file.exists()) {
            plugin.saveResource("teams.yml", false);
        }
        teamsConfig = YamlConfiguration.loadConfiguration(file);
    }

    // Сохраняет текущие команды в файл
    public void saveTeams() {
        try {
            teamsConfig.save(new File(plugin.getDataFolder(), "teams.yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить teams.yml", e);
        }
    }


    // Настройка всех команд
    public void setupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ConfigurationSection teamsSection = teamsConfig.getConfigurationSection("teams");

        if (teamsSection == null) {
            plugin.getLogger().warning("В teams.yml нет секции 'teams'.");
            return;
        }

        for (String teamName : teamsSection.getKeys(false)) {
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            // Основные настройки
            team.setAllowFriendlyFire(teamsSection.getBoolean(teamName + ".friendly-fire", true));
            team.setCanSeeFriendlyInvisibles(teamsSection.getBoolean(teamName + ".see-invisible", false));
            team.setPrefix(ChatColor.translateAlternateColorCodes('&',
                    teamsSection.getString(teamName + ".prefix", "")));

            // Настройка опций: коллизия и видимость
            try {
                String nameTag = teamsSection.getString(teamName + ".nametag-visibility", "ALWAYS").toUpperCase();
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.valueOf(nameTag));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверное значение 'nametag-visibility' для команды " + teamName);
            }

            try {
                String collision = teamsSection.getString(teamName + ".collision-rule", "ALWAYS").toUpperCase();
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.valueOf(collision));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверное значение 'collision-rule' для команды " + teamName);
            }

            // Добавление участников
            if (teamsSection.contains(teamName + ".members")) {
                List<String> members = teamsSection.getStringList(teamName + ".members");

                for (String playerName : members) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                    team.addEntry(offlinePlayer.getName());
                }
            }
        }
    }


    // Обновляет файл teams.yml при изменении команды
    public void updateTeamInFile(String teamName, Team team) {
        teamsConfig.set("teams." + teamName + ".prefix", team.getPrefix());
        teamsConfig.set("teams." + teamName + ".friendly-fire", team.allowFriendlyFire());
        teamsConfig.set("teams." + teamName + ".see-invisible", team.canSeeFriendlyInvisibles());
        teamsConfig.set("teams." + teamName + ".nametag-visibility",
                team.getOption(Team.Option.NAME_TAG_VISIBILITY).name());
        teamsConfig.set("teams." + teamName + ".collision-rule",
                team.getOption(Team.Option.COLLISION_RULE).name());
        teamsConfig.set("teams." + teamName + ".members",
                team.getEntries().stream().toList());
        saveTeams();
    }

    // Добавляет одного игрока в команду по его выбранному миру
    public void assignPlayerToTeam(Player player) {
        String world = plugin.getPlayerWorld(player); // ← реализуй этот метод в своем плагине

        if (world == null || world.isEmpty()) return;

        // Удаляем игрока из всех текущих команд
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }

        // Назначаем в нужную команду
        Team targetTeam = scoreboard.getTeam(world);
        if (targetTeam == null) {
            targetTeam = scoreboard.registerNewTeam(world);
            targetTeam.setPrefix(ChatColor.GRAY + "[" + world + "] ");
            targetTeam.setCanSeeFriendlyInvisibles(false);
            targetTeam.setAllowFriendlyFire(false);
        }

        targetTeam.addEntry(player.getName());
    }

    // Полная перезагрузка
    public void reloadTeams() {
        loadTeams();
        setupTeams();
    }
}