/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners;

import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author alexander
 */
public class PlayerQuitCore implements Listener {

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(JawaCore.plugin, new Runnable() {
            @Override
            public void run() {
                PlayerManager.getPlayerDataObject(event.getPlayer().getUniqueId()).onQuitUpdate();
            }
        });
    }

}
