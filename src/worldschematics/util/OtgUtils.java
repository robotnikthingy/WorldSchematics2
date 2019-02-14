package worldschematics.util;

import com.pg85.otg.BiomeIds;
import com.pg85.otg.LocalBiome;
import com.pg85.otg.LocalWorld;
import org.bukkit.World;
import worldschematics.WorldSchematics;

import java.io.File;

import static com.pg85.otg.OTG.getWorld;

public class OtgUtils {

    public static String getBiomeName(String worldName, int x, int z)
    {
        LocalWorld world = getWorld(worldName);
        if (world == null)
        {
            // World isn't loaded by Terrain Control
            return null;
        }

        LocalBiome biome = world.getBiome(x, z);
        BiomeIds biomeIds = biome.getIds();

        DebugLogger.log("Biome in OTG world is " + biome.getName() + " biome, with id " + biomeIds.getGenerationId(), DebugLogger.DebugType.SCHEMATICSPAWNING);

        return biome.getName();
    }

    public static boolean isOtgWorld (World world){

        String worldName = world.getName();

        //Since we cant get this through the OTG plugin itself, hacky way of doing it by checking if a world with this name exists in the OTG plugin directory
        File baseOtgDirectory = new File(WorldSchematics.getBaseServerDirectory() + "/plugins/OpenTerrainGenerator/worlds/" + worldName);


        if(baseOtgDirectory.exists()){
            DebugLogger.log("World is an OTG world", DebugLogger.DebugType.SCHEMATICSPAWNING);
            return true;
        }else{
            DebugLogger.log("World is not an OTG world", DebugLogger.DebugType.SCHEMATICSPAWNING);
            return false;
        }
    }
}
