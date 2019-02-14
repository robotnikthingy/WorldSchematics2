package worldschematics.loot;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LootTable {

    private Chest chest;
    private File DataFolder = WorldSchematics.getFilepath();
    private String LootTable;
    private File LootFile;
    private FileConfiguration LootConfig;

    private int MaxItems;
    private int MinItems;
    private Boolean OverwriteContents;

    public LootTable(org.bukkit.block.Chest chest, String LootTable, Boolean OverwriteContents) {
        DebugLogger.log("===Created loot Table for Chest===", DebugLogger.DebugType.LOOTTABLE);

        this.chest = chest;
        this.LootTable = LootTable;
        this.OverwriteContents = OverwriteContents;

        //load the loot table config file
        LootFile = new File(DataFolder + "/LootTables/" + LootTable + ".yml");
        LootConfig = YamlConfiguration.loadConfiguration(LootFile);

        DebugLogger.log("path of loot file is " + LootFile.getAbsolutePath(), DebugLogger.DebugType.LOOTTABLE);

        MaxItems = LootConfig.getInt("Options.MaxItems", 1);
        MinItems = LootConfig.getInt("Options.MinItems", 1);

        //make sure maxitems is not over 27
        if (MaxItems > 27) {
            MaxItems = 27;
            DebugLogger.log("MaxItems for loot table " + LootTable + " is above 27, the maximum amount of items allowes in a chest!", DebugLogger.DebugType.MISC);

        }
    }

    //generates a loot table file, using contents from chest a player is looking at
    public void Generate() {
        //ToDo: create a command which uses this.
    }

    //fill a chest with loot from a loot table
    public void FillChest() {
        Map<LootItem, Double> items = new HashMap<>();
        Random rand = new Random(System.currentTimeMillis());

        ConfigurationSection Loot = LootConfig.getConfigurationSection("loot");

        //try with capital L, since some owners are using lowercase l due to old documentation
        if (Loot == null) {
            DebugLogger.log("loot not found trying to find configsection 'Loot'", DebugLogger.DebugType.LOOTTABLE);
            Loot = LootConfig.getConfigurationSection("Loot");

        }

        DebugLogger.log("Generating loot", DebugLogger.DebugType.LOOTTABLE);

        if (Loot != null) {
            //got through each item and add it and its chance to the map
            DebugLogger.log("Items in Config:", DebugLogger.DebugType.LOOTTABLE);

            for (String ConfigItemString : Loot.getKeys(false)) {
                DebugLogger.log("Config Item: " + ConfigItemString);
                LootItem item = new LootItem(ConfigItemString, Loot);
                item.randomize();
                DebugLogger.log("-- " + item.GetItemStack().getType().toString());
                items.put(item, item.GetChance());
            }


            //now we place the items in the chest. Iterate through slots in chance MinAmount to MaxAmount of times
            //use this to get a random lootitem
            //ItemStack random = getFromWeightedMap(items);
            Inventory chestInv = chest.getBlockInventory();


            //emtpy inventory if option is set
            if (OverwriteContents == true) {
                chestInv.clear();
            }

            if (chest.getBlock().getType() == Material.AIR) {
                DebugLogger.log("Chest is actually an air block. This normally shouldn't happen!", DebugLogger.DebugType.LOOTTABLE);
                return;
            }


            DebugLogger.log("Chest empty, placing loot", DebugLogger.DebugType.LOOTTABLE);

            int ItemAmount = ThreadLocalRandom.current().nextInt(MinItems, MaxItems + 1);
            DebugLogger.log("Amount of items stacks being placed: " + ItemAmount, DebugLogger.DebugType.LOOTTABLE);
            //lets place the items randomly in the chest
            for (int i = 0; i < ItemAmount; i++) {
                //chest has 27 slots
                int n = rand.nextInt(26);

                ItemStack InvSlot = chestInv.getItem(n);
                //get an item from weighted item table
                LootItem lItem = getFromWeightedMap(items);

                lItem.randomize();


//            worldschematics.getInstance().debug("We will place item in slot " + n, DebugType.LOOTTABLE);
                //If a slot returns null then its empty
                if (InvSlot != null) {
                    //means there is an item in the chest, so we dont want to count that as placing a new item
                    i--;
                }

//            worldschematics.getInstance().debug("The item we will be placing is" + item.getType(), DebugType.LOOTTABLE);
                if (lItem.GetItemStack() != null) {
                    chestInv.setItem(n, lItem.GetItemStack());
                }

            }

        } else {
            DebugLogger.log("loot file is empty or does not exist!", DebugLogger.DebugType.LOOTTABLE);
        }
    }

    public <T> T getFromWeightedMap(Map<T, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            DebugLogger.log("Weighted table is empty!", DebugLogger.DebugType.LOOTTABLE);
            return null;
        }
        double chance = ThreadLocalRandom.current().nextDouble() * weights.values().stream().reduce(0D, Double::sum);
        AtomicDouble needle = new AtomicDouble();
        return weights.entrySet().stream().filter((ent) -> {
            return needle.addAndGet(ent.getValue()) >= chance;
        }).findFirst().map(Map.Entry::getKey).orElse(null);
    }

    private static boolean IsEmpty(Inventory inv) {
        for (ItemStack it : inv.getContents()) {
            if (it != null)
                return false;
        }

        return true;
    }

}


