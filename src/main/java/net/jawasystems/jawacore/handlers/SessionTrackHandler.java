/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.handlers;

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
        getSession(player).closeSession(player.getLocation());
    }
    
    public static void closeAllSessions() {
        Bukkit.getServer().getOnlinePlayers().forEach((player) ->{
            closeSession(player);
        });
    }
    
    public static void openSession(Player player){
        SESSIONS.put(player.getUniqueId(), new Session(player));
    }
    
    public static void trackDeath(Player player, EntityDeathEvent deathEvent, EntityDamageEvent damageEvent, Location location, Player killer){
        getSession(player).trackDeath(deathEvent, damageEvent, location, killer);
    }
    
    public static void trackConsumption(Player player, ItemStack itemStack){
        getSession(player).consumeItem(itemStack);
    }
    
    public static String getSessionID(Player player){
        if (JawaCore.trackSessions())
            return getSession(player).getSessionID();
        else 
            return null;
    }
    
    public static String getSessionID(UUID uuid){
        return getSession(uuid).getSessionID();
    }
    
}
