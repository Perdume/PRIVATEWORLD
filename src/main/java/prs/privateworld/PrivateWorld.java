package prs.privateworld;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import prs.API.PWmanager;
import prs.Command.AdminCommand;
import prs.Command.TabComplete;
import prs.Command.UserCommand;
import prs.Data.ConfigManager;
import prs.Data.WorldManager;
import prs.Main.Chating;
import prs.Main.EventHandler;
import prs.Rollback.WorldRollBack;
import prs.ScoreBoard.scoreboard;
import prs.WorldGuard.Lobby;
import prs.gui.Inv;

public final class PrivateWorld extends JavaPlugin implements Listener {
    public static Plugin instance;
    public WorldManager worldManager;
    public ConfigManager ConfigManager;
    public Inv inv;

    @Override
    public void onEnable() {
        instance = this;
        this.inv = new Inv();
        this.worldManager = new WorldManager(this);
        this.ConfigManager = new ConfigManager(this);
        this.getCommand("PrivateWorldAdmin").setExecutor(new AdminCommand());
        this.getCommand("PrivateWorldAdmin").setTabCompleter(new TabComplete());
        this.getCommand("PrivateWorld").setExecutor(new UserCommand());
        this.getCommand("PrivateWorld").setTabCompleter(new TabComplete());
        this.worldManager = new WorldManager(this);
        Bukkit.getServer().getPluginManager().registerEvents(new EventHandler(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Chating(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Lobby(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WorldRollBack(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new scoreboard(), this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {

            @Override
            public void run() {

                for(Player player : Bukkit.getOnlinePlayers()) {
                    scoreboard sc = new scoreboard();
                    sc.updateScoreboard(player);
                }

            }

        }.runTaskTimer(this, 20L, 20L);


    }

    @Override
    public void onDisable() {
    }
    public PWmanager getPWmanager() {
        return new PWmanager();
    }


}
