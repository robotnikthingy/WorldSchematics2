package worldschematics.loot;

import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.magic.MagicController;
import com.elmakers.mine.bukkit.magic.MagicPlugin;
import com.shampaggon.crackshot.CSUtility;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.AbstractItemStack;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootItem {

    String ConfigItemName;
    private String LootTable;
    private File DataFolder = WorldSchematics.getFilepath();
    private File LootFile;
    private ConfigurationSection LootConfig;

    //Itemstack represents an item in inventory
    private ItemStack item;
    private ItemMeta ItemData;


    private itemType Type;

    private String CustomPluginItemName;

    private double Chance;

    private int MinAmount;

    private int MaxAmount;

    private int Amount;

    private Boolean Unbreakable;

    private Boolean HideAttributes;

    private String ConfigMaterial;

    private String texturePlayer;

    private int ID;
    private short Data;

    private List<String> Lore;


    //ToDo: This is all messy and needs to be cleaned up

    LootItem(String ConfigItemName, ConfigurationSection LootTable) {
        this.ConfigItemName = ConfigItemName;
        this.LootConfig = LootTable.getConfigurationSection(ConfigItemName);

        this.Type = itemType.valueOf(LootConfig.getString("Type", "ITEM").toUpperCase());
        this.Chance = LootConfig.getDouble("Chance", 0);

        this.MinAmount = LootConfig.getInt("MinAmount", 1);
        this.MaxAmount = LootConfig.getInt("MaxAmount", 1);

        this.CustomPluginItemName = LootConfig.getString("Name", "none");

        if(LootConfig.contains("Material")){
            this.ConfigMaterial = LootConfig.getString("Material").toUpperCase();
        }

        this.ID = LootConfig.getInt("ID", 0);
        this.Data = (short) LootConfig.getInt("Data", 0);

        this.Lore = LootConfig.getStringList("Lore");

        this.texturePlayer = LootConfig.getString("PlayerTexture", "");

        this.Unbreakable = LootConfig.getBoolean("Unbreakable", false);
        this.HideAttributes = LootConfig.getBoolean("HideAttributes", false);

        this.Amount = 1;


        //if this is the case then we need to create the item ourselves from scratch
        if (Type == itemType.ITEM) {
            //create the item, and set data and amount
            //try and see if item was entered as a material name, if not have it entered as an ID
            if (ConfigMaterial != null) {
                try{
                    item = new ItemStack(Material.valueOf(ConfigMaterial), Amount, Data);
                }catch (java.lang.IllegalArgumentException e){
                    DebugLogger.log("Unknown item material type: " + ConfigMaterial, DebugLogger.DebugType.WARNING);
                    item = new ItemStack(Material.AIR);
                }

            } else {
                item = new ItemStack(Material.AIR, Amount, Data);
            }

            //get Item MetaData so we can modify it
            ItemData = item.getItemMeta();

            //set name, if any
            if (LootConfig.getString("Display") != null) {
                ItemData.setDisplayName(LootConfig.getString("Display"));
            }

            //Set lore, if any
            if (Lore != null && Lore.isEmpty() != true) {
                ItemData.setLore(Lore);
            }


            //Set enchantments, if any
            if (LootConfig.getList("Enchantments") != null) {
                //ItemData.addEnchant(arg0, arg1, true);
                ArrayList<String> EnchantList = (ArrayList<String>) LootConfig.getList("Enchantments");
                for (String ConfigEnchantment : EnchantList) {
                    //parse the enchantment and the power level
                    String[] SplitEnchantment = ConfigEnchantment.split(":");
                    int EnchantLevel = Integer.parseInt(SplitEnchantment[1]);

                    //adds the enchantment to the item
                    ItemData.addEnchant(Enchantment.getByName(SplitEnchantment[0]), EnchantLevel, true);
                }
            }

            //set unbreakable or not
            if (Unbreakable == true) {
                ItemData.spigot().setUnbreakable(true);
            }


            //set flags
            if (HideAttributes == true) {
                ItemData.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            }

            //ToDo - Find better way to apply item metadata, maybe use bukkit serializable for config files
            //set skull data if it is a skull
            if (!texturePlayer.equals("")) {
                SkullMeta meta = (SkullMeta) ItemData;
                meta.setOwner(texturePlayer);
                item.setItemMeta(meta);
            } else {
                //set the item metadata now that we have modified it
                item.setItemMeta(ItemData);
            }

            randomize();

            //and finally set the amount
            item.setAmount(Amount);

            ItemData = item.getItemMeta();

        }

        if (Type == itemType.CRACKSHOTITEM) {
            //check if crackshot plugin is installed first
            if (WorldSchematics.getMythicMobsInstalled() == true) {
                CSUtility csutil = new CSUtility();

                //add the crackshot weapon
                item = csutil.generateWeapon(CustomPluginItemName);

                item.setAmount(Amount);
            } else {
                WorldSchematics.getInstance().getLogger().info("Tried to place CrackShot item in chest, but CrackShot is not installed!");
            }
        }

        if (Type == itemType.MYTHICMOBSITEM) {
            //check if MythicMobs plugin is installed first
            if (WorldSchematics.getMythicMobsInstalled() == true) {
                MythicItem mi = MythicMobs.inst().getItemManager().getItem(CustomPluginItemName).get();
                //to do, take into account Minamount and Maxamount
                AbstractItemStack is = mi.generateItemStack(Amount);

                //add the mythicmobs item
                item = BukkitAdapter.adapt(is);
            } else {
                WorldSchematics.getInstance().getLogger().info("Tried to place MythicMobs item in chest, but MythicMobs is not installed!");
            }

        }

        if (Type == itemType.MAGICITEM) {
            //check if MythicMobs plugin is installed first
            if (WorldSchematics.getMythicMobsInstalled() == true) {
                MagicAPI mAPI = MagicPlugin.getAPI();


                MageController mg = mAPI.getController();

                com.elmakers.mine.bukkit.api.item.ItemData MagicItem = mg.getItem(CustomPluginItemName);


                //add the magic item
                item = MagicItem.getItemStack(Amount);
            } else {
                WorldSchematics.getInstance().getLogger().info("Tried to place Magic item in chest, but Magic is not installed!");
            }

        }

        if (Type == itemType.MAGICWAND) {
            //check if MythicMobs plugin is installed first
            if (WorldSchematics.getMythicMobsInstalled() == true) {
                MagicAPI mAPI = MagicPlugin.getAPI();


                MageController mg = mAPI.getController();

                Wand MagicItem = mg.createWand(CustomPluginItemName);


                //add the magic wand item
                item = MagicItem.getItem();

            } else {
                WorldSchematics.getInstance().getLogger().info("Tried to place Magic Wand item in chest, but Magic is not installed!");
            }

        }

    }

    public itemType getType() {
        return Type;
    }

    public enum itemType {
        ITEM, CRACKSHOTITEM, MYTHICMOBSITEM, MAGICITEM, MAGICWAND

    }


    void randomize() {
        if (MinAmount == MaxAmount) {
            item.setAmount(MinAmount);
        } else {
            Amount = ThreadLocalRandom.current().nextInt(MinAmount, MaxAmount + 1);
            item.setAmount(Amount);
        }
    }

    public ItemStack GetItemStack() {
        return item;
    }

    public ItemMeta GetItemMeta() {
        return ItemData;
    }

    public double GetChance() {
        return Chance;
    }

    public itemType GetType() {
        return Type;
    }
}
