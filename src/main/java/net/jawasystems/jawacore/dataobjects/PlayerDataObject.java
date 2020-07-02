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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.PlayerManager;
import net.jawasystems.jawacore.events.PlayerInfoLoaded;
import net.jawasystems.jawacore.events.PlayerRankChange;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.handlers.PlayerDataHandler;
import static net.jawasystems.jawacore.handlers.PlayerDataHandler.handlerSlug;
import static net.jawasystems.jawacore.handlers.PlayerDataHandler.ipData;
import static net.jawasystems.jawacore.handlers.PlayerDataHandler.nameData;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.search.SearchHit;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alexander
 */
public class PlayerDataObject {

    private static final Logger LOGGER = Logger.getLogger("PlayerDataObject");
    private UUID player;
    private JSONObject banData;
    private JSONObject playerData;
    private JSONObject homeData;

    private UUID privateConversation;

    private ChatColor rankColor;

    public PlayerDataObject(UUID player) {
        this.player = player;
        //This should ensure that all objects are initialized, this way even if they are empty they wont' error
        playerData = new JSONObject();
        banData = new JSONObject();
        homeData = new JSONObject();
        privateConversation = null;
    }

    public void addBanData(Map banData) {
        this.banData = new JSONObject(banData);
    }

    public void addPlayerData(Map playerData) {
        this.playerData = new JSONObject(playerData);
    }

    public void addHomeData(Map homeData) {
        this.homeData = new JSONObject(homeData);
    }

    public String getPlayerUUID() {
        return player.toString();
    }

    /**
     * Adds Data from an ES search. The index will specify what kind of data is
     * being received.
     *
     * @param index - players, bans, or homes
     * @param data
     */
    public void addSearchData(String index, Map data) {
        switch (index) {
            case "players": {
                this.playerData = new JSONObject(data);
                break;
            }
            case "bans": {
                this.banData = new JSONObject(data);
                break;
            }
            case "homes": {
                this.homeData = new JSONObject(data);
                break;
            }
        }
    }

    public JSONObject getPlayerData() {
        return playerData;
    }

    public boolean containsBanData() {
        return !banData.isEmpty();
    }

    public boolean containsPlayerData() {
        return !playerData.isEmpty();
    }

    public boolean containsHomeData() {
        return !homeData.isEmpty();
    }

    public boolean isOnline() {
        if (Bukkit.getServer().getPlayer(player) != null) {
            return true;
        } else {
            return false;
        }
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(player);
    }

    //##########################################################################
    //#   Player ban gets
    //##########################################################################
    public String getLatestBanDate() {
        if (playerData.has("current-ban")) return (String) playerData.get("current-ban"); //FIXME this is to deal with a typo in the legacy ban data in the repo
        else return (String) playerData.get("latest-ban");
    }

    private JSONObject getBanEntry(String banDateTime) {
        return banData.getJSONObject(banDateTime);
    }

    public String getBanReason(String banDateTime) {
        return (String) getBanEntry(banDateTime).get("reason");
    }

    public String getBannedBy(String banDateTime) {
        return (String) getBanEntry(banDateTime).get("banned-by");
    }

    /** Return a string form of the "banned-until" this should be a LocalDateTime
     * object in string format.
     * @param banDateTime
     * @return 
     */
    public String getBannedUntil(String banDateTime) {
        return getBanEntry(banDateTime).getString("banned-until");
    }
    
    public String isBannedUntil(){
        return getBannedUntil(getLatestBanDate());
    }

    public String getBannedUnreason(String banDateTime) {
        return (String) getBanEntry(banDateTime).get("unreason");
    }

    public String getBannedUnBy(String banDateTime) {
        return (String) getBanEntry(banDateTime).get("unbanned-by");
    }

    public String unbannedOn(String banDateTime) {
        return (String) getBanEntry(banDateTime).get("unbanned-on");
    }
    
    public boolean isBanLocked(){
        if (getLatestBan().has("ban-lock")){
            return getLatestBan().getBoolean("ban-lock");
        } else return false;
    }
    
