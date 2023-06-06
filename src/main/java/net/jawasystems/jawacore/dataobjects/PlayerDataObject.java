/*
 * Copyright (C) 2019 alexander
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.jawasystems.jawacore.dataobjects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.PlayerManager;
import net.jawasystems.jawacore.events.PlayerInfoLoaded;
import net.jawasystems.jawacore.events.PlayerRankChange;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.handlers.LocationDataHandler;
import net.jawasystems.jawacore.handlers.PlayerDataHandler;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alexander
 */
public class PlayerDataObject {

    private static final Logger LOGGER = Logger.getLogger("PlayerDataObject");
//    private final HashMap<String,HomeObject> HOMELIST = new HashMap();
    private final UUID PLAYER;
    private Map<String, JSONObject> banData;
    private JSONObject playerData;
    private Map<String, JSONObject> homeData;
    private Map<String, String> homeDataNames;

    private UUID privateConversation;

    private ChatColor rankColor;
    
    private final HashMap<String, JSONObject> ZONEPERMISSIONS = new HashMap();

    /** Build a player data object and add all data. This is for an existing player.
     * @param player The UUID for the player
     * @param playerData The player data for this should not be empty or null
     * @param banData The ban data for this player. This can be null or an empty map for a player with no bans
     * @param homeData The home data for this player. This can be null or an empty map for a player with no bans
     */
    public PlayerDataObject(UUID player, Map<String, Object> playerData, Map<String, JSONObject> banData, Map<String, JSONObject> homeData) {
        //Set the player's UUID
        this.PLAYER = player;
        
        //Install the player's data
        this.playerData = new JSONObject(playerData);

        
        //Install the player's ban data
        if (banData == null || banData.isEmpty()){
            this.banData = new HashMap();
        } else {
            this.banData =  banData;
        }
        
        //Install the player's home data
        if (homeData == null || homeData.isEmpty()){
            this.homeData = new HashMap();
            this.homeDataNames = new HashMap();
        } else {
            this.homeData = homeData;
            this.homeDataNames = new HashMap();
            for (String homeID : homeData.keySet()){
                this.homeDataNames.put(homeData.get(homeID).getString("name"), homeID);
            }
        }
        privateConversation = null;
        //buildHomeList();
    }
    
    /** Build a player data object for a new player. This will install the player into 
     * the elasticsearch database as well.
     * @param player
     * @param playerName
     * @param ipAddr 
     */
    public PlayerDataObject (UUID player, String playerName, String ipAddr) {
        this.PLAYER = player;
        this.playerData = new JSONObject();
        this.banData = new HashMap();
        this.homeData = new HashMap();
        this.homeDataNames = new HashMap();
        installPlayer(playerName, ipAddr);
    }


    /** Add player data to the object. If the item is null it will not be added.
     * @param banData The ban data for this player. This can be null or an empty map for a player with no bans
     * @param homeData The home data for this player. This can be null or an empty map for a player with no bans
     */
    public void addPlayerData(Map<String,JSONObject> banData,Map<String,JSONObject> homeData) {

        //Install the player's ban data
        if (banData != null && !banData.isEmpty()){
            this.banData = banData;
        }
        
        //Install the player's home data
        if (homeData != null && !homeData.isEmpty()){
            this.homeData = homeData;
            for (String homeID : homeData.keySet()){
                this.homeDataNames.put(homeData.get(homeID).getString("name"), homeID);
            }
        }
         
    }
    
    /** Add the server specific JSONobject to the home-data
     */
//    private void addServerHomeObject(){
////        this.homeData.isEmpty()
//        playerData.getJSONObject("home-data").put(JawaCore.getServerName().toLowerCase(), new JSONObject());
//    }
    
//    public void addHomeData(Map homeData) {
//        //this.homeData = new JSONObject(homeData);
//    }

    /** Returns the player's UUID in string form.
     * @return 
     */
    public String getPlayerUUID() {
        return PLAYER.toString();
    }

//    /** Return the entire player data JSONObject.
//     * @return All of the player Data in JSONObject form.
//     */
//    public JSONObject getPlayerData() {
//        return playerData;
//    }

    /** Returns true if the player has any ban data. This may be current or historical bans
     * across any server.
     * @return True if there are any ban records. False if there are none.
     */
    public boolean containsBanData() {
//        return !playerData.getJSONArray("ban-data").isEmpty();
        return !banData.isEmpty();
    }

//    public boolean containsPlayerData() {
//        return !playerData.isEmpty();
//    }

    /** Returns true if the player's data contains home info for the server.
     * @return True if the playerdata contains home data for the server
     */
    public boolean containsHomeData() {
        return !homeData.isEmpty();
//        return !playerData.getJSONObject("home-data").isEmpty() || playerData.getJSONObject("home-data").has(JawaCore.getServerName().toLowerCase());
    }

    /** Is this user online.
     * @return True if the user is online on this server. False otherwise.
     */
    public boolean isOnline() {
        if (Bukkit.getServer().getPlayer(PLAYER) != null) {
            return true;
        } else {
            return false;
        }
    }

    /** Return the player object.
     * @return the player object, null if the player is not online
     */
    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(PLAYER);
    }

    //##########################################################################
    //#   Player ban gets
    //##########################################################################
