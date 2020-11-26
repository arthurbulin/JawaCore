package net.jawasystems.jawacore;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.debug.DebugCommand;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.listeners.PlayerPreJoin;
import net.jawasystems.jawacore.listeners.PlayerQuit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class JawaCore extends JavaPlugin {
    public static JawaCore plugin;
    public static boolean debug;
    private static Configuration config;
    
    private static final Logger LOGGER = Logger.getLogger("JawaCore");
    private static final HashMap<String, Configuration> pluginConfigurations = new HashMap();;
    private static final HashMap<String, Configuration> pluginChangeLogs = new HashMap();

    @Override
    public void onEnable() {
        plugin = this;

        loadConfig();

        if (ESHandler.startESHandler(config.getString("eshost", "localhost"), config.getInt("esport", 9200), config.getString("esuser","mc_server"), config.getString("espass","password"), config.getBoolean("debug", false))) {

            PlayerManager.generateCleanupTask();

            this.getCommand("debug").setExecutor(new DebugCommand());

            getServer().getPluginManager().registerEvents(new PlayerPreJoin(), this);
            //getServer().getPluginManager().registerEvents(new PlayerJoin(), this);
            getServer().getPluginManager().registerEvents(new PlayerQuit(), this);

        } else {
            LOGGER.log(Level.SEVERE, "A link to the database was unable to be established. Shutting down server.");
            plugin.getServer().shutdown();
        }

    }

    @Override
    public void onDisable() {
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

        debug = config.getBoolean("debug", true);
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

}
