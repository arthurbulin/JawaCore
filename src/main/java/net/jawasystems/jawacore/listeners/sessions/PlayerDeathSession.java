/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners.sessions;

import net.jawasystems.jawacore.handlers.SessionTrackHandler;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class PlayerDeathSession implements Listener {

    @EventHandler
    public static void onPlayerDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() == EntityType.PLAYER) {
            EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
            Player killer = event.getEntity().getKiller();
            Location deathLocation = event.getEntity().getLocation();
            
            SessionTrackHandler.trackDeath((Player) event.getEntity(), event, damageEvent, deathLocation, killer);
            

        }
    }

}
