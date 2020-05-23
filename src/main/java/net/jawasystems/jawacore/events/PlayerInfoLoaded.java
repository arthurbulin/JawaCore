/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.events;

import java.util.UUID;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Arthur Bulin
 */
public class PlayerInfoLoaded extends Event{
    private static final HandlerList handlers = new HandlerList();
    private static UUID uuid;
    private static PlayerDataObject playerData;
    
    
    public PlayerInfoLoaded(UUID who, PlayerDataObject data) {
        super(true); //Makes the event async
        PlayerInfoLoaded.uuid = who;
        PlayerInfoLoaded.playerData = data;
        System.out.println(playerData.getName() + " has been loaded");
    }
    
    public Player getPlayer() {
        return playerData.getPlayer();
    }
    
    public UUID getUniqueID(){
        return uuid;
    }
    
    public PlayerDataObject getPlayerDataObject(){
        return playerData;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }   
}
