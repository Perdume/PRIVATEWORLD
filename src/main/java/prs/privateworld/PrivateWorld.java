package prs.privateworld;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import prs.Command.AdminCommand;
import prs.Command.TabComplete;
import prs.Command.UserCommand;
import prs.Data.ConfigManager;
import prs.Data.WorldConfig;
import prs.Main.Chatting;
import prs.Main.EventHandler;
import prs.ScoreBoard.scoreboard;
import prs.world.WorldManager;

public final class PrivateWorld extends JavaPlugin implements Listener {
    public static Plugin instance;
    public WorldConfig worldManager;//Config
    public prs.world.WorldManager Worlds;

    @Override
    public void onEnable() {
        instance = this;
        this.worldManager = new WorldConfig(this);
        this.Worlds = new WorldManager();
        this.getCommand("PrivateWorldAdmin").setExecutor(new AdminCommand());
        this.getCommand("PrivateWorldAdmin").setTabCompleter(new TabComplete());
        this.getCommand("PrivateWorld").setExecutor(new UserCommand());
        this.getCommand("PrivateWorld").setTabCompleter(new TabComplete());
        Bukkit.getServer().getPluginManager().registerEvents(new EventHandler(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Chatting(), this);
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
        worldManager.save();
    }


}
