package worldschematics;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BlockVector;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import worldschematics.schematicBlock.schematicContainer;
import worldschematics.schematicBlock.schematicMarker;
import worldschematics.schematicBlock.schematicSpawner;
import worldschematics.util.BruteForceDebug;
import worldschematics.util.DebugLogger;
import worldschematics.util.OtgUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class SpawnSchematic {

    Random SpawnChance;
    Boolean nospawn;
    private String name;
    private World world;
    private Random rand;
    //config options
    private String place;
    private Boolean pasteair;
    private Boolean restrictbiomes;
    private ArrayList<String> BiomesList = new ArrayList<>();
    private ArrayList<String> BlockBlacklist = new ArrayList<>();
    private ArrayList<String> RegionFlagList = new ArrayList<>();
    private Boolean whitelistmode;
    private Boolean biomeblacklistmode;
    private Boolean randomrotate;
    private double ChanceOfSpawn;
    private int basementdepth;
    private int anywhereminY;
    private int anywheremaxY;
    private int TimesSpawned;
    private int maxspawns;

    private Location location;

    private Chunk chunk;
    private int ChunkX;
    private int ChunkZ;

    private FileConfiguration data;
    private FileConfiguration BlockdataConfig;

    private File ConfigFile;
    private File BlockdataConfigFile;

    //used to randomly place a schematic within a chunk
    private Random SchematicRandX = new Random();
    private Random SchematicRandY = new Random();
    private Random SchematicRandZ = new Random();

    //dimensions and stuff of the schematic
    //dimensions and stuff of the schematic
    private int width;
    private int length;
    private int height;
    private BlockVector3 origin;
    private Vector3 offset;

    private int configOffsetX;
    private int configOffsetY;
    private int configOffsetZ;

    private boolean enabled;

    private File worldPath;
    private File SchematicFile;

    private double version;

    private File datafolder = WorldSchematics.getFilepath();

    //special blocks
    private ArrayList<schematicSpawner> spawners = new ArrayList<schematicSpawner>();
    private ArrayList<schematicContainer> containers = new ArrayList<schematicContainer>();
    private ArrayList<schematicMarker> markers = new ArrayList<>();

    //load schematic info and config file info here as well when constructed
    @SuppressWarnings("deprecation")
    public SpawnSchematic(String name, World world) throws IOException, JSONException, ParseException {
        DebugLogger.log("==Schematic Properties==", DebugLogger.DebugType.SCHEMATICINFO);

        //load the schematic
        this.name = name;
        this.world = world;

        DebugLogger.log("Name: " + this.name, DebugLogger.DebugType.SCHEMATICINFO);

        worldPath = new File(datafolder + "/Schematics/" + world.getName());
        SchematicFile = new File(worldPath, name + ".schematic");
        //test if sponge schematic instead
        if(SchematicFile.exists() == false){
            DebugLogger.log("Schematic is Sponge Schematic", DebugLogger.DebugType.SCHEMATICINFO);
            SchematicFile = new File(worldPath, name + ".schem");
        }
        DebugLogger.log("Schematic path: " + SchematicFile.getCanonicalPath(), DebugLogger.DebugType.SCHEMATICINFO);

        //load schematic into temporary clipboard so we can read some of its properties
        ClipboardFormat format = ClipboardFormats.findByFile(SchematicFile);
        try (ClipboardReader reader = format.getReader(new FileInputStream(SchematicFile))) {
            Clipboard TempCC = reader.read();

            this.width = TempCC.getDimensions().getX();
            this.length = TempCC.getDimensions().getZ();
            this.height = TempCC.getDimensions().getY();

            this.origin = TempCC.getOrigin();
            //this.offset = TempCC.;

        }


        BlockdataConfigFile = new File(worldPath, name + "-blockdata.yml");
        ConfigFile = new File(worldPath, name + ".yml");

        if (!BlockdataConfigFile.exists())
            DebugLogger.log("Schematic config file doesnt exist", DebugLogger.DebugType.SCHEMATICINFO);

        //load info from config file
        loadConfig();

        //load special blocks in the schematic
        loadSchematicBlocks();


        DebugLogger.log("========================", DebugLogger.DebugType.SCHEMATICINFO);


    }

    public SpawnSchematic(SpawnSchematic s) throws IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        this(s.name, s.world);
    }

    public void spawn(Location location) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        spawn(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, false);
    }

    public void spawn(Location location, Boolean skipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        spawn(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, skipChecks);
    }

    public void spawn(Location location, int rotation, Boolean skipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        spawn(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), rotation, skipChecks);
    }

    public void spawn(World world, int x, int y, int z) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        spawn(world, x, y, z, -1, false);
    }

    public void spawn(World world, int x, int y, int z, Boolean SkipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        spawn(world, x, y, z, -1, SkipChecks);
    }

    //check for valid spawn location, handle rotation, etc, here
    public void spawn(World world, int x, int y, int z, int parRotation, Boolean SkipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JSONException, ParseException {
        DebugLogger.log("==Spawning Debug Info==", DebugLogger.DebugType.SCHEMATICSPAWNING);

        int rotation = 0;


        location = new Location(world, x, y, z);

        chunk = location.getChunk();
        ChunkX = chunk.getX() * 16;
        ChunkZ = chunk.getZ() * 16;

        ClipboardFormat format = ClipboardFormats.findByFile(SchematicFile);
        try (ClipboardReader reader = format.getReader(new FileInputStream(SchematicFile))) {
            Clipboard clipboard = reader.read();

            BiomesList = (ArrayList<String>) data.getList("biomelist");

            DebugLogger.log("Spawning Schematic " + name + " at position: " + x + " " + y + " " + z, DebugLogger.DebugType.SCHEMATICSPAWNING);


            if (randomrotate == true && parRotation != -1) {
                int randomNum = 0 + (int) (Math.random() * 4);

                if (randomNum == 1) {
                    rotation = 90;
                } else if (randomNum == 2) {
                    rotation = 180;
                } else if (randomNum == 3) {
                    rotation = 270;
                } else {
                    rotation = 0;
                }
                //offset = cuboidClipboard.getOffset();
            }

            //com.sk89q.worldedit.Vector configOffset = new Vector(configOffsetX + cuboidClipboard.getOffset().getBlockX(),configOffsetY + cuboidClipboard.getOffset().getBlockY(),configOffsetZ + cuboidClipboard.getOffset().getBlockZ());

            //cuboidClipboard.setOffset(configOffset);

            DebugLogger.log("Rotation = " + rotation, DebugLogger.DebugType.SCHEMATICSPAWNING);

            Boolean BiomeCheck = false;
            Boolean HeightCheck = false;
            Boolean SpawnLimitCheck = false;

            Boolean BlockCheck = false;

            //check minY and maxY
            if (y < anywheremaxY || y > anywhereminY) {
                HeightCheck = true;
            } else {
                DebugLogger.log("Schematic was set to be spawned outside of anywhereminY and anywheremax, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
            }

            //check spawn limits
            if (maxspawns == 0) {
                SpawnLimitCheck = true;
            } else {
                if (TimesSpawned >= maxspawns) {
                    DebugLogger.log("Schematic has reached the maximum amount of times it can be spawned, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    SpawnLimitCheck = false;
                } else {
                    SpawnLimitCheck = true;
                }
            }

            //check biome
            if (restrictbiomes == false) {
                BiomeCheck = true;
                DebugLogger.log("restrictbiomes set to false, skipping biome check", DebugLogger.DebugType.SCHEMATICSPAWNING);
            } else {
                Boolean DoesBiomeMatchList = false;
                String CurrentBiome = world.getBiome(x, z).name().toUpperCase();

                //handle if this is an OTG biome
                if (OtgUtils.isOtgWorld(world)) {
                    CurrentBiome = OtgUtils.getBiomeName(world.getName(), x, z).toUpperCase();
                }


                DebugLogger.log("Checking biome of chunk. Biome is: " + CurrentBiome, DebugLogger.DebugType.SCHEMATICSPAWNING);


                for (String BiomeName : BiomesList) {
                    BiomeName = BiomeName.toUpperCase();
                    DebugLogger.log("Blacklist BiomeName = " + BiomeName, DebugLogger.DebugType.SCHEMATICSPAWNING);


                    if (BiomeName.equals(CurrentBiome)) {
                        DoesBiomeMatchList = true;
                    }
                }


                if (DoesBiomeMatchList) {
                    if (biomeblacklistmode) {
                        BiomeCheck = false;
                        DebugLogger.log("Biome schematic is set to spawn in is on blacklist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    } else {
                        BiomeCheck = true;
                    }
                } else {
                    if (biomeblacklistmode) {
                        BiomeCheck = true;
                    } else {
                        BiomeCheck = false;
                        DebugLogger.log("Biome schematic is set to spawn in is not on whitelist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    }
                }


            }

            //check block schematic is spawning on
            if (BlockBlacklist != null && !BlockBlacklist.isEmpty()) {
                Boolean DoesBlockMatchList = false;
                DebugLogger.log("Whitelist/Blacklist is not empty, checking list", DebugLogger.DebugType.SCHEMATICSPAWNING);

                for (String MaterialName : BlockBlacklist) {
                    //get block below the paste position
                    Location loc = new Location(world, x, y - 1, z);
                    Block b = loc.getBlock();
                    DebugLogger.log("Checking if block below schematic is " + MaterialName, DebugLogger.DebugType.SCHEMATICSPAWNING);
                    DebugLogger.log("Block below schematic location is " + b.getType().toString(), DebugLogger.DebugType.SCHEMATICSPAWNING);
                    if (b.getType().toString().equals(MaterialName)) {

                        DebugLogger.log("Blocks below schematic match block on list", DebugLogger.DebugType.SCHEMATICSPAWNING);
                        DoesBlockMatchList = true;
                    }
                }


                if (DoesBlockMatchList) {
                    if (whitelistmode) {
                        BlockCheck = true;
                    } else {
                        BlockCheck = false;
                        DebugLogger.log("Blocks below schematic are on blacklist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    }
                } else {
                    if (whitelistmode) {
                        BlockCheck = false;
                        DebugLogger.log("Blocks below schematic are not on whitelist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    } else {
                        BlockCheck = true;
                    }
                }


            } else {
                //no blocks in blacklist/whitelist
                BlockCheck = true;
            }


            if (BiomeCheck == true && HeightCheck == true && SpawnLimitCheck == true && BlockCheck == true || SkipChecks == true) {

                if (WorldSchematics.getShowLocation() == true) {
                    WorldSchematics.getInstance().getLogger().info("Schematic passed all checks. Spawned schematic at: " + x + " " + y + " " + z);
                }

                //increase spawn count
                if (maxspawns > 0) {
                    data.set("TimesSpawned", TimesSpawned++);
                    data.save(ConfigFile);
                }

                //overlooked the pasteair option in the config, so invert whatever the boolean is
                //cuboidClipboard.paste(editsession, pasteposition, !pasteair);

                //code for manipulating schematic in the clipboard and pasting it
                com.sk89q.worldedit.world.World weWorld = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world);
                try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1)) {

                    AffineTransform transform = new AffineTransform();
                    transform = transform.rotateZ(rotation);

                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    //apply rotation
                    holder.setTransform(holder.getTransform().combine(transform));
                    //perform paste
                    Operation operation = holder
                            .createPaste(editSession)
                            .to(BlockVector3.at(x, y, z))
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(operation);
                }


                //fill chests and add spawns once schematic is pasted
                //applyBlockData(rotation, pasteposition, offset, transform);

                /*
                //create WorldGuard region
                if (data.getBoolean("regionSettings.createWorldGuardRegion", false) == true) {
                    String regionName = "WorldSchamatics_" + world.getName() + "_x" + pasteposition.getBlockX() + "_y" + pasteposition.getBlockY() + "_z" + pasteposition.getBlockZ() + "_" + name;
                    createWorldGuardRegion(regionName, (BlockVector) extent.getMaximumPoint(), (BlockVector) extent.getMinimumPoint());

                    RegionManager rm = WGBukkit.getRegionManager(world);
                    ProtectedRegion schRegion = rm.getRegion(regionName);

                    for (String flagString : RegionFlagList) {
                        String[] flagAndValue = flagString.split("\\|");

                        Flag sf = DefaultFlag.fuzzyMatchFlag(flagAndValue[0]);

                        schRegion.setFlag(sf, StateFlag.State.valueOf(flagAndValue[1]));

                    }
                }
                */

            }
        }

        DebugLogger.log("==End Spawning Debug Info==", DebugLogger.DebugType.SCHEMATICSPAWNING);
    }


    void loadConfig() throws IOException, JSONException, ParseException {

        //load config file for the schematic
        if (!ConfigFile.exists()) {
            DebugLogger.log("Schematic doesnt have config file, creating config file", DebugLogger.DebugType.SCHEMATICINFO);
            WorldSchematics.getInstance().copy(WorldSchematics.getInstance().getResource("ExampleSchematic.yml"), ConfigFile);
        }

        //load config file for the schematic blockdata
        if (!BlockdataConfigFile.exists()) {
            DebugLogger.log("Schematic doesnt have config file, creating config file", DebugLogger.DebugType.SCHEMATICINFO);
            WorldSchematics.getInstance().copy(WorldSchematics.getInstance().getResource("ExampleSchematic-blockdata.yml"), BlockdataConfigFile);
            //populate the config file with spawners in schematic, if there are any
            BlockdataConfig = YamlConfiguration.loadConfiguration(BlockdataConfigFile);
        }

        data = YamlConfiguration.loadConfiguration(ConfigFile);
        BlockdataConfig = YamlConfiguration.loadConfiguration(BlockdataConfigFile);

        populateEntityConfig();


        DebugLogger.log("Name: " + this.name, DebugLogger.DebugType.SCHEMATICINFO);

        pasteair = data.getBoolean("pasteair", false);
        place = data.getString("place", "ground");

        restrictbiomes = data.getBoolean("restrictbiomes", false);

        basementdepth = data.getInt("HeightAdjustment", 0);
        anywhereminY = data.getInt("minY", 60);
        anywheremaxY = data.getInt("maxY", 70);

        TimesSpawned = data.getInt("TimesSpawned", 0);
        maxspawns = data.getInt("maxspawns", 0);

        SpawnChance = new Random();

        BlockBlacklist = (ArrayList<String>) data.getList("blacklist");

        BiomesList = (ArrayList<String>) data.getList("biomelist");

        biomeblacklistmode = data.getBoolean("biomeblacklistmode", false);

        whitelistmode = data.getBoolean("whitelistmode", false);

        randomrotate = data.getBoolean("randomrotate", true);

        ChanceOfSpawn = data.getDouble("chance", 100);

        basementdepth = data.getInt("HeightAdjustment", 0);

        anywhereminY = data.getInt("minY", 60);
        anywheremaxY = data.getInt("maxY", 70);

        TimesSpawned = data.getInt("TimesSpawned", 0);
        maxspawns = data.getInt("maxspawns", 0);

        version = data.getDouble("ConfigVersion", 1.0);

        configOffsetX = data.getInt("offset.x", 0);
        configOffsetY = data.getInt("offset.y", 0);
        configOffsetZ = data.getInt("offset.z", 0);

        RegionFlagList = (ArrayList<String>) data.getList("regionSettings.regionFlags");

        enabled = data.getBoolean("enabled", true);
    }

    public void applyBlockData(int rotation, Vector3 pasteposition, Vector3 offset, AffineTransform transform) throws IOException, com.sk89q.worldedit.world.DataException, WorldEditException, JSONException, ParseException {
        DebugLogger.log("Applying Blockdata to the Schematic", DebugLogger.DebugType.SCHEMATICSPAWNING);
        //loop through all blocks in the config
        ConfigurationSection blocksSection = BlockdataConfig.getConfigurationSection("Blocks");

        if (blocksSection != null) {
            for (String block : blocksSection.getKeys(false)) {


                String type = blocksSection.getString(block + ".type");


                //offset is sometimes a deciminal ending in .9999, so need to always round up when converting to int
                int OffsetX = (int) Math.round(offset.getX());
                int OffsetY = (int) Math.round(offset.getY());
                int OffsetZ = (int) Math.round(offset.getZ());

                int ConfigX = (int) blocksSection.getDouble(block + ".x");
                int ConfigZ = (int) blocksSection.getDouble(block + ".z");

                int x = 0;
                int y = (int) (pasteposition.getY() + OffsetY + blocksSection.getDouble(block + ".y"));
                int z = 0;

                //handle if the schematic was rotated
                if (rotation == 0) {
                    x = (int) (pasteposition.getX() + OffsetX + ConfigX);
                    z = (int) (pasteposition.getZ() + OffsetZ + ConfigZ);
                } else if (rotation == 90) {
                    x = (int) (pasteposition.getX() - OffsetZ - ConfigZ);
                    z = (int) (pasteposition.getZ() + OffsetX + ConfigX);
                } else if (rotation == 180) {
                    x = (int) (pasteposition.getX() - OffsetX - ConfigX);
                    z = (int) (pasteposition.getZ() - OffsetZ - ConfigZ);
                } else if (rotation == 270) {
                    x = (int) (pasteposition.getX() + OffsetZ + ConfigZ);
                    z = (int) (pasteposition.getZ() - OffsetX - ConfigX);
                }


                Location BlockLocation = new Location(world, x, y, z);


                Block nbtBlock = BlockLocation.getBlock();
                DebugLogger.log("Testing NBT Block settings for config block: " + block, DebugLogger.DebugType.SCHEMATICSPAWNING);

                DebugLogger.log("NBT Block XYZ in world = " + x + " " + y + " " + z + ". Block type is " + nbtBlock.getType().toString(), DebugLogger.DebugType.SCHEMATICSPAWNING);


                //handle any spawners. Spawner will be depreciated and replaced by marker system in the future, but we still need
                //to handle this for servers which still use the spawner system
                if (type.equalsIgnoreCase("mythicspawner") || type.equalsIgnoreCase("spawner") || type.equalsIgnoreCase("mob") || type.equalsIgnoreCase("mythicmob")) {
                    DebugLogger.log("Special block is a mob spawner", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    schematicSpawner spawner = getSpawner(block);
                    spawner.createInWorld(BlockLocation);
                }

                //Will fill the chest with loot
                if (type.equalsIgnoreCase("container")) {
                    DebugLogger.log("Special block is a chest or container", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    schematicContainer container = getContainer(block);
                    container.createInWorld(BlockLocation);

                    if (!nbtBlock.getType().toString().equals(Material.CHEST.toString())) {
                        BruteForceDebug btd = new BruteForceDebug();
                        btd.log("Schematic is: " + name);
                        btd.log("Offset xyz is: " + OffsetX + " " + OffsetY + " " + OffsetZ);
                        btd.log("Config x z is: " + ConfigX + " " + ConfigZ);
                        Location newLocation = btd.bruteForceBlockLocation(BlockLocation, Material.CHEST, rotation, pasteposition);
                        container.createInWorld(newLocation);
                    }
                }

                //handle markers
                if (type.equalsIgnoreCase("marker")) {
                    DebugLogger.log("Special block is a marker", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    schematicMarker marker = getMarker(block);
                    if (marker != null) {
                        marker.createInWorld(BlockLocation);

                    } else {
                        DebugLogger.log("Unable to find marker!", DebugLogger.DebugType.MISC);

                    }

                }
            }
        } else {
            DebugLogger.log("Blockdata file is corrupt or improperly configured, check configuration file", DebugLogger.DebugType.SCHEMATICSPAWNING);
        }

    }

    private void populateEntityConfig() throws IOException {
        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(SchematicFile);
        try (ClipboardReader reader = format.getReader(new FileInputStream(SchematicFile))) {
            clipboard = reader.read();


            //gets the first block in the schematic
            int SpecialBlockCount = 0;
            int MarkerCount = 0;

            if (clipboard != null) {
                DebugLogger.log("Schematic Dimensions: X=" + clipboard.getDimensions().getX() + " Y=" + clipboard.getDimensions().getY() + " Z=" + clipboard.getDimensions().getZ(), DebugLogger.DebugType.MOBSPAWNING);

                //iterate over every block in the schematic
                for (int x = 0; x < clipboard.getDimensions().getX(); x++) {
                    for (int y = 0; y < clipboard.getDimensions().getY(); y++) {
                        for (int z = 0; z < clipboard.getDimensions().getZ(); z++) {
//						plugin.debug("Block at " + x + " " + y + " " + z);
                            BlockVector3 bVector = BlockVector3.at(x, y, z);

                            BlockState CurrentBlock = clipboard.getBlock(bVector);


                            BlockType bType = CurrentBlock.getBlockType();
                            BaseBlock bBlock = CurrentBlock.toBaseBlock();


                            DebugLogger.log("Block at " + x + " " + y + " " + z + " is: " + bType.getId(), DebugLogger.DebugType.MOBSPAWNING);

                            //if block is a spawner
                            if (bType.getId().equals(BlockTypes.SPAWNER)) {

                                SpecialBlockCount++;
                                String BlockName = "BlockNBT" + SpecialBlockCount;

                                DebugLogger.log("Block at " + x + " " + y + " " + z + " is a spawner", DebugLogger.DebugType.MOBSPAWNING);

                                if (BlockdataConfig.contains("Blocks." + BlockName) == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName);
                                }

                                if (BlockdataConfig.contains("Blocks." + BlockName + ".type") == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".type");
                                    BlockdataConfig.set("Blocks." + BlockName + ".type", "spawner");
                                }

                                BlockdataConfig.createSection("Blocks." + BlockName + ".x");
                                BlockdataConfig.createSection("Blocks." + BlockName + ".y");
                                BlockdataConfig.createSection("Blocks." + BlockName + ".z");

                                if (BlockdataConfig.contains("Blocks." + BlockName + ".mobs") == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".mobs");
                                }

                                if (BlockdataConfig.contains("Blocks." + BlockName + ".properties") == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".properties");
                                }


                                BlockdataConfig.set("Blocks." + BlockName + ".x", x);
                                BlockdataConfig.set("Blocks." + BlockName + ".y", y);
                                BlockdataConfig.set("Blocks." + BlockName + ".z", z);

                                BlockdataConfig.save(BlockdataConfigFile);
                            }

                            //if block is a chest of any type
                            if (bType.getId().equals(BlockTypes.CHEST) || bType.equals(BlockTypes.TRAPPED_CHEST)) {

                                SpecialBlockCount++;
                                String BlockName = "BlockNBT" + SpecialBlockCount;

                                DebugLogger.log("Block at " + x + " " + y + " " + z + " is a container", DebugLogger.DebugType.LOOTTABLE);

                                if (BlockdataConfig.contains("Blocks." + BlockName) == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName);
                                }

                                if (BlockdataConfig.contains("Blocks." + BlockName + ".type") == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".type");
                                    BlockdataConfig.set("Blocks." + BlockName + ".type", "container");
                                }

                                BlockdataConfig.createSection("Blocks." + BlockName + ".x");
                                BlockdataConfig.createSection("Blocks." + BlockName + ".y");
                                BlockdataConfig.createSection("Blocks." + BlockName + ".z");

                                if (BlockdataConfig.contains("Blocks." + BlockName + ".loottables") == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".loottables");
                                }

                                if (BlockdataConfig.contains("Blocks." + BlockName + ".properties") == false) {
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".properties");
                                }


                                BlockdataConfig.set("Blocks." + BlockName + ".x", x);
                                BlockdataConfig.set("Blocks." + BlockName + ".y", y);
                                BlockdataConfig.set("Blocks." + BlockName + ".z", z);

                                BlockdataConfig.save(BlockdataConfigFile);
                            }

                            //if block is a marker sign (wall sign or standing sign)
                            if (bType.getId().equals(BlockTypes.SIGN) || bType.equals(BlockTypes.WALL_SIGN)) {

                                DebugLogger.log("Block at " + x + " " + y + " " + z + " is a sign", DebugLogger.DebugType.MARKER);

                                //create a dummy sign, and load it with NBT data from the sign block, and then read the lines of the sign
                                //Kinda hacky but its the best method of reading data from the sign so far
                            /*
                            SignBlock sign;

                            if(CurrentBlock.getType() == 63){
                                sign = new SignBlock(63,0);
                            }else{
                                sign = new SignBlock(68,0);
                            }

                            sign.setNbtData(CurrentBlock.getNbtData());
                            */
                                String[] signText = new String[3];
                                signText[0] = bBlock.getNbtData().getString("Text1");
                                signText[1] = bBlock.getNbtData().getString("Text2");

                                DebugLogger.log("Line1 of sign says: " + signText[0], DebugLogger.DebugType.MARKER);
                                DebugLogger.log("Line2 of sign says: " + signText[1], DebugLogger.DebugType.MARKER);

                                //{"text":""} is a blank sign, we dont do anything with those
                                if (!signText[0].equals("{\"text\":\"\"}") && !signText[1].equals("{\"text\":\"\"}")) {
                                    signText[0] = parseSignText(signText[0]);
                                    signText[1] = parseSignText(signText[1]);
                                    DebugLogger.log("Line1 of sign after parsing: " + signText[0], DebugLogger.DebugType.MARKER);
                                    DebugLogger.log("Line2 of sign after parsing: " + signText[1], DebugLogger.DebugType.MARKER);
                                }


                                if (signText[0].toLowerCase().equalsIgnoreCase("[Marker]") && !signText.equals("{\"text\":\"\"}") && !signText[1].contains("/") && !signText[1].contains("\\")) {
                                    MarkerCount++;

                                    String BlockName = "Marker" + MarkerCount + "_" + signText[1];

                                    if (BlockdataConfig.contains("Blocks." + BlockName) == false) {
                                        BlockdataConfig.createSection("Blocks." + BlockName);
                                    }

                                    if (BlockdataConfig.contains("Blocks." + BlockName + ".type") == false) {
                                        BlockdataConfig.createSection("Blocks." + BlockName + ".type");
                                        BlockdataConfig.set("Blocks." + BlockName + ".type", "marker");
                                    }

                                    if (BlockdataConfig.contains("Blocks." + BlockName + ".subtype") == false) {
                                        BlockdataConfig.createSection("Blocks." + BlockName + ".subtype");
                                        BlockdataConfig.set("Blocks." + BlockName + ".subtype", "none");
                                    }


                                    BlockdataConfig.createSection("Blocks." + BlockName + ".x");
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".y");
                                    BlockdataConfig.createSection("Blocks." + BlockName + ".z");

                                    if (BlockdataConfig.contains("Blocks." + BlockName + ".properties") == false) {
                                        BlockdataConfig.createSection("Blocks." + BlockName + ".properties");
                                    }

                                    if (BlockdataConfig.contains("Blocks." + BlockName + ".schematics") == false) {
                                        BlockdataConfig.createSection("Blocks." + BlockName + ".schematics");
                                    }

                                    if (BlockdataConfig.contains("Blocks." + BlockName + ".mobs") == false) {
                                        BlockdataConfig.createSection("Blocks." + BlockName + ".mobs");
                                    }

                                    BlockdataConfig.set("Blocks." + BlockName + ".x", x);
                                    BlockdataConfig.set("Blocks." + BlockName + ".y", y);
                                    BlockdataConfig.set("Blocks." + BlockName + ".z", z);

                                    BlockdataConfig.save(BlockdataConfigFile);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadSchematicBlocks() {
        DebugLogger.log("Schematic special blocks", DebugLogger.DebugType.SCHEMATICINFO);
        ConfigurationSection blocksSection = BlockdataConfig.getConfigurationSection("Blocks");
        if (blocksSection != null) {
            for (String block : blocksSection.getKeys(false)) {
                String type = blocksSection.getString(block + ".type");
                DebugLogger.log("Found block " + block + ", type is " + type, DebugLogger.DebugType.SCHEMATICINFO);

                Location locationInSchematic = new Location(world, blocksSection.getDouble(block + ".x"), blocksSection.getDouble(block + ".y"), blocksSection.getDouble(block + ".z"));
                ConfigurationSection section = blocksSection.getConfigurationSection(block);


                if (type.equals("spawner")) {

                    spawners.add(new schematicSpawner(locationInSchematic, section, block, schematicSpawner.spawnerType.MOBSPAWNER));
                }

                if (type.equals("mob")) {
                    spawners.add(new schematicSpawner(locationInSchematic, section, block, schematicSpawner.spawnerType.MOB));
                }

                if (type.equals("mythicspawner")) {
                    spawners.add(new schematicSpawner(locationInSchematic, section, block, schematicSpawner.spawnerType.MYTHICMOBSPAWNER));
                }

                if (type.equals("mythicmob")) {
                    spawners.add(new schematicSpawner(locationInSchematic, section, block, schematicSpawner.spawnerType.MYTHICMOB));
                }

                //add containers. only one type of container right now
                if (type.equals("container")) {
                    containers.add(new schematicContainer(locationInSchematic, section, block, schematicContainer.containerType.CHEST));
                }

                //add markers
                if (type.equals("marker")) {
                    markers.add(new schematicMarker(locationInSchematic, section, block));
                }
            }
        } else {
            DebugLogger.log("-blockdata file is empty, schematic contains no special blocks", DebugLogger.DebugType.SCHEMATICINFO);
        }
    }

    private schematicSpawner getSpawner(String name) {
        for (schematicSpawner sp : spawners) {
            if (sp.getName().equals(name)) {
                return sp;
            }
        }

        return null;
    }

    private schematicContainer getContainer(String name) {
        for (schematicContainer ct : containers) {
            if (ct.getName().equals(name)) {
                return ct;
            }
        }

        return null;
    }

    private schematicMarker getMarker(String name) {
        for (schematicMarker mk : markers) {
            if (mk.getName().equals(name)) {
                return mk;
            }
        }

        return null;
    }

    //hacky way of getting text from sign data since it seems like worldedits method to parse NBT strings does not work
    private String parseSignText(String nbtString) {
        int firstQuoteIndex = nbtString.indexOf("{\"text\":\"") + 9;
        String newString = "NONE";

        DebugLogger.log("firstQuoteIndex = " + firstQuoteIndex, DebugLogger.DebugType.MARKER);

        if (firstQuoteIndex > 0 && firstQuoteIndex != -8) {
            try {
                newString = nbtString.substring(firstQuoteIndex);
            } catch (StringIndexOutOfBoundsException e) {
                DebugLogger.log("some error which makes no sense happened. If markers dont work, try re-exporting your schematic", DebugLogger.DebugType.MARKER);
            }

        }

        DebugLogger.log("newString = " + newString, DebugLogger.DebugType.MARKER);
        int lastQuoteIndex = newString.indexOf("\"}]");
        DebugLogger.log("lastQuoteIndex = " + lastQuoteIndex, DebugLogger.DebugType.MARKER);
        if (lastQuoteIndex > 0) {
            newString = newString.substring(0, lastQuoteIndex).replace(' ', '_');
        }
        DebugLogger.log("newString = " + newString, DebugLogger.DebugType.MARKER);
        return newString;

    }

    public World getWorld() {
        return world;
    }

    public String getName() {
        return name;
    }

    public FileConfiguration getSchematicConfig() {
        return data;
    }

    public FileConfiguration getSchematicDataConfig() {
        return BlockdataConfig;
    }

    public int getConfigOffsetZ() {
        return configOffsetZ;
    }

    public void setConfigOffsetZ(int configOffsetZ) {
        this.configOffsetZ = configOffsetZ;
    }

    public int getConfigOffsetY() {
        return configOffsetY;
    }

    public void setConfigOffsetY(int configOffsetY) {
        this.configOffsetY = configOffsetY;
    }

    public int getConfigOffsetX() {
        return configOffsetX;
    }

    public void setConfigOffsetX(int configOffsetX) {
        this.configOffsetX = configOffsetX;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    //gets a config option from a schematic config file
//	public String GetConfigOption(){
//		
//	}
}
