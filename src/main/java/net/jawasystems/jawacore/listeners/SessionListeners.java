/*
 * The MIT License
 *
 * Copyright 2022 Arthur Bulin.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.jawasystems.jawacore.listeners;

import java.util.logging.Level;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.handlers.SessionTrackHandler;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 *
 * @author Arthur Bulin
 */
public class SessionListeners implements Listener {

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        SessionTrackHandler.closeSession(event.getPlayer());
    }

    @EventHandler
    public static void onTeleportEvent(PlayerTeleportEvent event) {
        //Get cause
        SessionTrackHandler.trackTP(event.getPlayer(), event.getFrom(), event.getTo(), event.getCause(), event.isCancelled());
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        SessionTrackHandler.openSession(event.getPlayer());
    }

    @EventHandler
    public static void onPlayerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
        SessionTrackHandler.trackGameMode(event.getPlayer(), event.getNewGameMode().toString());
    }

    @EventHandler
    public static void onPlayerDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() == EntityType.PLAYER) {
            EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
            Player killer = event.getEntity().getKiller();
            Location deathLocation = event.getEntity().getLocation();
            SessionTrackHandler.trackDeath((Player) event.getEntity(), event, damageEvent, deathLocation, killer);
        }
    }

    @EventHandler
    public static void onPlayerConsumeEvent(PlayerItemConsumeEvent event) {
        SessionTrackHandler.trackConsumption(event.getPlayer(), event.getItem());
    }

    @EventHandler
    public static void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (JawaCore.debug) {
//            PlayerCommandPreprocessSession.LOGGER.log(Level.INFO, event.getPlayer().getName() + " used command " + event.getMessage());
        }
        SessionTrackHandler.trackCommands(event.getPlayer(), event.getMessage());
    }
    
//        @EventHandler
//        public static void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event){
//            event.getRightClicked().
//        }
    
//            @EventHandler
//        public static void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event){
//            event.getRightClicked().
//        }
//    
    
}
