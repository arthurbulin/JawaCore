package net.jawasystems.jawacore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.debug.DebugCommand;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.handlers.SessionTrackHandler;
import net.jawasystems.jawacore.handlers.StandardMessages;
import net.jawasystems.jawacore.listeners.sessions.PlayerJoinSession;
import net.jawasystems.jawacore.listeners.PlayerPreJoin;
import net.jawasystems.jawacore.listeners.PlayerQuitCore;
import net.jawasystems.jawacore.listeners.sessions.PlayerConsumeEventSession;
import net.jawasystems.jawacore.listeners.sessions.PlayerDeathSession;
import net.jawasystems.jawacore.listeners.sessions.PlayerQuitSession;
import net.jawasystems.jawacore.listeners.sessions.TeleportEventListenerSession;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class JawaCore extends JavaPlugin {
    public static JawaCore plugin;
    public static boolean debug;
    private static boolean trackSessions;
    private static Configuration config;
    private static String serverName;
    
    private static final Logger LOGGER = Logger.getLogger("JawaCore");
    private static final HashMap<String, Configuration> pluginConfigurations = new HashMap();;
    private static final HashMap<String, Configuration> pluginChangeLogs = new HashMap();

    @Override
    public void onEnable() {
        plugin = this;

        loadConfig();

        if (ESHandler.startESHandler(config.getString("eshost", "localhost"), config.getInt("esport", 9200), config.getString("esuser","mc_server"), config.getString("espass","password"), config.getBoolean("debug", false))) {
            //Schedule maintenance tasks
            PlayerManager.generateCleanupTask();

            //Register core commands
            this.getCommand("debug").setExecutor(new DebugCommand());

            //Register Core events
            getServer().getPluginManager().registerEvents(new PlayerPreJoin(), this);
            getServer().getPluginManager().registerEvents(new PlayerQuitCore(), this);
            
//            if (!validateIndex("servers")) {
//                LOGGER.log(Level.INFO, "No servers index was found. One will now be created.");
//                if (createIndex("servers", null)){
//                    //Create the entry for this server
//                    
//                }
//            }
            
            //Validate the players index
            if (!validateIndex("players")){
                LOGGER.log(Level.INFO, "No players index has been found. All JawaPlugins require a valid players index. Attempting to create.");
                if (!createPlayersIndex()){
                    LOGGER.log(Level.SEVERE, "All attempts to validate/create the players index have failed. JawaPlugins cannot operate without this index. Shutting down server to prevent unmanaged access.");
                    plugin.getServer().shutdown();
                }
            }
            
            //If this server is tracking sessions initialize the needed items and validate the index
            if (trackSessions) startSessiontracking();
            else LOGGER.log(Level.INFO, "This server will not log sessions. To turn on session logging set \"track-sessions\" to \"true\" in the JawaCore config.yml");
            

        } else {
            LOGGER.log(Level.SEVERE, "A link to the database was unable to be established. Shutting down server to prevent unmanaged access.");
            plugin.getServer().shutdown();
        }

    }

    @Override
    public void onDisable() {
        if (trackSessions) SessionTrackHandler.closeAllSessions();
        ESHandler.shutdown();
    }

    /**
     * Loads the configuration file from storage and loads the values into
     * static references within the plugin.
     */
    public void loadConfig() {
        LOGGER.log(Level.INFO, "JawaCore Config loading: ");
        //Handle the config generation and loading
        this.saveDefaultConfig();
        config = this.getConfig();

        trackSessions = config.getBoolean("track-sessions", false);
        
        debug = config.getBoolean("debug", true);
        if (debug){
            LOGGER.info("Debug is turned on! This is not recommended unless you are a dev or are tracking a problem!");
            LOGGER.info("If you are experiencing problems in a clean run environment please contact the dev on github.");
            LOGGER.info("If you are not running in a clean environment (just Jawa plugins) then please do not report your issue to the dev at this time.");
            LOGGER.info("You may switch debug to OFF by changing the debug paramater in the config to false.");
        } else {
            LOGGER.info("Debug is turned off");
        }
        
        serverName = config.getString("server-name","main");
        LOGGER.log(Level.INFO, "This server is: {0}", serverName);
        StandardMessages.loadMessages(config.getConfigurationSection("messages"));
    }
    
    public static JawaCore getPlugin(){
        return plugin;
    }
    
    public static void receiveConfigurations(String pluginName, Configuration configuration){
        pluginConfigurations.put(pluginName, configuration);
        LOGGER.log(Level.INFO, "Configuration received from {0}", pluginName);
    }
    
    public static void receiveChangeLog(String pluginName, Configuration changeLog){
        pluginChangeLogs.put(pluginName, changeLog);
        LOGGER.log(Level.INFO, "Changelog received from {0}", pluginName);
    }
    
    public static Configuration getPluginConfiguration(String pluginName){
        return pluginConfigurations.get(pluginName);
    }
    
    public static Configuration getChangeLog(String pluginName){
        return pluginChangeLogs.get(pluginName);
    }
    
    /** Get the server name as set in the config.
     * @return 
     */
    public static String getServerName(){
        return serverName;
    }
    
    /** Returns true if this server is tracking user sessions
     * @return 
     */
    public static boolean trackSessions(){
        return trackSessions;
    }
    
    /** Start tracking sessions. If the index doesn't exist this will create it.
     */
    public void startSessiontracking() {
        LOGGER.log(Level.INFO, "Validating the sessions index");
        boolean indexExists = validateIndex("sessions");
        if (!indexExists) {
            LOGGER.log(Level.INFO, "Creating the sessions index");
            indexExists = createIndex("sessions", null);
            
        }

        if (indexExists) {
            getServer().getPluginManager().registerEvents(new PlayerJoinSession(), this);
            getServer().getPluginManager().registerEvents(new PlayerQuitSession(), this);
            getServer().getPluginManager().registerEvents(new TeleportEventListenerSession(), this);
            getServer().getPluginManager().registerEvents(new PlayerConsumeEventSession(), this);
            getServer().getPluginManager().registerEvents(new PlayerDeathSession(), this);
//            getServer().getPluginManager().registerEvents(new TeleportEventListenerSession(), this);
        } else {
            LOGGER.log(Level.SEVERE, "All attempts to create/validate the sessions index failed. Session logging is disabled.");
            trackSessions = false;
        }
    }
    
    /** Attempt to create the players index and validate a successful creation.
     * @return 
     */
    private static boolean createPlayersIndex(){
        boolean created = createIndex("players", IndexMappings.getPlayerIndexMappings());
        return created || validateIndex("players");
    }
    
    /** Check the existence of an index. If it exists this returns true. If it does not
     * this return false. Appropriate messages are logged during validation.
     * @param indexName
     * @return 
     */
    public static boolean validateIndex(String indexName){
        try {
            boolean result = ESHandler.indexExists(indexName);
            if (result){
                LOGGER.log(Level.INFO, "Validation of {0} complete", indexName);
            } else {
                LOGGER.log(Level.INFO, "The {0} index does not exist", indexName);
            }
            return result;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, 
                    //Insert cancer here
                    "UWU I have faiwed. Thewe was an ewwow vawidating the existence of the {0} indwex. "
                            + "Please ensure that {1} has valid read/write and transport permissions inside kibana.", 
                    new Object[]{indexName, config.getString("esuser","mc_server")});
            return false;
        }
    }
    /** Creates an index with the name "name" and a mapping using "mappings". 
     * If there is no mapping mappings should be null. This is exposed publicly
     * so that any plugin may create an index it needs.
     * @param index The index to be created
     * @param mappings The mappings for the specified index
     * @return True if shards acknowledge start before the end of creation. False if not, or on error. Index existence should still be
     * checked if false is returned.
     */
    public static boolean createIndex(String index, Map<String, Object> mappings){
        boolean response;
        if (mappings == null){
            response = ESHandler.createIndex(index);
        } else {
            response = ESHandler.createIndex(index, mappings);
        }
        
        //If the response if true, short circuit and send true, if not double check and return validateIndex result
        if (response) LOGGER.log(Level.INFO, "Index {0} successfully created", index);
        
        return response || validateIndex(index);

    }
    
//    public static void createServerEntry(){
//        JSONObject serverEntry = new JSONObject();
//        serverEntry.put("name", serverName);
//        serverEntry.put(serverName, value)
//    }

}
