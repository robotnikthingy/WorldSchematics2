package worldschematics.schematicBlock;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;


class AbstractSchematicBlock {
    Location schematicLocation;
    ConfigurationSection configSection;
    String name;

    AbstractSchematicBlock(Location schematicLocation, ConfigurationSection configSection, String name){
        this.schematicLocation = schematicLocation;
        this.configSection = configSection;
        this.name = name;

    }

     public String getName() {
        return name;
    }

     public void setName(String name) {
        this.name = name;
    }



     public enum blockType {
        SPAWNER, CONTAINER, MARKER
    }

     public Location getLocation() {
        return schematicLocation;
    }

     public void setLocation(Location location) {
        this.schematicLocation = location;
    }


}
