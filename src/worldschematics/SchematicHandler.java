package worldschematics;

import com.sk89q.worldedit.EditSession;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Created by Robotnik on 8/10/2017.
 * Will handle the loading and pasting of schematics depending on how the server is configured.
 * Can be set to use Worledit, AsyncWorldedit, Fast Async WorldEdit, and possibly my own schematic reader/paster in the future
 */
public class SchematicHandler {

    HandlerType type = HandlerType.WORLDEDIT ;
    int posX;
    int posY;
    int posZ;
    World world;
    EditSession editsession;


    SchematicHandler(EditSession es, HandlerType type, Location location){
        this.type = type;
        editsession = es;
        posX = location.getBlockX();
        posY = location.getBlockY();
        posZ = location.getBlockZ();
        world = location.getWorld();
    }

     enum HandlerType {
        WORLDEDIT, WORLDEDIT6, ASYCNWORLDEDIT, FASTASYCNWORLDEDIT
    }

     void paste(){
        if(type == HandlerType.WORLDEDIT)
            pasteWE();
        else if(type == HandlerType.WORLDEDIT6)
            pasteWE6();
        else if(type == HandlerType.ASYCNWORLDEDIT)
            pasteAWE();
        else if(type == HandlerType.FASTASYCNWORLDEDIT)
            pasteFAWE();

    }

    //Paste schematic using depreciated WE API
     void pasteWE(){


    }

    //Paste schemtatic using new WE API
     void pasteWE6(){
/*        File file = new File("myFile"); // The schematic file
        Vector to = new Vector(0, 0, 0); // Where you want to paste

        World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        Clipboard clipboard = ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);
        Region region = clipboard.getRegion();

        Extent extent = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        AffineTransform transform = new AffineTransform();


        transform = transform.rotateY(90); // E.g. rotate 90
        extent = new BlockTransformExtent(clipboard, transform, worldData.getBlockRegistry());


        ForwardExtentCopy copy = new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
        if (!transform.isIdentity()) copy.setTransform(transform);
        if (ignoreAirBlocks) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        Operations.completeLegacy(copy);*/

    }

    //Paste schematic using AsyncWorldEdit API
     void pasteAWE(){

    }

    //Paste schematics using Fast Async WorldEdit API
     void pasteFAWE(){

    }
}
