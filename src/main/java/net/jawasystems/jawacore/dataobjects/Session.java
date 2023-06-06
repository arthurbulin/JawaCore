/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.dataobjects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.handlers.LocationDataHandler;
import net.jawasystems.jawacore.handlers.MaterialHandler;
import net.jawasystems.jawacore.handlers.SessionTrackHandler;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class Session {
    
    private static final Logger LOGGER = Logger.getLogger("JawaCore][Session");
    
    private String sessionID;
    private final UUID UUID;
//    private final String LOGINDATETIME;
    private final JSONObject SESSION = new JSONObject();
//    private final JSONObject TRACKS = new JSONObject();
    
    private final JSONArray TELEPORTEVENTS = new JSONArray();
    private final JSONArray DEATHEVENTS = new JSONArray();
    private final JSONArray CONSUMED = new JSONArray();
    private final JSONArray COMMANDS = new JSONArray();
    private final JSONArray GAMEMODE = new JSONArray();
    
    private final String NAME;

    
    /** Create a new session to track player activities.
     * @param player
     * @param event 
     */
    public Session(Player player){
        this.UUID = player.getUniqueId();
//        LocalDateTime now = LocalDateTime.now();
//        
//        this.LOGINDATETIME = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        //sessionID = this.UUID.toString().concat("-").concat(String.valueOf(now.toEpochSecond(ZoneOffset.UTC)));
        
        //SESSION.put("SESSIONID", SESSIONID)
        JSONObject loginData = new JSONObject();
        SESSION.put("uuid", UUID.toString());
        loginData.put("login-date", now());
        loginData.put("login-game-mode", player.getGameMode().toString());
        loginData.put("login-location", LocationDataHandler.packLocation(player.getLocation()));
        loginData.put("login-exhaustion",player.getExhaustion());
        loginData.put("login-xp-to-level",player.getExp());
        loginData.put("login-level",player.getLevel());
        loginData.put("login-food",player.getFoodLevel());
        loginData.put("login-health",player.getHealth());
        loginData.put("login-saturation",player.getSaturation());
        SESSION.put("login-data", loginData);
        SESSION.put("server", JawaCore.getServerName());
        SESSION.put("ip", player.getAddress().getAddress().toString().replace("/", ""));
        
        NAME = player.getName();
        
        ESHandler.asyncDataIndexWithIDReturn("sessions", SESSION);
        
        LOGGER.log(Level.INFO, "Initializing session for {0}", NAME);
    }
    
    /** Return the session ID. This is needed for consistent updates.
     * @param sessionID 
     */
    public void returnSessionID(String sessionID){
        this.sessionID = sessionID;
    }
    
    /** *  End a player's session.Commit all data to SESSION and async commit to the index.
     * @param player 
     */
    public void closeSession(Player player){
        JSONObject logoutData = new JSONObject();
        logoutData.put("logout-exhaustion",player.getExhaustion());
        logoutData.put("logout-xp-to-level",player.getExp());
        logoutData.put("logout-level",player.getLevel());
        logoutData.put("logout-food",player.getFoodLevel());
        logoutData.put("logout-game-mode",player.getGameMode());
        logoutData.put("logout-health",player.getHealth());
        logoutData.put("logout-saturation",player.getSaturation());
        logoutData.put("logout-date", now());
        logoutData.put("logout-location", LocationDataHandler.packLocation(player.getLocation()));
        SESSION.put("teleport-data", TELEPORTEVENTS);
        SESSION.put("death-data", DEATHEVENTS);
        SESSION.put("consumption-data", CONSUMED);
        SESSION.put("command-data", COMMANDS);
        SESSION.put("game-mode-data", GAMEMODE);
//        SESSION.put("TRACKS", TRACKS);
        
        //Async update
//        ESHandler.runAsyncSingleIndexRequest(ESRequestBuilder.createIndexRequest("sessions", sessionID, SESSION));
        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(new JSONObject().put("logout-data", logoutData), "sessions", sessionID, true));
        SessionTrackHandler.unregisterSession(UUID);
        LOGGER.log(Level.INFO, "Session for {0} has been closed sessionID:{1}", new Object[]{NAME, sessionID});
    }
    
    /** Log a player's teleport event.
     * @param fromLocation
     * @param toLocation
     * @param cause
     * @param canceled 
     */
    public void trackTP(Location fromLocation, Location toLocation, PlayerTeleportEvent.TeleportCause cause, boolean canceled){
        JSONObject teleportEvent = new JSONObject();
        teleportEvent.put("tp-date", now());
        teleportEvent.put("cause", cause.toString());
        teleportEvent.put("canceled", canceled);
        teleportEvent.put("from", LocationDataHandler.packLocation(fromLocation));
        teleportEvent.put("to", LocationDataHandler.packLocation(toLocation));
        TELEPORTEVENTS.put(teleportEvent);
//        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(new JSONObject().put("teleport-data", TELEPORTEVENTS), "sessions", sessionID, true));
        if (JawaCore.debug) LOGGER.log(Level.INFO, "Teleport event logged for {0} Cause:{1} From:{2} To:{3} Canceled:{4}", new Object[]{UUID.toString(), cause.toString(), fromLocation, toLocation, canceled});

    }
    
    /** Log a player's death statistics.
     * @param entityDeathEvent
     * @param damageEvent
     * @param location
     * @param killer 
     */
    public void trackDeath(EntityDeathEvent entityDeathEvent, EntityDamageEvent damageEvent, Location location, Player killer){
        JSONObject deathEvent = new JSONObject();
        if (killer != null) deathEvent.put("killer", killer.getUniqueId());
        deathEvent.put("death-date", now());
        deathEvent.put("cause", damageEvent.getCause().toString());
        deathEvent.put("location", LocationDataHandler.packLocation(location));
        deathEvent.put("final-damage", damageEvent.getFinalDamage());
        deathEvent.put("raw-damage", damageEvent.getDamage());
        deathEvent.put("exp-dropped", entityDeathEvent.getDroppedExp());
        deathEvent.put("inv-dropped", MaterialHandler.packInventory(entityDeathEvent.getDrops()));
        DEATHEVENTS.put(deathEvent);
//        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(new JSONObject().put("death-data", DEATHEVENTS), "sessions", sessionID, true));
        if (JawaCore.debug) LOGGER.log(Level.INFO, "Death event logged for {0} Cause:{1} ", new Object[]{UUID.toString(), damageEvent.getCause().toString()});
    }
    
    /** Log an item consumption.
     * @param itemStack 
     * @param location 
     */
    public void consumeItem(ItemStack itemStack, Location location){
        JSONObject consumption = new JSONObject();
        consumption.put("consume-date", now());
        consumption.put("item-stack", MaterialHandler.packItemStack(itemStack));
        consumption.put("location", LocationDataHandler.packLocation(location));
        CONSUMED.put(consumption);
//        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(new JSONObject().put("consumption-data", CONSUMED), "sessions", sessionID, true));
    }
    
    /** Log a command run
     * @param commands 
     * @param location 
     */
    public void runCommand(String commands, Location location){
        JSONObject command = new JSONObject();
        command.put("command-date", now());
        command.put("commands", commands);
        command.put("location", LocationDataHandler.packLocation(location));
        COMMANDS.put(command);
//        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(new JSONObject().put("command-data", COMMANDS), "sessions", sessionID, true));
    }

    /** Log a change to the player's gamemode
     * @param gamemode
     * @param location 
     */
    public void changeGameMode(String gamemode, Location location){
        JSONObject gameModeData = new JSONObject();
        gameModeData.put("date", now());
        gameModeData.put("game-mode", gamemode);
        gameModeData.put("location", LocationDataHandler.packLocation(location));
        GAMEMODE.put(gameModeData);
//        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(new JSONObject().put("game-mode-data", GAMEMODE), "sessions", sessionID, true));
    }
    
    
    /** Get the LocalDateTime of now formatted in ISO LOCAL DATE TIME format.
     * @return String of representation of the LocalDateTime in ISO_LOCAL_DATE_TIME format
     */
    private String now(){
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String getSessionID() {
        return sessionID;
    }
}