    public String getBanLockAdmin(){
        UUID adminUUID = UUID.fromString(getLatestBan().getString("ban-lock-by"));
        return PlayerManager.getPlayerDataObject(adminUUID).getFriendlyName();
    }
    
    public String getBanLockReason(){
        return getLatestBan().getString("ban-lock-reason");
    }
    /** Is a particular ban active.
     * @param banDateTime
     * @return 
     */
    public boolean getBanState(String banDateTime) {
        return Boolean.valueOf((String) getBanEntry(banDateTime).get("active"));
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

    /** Returns a set of the keys relating to bans.
     * @return 
     */
    public Set getListOfBans() {
        return banData.keySet();
    }
    
    public JSONObject getBanData(){
        return banData;
    }

    /** Bans a player.
     * @param commandSender - Command Sender of the ban command
     * @param parsedArguments - Arguments containing the reason and if needed b (by) flag if from the console
     * @param banDate - LocalDateTime of the ban. Usually LocalDateTime.now()
     */
    public void banPlayer(CommandSender commandSender, HashMap<String, String> parsedArguments, LocalDateTime banDate) { //TODO need to move the commandSender to PlayerDataObject
        if (!containsBanData()) banData = new JSONObject();
        banData.put(banDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), PlayerDataHandler.assembleBanData(commandSender, parsedArguments, banDate));
        
        playerData.put("banned", true);
        playerData.put("latest-ban", banDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        updatePlayerDataAsync();
        updatePlayerBanDataAsync();
    }
    
    /** Unbans a player.
     * @param admin - The PlayerDataObject of the admin performing the ban.
     * @param parsedArguments - The HashMap containing the the required information for unban data (reason)
     * @param unbanDateTime - LocalDateTime object of the unban. Usually LocalDateTime.now()
     */
    public void unbanPlayer(PlayerDataObject admin, HashMap<String, String> parsedArguments, LocalDateTime unbanDateTime){
        banData.put(getLatestBanDate(), PlayerDataHandler.assembleUnBanData(admin, parsedArguments, unbanDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        playerData.put("banned", false);
        updatePlayerDataAsync();
        updatePlayerBanDataAsync();
    }
    
    /** Run's a search request for an entry in the bans index with _id matching the player's 
     * UUID. If no result is found then they player's ban data is initialized with a blank object.
     */
    public void loadPlayerBanData(){
        try {
            SearchHit[] hits = ESHandler.runSearchRequest(ESRequestBuilder.buildSearchRequest("bans", "_id", player.toString()));
            banData = new JSONObject(hits[0].getSourceAsMap());
        } catch (Exception e){
            banData = new JSONObject();
        }
    }
    
    /** Gets the latest ban as a JSONObject. This gets the player's latest ban data
     * whether they are still banned or not.
     * @return 
     */
    public JSONObject getLatestBan() {
        return getBanEntry(getLatestBanDate());
    }
    
    /** Given a string, get's the latest ban and updates it.
     * @param reason - The string reason that updates the latest ban.
     */
    public void updateBan(String reason){
        banData.put(getLatestBanDate(), getLatestBan().put("reason", reason));
        updatePlayerBanDataAsync();
    }
    
    public void banExpired() {
        JSONObject expireData = PlayerDataHandler.assembleExpiredUnBanData();
        for (String key : expireData.keySet()) {
            banData.getJSONObject(getLatestBanDate()).put(key, expireData.get(key));
        }
        playerData.put("banned", false);
        updatePlayerDataAsync();
        updatePlayerBanDataAsync();
        scheduleMessage(ChatColor.GREEN + " > " + ChatColor.YELLOW + "Your current ban has expired. Please remember to behave yourself.", 40);
        Bukkit.broadcast(ChatColor.YELLOW + " > !!!! " + getFriendlyName() + ChatColor.YELLOW + " !!!! has been auto unbanned due to ban expiration.", "jawachat.opchat");
    }

    //##########################################################################
    //#   Player data gets
    //##########################################################################
    public String getRank() {
        return playerData.getString("rank");
    }
    
    /** If the player has existing rank change data it is returned. If the user
     * has no existing rank change data then null is returned.
     * @return 
     */
    public JSONObject getRankData() {
        if (playerData.has("rank-data")) {
            return playerData.getJSONObject("rank-data");
        } else {
            return null;
        }
    }

    /** Returns the player UUID.
     * @return 
     */
    public UUID getUniqueID() {
        return player;
    }

    /** Changes the player's rank and records the UUID of admin, time, new, and old ranks.
     * Then notifies the player that their rank has been changed.
     * @param newRank
     * @param adminUUID
     * @param rankColor 
     */
    public void setRank(String newRank, UUID adminUUID, ChatColor rankColor) {
        newRank = newRank.toLowerCase();
        playerData.put("rank-data", PlayerDataHandler.createPlayerRankChangeData(getRank(), newRank, adminUUID.toString()));
        playerData.put("rank", newRank);
        setRankColor(rankColor);
        Bukkit.getServer().getPluginManager().callEvent(new PlayerRankChange(player, newRank));
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
            setName(getPlayer().getName());
        }
    }

    public int getPlayTime() {
        return (int) playerData.get("play-time");
    }

    public LocalDateTime getLastLogin() {
        return LocalDateTime.parse((String) playerData.get("last-login"));
    }

    public void setLastLogin() {
        playerData.put("last-login", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        Logger.getLogger(PlayerDataObject.class.getName()).log(Level.FINEST, "Recording last login date for player: {0} as {1}", new Object[]{player.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)});
    }

    public LocalDateTime getLastLogout() {
        return LocalDateTime.parse((String) playerData.get("last-logout"));
    }

    public void setLastLogout() {
        playerData.put("last-logout", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        Logger.getLogger(PlayerDataObject.class.getName()).log(Level.FINEST, "Recording last logout date for player: {0} as {1}", new Object[]{player.toString(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)});
    }

    public JSONArray getIPArray() {
        return playerData.getJSONArray("ips");
    }

    public void setIPArray(JSONArray ipData) {
        playerData.put("ips", ipData);
    }

    public void updateIPArray(String ip) {
        JSONArray ips = PlayerDataHandler.ipData(ip, getIPArray());
        if (ips != null) {
            setIPArray(ips);
        }
    }

    public boolean isBanned() {
        return playerData.getBoolean("banned");
    }

    public JSONArray getNameArray() {
        return new JSONArray(String.valueOf(playerData.get("name-data")));
    }

    public void setNameArray(JSONArray nameData) {
        playerData.put("name-data", nameData);
    }

    public void updateNameArray(String name) {
        JSONArray nameData = PlayerDataHandler.nameData(name, getNameArray());
        if (nameData != null) {
            setNameArray(nameData);
        }
    }

    public String getIP() {
        return (String) playerData.get("ip");
    }

    public void setIP(String ip) {
        playerData.put("ip", ip);
    }

    public void updateIP(String ip) {
        if (!getIP().equals(ip)) {
            setIP(ip);
        }
    }

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
     * @param isMuted 
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
    public boolean homeExists(String homeName) {
        return homeData.keySet().contains(homeName);
    }

    /**
     * This will return the home entry in for that name. It will need to be
     * location processed to be usable.
     *
     * @param homeName
     * @return
     */
    public JSONObject getHome(String homeName) {
        return homeData.getJSONObject(homeName);
    }

    public Set getHomeEntries() {
        return homeData.keySet();
    }
    
    public List<String> getHomeList() {
        return new ArrayList<String>(homeData.keySet());
    }

    public boolean containsHome(String homeName) {
        if (containsHomeData()) {
            return homeData.keySet().contains(homeName);
        } else {
            return false;
        }
    }

    public void setHome(String homeName, JSONObject location) {
        homeData.put(homeName, location);
        updateHomeDataAsync();
    }
    
    public void removeHome(String homeName){
        homeData.remove(homeName);
        ESHandler.singleAsyncUpdateRequest(ESRequestBuilder.requestFieldRemoval(player.toString(), homeName));
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
    /**
     * Triggers a multiIndex search that will update the user's information
     * within this object.
     */
    public void updateDataContents() {
        MultiSearchResponse response = ESHandler.runMultiSearchRequest(ESRequestBuilder.getAllUserData(player));
        Item[] responses = response.getResponses();
        for (Item response1 : responses) {
            if (!response1.isFailure()) {
                switch (response1.getResponse().getHits().getHits()[0].getIndex()) {
                    case "players": {
                        addPlayerData(response1.getResponse().getHits().getHits()[0].getSourceAsMap());
                        break;
                    }
                    case "bans": {
                        addBanData(response1.getResponse().getHits().getHits()[0].getSourceAsMap());
                        break;
                    }
                    case "homes": {
                        addHomeData(response1.getResponse().getHits().getHits()[0].getSourceAsMap());
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Triggers an AsyncUpdate that commits the working playerData to the ES db.
     */
    public void updatePlayerDataAsync() {
        ESHandler.asyncUpdateData(player, playerData, "players");
    }

    public void updatePlayerBanDataAsync() {
        ESHandler.asyncUpdateData(player, banData, "bans");
    }
    
    public void updateHomeDataAsync(){
        ESHandler.asyncUpdateData(player, homeData, "homes");
    }

    /**
     * Triggers Synchronous Update that commits the working playerData to the ES
     * db. This should only be used when called from an already asynchronous
     * thread.
     *
     * @return
     */
    public boolean updatePlayerData() {
        return ESHandler.singleUpdateRequest(ESRequestBuilder.updateRequestBuilder(playerData, "players", player.toString(), true));
    }

    /**
     * * Adds the data to the player object, caches it with the PlayerManager,
     * and executes a synchronous ES db update.Returns true if the update
     * succeeds.This is only intended for first player joins.
     *
     * @param name
     * @param ip
     * @param playerData
     * @return
     */
    public boolean installPlayer(String name, String ip) {
        this.playerData = firstTimePlayer(name, ip);
        this.banData = new JSONObject();
        this.homeData = new JSONObject();
        registerPlayer();
        if (updatePlayerData()) {
            callPlayerInfoLoaded();
            //getPlayer().sendMessage(ChatColor.GREEN + " > You have been installed!");
            Logger.getLogger(PlayerDataObject.class.getName()).log(Level.INFO, "Player: {0} has been installed.", getName());
            return true;
        } else {
            Logger.getLogger(PlayerDataObject.class.getName()).log(Level.SEVERE, "Unable to index player: {0}:{1}", new Object[]{player.toString(), getName()});
            return false;
        }
    }

    /**
     * Update a player's changed details when they join.
     * @param name
     * @param ip
     */
    public void onJoinUpdate(String name, String ip) {
        validateData(name, ip);
        setLastLogin();
        updateName(name);
        updateNameArray(name);
        updateIP(ip);
        updateIPArray(ip);
        repairMalformedData();
        if (updatePlayerData()) {
            callPlayerInfoLoaded();
        }
        //updatePlayerData();
        
    }
    
    public void repairMalformedData(){
        if (playerData.has("rank-data") && playerData.getJSONObject("rank-data").keySet().contains("rank-data")){
            LOGGER.log(Level.INFO, "Malformed rank-history data has been detected for {0}:{1}. Attempting to fix.", new Object[]{getName(), player.toString()});
            //System.out.println("[PlayerDataObject] Malformed rank-history data has been detected. Attempting to fix.");
            //PDO data correction
            JSONObject rankData = playerData.getJSONObject("rank-data");
            JSONObject tmpRankEntry = rankData.getJSONObject("rank-data");
            rankData.remove("rank-data");
            String tmpKey = String.valueOf(tmpRankEntry.keySet().toArray()[0]);
            rankData.put(tmpKey, tmpRankEntry.getJSONObject(tmpKey));
            playerData.remove("rank-data");
            playerData.put("rank-data", rankData);
//            
//            JSONObject updateRankData = new JSONObject();
//            updateRankData.put("rank-data", rankData);
            
            //Database correction
            ESHandler.correctMalformedField("rank-data", "players", player);
        }
    }

    /**
     * Updates a player's logout details when they quit. It removes them from
     * the PlayerManager.
     */
    public void onQuitUpdate() {
        setLastLogout();
        updatePlayerData();
        PlayerManager.removePlayer(player);
    }

    public void registerPlayer() {
        PlayerManager.addPlayer(player, this);
    }

    //##########################################################################
    //#   Debug
    //##########################################################################
    public void spillData() {
        LOGGER.log(Level.INFO,"PlayerDataObject Spilling data for {0}:{1}\n{2}", new Object[]{getName(),player.toString(),playerData.toString(4)});
        //System.out.println("PlayerDataObject Spilling data for player: " + player.toString());
        //System.out.println(playerData);
    }

    //##########################################################################
    //#   Events
    //##########################################################################
    private void callPlayerInfoLoaded() {
        Bukkit.getServer().getPluginManager().callEvent(new PlayerInfoLoaded(player, this));
    }

    /**
     * Creates the player data for committing to the ElasticSearch index.
     *
     * @param name
     * @param ip
     * @param autoElevate
     * @param player
     * @return
     */
    public JSONObject firstTimePlayer(String name, String ip) {
        playerData.put("first-login", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        setLastLogin();
        setLastLogout();
        playerData.put("play-time", 0);

        playerData.put("name", name);
        playerData.put("name-data", nameData(name, new JSONArray()));

        playerData.put("banned", false);
        playerData.put("nick", "");
        playerData.put("nick-data", new JSONArray());
        playerData.put("tag", "");
        playerData.put("star", "");
        setIP(ip);
        playerData.put("ips", ipData(ip, new JSONArray()));

        playerData.put("rank", "guest");

        if (JawaCore.debug) {
            LOGGER.log(Level.INFO, "First-time player data generated as follows: {0}", playerData.toString());
            //System.out.print(handlerSlug + "firstTimePlayer data created as follows: " + playerData.toString());
        }

        return playerData;
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
        return otherPlayer.getUniqueId().toString().equals(player.toString());
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
        String code = "#" + String.valueOf(Math.abs((player.toString() + LocalDateTime.now()).hashCode())) + "#";
        Logger.getLogger("PlayerDataObject").log(Level.INFO, "Generated a discord link code for {0}. Code: {1}", new Object[]{getName(), code});
        
//        boolean newData = !playerData.has("discord-data");
//        JSONObject discordData = createDiscordData(newData);
//        discordData.put("discord-code", code);
//        playerData.put("discord-data", discordData);
//        updatePlayerDataAsync();
        return code;
    }
    
    public String generateTestCode(){
        return "#" + String.valueOf(Math.abs((player.toString() + LocalDateTime.now()).hashCode())) + "#";
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
        if (!playerData.has("play-time")) {
            playerData.put("play-time", 0);
            repaired = true;
            itemsRepaired.add("play-time");
        }

        if (!playerData.has("name")) {
            playerData.put("name", name);
            repaired = true;
            itemsRepaired.add("name");
        }
        if (!playerData.has("name-data")) {
            playerData.put("name-data", nameData(name, new JSONArray()));
            repaired = true;
            itemsRepaired.add("name-data");
        }

        if (!playerData.has("banned")) {
            playerData.put("banned", false);
            repaired = true;
            itemsRepaired.add("banned");
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
            setIP(ip);
            repaired = true;
            itemsRepaired.add("ip");
        }
        if (!playerData.has("ips")) {
            playerData.put("ips", ipData(ip, new JSONArray()));
            repaired = true;
            itemsRepaired.add("ips");
        }

        if (!playerData.has("rank")) {
            playerData.put("rank", "guest");
            repaired = true;
            itemsRepaired.add("rank");
        }
        
        if (!itemsRepaired.isEmpty()) {
            Logger.getLogger("PlayerDataObject").log(Level.INFO, "Data validation has found malformed player data for {0}({1}) and has repaired the following items: {2}", new Object[]{playerData.getString("name"), player.toString(), String.join(", ", itemsRepaired)});
        }
        
        return repaired;
    }

}
