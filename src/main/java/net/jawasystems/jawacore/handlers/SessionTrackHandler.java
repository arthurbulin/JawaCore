/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.dataobjects.Session;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class SessionTrackHandler {
    
    private static final Logger LOGGER = Logger.getLogger("JawaCore][SessionTrackHandler");
    
    private static final HashMap<UUID, Session> SESSIONS = new HashMap();
    
    
    private static Session getSession(Player player){
        return SESSIONS.get(player.getUniqueId());
    }
    
    private static Session getSession(UUID uuid){
        return SESSIONS.get(uuid);
    }
    
    /** Logs a teleport event in the player's session.
     * @param player
     * @param fromLoc
     * @param toLoc
     * @param cause
     * @param cancel 
     */
    public static void trackTP(Player player, Location fromLoc, Location toLoc, TeleportCause cause, boolean cancel) {
        getSession(player).trackTP(fromLoc, toLoc, cause, cancel);
    }
    
    /** Close a player's session. The session itself will async commit data to
     * elasticsearch and remove itself from the sessions map.
     * @param player 
     */
    public static void closeSession(Player player){
        getSession(player).closeSession(player);
    }
    
    /** Close all tracking sessions for all players.
     */
    public static void closeAllSessions() {
        Bukkit.getServer().getOnlinePlayers().forEach((player) ->{
            closeSession(player);
        });
    }
    
    /** Remove the session from the list of sessions.
     * @param uuid  UUID of the player who's session is being removed
     */
    public static void unregisterSession(UUID uuid){
        SESSIONS.remove(uuid);
    }
        
    
    /** Initiate a session track for the specified player.
     * @param player 
     */
    public static void openSession(Player player){
        SESSIONS.put(player.getUniqueId(), new Session(player));
    }

    /** Log a player's death
     * @param player the player who triggered the death event
     * @param deathEvent the Death event
     * @param damageEvent the damage event leading to the Death event
     * @param location the location of the player at the time of death
     * @param killer the killer of the player
     */
    public static void trackDeath(Player player, EntityDeathEvent deathEvent, EntityDamageEvent damageEvent, Location location, Player killer){
        getSession(player).trackDeath(deathEvent, damageEvent, location, killer);
    }
    
    /** Track the consumption of a an item by a player
     * @param player the player in question
     * @param itemStack the item stack consume by the player
     */
    public static void trackConsumption(Player player, ItemStack itemStack){
        getSession(player).consumeItem(itemStack, player.getLocation());
    }
    
    /** Track any command sent by the player to the server.
     * @param player the player in question
     * @param commands a collection of the player sent top level command
     */
    public static void trackCommands(Player player, String commands){
        getSession(player).runCommand(commands, player.getLocation());
    }
    
    public static void trackGameMode(Player player, String gamemode){
        getSession(player).changeGameMode(gamemode, player.getLocation());
    }
    
    public static void trackArmorStandManipulate(Player player, ItemStack itemStack){
        
    }
    
    /** Get the session ID of the player
     * @param player
     * @return 
     */
    public static String getSessionID(Player player){
        if (JawaCore.trackSessions())
            return getSession(player).getSessionID();
        else 
            return null;
    }
    
    public static String getSessionID(UUID uuid){
        return getSession(uuid).getSessionID();
    }
    
    public static void registerSessionID(String id, String uuid){
        getSession(UUID.fromString(uuid)).returnSessionID(id);
    }

}
