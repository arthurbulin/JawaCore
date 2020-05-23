/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.events;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Arthur Bulin
 */
public class PlayerRankChange extends Event{
    private static final HandlerList handlers = new HandlerList();
    static UUID uuid;
    private static String rank;
    
    public PlayerRankChange(UUID who, String newRank) {
        PlayerRankChange.uuid = who;
        PlayerRankChange.rank = newRank;
        //PlayerRankChange.pdObject = pdObject;
        //System.out.println("PlayerRankChangeCalled for " + who.getDisplayName() + " with rank " + newRank);
    }
    
//    public Player getPlayer() {
//        return player;
//    }
    
    public String getRank() {
    //    return pdObject.getRank();
        return rank;
    }
    
    public UUID getUUID(){
        return uuid;
    }
        
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    } 
}
