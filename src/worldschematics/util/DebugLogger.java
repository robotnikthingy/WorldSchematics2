package worldschematics.util;

import worldschematics.WorldSchematics;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Handles logging of all debug info for the plugin
 */
public class DebugLogger {


    private static boolean debug = false;

    private static boolean debugLootTables = false;
    private static boolean debugSchematicInfo = false;
    private static boolean debugSchematicSpawning = false;
    private static boolean debugMobSpawning = false;
    private static boolean debugWorldGeneration = false;
    private static boolean debugMarkers = false;

    private static boolean logToFile;

    private static Logger logger;

    private static File logFile;

    public DebugLogger() {
        logger = WorldSchematics.getInstance().getLogger();
        createLogFile();
    }


    public enum DebugType {
        LOOTTABLE, LOOT, MISC, SCHEMATICINFO, SCHEMATICSPAWNING, MOBSPAWNING, WORLDGENERATION, MARKER, INFO, WARNING
    }


    public static void log(String message) {
        log(message, DebugType.MISC);
    }

    // logs messages only if debug option in config is set to true
    public static void log(String message, DebugType type) {

        if(type == DebugType.WARNING){
            logger.info("[WARNING] " + message);
        }

        if (debug == true) {
            if (type == DebugType.MISC)
                logger.info("[Debug] " + message);
            //writeToLogFile("[Debug] " + message);
            if (type == DebugType.SCHEMATICSPAWNING)
                if (debugSchematicSpawning == true)
                    logger.info("[Debug - SchematicSpawning] " + message);
            //writeToLogFile("[Debug - SchematicSpawning] " + message);
            if (type == DebugType.SCHEMATICINFO)
                if (debugSchematicInfo == true)
                    logger.info("[Debug - SchematicInfo] " + message);
            //writeToLogFile("[Debug - SchematicInfo] " + message);
            if (type == DebugType.LOOTTABLE || type == DebugType.LOOT)
                if (debugLootTables == true)
                    logger.info("[Debug - loot] " + message);
            //writeToLogFile("[Debug - loot] " + message);
            if (type == DebugType.MOBSPAWNING)
                if (debugMobSpawning == true)
                    logger.info("[Debug - MobsSpawning] " + message);
            //writeToLogFile("[Debug - MobsSpawning] " + message);
            if (type == DebugType.WORLDGENERATION)
                if (debugWorldGeneration == true)
                    logger.info("[Debug - World Generation] " + message);
            //writeToLogFile("[Debug - World Generation] " + message);
            if (type == DebugType.MARKER)
                if (debugMarkers == true)
                    logger.info("[Debug - Markers] " + message);
        }
    }

    public static void writeToLogFile(String message) {
        if (logToFile == true && WorldSchematics.getInstance().isFinishedLoading() == true && logFile.exists()) {
            try {
                FileOutputStream fos = new FileOutputStream(logFile);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                bw.write(message);
                bw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                logger.info("ERROR: Unable to write to Log file. File not does not exist");
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("ERROR: Unable to write to Log file. Please make sure files exists and file is not protected");
            }

        }

    }

    private void createLogFile() {
        //create log file if it has not been created
        if (logToFile == true) {
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            logFile = new File(WorldSchematics.getInstance().getDataFolder() + "/logs/" + "WorldSchematics2_" + timeStamp + ".txt");

            if (!logFile.exists()) {
                try {
                    //create any directories needed
                    logFile.getParentFile().mkdirs();

                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        logger.info("Debug set to: " + debug);
        DebugLogger.debug = debug;
    }

    public boolean isDebugLootTables() {
        return debugLootTables;
    }

    public void setDebugLootTables(boolean debugLootTables) {
        logger.info("LootTable Debug set to: " + debugLootTables);
        DebugLogger.debugLootTables = debugLootTables;
    }

    public boolean isDebugSchematicInfo() {
        return debugSchematicInfo;
    }

    public void setDebugSchematicInfo(boolean debugSchematicInfo) {
        logger.info("Schematic Info Debug set to: " + debugSchematicInfo);
        DebugLogger.debugSchematicInfo = debugSchematicInfo;
    }

    public boolean isDebugSchematicSpawning() {
        return debugSchematicSpawning;
    }

    public void setDebugSchematicSpawning(boolean debugSchematicSpawning) {
        logger.info("Schematic Spawning Debug set to: " + debugSchematicSpawning);
        DebugLogger.debugSchematicSpawning = debugSchematicSpawning;
    }

    public boolean isDebugMobSpawning() {
        return debugMobSpawning;
    }

    public void setDebugMobSpawning(boolean debugMobSpawning) {
        logger.info("Mobspawning debug set to: " + debugMobSpawning);
        DebugLogger.debugMobSpawning = debugMobSpawning;
    }

    public boolean isDebugWorldGeneration() {
        return debugWorldGeneration;
    }

    public void setDebugWorldGeneration(boolean debugWorldGeneration) {
        logger.info("WorldGeneration debug set to: " + debugWorldGeneration);
        DebugLogger.debugWorldGeneration = debugWorldGeneration;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public void setLogToFile(boolean logToFile) {
        logger.info("Log to file set to: " + logToFile);
        DebugLogger.logToFile = logToFile;
    }

    public boolean isDebugMarkers() {
        return debugMarkers;
    }

    public void setDebugMarkers(boolean debugMarkers) {
        logger.info("Markers Debug set to: " + debugMarkers);
        DebugLogger.debugMarkers = debugMarkers;
    }


}
