package prs.privateworld;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import prs.command.AdminCommand;
import prs.command.TabComplete;
import prs.command.UserCommand;
import prs.data.ConfigManager;
import prs.data.WorldConfig;
import prs.data.WorkshopManager;
import prs.main.Chatting;
import prs.main.EventHandler;
import prs.scoreboard.WorldScoreboard;
import prs.world.WorldManager;

public final class PrivateWorld extends JavaPlugin implements Listener {
    public static Plugin instance;
    public WorldConfig worldManager;
    public ConfigManager configManager;
    public WorkshopManager workshopManager;
    public prs.world.WorldManager Worlds;

    @Override
    public void onEnable() {
        instance = this;
        this.worldManager = new WorldConfig(this);
        this.configManager = new ConfigManager(this);
        this.workshopManager = new WorkshopManager(this);
        this.Worlds = new WorldManager();
        this.getCommand("PrivateWorldAdmin").setExecutor(new AdminCommand());
        this.getCommand("PrivateWorldAdmin").setTabCompleter(new TabComplete());
        this.getCommand("PrivateWorld").setExecutor(new UserCommand());
        this.getCommand("PrivateWorld").setTabCompleter(new TabComplete());
        Bukkit.getServer().getPluginManager().registerEvents(new EventHandler(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Chatting(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WorldScoreboard(), this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        // Update scoreboard every 4 seconds (80 ticks) instead of every second
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    WorldScoreboard sc = new WorldScoreboard();
                    sc.updateScoreboard(player);
                }
            }
        }.runTaskTimer(this, 20L, 80L);
    }

    @Override
    public void onDisable() {
        worldManager.save();
    }
}
