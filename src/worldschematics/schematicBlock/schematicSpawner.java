package worldschematics.schematicBlock;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import io.lumine.xikage.mythicmobs.spawning.spawners.MythicSpawner;
import io.lumine.xikage.mythicmobs.spawning.spawners.SpawnerManager;
import io.lumine.xikage.mythicmobs.util.types.RandomInt;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

import java.util.ArrayList;
import java.util.Random;

/**
 * represents a spawner within a schematic. Will be depreciated with the introduction of the more versatile marker system
 */
public class schematicSpawner extends worldschematics.schematicBlock.AbstractSchematicBlock {

    ArrayList<String> mobsList = new ArrayList<>();
    spawnerType spawnType;


    public schematicSpawner(Location schematicLocation, ConfigurationSection configSection, String name, spawnerType type) {
        super(schematicLocation, configSection, name);
        this.spawnType = type;
        mobsList = (ArrayList<String>) configSection.getList("mobs");
    }

    public ArrayList<String> getMobsList() {
        return mobsList;
    }

    public void setMobsList(ArrayList<String> mobsList) {
        this.mobsList = mobsList;
    }

    public enum spawnerType {
        MOB, MOBSPAWNER, MYTHICMOB, MYTHICMOBSPAWNER
    }

    public void createInWorld(Location worldLocation) {
        //check if the list has mobs in it first
        if (mobsList != null && !mobsList.isEmpty()) {
            if (spawnType == spawnerType.MOB) {
                createMob(worldLocation);
            }

            if (spawnType == spawnerType.MYTHICMOB) {
                createMythicMob(worldLocation);
            }

            if (spawnType == spawnerType.MOBSPAWNER) {
                createMobSpawner(worldLocation);
            }

            if (spawnType == spawnerType.MYTHICMOBSPAWNER) {
                createMythicMobSpawner(worldLocation);
            }
        } else {
            DebugLogger.log("Mobs list for the spawner is empty, leaving as is", DebugLogger.DebugType.MOBSPAWNING);
        }

    }

    void createMythicMobSpawner(Location worldLocation) {
        DebugLogger.log("Found MythicMobs spawner in config, attempting to create", DebugLogger.DebugType.MOBSPAWNING);
        if (WorldSchematics.getMythicMobsInstalled() == true) {
            Random randomizer = new Random();
            String RandomMob = getMobsList().get(randomizer.nextInt(getMobsList().size()));
            SpawnerManager sm = MythicMobs.inst().getSpawnerManager();
            Block block = worldLocation.getBlock();
            String spawnerName = "WorldSchamatics_" + worldLocation.getWorld().getName() + "_x" +worldLocation.getBlockX() + "_y" + worldLocation.getBlockY() + "_z" + worldLocation.getBlockZ() + "_" + RandomMob;
            block.setType(Material.AIR);

            sm.createSpawner(spawnerName, worldLocation, RandomMob);

            DebugLogger.log("Creating MythicMobs spawner using mob " + RandomMob + " at " + worldLocation.getBlockX() + " " + worldLocation.getBlockY() + " " + worldLocation.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);


            //set options for the spawner
            MythicSpawner spawner = sm.getSpawnerByName(spawnerName);

            if (configSection.contains("properties.checkforplayers") == true) {
                spawner.setCheckForPlayers(false);
            }

            if (configSection.contains("properties.cooldown") == true) {
                spawner.setCooldownSeconds(configSection.getInt("properties.cooldown"));
            }

            if (configSection.contains("properties.group") == true) {
                spawner.setGroup(configSection.getString("properties.group"));
            }

            //if (configSection.contains("properties.healonleash") == true) {
                //spawner.setHealLeashedMobs(configSection.getBoolean("properties.healonleash"));
            //}

            if (configSection.contains("properties.leashrange") == true) {
                spawner.setLeashRange(configSection.getInt("properties.leashrange"));
            }

            if (configSection.contains("properties.maxmobs") == true) {
                spawner.setMaxMobs(configSection.getInt("properties.maxmobs"));
            }

            //if (configSection.contains("properties.moblevel") == true) {
               // spawner(configSection.getInt("properties.moblevel"));
           // }

            if (configSection.contains("properties.mobsperspawn") == true) {
                spawner.setMobsPerSpawn(configSection.getInt("properties.mobsperspawn"));
            }

            if (configSection.contains("properties.radius") == true) {
                spawner.setSpawnRadius(configSection.getInt("properties.radius"));
            }

            if (configSection.contains("properties.showflames") == true) {
                spawner.setShowFlames(configSection.getBoolean("properties.showflames"));
            }

            if (configSection.contains("properties.warmup") == true) {
                spawner.setWarmupSeconds(configSection.getInt("properties.warmup"));
            }
        } else {
            WorldSchematics.getInstance().getLogger().info("tried creating MythicMobs spawner but MythicMobs is not installed");
        }
    }

