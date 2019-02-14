package worldschematics;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.DataException;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import worldschematics.util.DebugLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

//Will handle loading schematics into memory, as well as spawning them in the world when the CHunklistener loads a chunk
//And determining if the schematic meets the conditions needed to be placed
public class schematicManager implements Listener {

    WorldSchematics plugin;
    private static ArrayList<SpawnSchematic> Schematics = new ArrayList<SpawnSchematic>();
    private static ArrayList<World> LoadedWorlds = new ArrayList<>();


    schematicManager(WorldSchematics core) throws DataException, ParseException, IOException {
        plugin = core;
        //LoadedWorlds = (ArrayList<World>) plugin.getServer().getWorlds();
        plugin.getLogger().info("Initializing Schematic Manager");
        for (World world : plugin.getServer().getWorlds()) {
            plugin.getLogger().info("Loading world: " + world.getName());
            loadWorld(world);
        }
    }

    private void init() {


    }

    // cant get a list of worlds on startup, so lets listen for them instead as
    // the server starts and loads them
    @EventHandler
    private void onWorldLoadEvent(WorldLoadEvent event) throws DataException, IOException, ParseException {
        World world = event.getWorld();
        loadWorld(world);
    }

    public void loadWorld(World world) throws DataException, ParseException, IOException {
        File WorldFolder = new File(WorldSchematics.PluginFolder + "/Schematics/" + world.getName());
        if (!WorldFolder.exists()) {
            plugin.getLogger().info("Folder for " + world.getName() + " doesnt exist, creating folder");
            WorldFolder.mkdirs();
        } else {
            DebugLogger.log("Folder for " + world.getName() + " already exists");
        }
        loadSchematics(world);

        //add to list of loaded worlds
        LoadedWorlds.add(world);
        DebugLogger.log("World " + world.getName() + " loaded");
    }

    //remove world if its unloaded
    @EventHandler
    private void onWorldUnloadEvent(WorldUnloadEvent event) {
        LoadedWorlds.remove(event.getWorld());

    }

    //whenever a new chunk is created
    @EventHandler
    private void onChunkPopulate(ChunkPopulateEvent event) throws IOException, WorldEditException,  ParseException, com.sk89q.worldedit.world.DataException {
        DebugLogger.log("new chunk created, looking to spawn schematic", DebugLogger.DebugType.WORLDGENERATION);

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();

        //If world is not loaded, load world
        if(LoadedWorlds.contains(world) == false){
            loadWorld(world);
        }


        if (chunk.load()) {
            if(Schematics.isEmpty()){
                DebugLogger.log("No schematics available for this world", DebugLogger.DebugType.WORLDGENERATION);

            }

            for (SpawnSchematic child : Schematics) {
                // check and make sure the schematic would be spawned in
                // this world
                if (child.getWorld().getName().equals(world.getName())) {
                    //check the chances of the schematic spawning here
                    Random SpawnChance = new Random();
                    //get chance of spawn from schematics config
                    double ChanceOfSpawn = child.getSchematicConfig().getDouble("chance", 10);
                    DebugLogger.log("chance of schematic spawning: " + ChanceOfSpawn, DebugLogger.DebugType.WORLDGENERATION);

                    if (0 + (10000 - 0) * SpawnChance.nextDouble() <= ChanceOfSpawn) {
                        spawnInChunk(chunk, world, child);
                    }
                }
            }
        }
    }

    private void loadSchematics(World world) throws DataException, IOException, ParseException {
        DebugLogger.log("Loading Schematics from world " + world.getName() + " into memory");
        File worldPath = new File(WorldSchematics.PluginFolder + "/Schematics/" + world.getName());
        File[] directoryListing = worldPath.listFiles();

        for (File child : directoryListing) {
            String fileext = FilenameUtils.getExtension(child.getAbsolutePath());
            if (fileext.equals("schematic") == true || fileext.equals("schem") == true) {
                String schematicfilename = FilenameUtils.removeExtension(child.getName());

                // check if config file exists and if not copy default config
                File ConfigFile = new File(worldPath, schematicfilename + ".yml");
                if (!ConfigFile.exists()) {
                    DebugLogger.log("Schematic doesnt have config file, creating config file");
                    plugin.copy(plugin.getResource("ExampleSchematic.yml"), ConfigFile);
                }

                SpawnSchematic ss = null;
                ss = new SpawnSchematic(schematicfilename, world);


                // make sure we set the world so we know which world this should
                // be set to spawn in
                Schematics.add(ss);
                DebugLogger.log("+ Loaded Schematic " + ss.getName());
            }
        }
    }

