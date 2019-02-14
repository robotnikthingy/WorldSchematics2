package worldschematics;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import worldschematics.util.DebugLogger;

import java.io.*;

public class WorldSchematics extends JavaPlugin implements Listener {

    static File PluginFolder;


    private static DebugLogger DBlogger;

    private static boolean finishedLoading = false;
    private static PluginDescriptionFile pdf;

    private WorldSchematics plugin = this;

    private static WorldSchematics instance;

    private static Boolean ShowLocation;
    private static Boolean SpawnSchematicsOn = true;
    private static Boolean MythicMobsInstalled = false;
    private static Boolean CrackShotInstalled = false;
    private static Boolean WorldGuardInstalled = false;
    private static Boolean MagicPluginInstalled = false;

    private static Boolean MythicMobsLoaded = false;

    private schematicManager sm;

    private static File baseServerDir;

    //on server start up
    public void onEnable() {
        PluginFolder = getDataFolder();
        instance = this;

        //Start the debugger
        DBlogger = new DebugLogger();

        //set debugger options
        DBlogger.setDebug(getConfig().getBoolean("debug", false));
        DBlogger.setDebugSchematicInfo(getConfig().getBoolean("debugOptions.debugSchematicInfo", false));
        DBlogger.setDebugSchematicSpawning(getConfig().getBoolean("debugOptions.debugSchematicSpawning", false));
        DBlogger.setDebugLootTables(getConfig().getBoolean("debugOptions.debugLootTables", false));
        DBlogger.setDebugMobSpawning(getConfig().getBoolean("debugOptions.debugMobSpawning", false));
        DBlogger.setDebugWorldGeneration(getConfig().getBoolean("debugOptions.debugWorldGeneration", false));
        DBlogger.setDebugMarkers(getConfig().getBoolean("debugOptions.debugMarkers", false));
        DBlogger.setLogToFile(getConfig().getBoolean("debugOptions.logToFile", false));

        //Start the schematicManager
        try {
            sm = new schematicManager(this);
        } catch (DataException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //need to see if worldedit is installed first or else this wont even work
        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            //we dont even initialize the plugin on startup if no WorldEdit
            getLogger().info("WorldEdit not Installed! This plugin requires WorldEdit to work. Plugin Disabled");
            return;
        }

        // check and see if any softdependencies are installed
        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            getLogger().info("MythicMobs detected, hooked into MythicMobs!");
            setMythicMobsInstalled(true);

        }

        if (getServer().getPluginManager().getPlugin("CrackShot") != null) {
            getLogger().info("CrackShot detected, hooked into CrackShot!");
            setCrackShotInstalled(true);
        }

        if (getServer().getPluginManager().getPlugin("Magic") != null) {
            getLogger().info("Magic detected, hooked into Magic!");
            setMagicPluginInstalled(true);


        }

        /*
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getLogger().info("WorldGuard detected, hooked into WorldGuard!");
            setWorldGuardInstalled(true);
        }
*/

        // register commands
        this.getCommand("worldschematics").setExecutor(this);

        // register listener
        //getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        getServer().getPluginManager().registerEvents(sm, this);


        try {
            loadPlugin();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataException e) {
            e.printStackTrace();
        }

        //start metrics
        Metrics metrics = new Metrics(this);

        pdf = this.getDescription();