//    private void buildBanSort(){
//        LocalDateTime latest = 
//        for (Object banEntry : playerData.getJSONArray("ban-data")){
//            ((JSONObject) banEntry).getString("date");
//        }
//    }
    
    /** Checks if the id given exists within the ban data
     * @param id the ban id
     * @return true if present within the ban data, false otherwise
     */
    public boolean isBanIDValid(String id){
        return banData.containsKey(id);
    }
    /** Returns a string representation of the player's latest ban datetime. This is a string representation
     * of a LocalDateTime with ISO_LOCAL_DATE_TIME formatting. Will return null if there are no ban entries or
     * the date entry is malformed
     * @return 
     */
    public String getLatestActiveBanDate() {
        JSONObject latestBan = getLatestActiveBanEntry();
        if (latestBan == null) return null;
        else return latestBan.getString("date");
    }
    
    /** Returns a JSONObject with the latest date key from the player's ban data array.
     * @return JSONObject with all ban data for the most recent ban.
     */
    public JSONObject getLatestActiveBanEntry() {
        if (banData.isEmpty()) return null;
        
        LocalDateTime latestDate = LocalDateTime.MIN;
        JSONObject latestEntry = null;
        for (JSONObject banEntry : banData.values()){
            if (!banEntry.getBoolean("active")) continue;
            String datetime = banEntry.getString("date");
            LocalDateTime test = LocalDateTime.parse(datetime);
            if (test.isAfter(latestDate)) {
              latestDate = test;
              latestEntry = banEntry;
            }
        }
        return latestEntry;
    }
    
    /** Returns the id of the player's latest ban
     * @return string id of the player's latest ban
     */
    public String getLatestActiveBanID() {
//        LOGGER.log(Level.INFO, "empty:" + banData.isEmpty());
        if (banData.isEmpty()) return null;
        
        LocalDateTime latestDate = LocalDateTime.MIN;
        String latestID = null;
        for (String banID : banData.keySet()){
            if (!banData.get(banID).getBoolean("active")) continue;
            String datetime = banData.get(banID).getString("date");
            LocalDateTime test = LocalDateTime.parse(datetime);
            if (test.isAfter(latestDate)) {
              latestDate = test;
              latestID = banID;
            }
        }
        return latestID;
    }
    

    /** Returns a ban entry corresponding to the input date. If one is not found this returns null.
     * @param banDateTime String formatted localdatetime using ISO_LOCAL_DATE_TIME.
     * @return The JSONObject representing the datetime of the ban
     */
    public JSONObject getBanEntry(String banDateTime) {
        JSONObject banEntry = null;
        for (JSONObject entry : banData.values()) {
            if (entry.getString("date").equals(banDateTime)){
                banEntry = entry;
            }
        }
        return banEntry;
        //return banData.getJSONObject(banDateTime);
    }
    
    /** Returns the JSONObject ban object requested
     * @param id
     * @return 
     */
    public JSONObject getBanEntryByID(String id) {
        return banData.get(id);
    }

    /** Returns the reason for a ban on the specified LocalDateTime
     * @param banDateTime The string representation of LocalDateTime ban identifier formatted with ISO_LOCAL_DATE_TIME.
     * @return 
     */
    public String getBanReason(String banDateTime) {
        return getBanEntry(banDateTime).getString("reason");
    }

    /** Return the string form of the UUID that banned the user. This should never be null. If 
     * the person was banned by the system then the uuid returned is 00000000-0000-0000-0000-000000000000.
     * @param banDateTime String form of the LocalDateTime that the user was banned in the ISO_LOCAL_DATE_TIME format.
     * @return 
     */
    public String getBannedBy(String banDateTime) {
        return getBanEntry(banDateTime).getString("banned-by");
    }

    /** Return a string form of the "banned-until" this should be a LocalDateTime
     * object in string format.
     * @param banDateTime
     * @return 
     */
    public String getBannedUntil(String banDateTime) {
        return getBanEntry(banDateTime).getString("banned-until");
    }
    
    /** Returns the LocalDateTime in string form with ISO_LOCAL_DATE_TIME formatting for the 
     * latest ban entry.
     * @return 
     */
    public String isBannedUntil(){
        return getLatestActiveBanEntry().getString("banned-until");
//        return getBannedUntil(getLatestActiveBanDate());
    }

    /** Returns the string of the unban reason for the specified ban
     * @param banDateTime The String representing the ban date time
     * @return the reason for unban in string form
     */
    public String getBannedUnreason(String banDateTime) {
        return getBanEntry(banDateTime).getString("unreason");
    }

    /** Returns the String version of the UUID of the staff who unbanned the player. This will be 00000000-0000-0000-0000-000000000000
     * for a system user (i.e. ban expiration)
     * @param banDateTime String version of the ban LocalDateTime in the ISO_LOCAL_DATE_TIME format
     * @return String representation of the UUID of the staff who unbanned the player
     */
    public String getBannedUnBy(String banDateTime) {
        return getBanEntry(banDateTime).getString("unbanned-by");
    }

    /** Returns the String form of the LocalDatetTime of when the ban was reversed.
     * @param banDateTime The String representation of the ban date time in the ISO_LOCAL_DATE_TIME format
     * @return the String version of the LocalDateTime when the user was unbanned
     */
    public String unbannedOn(String banDateTime) {
        return getBanEntry(banDateTime).getString("unbanned-on");
    }
    
    /** Returns true if the user is ban locked by a server owner.
     * @return True if the user cannot be unbanned until the ban lock is lifted. False if not banlocked
     */
    public boolean isBanLocked(){
        for (JSONObject ban : banData.values()) {
            if (ban.getBoolean("active") && ban.getBoolean("ban-lock")) {
                return true;
            }
        }
        return false;
//        if (getLatestActiveBanEntry().has("ban-lock")){
//            return getLatestActiveBanEntry().getBoolean("ban-lock");
//        } else return false;
    }
    
    /** Get the UUID of the owner who has ban locked the user
     * @return String of the UUID of the owner who ban locked the user. null if the user is not ban locked
     */
    public String getBanLockAdmin(){
        for (JSONObject ban : banData.values()) {
            if (ban.getBoolean("active") && ban.getBoolean("ban-lock")) {
                return ban.getString("ban-lock-by");
            }
        }
        return null;
        //UUID adminUUID = UUID.fromString(getLatestActiveBanEntry().getString("ban-lock-by"));
//        return PlayerManager.getPlayerDataObject(adminUUID).getFriendlyName();
    }
    
    /** Get the reason a user was ban locked by an owner.
     * @return The string reason for the ban lock reason. Null if the user is not ban-locked or there is no reason.
     */
    public String getBanLockReason(){
        for (JSONObject ban : banData.values()) {
            if (ban.getBoolean("active") && ban.getBoolean("ban-lock")) {
                return ban.getString("ban-lock-reason");
            }
        }
        return null;
    }
    /** Is a particular ban active.
     * @param banDateTime
     * @return 
     */
    public boolean getBanState(String banDateTime) {
        return getBanEntry(banDateTime).getBoolean("active");
    }

    /** Was the player banned via console.
     * @param banDateTime - The string of the ban DateTime
     * @return 
     */
    public boolean isConsoleBan(String banDateTime) {
        return getBanEntry(banDateTime).getBoolean("via-console");
    }

    /** Was the player unbanned via console.
     * @param banDateTime - The string of the ban DateTime
     * @return 
     */
    public boolean isConsoleUnban(String banDateTime) {
        return getBanEntry(banDateTime).getBoolean("unbanned-via-console");
    }

    /** Returns a JSONArray with the dates of all bans as strings.
     * @return 
     */
    public HashSet getListOfBans() {
        HashSet tmp = new HashSet();
        for (JSONObject entry : banData.values()){
            tmp.add(entry.getString("date"));
        }
        //return banData.keySet();
        return tmp;
    }
    
    /** Return a new Map of the ban entries. This is a copy and may be mutated as needed
     * @return 
     */
    public Map<String,JSONObject> getBanData(){
        return new HashMap(banData);
    }

    /** Bans a player.
     * @param reason
     * @param adminUUID
     * @param console
     * @param banLength
     */
    public void banPlayer(String reason, UUID adminUUID, boolean console, LocalDateTime banLength) {
        JSONObject newBan = PlayerDataHandler.assembleBanData(reason, adminUUID, PLAYER, console, banLength);
        ESHandler.asyncDataIndexWithIDReturn("bans", newBan);
    }
    
    /** Accepts a newly index ban id and JSONObject and integrates it into the list.
     * @param id
     * @param newBan 
     */
    public void returnBanID(String id, JSONObject newBan){
        banData.put(id, newBan);
    }
    
    /** Unbans a player.
     * @param unreason
     * @param adminUUID
     * @param console
     */
    public void unbanPlayer(String unreason, UUID adminUUID, boolean console){
        String id = "";
        for (String banID : banData.keySet()){
            if (banData.get(banID).getBoolean("active")){
                //Update by pass not copy?? 
                PlayerDataHandler.assembleUnBanData(unreason, adminUUID, console, banData.get(banID));
                id = banID;
//                banData.put(id, unBanData);
                break;
            }
        }
        
        if (!id.equals("")) updateBanDataAsync(id);
        //Does nothing if called and there's no ban to undo
    }

    /** Given a string, gets the id'd ban and updates it.
     * @param id The id of the ban
     * @param reason - The string reason that updates the ban.
     * @param updater
     */
    public void updateBan(String id, String reason, String updater){
        
        JSONObject updateData = new JSONObject();
        updateData.put("update-by", updater);
        updateData.put("update-date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        updateData.put("original-reason", banData.get(id).get("reason"));
        updateData.put("new-reason", reason);
        
        banData.get(id).put("reason", reason);
        if (!banData.get(id).has("update-data")) {
            banData.get(id).put("update-date", new JSONArray());
        } 
        
        //See if this is the first time the ban data is being updated
        if (banData.get(id).has("update-data")) {
            banData.get(id).getJSONArray("update-data").put(updateData);
        } else {
            banData.get(id).put("update-data", new JSONArray());
            banData.get(id).getJSONArray("update-data").put(updateData);
        }

        updateBanDataAsync(id);
    }
    
    /** trigger the needed events when a ban expires
     * @param id
     */
    public void banExpired(String id) {
        PlayerDataHandler.assembleExpiredUnBanData(banData.get(id));
        updateBanDataAsync(id);
        scheduleMessage(ChatColor.GREEN + "> " + ChatColor.YELLOW + "Your current ban has expired. Please remember to behave yourself.", 40);
        //  FIXME Need to queue up notifications for staff
//        Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + " > !!!! " + getFriendlyName() + ChatColor.YELLOW + " !!!! has been auto unbanned due to ban expiration.");
        Bukkit.broadcast(ChatColor.YELLOW + "> !!!! " + getFriendlyName() + ChatColor.YELLOW + " !!!! has been auto unbanned due to ban expiration.", Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    //##########################################################################
    //#   Player data gets
    //##########################################################################
    /** Returns the player rank
     * @return the player's rank
     */
    public String getRank() {
        return playerData.getString("rank");
    }
    
    /** If the player has existing rank change data it is returned. If the user
     * has no existing rank change data then null is returned.
     * @return 
     */
    public JSONArray getRankData() {
        if (playerData.has("rank-data")) {
            return playerData.getJSONArray("rank-data");
        } else {
            return null;
        }
    }

    /** Returns the player UUID.
     * @return 
     */
    public UUID getUniqueID() {
        return PLAYER;
    }

    /** Changes the player's rank and records the UUID of admin, time, new, and old ranks.
     * Then notifies the player that their rank has been changed.
     * @param newRank
     * @param adminUUID
     * @param rankColor 
     */
    public void setRank(String newRank, UUID adminUUID, ChatColor rankColor) {
        newRank = newRank.toLowerCase();
        playerData.getJSONArray("rank-data").put(PlayerDataHandler.createPlayerRankChangeData(getRank(), newRank, adminUUID.toString()));
        
        playerData.put("rank", newRank);
        setRankColor(rankColor);
        Bukkit.getServer().getPluginManager().callEvent(new PlayerRankChange(PLAYER, newRank));
        //sendMessageIf(ChatColor.GREEN + " > Your rank has been changed to " + newRank);
        updatePlayerDataAsync();
    }

    /** Returns the player's minecraft name
     * @return 
     */
    public String getName() {
        return playerData.getString("name");
    }

    /** Sets the player's minecraft name. This should only be done on minecraft account
     * name changes. NOT NickNames!!
     * @param name 
     */
    private void setName(String name) {
        playerData.put("name", name);
    }

    /** Updates the player's minecraft account name in the record.
     * @param name 
     */ //TODO Add minecraft account name tracking
    private void updateName(String name) {
        if (!getName().equals(name)) {
            setName(name);
        }
    }

    public int getPlayTime() {
        return playerData.getInt("play-time");
    }

    public LocalDateTime getLastLogin() {
        return LocalDateTime.parse(playerData.getString("last-login"));
    }

    public void setLastLogin() {
        playerData.put("last-login", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        LOGGER.log(Level.FINEST, "Recording last login date for player: {0} as {1}", new Object[]{PLAYER.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)});
    }

    public LocalDateTime getLastLogout() {
        return LocalDateTime.parse(playerData.getString("last-logout"));
    }

    public void setLastLogout() {
        playerData.put("last-logout", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        LOGGER.log(Level.FINEST, "Recording last logout date for player: {0} as {1}", new Object[]{PLAYER.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)});
    }


    /** Determine if an ip-data update is required
     * @param ip the ip address to check
     */
    public void updateIPData(String ip) {
        if (!playerData.getString("ip").equals(ip)){
            if (JawaCore.debug) LOGGER.log(Level.INFO, "{0} has a new IP. It was: {1} and it is now: {2}", new Object[]{PLAYER.toString(), playerData.getString("ip"), ip});
            PlayerDataHandler.ipData(ip, playerData.getJSONArray("ip-data"));
        }
    }

    /** Determines if a player has any active bans. If a ban is active, no matter
     * when or in what order it was done. This returns true. 
     * @return true or false if the player is banned or not respectivly 
     */
    public boolean isBanned() {
        for (JSONObject ban : banData.values()) {
            if (ban.getBoolean("active")) {
                return true;
            }
        }
        return false;
    }

    /** Determine if a name-data update is required
     * @param newName the name to check
     */
    public void updateNameData(String newName) {
        if (!playerData.getString("name").equals(newName)) {
            if (JawaCore.debug) LOGGER.log(Level.INFO, "{0} has a new minecraft name. It was: {1} and it is now: {2}", new Object[]{PLAYER.toString(), playerData.getString("name"), newName});
            PlayerDataHandler.nameData(newName, playerData.getJSONArray("name-data"));
        }
    }

    /** Get the currently used IP for the player
     * @return the string of the ip address
     */
    public String getIP() {
        return playerData.getString("ip");
    }
    
    /** Get the jsonarray of jsonobjects containing the ip data and dates of use
     * @return JSONArray<JSONObject> of the IP data
     */
    public JSONArray getIPData(){
        return playerData.getJSONArray("ip-data");
    }

    /** Determine if the passed IP matches the player's current IP. If not update the
     * recorded IP
     * @param ip the ip address to check
     */
    public void updateIP(String ip) {
        if (!playerData.getString("ip").equals(ip)) {
            playerData.put("ip", ip);
        }
    }

    /** Not implemented. Need to find a good way to get immunity into the pdo.
     * 
     */
    public void getImmunity() {
        //TODO FIXME Immunity through player object
    }
    
    /** Returns true if a player is muted. If a player is not muted it returns false.
     * @return 
     */
    public boolean isMuted(){
        if (playerData.has("mute")) return playerData.getBoolean("mute");
        else return false;
    }
    
    /** Sets the mute state of a player. 
     */
    public void mute() {
        playerData.put("mute", true);
        updatePlayerDataAsync();
    }

    /** Toggles the mute state of the player to the inverse of its current state.
     */
    public void unMute(){
        playerData.put("mute", false);
        updatePlayerDataAsync();
    }


    //##########################################################################
    //#   Player home methods
    //##########################################################################

    /** Assemble the home list from the home-data.
     */
//    private void buildHomeList(){
//        for(Object homeEntry : playerData.getJSONObject("home-data").getJSONArray(JawaCore.getServerName().toLowerCase())){
//                    HomeObject homeObj = new HomeObject((JSONObject) homeEntry);
//                    HOMELIST.put(homeObj.getName(), homeObj);
//        }
//    }
    
    /** add the newly indexed home ID to the player's home list
     * @param id
     * @param data 
     */
    public void returnHomeID(String id, JSONObject data){
        homeData.put(id, data);
        homeDataNames.put(data.getString("name"), id);
    }
    
    /** This will return the home entry in for that name. It will need to be
     * location processed to be usable.
     * @param homeName
     * @return jsonobject of the home
     */
    public JSONObject getHome(String homeName) {
        return homeData.get(homeDataNames.get(homeName));
        //return playerData.getJSONObject("home-data").getJSONObject(JawaCore.getServerName().toLowerCase()).getJSONObject(homeName);
    }
    
    /** This will return the location for the home entry for that name. It will need to be
     * location processed to be usable.
     * @param homeName
     * @return location of the home
     */
    public Location getHomeLocation(String homeName) {
        return LocationDataHandler.unpackLocation(homeData.get(homeDataNames.get(homeName)));
    }
    

    /** Returns a List<String> with the names of homes. If there are no homes list is empty.
     * @return List<String> object containing a list of available homes.
     */
    public List<String> getHomeList() {        
        return new ArrayList(homeDataNames.keySet());
    }

    /** Returns true is a matching homeName is found within the home list. This will
     * only check if this home exists for this particular server. This is case SENSATIVE.
     * @param homeName Home name to access.
     * @return True is home is present in this server's home list for the user, otherwise false.
     */
    public boolean containsHome(String homeName) {
        return homeDataNames.containsKey(homeName);
    }

    /** Creates a new homeObject in the HOMELIST and generates the accompanying JSON
     * entry within the playerData. Executes an Async update of player data.
     * @param player
     * @param homeName 
     */
    public void setHome(Player player, String homeName) {
        JSONObject newHome = LocationDataHandler.packLocation(player.getLocation());
        newHome.put("owner", PLAYER.toString());
        newHome.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        newHome.put("server", JawaCore.getServerName().toLowerCase());
        newHome.put("name", homeName);
        newHome.put("deleted", false);
        ESHandler.asyncDataIndexWithIDReturn("homes", newHome);
        
    }
    
    /** Marks the identified home as deleted. This will no longer be retrieved.
     * @param homeName 
     */
    public void removeHome(String homeName){
        homeData.get(homeDataNames.get(homeName)).put("deleted", true);
        updateHomeDataAsync(homeDataNames.get(homeName));
        homeData.remove(homeDataNames.get(homeName));
        homeDataNames.remove(homeName);
    }
    
    /** Returns the number of homes set for a user
     * @return 
     */
    public int getHomeCount(){
        return homeData.size();
    }

    //##########################################################################
    //#   Player name gets
    //########################################################################## 
    public String getStar() {
        if (playerData.getString("star").equals("r")) {
            return ChatColor.RED + "*";
        } else if (playerData.getString("star").equals("y")) {
            return ChatColor.YELLOW + "*";
        } else if (playerData.getString("star").equals("g")) {
            return ChatColor.GREEN + "*";
        } else {
            return "";
        }
    }

    public void setStar(String star) {
        if (star.equals("r")) {
            playerData.put("star", "r");
        } else if (star.equals("y")) {
            playerData.put("star", "y");
        } else if (star.equals("g")) {
            playerData.put("star", "g");
        } else if (star.equals("")) {
            playerData.put("star", "");
        }
        updatePlayerDataAsync();
    }

    public String getNickName() {
        return playerData.getString("nick");
    }

    /**
     * Adds a nick and resolves the nick-data attribute for update. Should only
     * be used with PlayerDataObjects that contain a player's full data.
     *
     * @param nick
     */
    public void setNick(String nick) {
        if (!nick.equals("")) {
            JSONArray nickData = PlayerDataHandler.nickData(nick, getNickData());
            if (nickData != null) {
                playerData.put("nick-data", nickData);
            }
        }
        playerData.put("nick", nick);
        updatePlayerDataAsync();
    }

    /**
     * Returns a nickname stripped of all color data
     *
     * @return
     */
    public String getPlainNick() {
        if (!getNickName().equals("")) {
            return playerData.getString("nick").replaceAll("&[a-f]|&[1-9]|&[k-r]", "");
        } else {
            return getName();
        }
    }

    public String getTag() {
        return playerData.getString("tag");
    }

    public void setTag(String tag) {
        playerData.put("tag", tag);
        updatePlayerDataAsync();
    }

    public JSONArray getNickData() {
        return playerData.getJSONArray("nick-data");
    }
    
    public String getFriendlyTag() {
        return ChatColor.translateAlternateColorCodes('&', getTag());
    }

    /**
     * Returns a colored name ready for sending. If a player doesn't have a
     * nickname this returns the player's their minecraft name with rank
     * coloring.
     *
     * @return
     */
    public String getFriendlyName() {
        if (getNickName().equals("")) {
            return getRankColor() + getName();
        } else {
            return ChatColor.translateAlternateColorCodes('&', getNickName());
        }
    }

    /**
     * Returns a colored name ready for sending. If a player doesn't have a
     * nickname this returns the player's their minecraft name with rank
     * coloring. This is backed by getFriendlyName()
     *
     * @return
     */
    public String getDisplayName() {
        return getFriendlyName();
    }

    public ChatColor getRankColor() {
        if (rankColor != null) {
            return rankColor;
        } else {
            return ChatColor.WHITE;
        }
    }

    public void setRankColor(ChatColor color) {
        rankColor = color;
    }

    //##########################################################################
    //#   Data updates
    //##########################################################################

    /** Triggers an AsyncUpdate that commits the working playerData to the ES db.
     */
    public void updatePlayerDataAsync() {
        ESHandler.asyncUpdateData(PLAYER, playerData, "players");
    }

    /** Triggers an AsyncUpdate that commits the working ban data to the ES db.
     * @param id
     */
    private void updateBanDataAsync(String id) {
        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(banData.get(id), "bans", id, true));
        //ESHandler.asyncUpdateData(PLAYER, banData, "bans");
    }
    
    /** Triggers an AsyncUpdate that commits the working home data to the ES db.
     * @param id
     */
    public void updateHomeDataAsync(String id){
        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.updateRequestBuilder(homeData.get(id), "homes", id, true));
//        ESHandler.asyncUpdateData(PLAYER, homeData, "homes");
    }

    /** Triggers Synchronous Update that commits the working playerData to the ES
     * db. This should only be used when called from an already asynchronous
     * thread.
     *
     * @return
     */
    public boolean updatePlayerData() {
        return ESHandler.singleUpdateRequest(ESRequestBuilder.updateRequestBuilder(playerData, "players", PLAYER.toString(), true));
    }

    /** Adds the data to the player object, caches it with the PlayerManager,
     * and executes a synchronous ES db update. Returns true if the update
     * succeeds.This is only intended for first player joins.
     * @param name
     * @param ip
     */
    private void installPlayer(String name, String ip) {
        firstTimePlayer(name, ip);
//        registerPlayer();
        if (updatePlayerData()) {
            //getPlayer().sendMessage(ChatColor.GREEN + " > You have been installed!");
            LOGGER.log(Level.INFO, "{0}Player: {1} has been installed.", new Object[]{ChatColor.GREEN, getName()});
            //return true;
        } else {
            LOGGER.log(Level.SEVERE, "Unable to index player: {0}:{1}", new Object[]{PLAYER.toString(), getName()});
            //return false;
        }
    }

    /** Update a player's changed details when they join.
     * @param name
     * @param ip
     */
    public void onJoinUpdate(String name, String ip) {
        validateData(name, ip);
        setLastLogin();
        
        //Update the name-data first
        updateNameData(name);
        //Update the name
        updateName(name);
        
        //Update the ip-data first
        updateIPData(ip);
        //Update the ip
        updateIP(ip);
        
//        repairMalformedData();
        
        if (updatePlayerData()) {
            callPlayerInfoLoaded();
        }
        //updatePlayerData();
        
    }
    
//    public void repairMalformedData(){
//        if (playerData.has("rank-data") && (playerData.getJSONObject("rank-data").keySet().contains("rank-data") || playerData.getJSONObject("rank-data").keySet().contains("rank"))){
//            LOGGER.log(Level.INFO, "Malformed rank-history data has been detected for {0}:{1}. Attempting to fix.", new Object[]{getName(), PLAYER.toString()});
//            //System.out.println("[PlayerDataObject] Malformed rank-history data has been detected. Attempting to fix.");
//            //PDO data correction
//            JSONObject rankData = playerData.getJSONObject("rank-data");
//            JSONObject tmpRankEntry = rankData.getJSONObject("rank-data");
//            rankData.remove("rank-data");
//            String tmpKey = String.valueOf(tmpRankEntry.keySet().toArray()[0]);
//            rankData.put(tmpKey, tmpRankEntry.getJSONObject(tmpKey));
//            playerData.remove("rank-data");
//            playerData.put("rank-data", rankData);
////            
////            JSONObject updateRankData = new JSONObject();
////            updateRankData.put("rank-data", rankData);
//            
//            //Database correction
//            ESHandler.correctMalformedField("rank-data", "players", PLAYER);
//        }
//    }
    
//    private void verifyPlayerData(){
//        
//    }
//    
//    private void verifyHomeData(){
////        if (playerData.has("home-data") && playerData.getJSONObject("home-data").has(JawaCore.getServerName())) {
////            for (String home : playerData.getJSONObject("home-data").getJSONObject(JawaCore.getServerName()))
////        }
//    }

    /**
     * Updates a player's logout details when they quit. It removes them from
     * the PlayerManager.
     */
    public void onQuitUpdate() {
        setLastLogout();
        updatePlayerData();
        PlayerManager.removePlayer(PLAYER);
    }

//    public void registerPlayer() {
//        PlayerManager.addPlayer(PLAYER, this);
//        //get home data since we let them join now
//        
//    }

    //##########################################################################
    //#   Debug
    //##########################################################################
    public void spillData() {
        LOGGER.log(Level.INFO,"PlayerDataObject Spilling data for {0}:{1}\n{2}", new Object[]{getName(),PLAYER.toString(),playerData.toString(4)});
        //System.out.println("PlayerDataObject Spilling data for player: " + player.toString());
        //System.out.println(playerData);
    }

    //##########################################################################
    //#   Events
    //##########################################################################
    public void callPlayerInfoLoaded() {
        Bukkit.getServer().getPluginManager().callEvent(new PlayerInfoLoaded(PLAYER, this));
    }

    /** Creates the player data for committing to the ElasticSearch index.
     * @param name The name of the player
     * @param ip IP of the player
     */
    public void firstTimePlayer(String name, String ip) {
        playerData.put("first-login", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        playerData.put("last-login", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        // Not setting logout here
        
//        playerData.put("play-time", 0);

        playerData.put("name", name);
        playerData.put("name-data", new JSONArray());
        PlayerDataHandler.nameData(name, playerData.getJSONArray("name-data"));
        
        playerData.put("nick", "");
        playerData.put("nick-data", new JSONArray());
        
        playerData.put("tag", "");
        playerData.put("star", "");
        
        //Create IP data
        playerData.put("ip", ip.replace("/", ""));
        playerData.put("ip-data", new JSONArray());
        PlayerDataHandler.ipData(ip, playerData.getJSONArray("ip-data"));

        //Setup rank data
        playerData.put("rank", "guest");
        playerData.put("rank-data", new JSONArray());
        //Create a system entry for the rank creation
        playerData.getJSONArray("rank-data").put(PlayerDataHandler.createPlayerRankChangeData("guest", "guest", "00000000-0000-0000-0000-000000000000"));
        
        playerData.put("single-permissions", new JSONArray());
        playerData.put("single-prohibitions", new JSONArray());
        
        playerData.put("safe-teleport", JawaCore.getPluginConfiguration("JawaCommands").getBoolean("teleport-settings.safe-tp-enabled", true));

        if (JawaCore.debug) {
            LOGGER.log(Level.INFO, "First-time player data generated as follows: {0}", playerData.toString(4));
            //System.out.print(handlerSlug + "firstTimePlayer data created as follows: " + playerData.toString());
        }
    }

    /**
     * Will send a message to the player if the player is online, otherwise it
     * does nothing.
     *
     * @param message
     */
    public void sendMessageIf(String message) {
        if (isOnline()) {
            sendMessage(message);
        }
    }
    
    public void sendMessageIf(BaseComponent[] message) {
        if (isOnline()) {
            getPlayer().spigot().sendMessage(message);
        }
    }

    /**
     * Sends a message to the player. This is nothing special, it just saves
     * getting the player from the object first.
     *
     * @param message
     */
    public void sendMessage(String message) {
        getPlayer().sendMessage(message);
    }

    /**
     * Sends messages to the player. This is nothing special, it just saves
     * getting the player from the object first.
     *
     * @param messages
     */
    public void sendMessage(String[] messages) {
        getPlayer().sendMessage(messages);
    }
    
    public void sendMessage(JSONArray messages) {
        for (Object msg : messages) {
            if (msg instanceof String) {
                sendMessageIf((String) msg);
            } else if (msg instanceof BaseComponent[]) {
                sendMessageIf((BaseComponent[]) msg);
            }
        }
    }
    
    public void scheduleMessage(String message, int delay) {
        scheduleMessage(new String[]{message}, delay);
    }
    
    public void scheduleMessage(String[] message, int delay) {
        Bukkit.getServer().getScheduler().runTaskLater(JawaCore.plugin, new Consumer<BukkitTask>() {
            @Override
            public void accept(BukkitTask t) {
                sendMessage(message);
            }
        }, delay);
    }

    public boolean equals(Player otherPlayer) {
        return otherPlayer.getUniqueId().toString().equals(PLAYER.toString());
    }

    public UUID getPrivateConversation() {
        return privateConversation;
    }

    public void startPrivateConversation(UUID privateConversation) {
        this.privateConversation = privateConversation;
    }

    public void endPrivateConversation() {
        privateConversation = null;
    }

    public boolean isHavingConversation() {
        if (privateConversation == null) {
            return false;
        } else {
            return true;
        }
    }
    
    /** Freeze a player.
     */
    public void freeze(){
        playerData.put("frozen", true);
        updatePlayerDataAsync();
    }
    
    /** Unfreeze a player.
     */
    public void thaw(){
        playerData.put("frozen", false);
        updatePlayerDataAsync();
    }
    
    /** Return true or false if a player is frozen
     * @return 
     */
    public boolean isFrozen(){
        if (playerData.has("frozen")){
            return playerData.getBoolean("frozen");
        } else {
            return false;
        }
    }
    
    /** Generate the initial discord data for a discord link. This will be called when
     * a player is linking to discord for the first time or has had their discord link
     * removed.
     * @return 
     */
    private JSONObject createDiscordData(boolean newData){
        JSONObject discordData = new JSONObject();
        discordData.put("linked", false);
        discordData.put("new", newData);
        return discordData;
    }
    
    /** Generate a new Discord link entry, generates a link code from the player UUID
     * string and the current LocalDataTime and returns that code.
     * @return 
     */
    public String generateDiscordCode(){
        String code = "#" + String.valueOf(Math.abs((PLAYER.toString() + LocalDateTime.now()).hashCode())) + "#";
        Logger.getLogger("PlayerDataObject").log(Level.INFO, "Generated a discord link code for {0}. Code: {1}", new Object[]{getName(), code});
        
//        boolean newData = !playerData.has("discord-data");
//        JSONObject discordData = createDiscordData(newData);
//        discordData.put("discord-code", code);
//        playerData.put("discord-data", discordData);
//        updatePlayerDataAsync();
        return code;
    }
    
    public String generateTestCode(){
        return "#" + String.valueOf(Math.abs((PLAYER.toString() + LocalDateTime.now()).hashCode())) + "#";
    }
    
    /** Returns true is a player is discord linked.
     * @return 
     */
    public boolean isDiscordLinked(){
        if (playerData.has("discord-data") && playerData.getJSONObject("discord-data").has("linked")){
            return playerData.getJSONObject("discord-data").getBoolean("linked");
        } else {
            return false;
        }
    }
    
    /** This executes a player linking and formally creates the linked relationship
     * in the database.
     * @param id
     * @param username
     * @return 
     */
    public boolean linkToDiscord(Long id, String username){
        if (!isDiscordLinked()) {
            playerData.put("discord-data", createDiscordData(true));
            playerData.getJSONObject("discord-data").put("discord-id", id);
            playerData.getJSONObject("discord-data").put("discord-name", username);
            playerData.getJSONObject("discord-data").put("linked", true);
            playerData.getJSONObject("discord-data").put("linked-on", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            updatePlayerDataAsync();
            return true;
        } else {
            return false;
        }
    }
    
    public String getDiscordName(){
        return playerData.getJSONObject("discord-data").getString("discord-name");
    }
    
    public Long getDiscordID(){
        return playerData.getJSONObject("discord-data").getLong("discord-id");
    }
    
    public JSONObject getDiscordData(){
        return playerData.getJSONObject("discord-data");
    }
    
//    public getChatChannels(){
//        
//    }
    
    public boolean validateData(String name, String ip){
        boolean repaired = false;
        List<String> itemsRepaired = new ArrayList();
        if (!playerData.has("first-login")) {
            playerData.put("first-login", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            repaired = true;
            itemsRepaired.add("first-login");
        }
        if (!playerData.has("last-login")){
            setLastLogin();
            repaired = true;
            itemsRepaired.add("last-login");
        }
        if (!playerData.has("last-logout")) {
            setLastLogout();
            repaired = true;
            itemsRepaired.add("last-logout");
        }

        if (!playerData.has("name")) {
            playerData.put("name", name);
            repaired = true;
            itemsRepaired.add("name");
        }
        if (!playerData.has("name-data")) {
            playerData.put("name-data", new JSONArray());
            PlayerDataHandler.nameData(name, playerData.getJSONArray("name-data"));
            repaired = true;
            itemsRepaired.add("name-data");
        }

        if (!playerData.has("nick")) {
            playerData.put("nick", "");
            repaired = true;
            itemsRepaired.add("nick");
        }
        
        if (!playerData.has("nick-data")) {
            playerData.put("nick-data", new JSONArray());
            repaired = true;
            itemsRepaired.add("nick-data");
        }
        
        if (!playerData.has("tag")) {
            playerData.put("tag", "");
            repaired = true;
            itemsRepaired.add("tag");
        }
        
        if (!playerData.has("star")) {
            playerData.put("star", "");
            repaired = true;
            itemsRepaired.add("star");
        }
        if (!playerData.has("ip")) {
            playerData.put("ip", ip);
            repaired = true;
            itemsRepaired.add("ip");
        }
        if (!playerData.has("ip-data")) {
            playerData.put("ip-data", new JSONArray());
            PlayerDataHandler.ipData(ip, playerData.getJSONArray("ip-data"));
            repaired = true;
            itemsRepaired.add("ip-data");
        }

        if (!playerData.has("rank")) {
            playerData.put("rank", "guest");
            repaired = true;
            itemsRepaired.add("rank");
        }
        
        if (!itemsRepaired.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Data validation has found malformed player data for {0}({1}) and has repaired the following items: {2}", new Object[]{playerData.getString("name"), PLAYER.toString(), String.join(", ", itemsRepaired)});
        }
        updatePlayerData();
        return repaired;
    }

    //##########################################################################
    //#   Player Comments
    //##########################################################################
    
    /** Check if the player data has admin comments.
     * @return 
     */
    public boolean containsComments(){
        return this.playerData.has("admin-comments") && !this.playerData.getJSONArray("admin-comments").isEmpty();
    }
    
    /** Add a comment to the player data.
     * @param comment String comment
     * @param adminUUID UUID of the admin adding the comment
     */
    public void addComment(String comment, UUID adminUUID){
        if (!this.playerData.has("admin-comments")){
            this.playerData.put("admin-comments", new JSONArray());
        }
        
        JSONObject commentObj = new JSONObject();
        commentObj.put("comment", comment);
        commentObj.put("admin", adminUUID.toString());
        commentObj.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        this.playerData.getJSONArray("admin-comments").put(commentObj);
        updatePlayerDataAsync();
    }
    
    /** Remove a specific comment with the index of commentNumber.
     * @param commentNumber 
     */
    public void removeComment(int commentNumber){
        this.playerData.getJSONArray("admin-comments").remove(commentNumber);
        updatePlayerDataAsync();
    }
    
    /** Get the JSONArray of comments
     * @return JSONArray of the comments
     */
    public JSONArray getComments(){
        if (!containsComments()) return null;
        return this.playerData.getJSONArray("admin-comments");
    }
    
    /** Is there a comment at the specified index.
     * @param ind
     * @return 
     */
    public boolean hasCommentAtIndex(int ind){
        return this.playerData.getJSONArray("admin-comments").length() >= ind;
    }
    
    /** Returns if the player has safe TP enabled. If there is no entry for it
     * it will be created with the server default value. FIXME needs soft depends coding
     * @return 
     */
    public boolean safeTeleportEnabled(){
        try {
            return playerData.getBoolean("safe-teleport");
        } catch (JSONException ex ) {
            LOGGER.log(Level.WARNING, playerData.getString("name") + " did not have an entry for safe-teleport. Adding entry...");
            playerData.put("safe-teleport", JawaCore.getPluginConfiguration("JawaCommands").getBoolean("teleport-settings.safe-tp-enabled", true));
            updatePlayerDataAsync();
            return JawaCore.getPluginConfiguration("JawaCommands").getBoolean("teleport-settings.safe-tp-enabled", true);
        }
    }
    
    /** Toggles between safe teleport on-off.
     */
    public void toggleSafeTeleport(){
        //Doesn't seem to be the most efficient but this way it runs the check and adds the value if need be
        if (safeTeleportEnabled()) {
            playerData.put("safe-teleport", false);
        } else {
            playerData.put("safe-teleport", true);
        }
    }
    
    public boolean getAllowedInZone(String zoneName){
        return ZONEPERMISSIONS.containsKey(zoneName) ? ZONEPERMISSIONS.get(zoneName).getBoolean("allowed") : true;
    }
    
    public boolean getRequiredInZone(String zoneName){
        return ZONEPERMISSIONS.containsKey(zoneName) ? ZONEPERMISSIONS.get(zoneName).getBoolean("required") : true;
    }
    
    public void setZonePermission(String zoneName, boolean playerAllowedInZone, boolean playerRequiredInZone){
        JSONObject zonePerms = new JSONObject();
        zonePerms.put("allowed", playerAllowedInZone);
        zonePerms.put("required", playerRequiredInZone);
        ZONEPERMISSIONS.put(zoneName, zonePerms);
    }
 
}
