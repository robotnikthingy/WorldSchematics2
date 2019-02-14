package worldschematics.schematicBlock;

import com.google.common.util.concurrent.AtomicDouble;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.DataException;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import worldschematics.schematicBlock.AbstractSchematicBlock;
import worldschematics.schematicBlock.schematicSpawner;
import worldschematics.schematicManager;
import worldschematics.util.DebugLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a Sign with [Marker] in its first line located inside a schematic, which will uses signs to determine the location of spawners and other features.
 */
public class schematicMarker extends AbstractSchematicBlock {

    HashMap<String, Double> schematicsList = new HashMap<String, Double>();
    ArrayList<String> blocksList = new ArrayList<String>();

    subType sType;
    int markerChance = 0;
    Random randomizer = new Random();

    public schematicMarker(Location schematicLocation, ConfigurationSection configSection, String name) {
        super(schematicLocation, configSection, name);

        if(EnumUtils.isValidEnum(subType.class,configSection.getString("subtype", "NONE").toUpperCase())){
            this.sType = subType.valueOf(configSection.getString("subtype", "NONE").toUpperCase());
        }else{
            DebugLogger.log("Invalid Marker subtype: " + configSection.getString("subtype"));
            this.sType = subType.valueOf("NONE");
        }

        DebugLogger.log("Marker subtype is: " + sType, DebugLogger.DebugType.MARKER);

        //chance of marker being replaced with something
        markerChance = configSection.getInt("markerChance", 100);

        if (sType == subType.SCHEMATIC) {
            if (configSection.contains("schematiclist")) {
                ConfigurationSection schematicsListSection = configSection.getConfigurationSection("schematiclist");
                for (String listName : schematicsListSection.getKeys(false)) {
                    DebugLogger.log("Found subschematic: " + listName, DebugLogger.DebugType.MARKER);
                    schematicsList.put(listName, schematicsListSection.getDouble(listName + ".chance", 0.0));
                }
            } else {
                DebugLogger.log("No schematicList is available for Marker with SCHEMATIC subtype", DebugLogger.DebugType.MISC);
            }
        }

        blocksList = (ArrayList<String>) configSection.getList("blocklist");
    }

    public enum subType {
        MOB, MOBSPAWNER, MYTHICMOB, MYTHICMOBSPAWNER, CHEST, TRAPPEDCHEST, SCHEMATIC, BLOCK,NONE
    }

    public void createInWorld(Location worldLocation) throws IOException, DataException, WorldEditException, JSONException, ParseException {

        if (sType == subType.MOB) {
            worldLocation.getBlock().setType(Material.AIR);
            schematicSpawner schmSpawner = new schematicSpawner(schematicLocation, configSection, name, schematicSpawner.spawnerType.MOB);
            schmSpawner.createInWorld(worldLocation);
        }

        if (sType == subType.MYTHICMOB) {
            worldLocation.getBlock().setType(Material.AIR);
            schematicSpawner schmSpawner = new schematicSpawner(schematicLocation, configSection, name, schematicSpawner.spawnerType.MYTHICMOB);
            schmSpawner.createInWorld(worldLocation);
        }

        if (sType == subType.MOBSPAWNER) {
            worldLocation.getBlock().setType(Material.SPAWNER);
            schematicSpawner schmSpawner = new schematicSpawner(schematicLocation, configSection, name, schematicSpawner.spawnerType.MOBSPAWNER);
            schmSpawner.createInWorld(worldLocation);
        }

        if (sType == subType.MYTHICMOBSPAWNER) {
            worldLocation.getBlock().setType(Material.AIR);
            schematicSpawner schmSpawner = new schematicSpawner(schematicLocation, configSection, name, schematicSpawner.spawnerType.MYTHICMOBSPAWNER);
            schmSpawner.createInWorld(worldLocation);
        }

        if (sType == subType.SCHEMATIC) {
            worldLocation.getBlock().setType(Material.AIR);
            createSchematic(worldLocation);
        }

        if (sType == subType.BLOCK) {
            createBlock(worldLocation);

        }

        //just replace marker with air
        if (sType == subType.NONE) {
            worldLocation.getBlock().setType(Material.AIR);
        }
    }

    private void createSchematic(Location worldLocation) throws IOException, DataException, WorldEditException, JSONException, ParseException {
        if (schematicsList.size() != 0) {
            String listSchematic = getFromWeightedMap(schematicsList);
            Boolean skipChecks = configSection.getBoolean("schematiclist." + listSchematic + ".skipChecks", true);
            //String schematicName = configSection.getString("schematiclist." + listSchematic + ".skipChecks", "NO_SCHEMATIC");
            int rotation = configSection.getInt("schematiclist." + listSchematic + ".rotation",-1);

            //wont spawn schematic is set to NO_SCHEMATIC
            if (!listSchematic.equalsIgnoreCase("NO_SCHEMATIC")) {


                if(rotation != -1){
                    schematicManager.spawn(listSchematic, worldLocation, rotation, skipChecks);
                    DebugLogger.log("Rotating schematic to: " + rotation, DebugLogger.DebugType.MARKER);
                }else{
                    schematicManager.spawn(listSchematic, worldLocation, skipChecks);
                }

            }

        } else {
            DebugLogger.log("Attempted to spawn sub-schematic, but there are none to choose from in config!", DebugLogger.DebugType.MISC);
        }
    }

    private void createBlock(Location worldLocation){
        if(!blocksList.isEmpty()){
            String randomBlock = blocksList.get(randomizer.nextInt(blocksList.size())).toUpperCase();
            worldLocation.getBlock().setType(Material.valueOf(randomBlock));
        }else{
            DebugLogger.log("Attempted to replace marker with block, but there are none to choose from in config!", DebugLogger.DebugType.MISC);
        }

    }

    public <T> T getFromWeightedMap(Map<T, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        double chance = ThreadLocalRandom.current().nextDouble() * weights.values().stream().reduce(0D, Double::sum);
        AtomicDouble needle = new AtomicDouble();
        return weights.entrySet().stream().filter((ent) -> {
            return needle.addAndGet(ent.getValue()) >= chance;
        }).findFirst().map(Map.Entry::getKey).orElse(null);
    }
}
