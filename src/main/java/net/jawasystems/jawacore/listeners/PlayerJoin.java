/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners;

import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 *
 * @author alexander
 */
public class PlayerJoin implements Listener {

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        
        //Should async update the player's data or install them
        Bukkit.getScheduler().runTaskAsynchronously(JawaCore.plugin, new Runnable() {
            @Override
            public void run() {
                //Create a new blank PlayerDataObject
                PlayerDataObject target = new PlayerDataObject(player.getUniqueId());

                //pdObject.onJoinUpdate();
                
            }
        });
    }

}
