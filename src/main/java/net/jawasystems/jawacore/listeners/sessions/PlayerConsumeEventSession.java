/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners.sessions;

import net.jawasystems.jawacore.handlers.SessionTrackHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class PlayerConsumeEventSession implements Listener {
    
    @EventHandler
    public static void onPlayerConsumeEvent(PlayerItemConsumeEvent event){
        SessionTrackHandler.trackConsumption(event.getPlayer(), event.getItem());
    }
    
}
