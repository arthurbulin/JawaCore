package net.jawasystems.jawacore;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.debug.DebugCommand;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.listeners.PlayerJoin;
import net.jawasystems.jawacore.listeners.PlayerPreJoin;
import net.jawasystems.jawacore.listeners.PlayerQuit;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author alexander
 */
public class JawaCore extends JavaPlugin {

    public static JawaCore plugin;
    public static boolean debug;
    private static Configuration config;
    public static RestHighLevelClient restClient;
    public static String eshost;
    public static int esport;

    public static ESHandler eshandler;
    //public static PlayerDataHandler playerDataHandler;
    public static PlayerManager playerManager;

    public final static String pluginSlug = "[JawaCore] ";
    
    private static LocalDateTime bootTime;

    @Override
    public void onEnable() {
        bootTime = LocalDateTime.now();
        plugin = this;
        Logger.getLogger(JawaCore.class.getName()).log(Level.INFO, "Initiallizing JawaCore.");
        loadConfig();
        
        startESHandler();
        
        playerManager = new PlayerManager(plugin);
        
        this.getCommand("debug").setExecutor(new DebugCommand());
        
        getServer().getPluginManager().registerEvents(new PlayerPreJoin(), this);
        //getServer().getPluginManager().registerEvents(new PlayerJoin(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(), this);

    }

    @Override
    public void onDisable() {
        try {
            restClient.close();
        } catch (IOException ex) {
            Logger.getLogger(JawaCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Loads the configuration file from storage and loads the values into
     * static references within the plugin.
     */
    public void loadConfig() {
        Logger.getLogger(JawaCore.class.getName()).log(Level.INFO, "JawaCore Config loading: ");
        //System.out.print(pluginSlug + "Loading configuration from file.");
        //Handle the config generation and loading
        this.saveDefaultConfig();
        config = this.getConfig();

        debug = config.getBoolean("debug", true);
        esport = config.getInt("esport", 9200);
        eshost = config.getString("eshost", "localhost");

        Logger.getLogger(JawaCore.class.getName()).log(Level.INFO, "debug: {0}, eshost: {1}, esport: {2}", new Object[]{debug, eshost, esport});

    }

    /**
     * Create the elasticsearch permissionsHandler instance needed to query the
     * ElasticSearch db.
     */
    public void startESHandler() {
        Logger.getLogger(JawaCore.class.getName()).log(Level.INFO, "Starting the ElasticSearch Database rest client.");
        //TODO add credentials handling
        restClient = new RestHighLevelClient(RestClient.builder(new HttpHost(eshost, esport, "http"))
                .setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder)
                        -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000)));
        boolean restPing;
        try {
            restPing = restClient.ping(RequestOptions.DEFAULT);
            Logger.getLogger(JawaCore.class.getName()).log(Level.INFO, "ElasticSearch database pings: {0}", restPing);
        } catch (IOException ex) {
            Logger.getLogger(JawaCore.class.getName()).log(Level.SEVERE, null, ex);
        }

        eshandler = new ESHandler(restClient, config);

    }

}
