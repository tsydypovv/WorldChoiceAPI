package me.m1ran.worldchoiceplugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Интерфейс API для плагина WorldChoice
 */
public interface WorldChoiceAPI {

    /**
     * Получить мир, к которому привязан игрок
     * @param player Игрок
     * @return Название выбранного мира ("world1" или "world2"), null если игрок не выбрал мир
     */
    String getPlayerWorld(Player player);

    /**
     * Проверить, выбрал ли игрок мир
     * @param player Игрок
     * @return true если игрок уже выбрал мир, иначе false
     */
    boolean hasChosenWorld(Player player);

    /**
     * Принудительно установить игроку выбранный мир
     * @param player Игрок
     * @param worldChoice Мир ("world1" или "world2")
     * @return true если операция выполнена успешно
     */
    boolean setPlayerWorld(Player player, String worldChoice);

    /**
     * Получить общий scoreboard, используемый плагином.
     * @return Scoreboard для отображения команд и префиксов
     */
    Scoreboard getScoreboard();

    /**
     * Создает и возвращает новый пустой Scoreboard.
     * @return новый Scoreboard
     */
    Scoreboard getNewScoreboard();

    Location getWorldSpawn1();
    Location getWorldSpawn2();

}