    //removes all schematics from memory
    void clearSchematics() {
        Schematics.clear();
    }

    //reloads all schematics
    void reloadSchematics() throws IOException, DataException,  ParseException {
        clearSchematics();
        for (World w : LoadedWorlds) {
            loadSchematics(w);
        }
    }

    void spawnInChunk(Chunk chunk, World world, SpawnSchematic schematic) throws com.sk89q.worldedit.world.DataException, IOException, WorldEditException, ParseException {
        //config data for the schematic
        FileConfiguration data = schematic.getSchematicConfig();
        // used to randomly place a schematic within a chunk
        Random SchematicRandX = new Random();
        Random SchematicRandY = new Random();
        Random SchematicRandZ = new Random();

        // positions of the chunk. no need for Y
        int ChunkX = chunk.getX() * 16;
        int ChunkZ = chunk.getZ() * 16;

        //
        int basementdepth = data.getInt("HeightAdjustment", 0);
        int anywhereminY = data.getInt("minY", 60);
        int anywheremaxY = data.getInt("maxY", 120);

        // position where we will paste the schematic
        Integer PastePosX = ChunkX + SchematicRandX.nextInt(16);
        Integer PastePosZ = ChunkZ + SchematicRandZ.nextInt(16);
        Integer PastePosY;

        //determine positioning
        String position = data.getString("place", "ground");

        //determine the Y value of where to put schematic
        if (position.equals("anywhere")) {
            PastePosY = anywhereminY + SchematicRandY.nextInt(anywheremaxY);
        } else if (position.equals("air")) {
            PastePosY = world.getHighestBlockYAt(PastePosX, PastePosZ) + 1 + SchematicRandY.nextInt(anywheremaxY);

        } else if (position.equals("underground")) {
            PastePosY = world.getHighestBlockYAt(PastePosX, PastePosZ) - SchematicRandY.nextInt(anywhereminY);
        } else {
            //paste on ground as default
            PastePosY = world.getHighestBlockYAt(PastePosX, PastePosZ);
        }
        //take basementdepth into account
        //NOTE: This config option is depreciated now
        PastePosY = PastePosY + basementdepth;

        //take into account offset
        PastePosX = PastePosX + schematic.getConfigOffsetX();
        PastePosY = PastePosY + schematic.getConfigOffsetY();
        PastePosZ = PastePosZ + schematic.getConfigOffsetZ();
        //defensive copy to prevent original from modification
        //SpawnSchematic sCopy = new SpawnSchematic(schematic);

        //may cause lag, will need to test to see if it still reads file from disk each time
        if (WorldSchematics.isSpawnSchematicsOn() && schematic.isEnabled()) {
            schematic.spawn(world, PastePosX, PastePosY, PastePosZ);
        }

    }

    public static void spawn(String schematicName, Location location) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        SpawnSchematic schematic = getSpawnSchematic(schematicName);

        //may cause lag, will need to test to see if it still reads file from disk each time
        if (schematic != null) {
            if (WorldSchematics.isSpawnSchematicsOn()) {
                schematic.spawn(location);
            }
        } else {
            DebugLogger.log("Attempted to spawn schematic, unable to find schematic: " + schematicName, DebugLogger.DebugType.MISC);
        }
    }

    public static void spawn(String schematicName, Location location, int rotation) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        spawn(schematicName,location,rotation,false);
    }

    public static void spawn(String schematicName, Location location, boolean skipChecks) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        spawn(schematicName,location,-1,skipChecks);
    }

    public static void spawn(String schematicName, Location location, int rotation, boolean skipChecks) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        //defensive copy to prevent original from modification
        SpawnSchematic sCopy = getSpawnSchematic(schematicName);

        //may cause lag, will need to test to see if it still reads file from disk each time
        if (sCopy != null) {
            if (WorldSchematics.isSpawnSchematicsOn()) {
                sCopy.spawn(location,rotation,skipChecks);
            }
        } else {
            DebugLogger.log("Attempted to spawn schematic, unable to find schematic: " + schematicName, DebugLogger.DebugType.MISC);
        }
    }

    //TO-DO: will scan through all chunks in a world and populate it with schematics
    void populateWorld(World world) {

    }

    public static SpawnSchematic getSpawnSchematic(String name) {
        for (SpawnSchematic schematic : Schematics) {
            if (name.equals(schematic.getName())) {
                return schematic;
            }
        }

        return null;
    }
}


