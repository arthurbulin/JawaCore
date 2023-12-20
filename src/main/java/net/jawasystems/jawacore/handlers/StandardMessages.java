/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.handlers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 *
 * @author alexander
 */
public class StandardMessages {
    
    //Will need to refactor and rename the enum when it is added to JawaChat evenetually
    public enum Message {
        PLAYERNOTFOUND,
        PERMISSIONMESSAGE
    }
    
    
    protected static String player_not_found;
    protected static String permission_msg;
    
    /** Get the String message associated with "message". Returns null if "message" is
     * not in the enum.
     * @param message
     * @return 
     */
    public static String getMessage(Message message){
        
        switch (message) {
            case PLAYERNOTFOUND:{
                return player_not_found;
            }
            case PERMISSIONMESSAGE:{
                return permission_msg;
            }
            default: {
                return null;
            }
        }
        
    }
    
    /** Send the enum msg to the specified player.
     * @param player
     * @param msg 
     */
    public static void sendMessage(Player player, Message msg){
        player.sendMessage(getMessage(msg));
    }
    
    /** Send the enum msg to the specified commandSender.
     * @param commandSender
     * @param msg 
     */
    public static void sendMesage(CommandSender commandSender, Message msg){
        commandSender.sendMessage(getMessage(msg));
    }
    
    /** Load configured messages
     * @param config 
     */
    public static void loadMessages(ConfigurationSection config){
        permission_msg = ChatColor.translateAlternateColorCodes('&', config.getString("permission-message","&c > You do not have permission to perform this command. If you believe this is in error please contact your server administrator"));
        player_not_found = ChatColor.translateAlternateColorCodes('&', config.getString("player-not-found","&c > Error: That player wasn't found! Try their actual minecraft name instead of nickname"));
    }
    
    
}
