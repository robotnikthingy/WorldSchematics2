package worldschematics.schematicBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import worldschematics.loot.LootTable;
import worldschematics.schematicBlock.AbstractSchematicBlock;
import worldschematics.util.DebugLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Represents a container, such as a chest, within a schematic.
 * */
public class schematicContainer extends AbstractSchematicBlock {

    ArrayList<String> lootTables = new ArrayList<>();
    containerType contType;

    public schematicContainer(Location location, ConfigurationSection configSection, String name, containerType type){
        super(location,configSection,name);
        lootTables = (ArrayList<String>) configSection.getList("loottables");
        this.contType = type;
    }

    public enum containerType {
        CHEST
    }

    public void createInWorld(Location worldLocation) throws IOException {
        DebugLogger.log("container detected in config, will try to fill with loot. Location is a " + worldLocation.getBlock().getType().toString() + " at " + worldLocation.getBlockX() + " " + worldLocation.getBlockY() + " " + worldLocation.getBlockZ(), DebugLogger.DebugType.LOOTTABLE);

        createContainer(worldLocation);
    }

    void createContainer(Location worldLocation) throws IOException {
        //check if block is chest first
        if (worldLocation.getBlock().getType() == Material.CHEST || worldLocation.getBlock().getType() == Material.TRAPPED_CHEST) {
            Random randomizer = new Random(System.currentTimeMillis());
            Chest chest = (Chest) worldLocation.getBlock().getState();
            Boolean OverwriteContents = false;

            //check options for the chest
            //see if we should overwrite the contents or not
            if (configSection.getBoolean("properties.DeleteContents", false) == true) {
                OverwriteContents = true;
            }

            //we need to make sure loot table list is not empty first
            if (lootTables != null) {
                String RandomLootTable = lootTables.get(randomizer.nextInt(lootTables.size()));
                LootTable loot = new LootTable(chest, RandomLootTable, OverwriteContents);
                loot.FillChest();
            }
        }else{
            DebugLogger.log("block is not a chest, this may happen when a world is first being generated", DebugLogger.DebugType.LOOT);

        }
    }
}
