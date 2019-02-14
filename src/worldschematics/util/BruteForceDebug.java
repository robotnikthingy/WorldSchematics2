package worldschematics.util;

import com.sk89q.worldedit.math.Vector3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import worldschematics.WorldSchematics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class BruteForceDebug {

    String debugFileName = WorldSchematics.getInstance().getDataFolder() + "/bruteforce.txt";

    public Location bruteForceBlockLocation(Location blockPosition, Material blockMaterial, int rotation, Vector3 schematicSpawnLocation) throws IOException {
        World world = blockPosition.getWorld();

        Location realLocation;

        PrintWriter writer = new PrintWriter(new FileWriter(debugFileName, true));

        writer.println("=============================================================================");
        writer.println("Block Location supposed to be at : " + blockPosition.getBlockX() + " " + blockPosition.getBlockY() + " " + blockPosition.getBlockZ());
        writer.println("Block material: " + blockMaterial.toString());
        writer.println("Schematic Rotation: " + rotation);
        writer.println("Schematic paste x y z: " + schematicSpawnLocation.getX() + " " + schematicSpawnLocation.getY() + " " + schematicSpawnLocation.getZ());


        Boolean found = false;

        int startSearchX = blockPosition.getBlockX() - 10;
        int endSearchX = blockPosition.getBlockX() + 10;

        int startSearchZ = blockPosition.getBlockZ() - 10;
        int endSearchZ = blockPosition.getBlockZ() + 10;

        DebugLogger.log("starting brute force search at x z: " + startSearchX + " " + startSearchZ + "   Ending search at x z: " + endSearchX + " " + endSearchZ);



        //only search x and z coordinates, y coordinate has always been correct
        for(int searchX = startSearchX; searchX < endSearchX; searchX++){
            for(int searchZ = startSearchZ; searchZ < endSearchZ; searchZ++){
                Block searchBlock = world.getBlockAt(searchX, blockPosition.getBlockY(),searchZ);

                if(searchBlock.getType().toString().equals(blockMaterial.toString())){
                    found = true;

                    int actualX = searchBlock.getX();
                    int actualZ = searchBlock.getZ();

                    int differenceX = blockPosition.getBlockX() - actualX;
                    int differenceZ = blockPosition.getBlockZ() - actualZ;

                    int pasteDifferenceX = (int) (schematicSpawnLocation.getX() - actualX);
                    int pasteDifferenceZ = (int) (schematicSpawnLocation.getZ() - actualZ);

                    DebugLogger.log("found block location placed at x z: " + actualX + " " + actualZ);

                    writer.println("Block actual location: " + actualX + " " + blockPosition.getBlockY() + " " + actualZ);

                    writer.println("Block difference from supposed location x z: " + differenceX + " " + differenceZ);

                    writer.println("Block difference from paste postion x z: " + pasteDifferenceX + " " + pasteDifferenceZ);

                    realLocation = new Location(world,actualX,blockPosition.getBlockY(),actualZ);

                    return realLocation;
                }
            }
            if(found == true){
                break;
            }
        }

        if(found != true){
            DebugLogger.log("Was not able to locate block after searching");

        }

        writer.println("=======end brute force===============================================");
        writer.close();

        realLocation = blockPosition;

        return realLocation;
    }

    public void log(String msg) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(debugFileName, true));

        writer.println(msg);
        writer.close();
    }
}
