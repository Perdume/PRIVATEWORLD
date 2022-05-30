package prs.parkour.Function;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import prs.Data.UserWorldManager;
import prs.Data.UserWorldParkourManager;
import prs.world.WorldManager;

import java.util.List;

public class InParkour implements Listener {
    @EventHandler
    public void CLK(PlayerInteractEvent e){
        if (e.getAction() == Action.PHYSICAL){
            if (e.getClickedBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE){
                status st = new status(e.getPlayer());
                if (st.getPlaying()){

                }
                else{

                }
            }
            if (e.getClickedBlock().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE){
                status st = new status(e.getPlayer());
                if (st.getPlaying()){

                }
            }

        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
            UserWorldParkourManager uwpm = new UserWorldParkourManager(e.getPlayer().getWorld());
            UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
            WorldManager wm = new WorldManager();
            if (wm.wcon(e.getPlayer().getWorld()).getPlayer() == e.getPlayer()) {
                if (e.getClickedBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE){
                    e.getPlayer().sendMessage(uwpm.getParkourFile().toString());
                    if ((Location) uwpm.getParkourFile().get("StartPoint") == null){
                        uwpm.getParkourFile().set("StartPoint", e.getClickedBlock().getLocation());
                        e.getPlayer().sendMessage(ChatColor.GREEN + "끝지점을 정해주세요(경형무게 갑압판 우클릭)");
                    }
                    if ((Location) uwpm.getParkourFile().get("EndPoint") == null){
                        if ((Location) uwpm.getParkourFile().get("StartPoint") != e.getClickedBlock().getLocation()) {
                            uwpm.getParkourFile().set("EndPoint", e.getClickedBlock().getLocation());
                            e.getPlayer().sendMessage(ChatColor.GREEN + "체크포인트를 정해주세요(진행순,중형무게 갑압판 우클릭) 다 만들면 도착부분을 우클릭해주세요(경형무게 갑압판 우클릭)");
                        }

                    }

                }
                if (e.getClickedBlock().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE){
                    List<Location> temp = null;
                    if ((Location) uwpm.getParkourFile().get("CheckPoint") == null){
                        temp.add(e.getClickedBlock().getLocation());
                    }
                    else{
                       temp = (List<Location>) uwpm.getParkourFile().get("CheckPoint");
                       temp.add(e.getClickedBlock().getLocation());
                       uwpm.getParkourFile().set("CheckPoint", temp);
                    }
                }
            }

        }
    }


}
