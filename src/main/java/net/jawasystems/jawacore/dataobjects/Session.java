/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.dataobjects;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.handlers.LocationDataHandler;
import net.jawasystems.jawacore.handlers.MaterialHandler;
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
    
    private final String SESSIONID;
    private final UUID UUID;
    private final String LOGINDATETIME;
    private final JSONObject SESSION = new JSONObject();
//    private final JSONObject TRACKS = new JSONObject();
    
    private final JSONArray TELEPORTEVENTS = new JSONArray();
    private final JSONArray DEATHEVENTS = new JSONArray();
    private final JSONArray CONSUMED = new JSONArray();
    
    private final String NAME;

    
    /** Create a new session to track player activities.
     * @param player
     * @param event 
     */
    public Session(Player player){
        this.UUID = player.getUniqueId();
        LocalDateTime now = LocalDateTime.now();
        
        this.LOGINDATETIME = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        SESSIONID = this.UUID.toString().concat("-").concat(String.valueOf(now.toEpochSecond(ZoneOffset.UTC)));
        
        //SESSION.put("SESSIONID", SESSIONID);
        SESSION.put("UUID", UUID.toString());
        SESSION.put("LOGINDATETIME", LOGINDATETIME);
        SESSION.put("LOGINGAMEMODE", player.getGameMode().toString());
        SESSION.put("LOGINLOCATION", LocationDataHandler.packLocation(player.getLocation()));
        SESSION.put("SERVER", JawaCore.getServerName());
        SESSION.put("IP", player.getAddress().getAddress().toString());
        
        NAME = player.getName();
        
        LOGGER.log(Level.INFO, "Session initialized for {0} sessionID:{1}", new Object[]{NAME, SESSIONID});
    }
    
    /** End a player's session. Commit all data to SESSION and async commit to the index.
     * @param location 
     */
    public void closeSession(Location location){
        SESSION.put("LOGOUTDATETIME", now());
        
        SESSION.put("LOGOUTLOCATION", LocationDataHandler.packLocation(location));
        SESSION.put("TELEPORTEVENTS", TELEPORTEVENTS);
        SESSION.put("DEATHEVENTS", DEATHEVENTS);
        SESSION.put("CONSUMED", CONSUMED);
//        SESSION.put("TRACKS", TRACKS);
        
        //Async update
        ESHandler.runAsyncSingleIndexRequest(ESRequestBuilder.createIndexRequest("sessions", SESSIONID, SESSION));
        LOGGER.log(Level.INFO, "Session for {0} has been closed sessionID:{1}", new Object[]{NAME, SESSIONID});
    }
    
    /** Log a player's teleport event.
     * @param fromLocation
     * @param toLocation
     * @param cause
     * @param canceled 
     */
    public void trackTP(Location fromLocation, Location toLocation, PlayerTeleportEvent.TeleportCause cause, boolean canceled){
        JSONObject teleportEvent = new JSONObject();
        teleportEvent.put("TPDATETIME", now());
        teleportEvent.put("CAUSE", cause.toString());
        teleportEvent.put("CANCELED", canceled);
        teleportEvent.put("FROM", LocationDataHandler.packLocation(fromLocation));
        teleportEvent.put("TO", LocationDataHandler.packLocation(toLocation));
        TELEPORTEVENTS.put(teleportEvent);
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
        if (killer != null) deathEvent.put("KILLER", killer.getUniqueId());
        deathEvent.put("DEATHDATETIME", now());
        deathEvent.put("CAUSE", damageEvent.getCause().toString());
        deathEvent.put("LOCATION", LocationDataHandler.packLocation(location));
        deathEvent.put("FINALDAMAGE", damageEvent.getFinalDamage());
        deathEvent.put("RAWDAMAGE", damageEvent.getDamage());
        deathEvent.put("EXPDROPPED", entityDeathEvent.getDroppedExp());
        deathEvent.put("DROPPEDINV", MaterialHandler.packInventory(entityDeathEvent.getDrops()));
        DEATHEVENTS.put(deathEvent);
        if (JawaCore.debug) LOGGER.log(Level.INFO, "Death event logged for {0} Cause:{1} ", new Object[]{UUID.toString(), damageEvent.getCause().toString()});
    }
    
    /** Log an item consumption.
     * @param itemStack 
     */
    public void consumeItem(ItemStack itemStack){
        JSONObject consumption = new JSONObject();
        consumption.put("CONSUMEDATETIME", now());
        consumption.put("ITEMSTACK", MaterialHandler.packItemStack(itemStack));
        CONSUMED.put(consumption);
    }
    
    
    /** Get the LocalDateTime of now formatted in ISO LOCAL DATE TIME format.
     * @return String of representation of the LocalDateTime in ISO_LOCAL_DATE_TIME format
     */
    private String now(){
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
