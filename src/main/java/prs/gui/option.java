package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import prs.Data.UserWorldManager;
import prs.Main.Chating;
import prs.privateworld.PrivateWorld;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.itemutils.ItemBuilder;

public class option {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    public Material glow(Material item2){
        ItemStack item = new ItemStack(item2);
        item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item2;
    }
    public void WorldOption(Player p, Object o){

        wm.inv.setPlayerInventoryOpenned(p, true);
        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 18, "Option"));
        UserWorldManager uwm;
        if (o == null) {
            uwm = new UserWorldManager(p.getWorld());
        }
        else{
            uwm = new UserWorldManager(Bukkit.getWorld((String) o));
        }
        if(uwm.getWorldFile().getBoolean("Option.Private") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.BARRIER)
                    .setName(ChatColor.RED + "월드비공개"), e -> {
                uwm.getWorldFile().set("Option.Private", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 0);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.BARRIER)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "월드비공개"), e -> {
                uwm.getWorldFile().set("Option.Private", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 0);
        }
        if(uwm.getWorldFile().getBoolean("Option.canDrop") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.STICK)
                    .setName(ChatColor.RED + "아이템 드롭 가능하게 하기"), e -> {
                if(uwm.getWorldFile().getBoolean("Option.canDrop") != true) {
                    uwm.getWorldFile().set("Option.canDrop", true);
                    uwm.saveUserFile();
                    WorldOption(p, o);
                }

            });
            gui.addButton(button, 1);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.STICK)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "아이템 드롭 가능하게 하기"), e -> {
                uwm.getWorldFile().set("Option.canDrop", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 1);
        }
        if(uwm.getWorldFile().getBoolean("Option.canBreak") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.NETHERITE_PICKAXE)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setName(ChatColor.RED + "블럭 파괴 가능하게 하기"), e -> {
                uwm.getWorldFile().set("Option.canBreak", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 2);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.NETHERITE_PICKAXE)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "블럭 파괴 가능하게 하기"), e -> {
                uwm.getWorldFile().set("Option.canBreak", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 2);
        }
        if(uwm.getWorldFile().getBoolean("Option.canPlace") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.BEDROCK)
                    .setName(ChatColor.RED + "블럭 설치 가능하게 하기"), e -> {
                uwm.getWorldFile().set("Option.canPlace", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 3);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.BEDROCK)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "블럭 설치 가능하게 하기"), e -> {
                uwm.getWorldFile().set("Option.canPlace", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 3);
        }
        //SHOOT
        if(uwm.getWorldFile().getBoolean("Option.canShoot") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.BOW)
                    .setName(ChatColor.RED + "활 쏘기"), e -> {
                uwm.getWorldFile().set("Option.canShoot", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 4);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.BOW)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "활 쏘기"), e -> {

                uwm.getWorldFile().set("Option.canShoot", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 4);
        }
        if(uwm.getWorldFile().getBoolean("Option.canCommand") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.COMMAND_BLOCK)
                    .setName(ChatColor.RED + "커맨드"), e -> {
                uwm.getWorldFile().set("Option.canCommand", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 5);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.COMMAND_BLOCK)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "커맨드"), e -> {
                uwm.getWorldFile().set("Option.canCommand", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 5);
        }
        if(uwm.getWorldFile().getBoolean("Option.canInteract") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.DISPENSER)
                    .setName(ChatColor.RED + "블럭 상호작용"), e -> {
                uwm.getWorldFile().set("Option.canInteract", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 6);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.DISPENSER)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "블럭 상호작용"), e -> {
                uwm.getWorldFile().set("Option.canInteract", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 6);
        }
        ItemButton button2 = ItemButton.create(new ItemBuilder(Material.NAME_TAG)
                .setName(ChatColor.BLUE + "월드이름 설정"), e -> {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "채팅으로 자신의 월드 이름을 적어주세요, Quit 메세지로 나올 수 있습니다");
            Chating cht = new Chating();
            cht.Chatset((Player) e.getWhoClicked(), "Name", e.getWhoClicked().getWorld());
            e.getWhoClicked().closeInventory();

        });
        gui.addButton(button2, 7);
        if(uwm.getWorldFile().getBoolean("Option.Redstone") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.REDSTONE)
                    .setName(ChatColor.RED + "레드스톤 작동 허용"), e -> {
                uwm.getWorldFile().set("Option.Redstone", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 8);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.REDSTONE)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .setName(ChatColor.GREEN + "레드스톤 작동 허용"), e -> {
                uwm.getWorldFile().set("Option.Redstone", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 8);
        }
        if(uwm.getWorldFile().getBoolean("Option.canPVP") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.DIAMOND_SWORD)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setName(ChatColor.RED + "PVP"), e -> {
                uwm.getWorldFile().set("Option.canPVP", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 9);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.DIAMOND_SWORD)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setName(ChatColor.GREEN + "PVP"), e -> {
                uwm.getWorldFile().set("Option.canPVP", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 9);
        }
        if(uwm.getWorldFile().getBoolean("Option.canshowFirework") != true) {
            ItemButton button = ItemButton.create(new ItemBuilder(Material.FIREWORK_ROCKET)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setName(ChatColor.RED + "폭죽 터뜨리기 허용"), e -> {
                uwm.getWorldFile().set("Option.canshowFirework", true);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 10);
        }
        else{
            ItemButton button = ItemButton.create(new ItemBuilder(Material.FIREWORK_ROCKET)
                    .addEnchant(Enchantment.DAMAGE_ALL, 1)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setName(ChatColor.GREEN + "폭죽 터뜨리기 허용"), e -> {
                uwm.getWorldFile().set("Option.canshowFirework", null);
                uwm.saveUserFile();
                WorldOption(p, o);

            });
            gui.addButton(button, 10);
        }
        ItemButton button3 = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                .setName(ChatColor.BLUE + "텔레포트 위치 설정"), e -> {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "스폰 위치를 본인 위치로 설정했습니다");
            uwm.getWorldFile().set("Option.TeleportLocation", e.getWhoClicked().getLocation());
            uwm.saveUserFile();
            e.getWhoClicked().closeInventory();

        });
        gui.addButton(button3, 11);
        ItemButton button4 = ItemButton.create(new ItemBuilder(Material.COMPASS)
                .setName(ChatColor.BLUE + "기본 게임모드 설정"), e -> {
            WorldGamemode(p, o);

        });
        gui.addButton(button4, 12);
        gui.open(p);
    }
    public void WorldGamemode(Player p, Object o) {

        wm.inv.setPlayerInventoryOpenned(p, true);
        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 9, "Option"));
        UserWorldManager uwm;
        if (o == null) {
            uwm = new UserWorldManager(p.getWorld());
        } else {
            uwm = new UserWorldManager(Bukkit.getWorld((String) o));
        }
        ItemButton srv = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                .setName(ChatColor.BLUE + "서바이벌"), e -> {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "설정했습니다");
            uwm.getWorldFile().set("Option.Gamemode", GameMode.SURVIVAL);
            uwm.saveUserFile();
            e.getWhoClicked().closeInventory();

        });
        gui.addButton(srv, 0);
        ItemButton crea = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                .setName(ChatColor.BLUE + "크리에이티브"), e -> {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "설정했습니다");
            uwm.getWorldFile().set("Option.Gamemode", GameMode.CREATIVE);
            uwm.saveUserFile();
            e.getWhoClicked().closeInventory();

        });
        gui.addButton(crea, 4);
        ItemButton spect = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                .setName(ChatColor.BLUE + "관전자"), e -> {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "설정했습니다");
            uwm.getWorldFile().set("Option.Gamemode", GameMode.SPECTATOR);
            uwm.saveUserFile();
            e.getWhoClicked().closeInventory();

        });
        gui.addButton(spect, 8);
        gui.open(p);
    }

}
