package me.m1ran.worldchoiceplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldChoicePlugin extends JavaPlugin implements Listener, WorldChoiceAPI {

    final Map<UUID, BukkitTask> repeatTasks = new HashMap<>();

    private Scoreboard scoreboard;
    private World world1;
    private World world2;
    private Location world1Spawn;
    private Location world2Spawn;
    private static WorldChoicePlugin instance;
    private TeamManager teamManager;

    @Override
    public void onEnable() {

        //Инициализация Scoreboard
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        teamManager = new TeamManager(this);
        teamManager.setupTeams(); // Восстанавливаем команды при запуске
        // Сохраняем конфиг по умолчанию, если он не существует
        saveDefaultConfig();
        instance = this;
        // Регистрация API (исправленная версия)
        getServer().getServicesManager().register(
                WorldChoiceAPI.class, // Интерфейс
                this, // Предоставляем экземпляр этого класса (this) как реализацию интерфейса
                this, // Владелец сервиса (плагин)
                ServicePriority.Normal
        );



        // Загружаем миры
        loadWorlds();

        // Регистрируем команды
        getCommand("chooseworld").setExecutor(new WorldChoiceCommand(this));
        getCommand("setspawn").setExecutor(new AdminCommands(this));
        getCommand("changeworldfor").setExecutor(new AdminCommands(this)); // Команда для админов
        getCommand("worldchoice-reload").setExecutor(new ReloadCommand(this));

        // Регистрируем обработчики событий
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            teamManager.assignPlayerToTeam(player);
        }

        getLogger().info("WorldChoicePlugin enabled!");
    }

    /**
     * Получить экземпляр плагина
     * @return экземпляр плагина
     */
    public static WorldChoicePlugin getInstance() {
        return instance;
    }

    // Реализация методов интерфейса WorldChoiceAPI



    @Override
    public String getPlayerWorld(Player player) {
        if (player == null) return null;
        return getConfig().getString("players." + player.getUniqueId() + ".world");
    }


    @Override
    public boolean hasChosenWorld(Player player) {
        return getPlayerWorld(player) != null;
    }

    @Override
    public boolean setPlayerWorld(Player player, String worldChoice) {
        if (player == null || worldChoice == null) return false;

        if (!worldChoice.equals("world1") && !worldChoice.equals("world2")) {
            return false;
        }

        getConfig().set("players." + player.getUniqueId() + ".world", worldChoice);
        getConfig().set("players." + player.getUniqueId() + ".name", player.getName());
        getConfig().set("players." + player.getUniqueId() + ".chosenAt", System.currentTimeMillis());
        saveConfig();

        return true;
    }

    @Override
    public Location getWorldSpawn1() {
        Location location = this.getWorld1Spawn();
        return location;
    }

    @Override
    public Location getWorldSpawn2() {
        Location location = this.getWorld2Spawn();
        return location;
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("WorldChoicePlugin disabled!");
    }

    private void loadWorlds() {
        FileConfiguration config = getConfig();

        // Названия миров из конфига
        String world1Name = config.getString("world1.name", "world");
        String world2Name = config.getString("world2.name", "world_nether");

        // Получаем миры по названиям
        world1 = Bukkit.getWorld(world1Name);
        world2 = Bukkit.getWorld(world2Name);

        // Проверка существования миров
        if (world1 == null) {
            getLogger().warning("World '" + world1Name + "' not found!");
        }

        if (world2 == null) {
            getLogger().warning("World '" + world2Name + "' not found!");
        }

        // Загружаем точки спавна
        if (config.contains("world1.spawn") && world1 != null) {
            double x = config.getDouble("world1.spawn.x");
            double y = config.getDouble("world1.spawn.y");
            double z = config.getDouble("world1.spawn.z");
            float yaw = (float) config.getDouble("world1.spawn.yaw");
            float pitch = (float) config.getDouble("world1.spawn.pitch");
            world1Spawn = new Location(world1, x, y, z, yaw, pitch);
        } else if (world1 != null) {
            world1Spawn = world1.getSpawnLocation();
        }

        if (config.contains("world2.spawn") && world2 != null) {
            double x = config.getDouble("world2.spawn.x");
            double y = config.getDouble("world2.spawn.y");
            double z = config.getDouble("world2.spawn.z");
            float yaw = (float) config.getDouble("world2.spawn.yaw");
            float pitch = (float) config.getDouble("world2.spawn.pitch");
            world2Spawn = new Location(world2, x, y, z, yaw, pitch);
        } else if (world2 != null) {
            world2Spawn = world2.getSpawnLocation();
        }
    }

    // Показать игроку выбор мира
    public void showWorldChoice(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Выбор мира ===");
        player.sendMessage(ChatColor.GREEN + "Выберите один из двух миров для игры:");
        player.sendMessage(ChatColor.AQUA + "/chooseworld 1" + ChatColor.WHITE + " - " + getConfig().getString("world1.description", "Мир 1"));
        player.sendMessage(ChatColor.AQUA + "/chooseworld 2" + ChatColor.WHITE + " - " + getConfig().getString("world2.description", "Мир 2"));
        player.sendMessage(ChatColor.RED + "Внимание! Выбор можно сделать только один раз!");
    }

    // Метод для выбора мира
    public boolean chooseWorld(Player player, int worldNumber) {
        // Проверяем, выбирал ли уже игрок мир
        if (hasChosenWorld(player)) {
            player.sendMessage(ChatColor.RED + "Вы уже выбрали мир и не можете его изменить!");
            return false;
        }

        // Определяем выбранный мир
        String worldChoice = (worldNumber == 1) ? "world1" : "world2";

        // Получаем название мира из конфига
        String worldName = getConfig().getString(worldChoice + ".name",
                worldChoice.equals("world1") ? "world" : "world_nether");

        // Проверяем существование мира
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            getLogger().severe("Мир '" + worldName + "' не найден! Проверьте конфигурацию.");
            player.sendMessage(ChatColor.RED + "Ошибка: выбранный мир не доступен. Сообщите администратору.");
            return false;
        }

        // Получаем точку спавна
        Location spawnLoc = (worldNumber == 1) ? world1Spawn : world2Spawn;

        // Дополнительная проверка точки спавна
        if (spawnLoc == null) {
            spawnLoc = targetWorld.getSpawnLocation();
            getLogger().warning("Точка спавна для мира " + worldName + " не установлена, используется стандартная");
        }

        // Сохраняем выбор игрока
        if (!setPlayerWorld(player, worldChoice)) {
            player.sendMessage(ChatColor.RED + "Ошибка при сохранении выбора мира.");
            return false;
        }

        // Отменяем повторяющееся сообщение
        if (repeatTasks.containsKey(player.getUniqueId())) {
            repeatTasks.get(player.getUniqueId()).cancel();
            repeatTasks.remove(player.getUniqueId());
        }

        // Телепортируем игрока
        try {
            player.teleport(spawnLoc);
            player.sendMessage(ChatColor.GREEN + "Вы успешно выбрали " +
                    getConfig().getString(worldChoice + ".description", "Мир " + worldNumber));
            return true;
        } catch (Exception e) {
            getLogger().severe("Ошибка при телепортации игрока " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Ошибка при телепортации. Сообщите администратору.");
            return false;
        }
    }

    // Метод для админов, позволяющий изменить выбранный мир игрока
    public boolean changeWorldFor(Player admin, String playerName, int worldNumber) {
        // Находим игрока по имени
        Player targetPlayer = Bukkit.getPlayer(playerName);
        String targetUUID;

        if (targetPlayer != null) {
            targetUUID = targetPlayer.getUniqueId().toString();
        } else {
            // Пытаемся найти UUID в сохраненных данных
            targetUUID = null;
            for (String uuidStr : getConfig().getConfigurationSection("players").getKeys(false)) {
                String savedName = getConfig().getString("players." + uuidStr + ".name");
                if (savedName != null && savedName.equalsIgnoreCase(playerName)) {
                    targetUUID = uuidStr;
                    break;
                }
            }

            if (targetUUID == null) {
                admin.sendMessage(ChatColor.RED + "Игрок не найден.");
                return false;
            }
        }

        // Изменяем выбранный мир
        String worldChoice = (worldNumber == 1) ? "world1" : "world2";
        getConfig().set("players." + targetUUID + ".world", worldChoice);
        getConfig().set("players." + targetUUID + ".changedBy", admin.getName());
        getConfig().set("players." + targetUUID + ".changedAt", System.currentTimeMillis());
        saveConfig();

        admin.sendMessage(ChatColor.GREEN + "Вы изменили выбранный мир для игрока " + playerName + " на " +
                getConfig().getString(worldChoice + ".description", "Мир " + worldNumber));

        // Если игрок онлайн, сообщить ему об изменении
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.YELLOW + "Администратор изменил ваш выбранный мир на " +
                    getConfig().getString(worldChoice + ".description", "Мир " + worldNumber));
        }

        return true;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public Location getWorld1Spawn() {
        String worldName = getConfig().getString("world1.name");
        World world = Bukkit.getWorld(worldName);

        double x = getConfig().getDouble("world1.spawn.x");
        double y = getConfig().getDouble("world1.spawn.y");
        double z = getConfig().getDouble("world1.spawn.z");

        float yaw = (float) getConfig().getDouble("world1.spawn.yaw");
        float pitch = (float) getConfig().getDouble("world1.spawn.pitch");
        world1Spawn = new Location(world, x, y, z, yaw, pitch);
        return world1Spawn;
    }

    public Location getWorld2Spawn() {
        String worldName = getConfig().getString("world2.name");
        World world = Bukkit.getWorld(worldName);

        double x = getConfig().getDouble("world2.spawn.x");
        double y = getConfig().getDouble("world2.spawn.y");
        double z = getConfig().getDouble("world2.spawn.z");

        float yaw = (float) getConfig().getDouble("world2.spawn.yaw");
        float pitch = (float) getConfig().getDouble("world2.spawn.pitch");
        world1Spawn = new Location(world, x, y, z, yaw, pitch);
        return world2Spawn;
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Override
    public Scoreboard getNewScoreboard() {
        return Bukkit.getScoreboardManager().getNewScoreboard();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teamManager.assignPlayerToTeam(event.getPlayer());
    }
}