        baseServerDir = getServer().getWorldContainer();


    }

    // returns plugin folder
    public static File getFilepath() {
        return PluginFolder;
    }


    //loads/reloads the plugin and all its options
    void loadPlugin() throws IOException, DataException {

        finishedLoading = false;



        File configFile = new File(PluginFolder + "/config.yml");
        File LootTableFolder = new File(getDataFolder() + "/LootTables");

        // make config file and plugin directory
        if (PluginFolder.exists() == false)
            PluginFolder.mkdir();

        if (LootTableFolder.exists() == false)
            LootTableFolder.mkdir();

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        //set debugger options
        DBlogger.setDebug(getConfig().getBoolean("debug", false));
        DBlogger.setDebugSchematicInfo(getConfig().getBoolean("debugOptions.debugSchematicInfo", false));
        DBlogger.setDebugSchematicSpawning(getConfig().getBoolean("debugOptions.debugSchematicSpawning", false));
        DBlogger.setDebugLootTables(getConfig().getBoolean("debugOptions.debugLootTables", false));
        DBlogger.setDebugMobSpawning(getConfig().getBoolean("debugOptions.debugMobSpawning", false));
        DBlogger.setDebugWorldGeneration(getConfig().getBoolean("debugOptions.debugWorldGeneration", false));
        DBlogger.setDebugMarkers(getConfig().getBoolean("debugOptions.debugMarkers", false));
        DBlogger.setLogToFile(getConfig().getBoolean("debugOptions.logToFile", false));

        ShowLocation = getConfig().getBoolean("ShowLocation", false);


        // update and create config files
        updateConfigs();

        finishedLoading = true;
    }

    // used to copy files from the .jar file to the config folders
    void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // returns an instance of the plugin
    public static WorldSchematics getInstance() {
        return instance;
    }

    // load schematics and associated config files into memory to improve
    // performance
    // so we dont use up resources reading/writing to disk when new chunks are
    // created


    // automatically update configs with new options and make this user friendly
    // for servers
    private void updateConfigs() throws IOException, DataException {
        updateConfigs(false);
    }

    // ToDo: Need to update this method since schematics are now kept in memory
    private void updateConfigs(boolean AddMissingOptions) throws IOException, DataException {
        getLogger().info("Updating and creating config files for schematics");
        for (World world : getServer().getWorlds()) {
            getLogger().info("Checking schematic configs for world " + world.getName());

            File worldPath = new File(PluginFolder + "/Schematics/" + world.getName());
            File[] directoryListing = worldPath.listFiles();
            for (File child : directoryListing) {
                String fileext = FilenameUtils.getExtension(child.getAbsolutePath());
                if (fileext.equals("schematic") == true) {
                    String schematicfilename = FilenameUtils.removeExtension(child.getName());
                    File ConfigFile = new File(worldPath, schematicfilename + ".yml");
                    if (!ConfigFile.exists()) {
                        getLogger().info("Schematic doesnt have config file, creating config file");
                        plugin.copy(plugin.getResource("ExampleSchematic.yml"), ConfigFile);
                    }


                    // if we should add missing config options. This deletes all
                    // comments in the config however
                    if (AddMissingOptions) {
                        getLogger().info("Checking config file for schematic " + schematicfilename);

                        FileConfiguration data = YamlConfiguration.loadConfiguration(ConfigFile);

                        // now lets check the config files and see if they are
                        // updated
                        if (data.contains("blacklist") == false) {
                            getLogger().info("config file doesnt contain blacklist option, adding it");
                            data.createSection("blacklist");
                            data.set("blacklist", "[]");
                        }

                        if (data.contains("whitelistmode") == false) {
                            getLogger().info("config file doesnt contain whitelistmode option, adding it");
                            data.createSection("whitelistmode");
                            data.set("whitelistmode", false);
                        }
                        data.save(ConfigFile);
                    }

                }
            }
        }

    }

    //return instance of WorldGuar
    public WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    // reloads the plugin and any schematics
    void reload() throws DataException, IOException, JSONException, ParseException {
        getLogger().info("Reloading Plugin");
        loadPlugin();
        sm.reloadSchematics();
        getLogger().info("Plugin reloaded!");
    }



    // handles commands typed in game
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)  throws JSONException {
        Boolean IsPlayer = false;
        Player player = null;
        if ((sender instanceof Player)) {
            player = (Player) sender;
            IsPlayer = true;
        }

        if (cmd.getName().equalsIgnoreCase("worldschematics") || cmd.getName().equalsIgnoreCase("worldschematics2")) {

            if (args.length == 0) {
                if (IsPlayer) {
                    player.sendMessage("Please enter a subcommand: worldschematics <reload/updateconfigs/info/on/off>");
                }

                getLogger().info("Please enter a subcommand: worldschematics <reload/updateconfigs/info/on/off>");

                return true;

                // there was a subcommand entered, lets see what it is
            } else {

                if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {

                    if (args[0].equalsIgnoreCase("off")) {

                        if (IsPlayer) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&4Schematic spawning has been turned off"));
                        }

                        getLogger().info("Schematic spawning has been turned off");
                        SpawnSchematicsOn = false;
                    }

                    if (args[0].equalsIgnoreCase("on")) {

                        if (IsPlayer) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&2Schematic spawning has been turned on"));
                        }

                        getLogger().info("Schematic spawning has been turned on");
                        SpawnSchematicsOn = true;
                    }

                }

                if (args[0].equalsIgnoreCase("reload")) {
                    //reload the configs and schematics
                    try {
                        reload();
                    } catch (DataException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    //send a message to the player if executed in game
                    if (IsPlayer) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Plugin reloaded!"));
                    }
                }

                if (args[0].equalsIgnoreCase("updateconfigs")) {
                    getLogger().info("Configs updated!");

                    if (IsPlayer) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Configs updated!"));
                    }

                    try {
                        updateConfigs(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DataException e) {
                        e.printStackTrace();
                    }
                }

                if (args[0].equalsIgnoreCase("info")) {
                    //display plugin version info
                    getLogger().info("WorldSchematics2 version: " + pdf.getVersion());


                    //send a message to the player if executed in game
                    if (IsPlayer) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "WorldSchematics2 version: " + pdf.getVersion()));
                    }
                }

                if (args[0].equalsIgnoreCase("spawn")) {
                    String shcemticsName = "";
                    shcemticsName = args[1];

                    if (!IsPlayer) {
                        getLogger().info("You can only run this command as a player");
                        return false;
                    }

                    Location spawnPos = player.getLocation();


                    try {
                        schematicManager.spawn(shcemticsName,spawnPos);
                    } catch (EmptyClipboardException e) {
                        e.printStackTrace();
                    } catch (com.sk89q.worldedit.world.DataException e) {
                        e.printStackTrace();
                    } catch (MaxChangedBlocksException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (WorldEditException e) {
                        e.printStackTrace();
                    }

                }

                //lists all schematics loaded by worldschematics
                if (args[0].equalsIgnoreCase("list")) {

                }

            }
        }

        return true;
    }

    public boolean isFinishedLoading() {
        return finishedLoading;
    }

    public static Boolean getMythicMobsInstalled() {
        return MythicMobsInstalled;
    }

    private static void setMythicMobsInstalled(Boolean mythicMobsInstalled) {
        MythicMobsInstalled = mythicMobsInstalled;
    }

    public static Boolean getCrackShotInstalled() {
        return CrackShotInstalled;
    }

    private static void setCrackShotInstalled(Boolean crackShotInstalled) {
        CrackShotInstalled = crackShotInstalled;
    }

    public static Boolean getWorldGuardInstalled() {
        return WorldGuardInstalled;
    }

    private static void setWorldGuardInstalled(Boolean worldGuardInstalled) {
        WorldGuardInstalled = worldGuardInstalled;
    }

    public static Boolean getMagicPluginInstalled() {
        return MagicPluginInstalled;
    }

    private static void setMagicPluginInstalled(Boolean magicPluginInstalled) {
        MagicPluginInstalled = magicPluginInstalled;
    }


    public static Boolean isSpawnSchematicsOn() {
        return SpawnSchematicsOn;
    }

    public static Boolean getShowLocation() {
        return ShowLocation;
    }

    public static File getBaseServerDirectory(){

        return baseServerDir;
    }

}
