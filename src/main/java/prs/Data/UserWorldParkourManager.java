package prs.Data;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class UserWorldParkourManager {


    World w;
    File UserFile;
    FileConfiguration UserConfig;

    //UserDataHandler user = new UserDataHandler(player.getUniqueId()); // Make sure that you have the player.getUniqueId()

    public UserWorldParkourManager(World w){

        this.w = w;

        UserFile = new File("plugins/PRSPARKOURSETTING/" + w.getName() + ".yml");

        UserConfig = YamlConfiguration.loadConfiguration(UserFile);

    }

    public void Createparkour(){

        if ( !(UserFile.exists()) ) {
            try {

                //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[MCEnhanced] Created a new File for " + player.getName() + "(" + player.getUniqueId() + ")");

                YamlConfiguration UserConfig = YamlConfiguration.loadConfiguration(UserFile);

                UserConfig.save(UserFile);


            } catch (Exception e) {

                e.printStackTrace();

                // Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[MCEnhanced] Could not create a new File for " + player.getName() + "(" + player.getUniqueId() + ")");

                //u.kickPlayer(ChatColor.RED + "We could not create a file for your account!"); // THE PLAYERS CONFIG NEEDS TO BE CREATED!!!!!!!!

            }
        }

    }


    public FileConfiguration getParkourFile(){

        return UserConfig;

    }
    public File getParkourFiles(){
        return UserFile;
    }


    public void saveUserFile(){

        try {

            getParkourFile().save(UserFile);

        } catch(Exception e) {

            e.printStackTrace();

        }

    }


}
