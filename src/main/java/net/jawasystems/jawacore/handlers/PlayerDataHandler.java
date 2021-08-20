/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package net.jawasystems.jawacore.handlers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Arthur Bulin
 */
public class PlayerDataHandler {
    
//    private static final Logger LOGGER = Logger.getLogger("PlayerDataHandler");
    
    /** Assemble a JSONObject that contains information for that specific ban and returns it in a top level ban object.
     * @param reason
     * @param adminUUID
     * @param console
     * @param banLength
     * @param banTime
     * @return
     */
    public static JSONObject assembleBanData(String reason, UUID adminUUID, boolean console, LocalDateTime banLength){
        //JSONObject topLevelBanObject = new JSONObject();
        JSONObject banData = new JSONObject();
        //String endOfBanDate = assessBanTime(parsedArguments, banTime);
        
        banData.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        banData.put("reason", reason);
       
        banData.put("banned-by", adminUUID.toString());
        banData.put("via-console", console);     
        
        banData.put("banned-until", banLength.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        //banData.put("latest-ban", banTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        banData.put("active", true);
        banData.put("ban-lock", false);
        
        //topLevelBanObject.put(banTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), banData);
        return banData;
    }
    
    
    public static void assembleUnBanData(String unreason, UUID adminUUID, boolean console, JSONObject banObject){
        banObject.put("unreason", unreason);
        banObject.put("active", false);
        banObject.put("unbanned-by", adminUUID.toString());
        banObject.put("unbanned-on", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        banObject.put("unbanned-via-console", console);
        
    }
    
    public static void assembleExpiredUnBanData(JSONObject latestBan){
        //JSONObject banData = new JSONObject();
        latestBan.put("unreason", "This ban has expired and the user is being unbanned by JawaCore.");
        latestBan.put("active", false);
        latestBan.put("unbanned-by", "00000000-0000-0000-0000-000000000000");
        latestBan.put("unbanned-on", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        //return banData;
    }
    
    /** Evaluates ban time and creates a string representing it in LocalDateTime format.
     * If ban has no end date then string is "forever".
     * @param time
     * @param dateTime
     * @return 
     */
    public static String assessBanTime(HashMap<String,String> time, LocalDateTime dateTime) {
        LocalDateTime adjustedDateTime = dateTime;
        if (time.containsKey("d") || time.containsKey("h") || time.containsKey("m")) {
            if (time.containsKey("d")) {
                adjustedDateTime = adjustedDateTime.plusDays(Long.valueOf(time.get("d")));
            }
            if (time.containsKey("h")) {
                adjustedDateTime = adjustedDateTime.plusHours(Long.valueOf(time.get("h")));
            }
            if (time.containsKey("m")) {
                adjustedDateTime = adjustedDateTime.plusMinutes(Long.valueOf(time.get("m")));
            }
            return adjustedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return LocalDateTime.of(3000, 1, 1, 0, 0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }  
    }
    
    /** *  Determine if an ip address is contained within the player's history set.If it isn't then add it and return it.
     * If it is already in the array then return null.
     * @param ipAddress
     * @param ipData
     * @return 
     */
    public static JSONArray ipData(String ipAddress, JSONArray ipData){
        if (!ipData.toList().contains(ipAddress)) {
            ipData.put(ipAddress);
            return ipData;
        } else {
            return null;
        }
    }
    
    /** Determine if a user's name is already saved in their historical name data.
     * If it isn't add it and return the JSONArray else it will return null. Should
     * be executed after checking if saved name does not equal new name.
     * @param name
     * @param nameData
     * @return 
     */
    public static JSONArray nameData(String name, JSONArray nameData){
        //System.out.println("namdData method:" + nameData);
        if (!nameData.toList().contains(name)){
            nameData.put(name);
            //System.out.println("nameData method:" + nameData);
            return nameData;
        } else return null;
    }

    /** *  Determine if a user's nick is already saved in their historical nick data.
     * If it isn't add it and return the JSONArray else it will return null.Should
     * be executed after checking if saved nick does not equal new nick.
     * @param nick
     * @param nickData
     * @return 
     */
    public static JSONArray nickData(String nick, JSONArray nickData) {
        if (!nickData.toList().contains(nick)) {
            nickData.put(nick);
            return nickData;
        } else return null;
    }
    
    public static JSONObject starData(JSONObject starData, String value) {
        switch (value) {
            case "new": {
                starData.put("promote", false);
                starData.put("probation", false);
                starData.put("consult", false);
                break;
            }
            case "promote": {
                starData.put("promote", !Boolean.valueOf(String.valueOf(starData.get("promote"))));
                break;
            }
            case "probation": {
                starData.put("probation", !Boolean.valueOf(String.valueOf(starData.get("probation"))));
                break;
            }
            case "consult": {
                starData.put("consult", !Boolean.valueOf(String.valueOf(starData.get("consult"))));
                break;
            }
            
        }

        return starData;

    }
    
    public static JSONObject createPlayerRankChangeData(String fromRank, String toRank, String byWhom){
        //JSONObject playerData = new JSONObject();
        JSONObject rankData = new JSONObject();
        //JSONObject rankDataTop = new JSONObject();
        
        rankData.put("from-rank", fromRank);
        rankData.put("to-rank", toRank);
        rankData.put("changed-by", byWhom);
        rankData.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        //rankDataTop.put(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), rankData);
        
        //playerData.put("rank", toRank);
        //playerData.put("rank-data", rankDataTop);
        
        return rankData;
    }
    
    /** Generate small JSONObject for updating a player's tag.
     * @param tag
     * @return 
     */
    public static JSONObject createPlayerTagChangeData(String tag){
        JSONObject tagChange = new JSONObject();
        
        tagChange.put("tag", tag);
        
        return tagChange;
    }
    
    
    /** Resolve a player name to a PlayerDataObject. This will return a data filled
     * PlayerDataObject if the player is found. This first checks is a player is online.
     * If they are then the UUID is extracted and the PlayerDataObject is populated with
     * data from a UUID index search. If the player is not online this calls ESHandler.findOfflinePlayer(target, true)
     * so that a data filled PlayerDataObject is returned. If no exact match is found to the player name field
     * this will return null to specify that a player of that name was not found. This sends
     * a unified error message to the commandSender that informs them the user was not found.
     * @param commandSender
     * @param target
     * @return
     * @throws IOException 
     */
    public static PlayerDataObject validatePlayer(CommandSender commandSender, String target) throws IOException{
        Player onlinePlayer = JawaCore.getPlugin().getServer().getPlayer(target);
        PlayerDataObject pdObject = null;
        if (onlinePlayer == null){ //If not online
            pdObject = ESHandler.findOfflinePlayer(target, true);
        } else { //If online. This should never be null then because the player has already been found
            pdObject = ESHandler.getPlayerData(target);
        }
        
        if (pdObject == null){
            commandSender.sendMessage(ChatColor.RED + " > Error: " + target + " was not resolvable as an online or offline player. Please be sure the name is exact and retry your command.");
            return null;
        } else {    
            return pdObject;
        }
    }
    
    
    
}