    void createMobSpawner(Location worldLocation) {
        DebugLogger.log("Found Mob Spawner in config, attempting to create", DebugLogger.DebugType.MOBSPAWNING);

        //try setting block as air then as spawner again an d see if that fixes issue on some servers
        worldLocation.getBlock().setType(Material.AIR);
        worldLocation.getBlock().setType(Material.SPAWNER);

        CreatureSpawner spawner = (CreatureSpawner) worldLocation.getBlock().getState();
        Random randomizer = new Random();
        String RandomMob = getMobsList().get(randomizer.nextInt(getMobsList().size()));

        /*
        worldschematics.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(worldschematics.getInstance(), new Runnable() {
            public void run() {
                spawner.setSpawnedType(EntityType.valueOf(RandomMob));
            }
        }, 20L);

        */

        spawner.setSpawnedType(EntityType.valueOf(RandomMob));

        //set cooldown if specified
        if (configSection.contains("properties.cooldown") == true) {
            spawner.setDelay(configSection.getInt("properties.cooldown"));

        }

        spawner.update();

        DebugLogger.log("Creating  " + RandomMob + " spawner at " + worldLocation.getBlockX() + " " + worldLocation.getBlockY() + " " + worldLocation.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);
    }

    void createMythicMob(Location worldLocation) {
        DebugLogger.log("Found MythicMob in config, attempting to spawn", DebugLogger.DebugType.MOBSPAWNING);
        if (WorldSchematics.getMythicMobsInstalled() == true) {
            Random randomizer = new Random();
            String RandomMob = getMobsList().get(randomizer.nextInt(getMobsList().size()));
            int Amount = configSection.getInt("properties.amount", 1);

            //just get the instance of mythicmobs
            //remove the spawner block itself
            worldLocation.getBlock().setType(Material.AIR);

            DebugLogger.log("Spawning " + Amount + " of Mythicmob " + RandomMob + " at " + worldLocation.getBlockX() + " " + worldLocation.getBlockY() + " " + worldLocation.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);
            //we need to loop the amount we want since there is no way to spawn a specific amount
            for (int i1 = 0; i1 < Amount; i1++) {
                if(MythicMobs.inst().getMobManager() != null){
                    if (configSection.contains("properties.level") == true) {
                        //if they want mob to be a specific level, we should take that into account
                        ActiveMob mob = MythicMobs.inst().getMobManager().spawnMob(RandomMob, worldLocation, configSection.getInt("properties.moblevel"));

                    } else {
                        ActiveMob mob = MythicMobs.inst().getMobManager().spawnMob(RandomMob, worldLocation);
                    }
                }else{
                    DebugLogger.log("Attempted to spawn MythicMob, but MythicMobs plugin is not loaded yet!", DebugLogger.DebugType.MOBSPAWNING);
                }

            }
        }
    }

    void createMob(Location worldLocation) {
        DebugLogger.log("Found Mob in config, attempting to spawn", DebugLogger.DebugType.MOBSPAWNING);
        Random randomizer = new Random();
        String RandomMob = getMobsList().get(randomizer.nextInt(getMobsList().size()));
        int Amount = configSection.getInt("properties.amount", 1);

        DebugLogger.log("Spawning " + Amount + "of Mob " + RandomMob + " at " + worldLocation.getBlockX() + " " + worldLocation.getBlockY() + " " + worldLocation.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);
        //we need to loop the amount we want since there is no way to spawn a specific amount
        for (int i1 = 0; i1 < Amount; i1++) {
            worldLocation.getWorld().spawnEntity(worldLocation, EntityType.valueOf(RandomMob));
        }
    }

}
