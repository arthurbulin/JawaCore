/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners.sessions;

import net.jawasystems.jawacore.handlers.SessionTrackHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class TeleportEventListenerSession implements Listener{
    
    @EventHandler
    public static void onTeleportEvent(PlayerTeleportEvent event) {
        //Get cause
        SessionTrackHandler.trackTP(event.getPlayer(), event.getFrom(), event.getTo(), event.getCause(), event.isCancelled());
        
    }
    
}
