/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.elasticsearch.action.search.MultiSearchRequest;

/**
 *
 * @author alexander
 */
public class PlayerManager {
    
    public static final String NOPLAYERERROR = ChatColor.RED + "> Error: That player is not found or is not online! Try their actual minecraft name instead of nickname.";

    private static JavaPlugin plugin = JawaCore.plugin;

    private static HashMap<UUID, PlayerDataObject> playerDataObjects = new HashMap();
    private static HashMap<String, UUID> nickNameMap = new HashMap();

    private static HashMap<UUID, PlayerDataObject> offlinePlayerCache = new HashMap();
    private static HashMap<String, UUID> offlineNickNameMap = new HashMap();
    private static HashMap<LocalDateTime, UUID> offlineCacheAccessTime = new HashMap();

    private static HashMap<String, UUID> playerDiscordCodes = new HashMap();
    private static HashMap<UUID, String> playerDiscordCodesReverse = new HashMap();

//    public PlayerManager(JavaPlugin plugin) {
//        this.plugin = plugin;
//        playerDataObjects = new HashMap();
//        nickNameMap = new HashMap();
//
//        offlinePlayerCache = new HashMap();
//        offlineNickNameMap = new HashMap();
//        offlineCacheAccessTime = new HashMap();
//
//        playerDiscordCodes = new HashMap();
//        playerDiscordCodesReverse = new HashMap();
//
//        generateCleanupTask();
//    }

    /**
     * Clear all data contained in this construct. Should be called on shut
     * down.
     */
    public void deconstruct() {
        playerDataObjects.clear();
        nickNameMap.clear();

        offlinePlayerCache.clear();
        offlineNickNameMap.clear();
        offlineCacheAccessTime.clear();
    }

    /**
     * Load a player into the cache. This should be done when a player joins the
     * server.
     *
     * @param player
     * @param pdObject
     */
    public static void addPlayer(Player player, PlayerDataObject pdObject) {
        addPlayer(player.getUniqueId(), pdObject);

    }

    public static void addPlayer(UUID uuid, PlayerDataObject pdObject) {
        playerDataObjects.put(uuid, pdObject);
        nickNameMap.put(pdObject.getPlainNick().toUpperCase().replaceAll(" ", "_"), uuid);
    }

    /**
     * Uncache a player. This should be called when a player disconnects from
     * the server.
     *
     * @param player
     */
    public static void removePlayer(UUID player) {
        //System.out.print("Plain nick: " + playerDataObjects.get(player).getPlainNick().toUpperCase().replaceAll(" ", "_"));
        //System.out.print(nickNameMap);
        if (nickNameMap.containsKey(playerDataObjects.get(player).getPlainNick().toUpperCase().replaceAll(" ", "_"))) {
            nickNameMap.remove(playerDataObjects.get(player).getPlainNick().toUpperCase().replaceAll(" ", "_"));
        }
        playerDataObjects.remove(player);
    }

    /**
     * Returns a player's dataobject. This will begin by converting the input
     * name into full uppercase and replacing spaces with underscores (ex
     * JAWA_MASTER for Jawa Master) This will search the player nick name map.
     * If there is no nick name found then it will attempt to get a bukkit
     * Player with that name. If that returns null it will execute
     * ESHandler.findOfflinePlayer(name, true) and return that data object, if
     * the user is online and found with either name or nick name then the
     * cached playerDataObject will be returned.
     *
     * This way only offline player information requests will call the database.
     *
     * @param name
     * @return
     */
    public static PlayerDataObject getPlayerDataObject(String name) {

        UUID uuid;
        if (nickNameMap.containsKey(name.toUpperCase().replaceAll(" ", "_"))) {
            uuid = nickNameMap.get(name.toUpperCase().replaceAll(" ", "_"));
        } else {
            Player target = plugin.getServer().getPlayer(name);
            if (target == null) {
                return getOfflinePlayer(name);
            } else {
                uuid = target.getUniqueId();
            }
        }

        return playerDataObjects.get(uuid);
    }

    public static PlayerDataObject getPlayerDataObject(UUID uuid) {
        if (playerDataObjects.containsKey(uuid)) {
            return playerDataObjects.get(uuid);
        } else {
            return getOfflinePlayer(uuid);
        }
    }

    public static PlayerDataObject getPlayerDataObject(Player player) {
        return playerDataObjects.get(player.getUniqueId());
    }

    /**
     * Finds an offline player and commits the corresponding dataobject to the
     * offline player cache.
     *
     * @param name
     * @return
     */
    private static PlayerDataObject getOfflinePlayer(String name) {
        if (offlineNickNameMap.containsKey(name.toUpperCase().replaceAll(" ", "_"))) {
            for (Entry ent : offlineCacheAccessTime.entrySet()) {
                if (ent.getValue().equals(offlineNickNameMap.get(name.toUpperCase().replaceAll(" ", "_")))) {
                    offlineCacheAccessTime.remove((LocalDateTime) ent.getKey());
                    offlineCacheAccessTime.put(LocalDateTime.now(), offlineNickNameMap.get(name.toUpperCase().replaceAll(" ", "_")));
                    break;
                }
            }
            return offlinePlayerCache.get(offlineNickNameMap.get(name.toUpperCase().replaceAll(" ", "_")));
        } else {
            PlayerDataObject pdObject = ESHandler.findOfflinePlayer(name, true);
            if (pdObject != null) {
                pdObject.loadPlayerBanData();
                offlineCacheAccessTime.put(LocalDateTime.now(), pdObject.getUniqueID());
                offlinePlayerCache.put(pdObject.getUniqueID(), pdObject);
                offlineNickNameMap.put(pdObject.getPlainNick().toUpperCase().replaceAll(" ", "_"), pdObject.getUniqueID());
            }
            return pdObject;
        }

//        if (pdObject != null) {
//            offlinePlayerCache.put(pdObject.getName(), pdObject);
//        }
    }

    private static PlayerDataObject getOfflinePlayer(UUID uuid) {
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("players", "_id", uuid.toString()));
        return ESHandler.runMultiIndexSearch(multiSearchRequest, new PlayerDataObject(uuid));
    }

    /**
     * Returns an online player. This method accepts Minecraft names or player
     * Nicknames. This will return null if the player is not resolvable;
     *
     * @param name
     * @return
     */
    public static Player getPlayer(String name) {

        if (nickNameMap.containsKey(name.toUpperCase().replaceAll(" ", "_"))) {
            return Bukkit.getPlayer(nickNameMap.get(name.toUpperCase().replaceAll(" ", "_"))); //Should never be null
        } else {
            Player target = plugin.getServer().getPlayer(name);
            if (target == null) {
                return null;
            } else {
                return target;
            }
        }
    }

    /**
     * Checks if the supplied string is a valid online player nickname or
     * minecraft name. If it is valid this returns true, else this returns
     * false.
     *
     * @param name
     * @return
     */
    public static boolean isValidPlayer(String name) {
        if (nickNameMap.containsKey(name.toUpperCase().replaceAll(" ", "_"))) {
            return true;
        } else {
            Player target = plugin.getServer().getPlayer(name);
            if (target == null) {
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Retreaves and validates an admin player.
     *
     * @param commandSender
     * @param parsedArguments
     * @return
     */
    public static PlayerDataObject getAdmin(CommandSender commandSender, HashMap<String, String> parsedArguments) {
        PlayerDataObject admin;
        if (commandSender instanceof Player) {
            if (parsedArguments.containsKey("b")) {
                commandSender.sendMessage(ChatColor.RED + " > Error: The by(-b) flag is only used when working from the console. This will be ignored.");
            }
            admin = getPlayerDataObject((Player) commandSender);
        } else {
            if (parsedArguments.containsKey("b")) {
                admin = getPlayerDataObject(parsedArguments.get("b"));
            } else {
                System.out.print(ChatColor.RED + " > Error: You must specify a username for yourself with the -b flag! Make sure to use your minecraft name not your nick!");
                return null;
            }
        }

        if (admin == null) {
            commandSender.sendMessage(ChatColor.RED + " > Error: That admin wasn't found! Try their actual minecraft name instead of nickname.");
            return null;
        } else {
            return admin;
        }

    }

    public static void generateCleanupTask() {
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(PlayerManager.plugin, new Runnable() {
            @Override
            public void run() {

                //Cleanup offline cached players that havent been accessed in 5 minutes
                int offlineRemoved = 0;
                LocalDateTime now = LocalDateTime.now();
                for (LocalDateTime accessTime : offlineCacheAccessTime.keySet()) {
                    if (now.isBefore(accessTime.plusMinutes(5))) {
                        if (offlinePlayerCache.containsKey(offlineCacheAccessTime.get(accessTime))) { //if key is in the offline cache
                            String nick = offlinePlayerCache.get(offlineCacheAccessTime.get(accessTime)).getPlainNick().toUpperCase().replaceAll(" ", "_");
                            if (offlineNickNameMap.containsKey(nick)) {
                                offlineNickNameMap.remove(nick);
                                offlinePlayerCache.remove(offlineCacheAccessTime.get(accessTime));
                                offlineCacheAccessTime.remove(accessTime);
                                offlineRemoved++;
                            } else {
                                clearOfflineCaches();
                            }

                        } else {
                            clearOfflineCaches();
                        }
                    }
                }
                //TODO add debug
                if (JawaCore.debug) //To shut this thing up
                {
                    Logger.getLogger("JawaCore][PlayerManager][Cleanup Task").log(Level.INFO, "Cleanup completed. {0} players removed from cache.", offlineRemoved);
                }

//                //Cleanup online player caches that are out of sync (shouldn't happen unless something weird happened)
//                if ((playerDataObjects.size() != nickNameMap.size()) || (playerDataObjects.size() != Bukkit.getServer().getOnlinePlayers().size())) {
//                    for (Player player : Bukkit.getServer().getOnlinePlayers()){
//                        if (player.getUniqueId())
//                    }
//                }
            }
        }, 6000, 6000);
    }

    private static void clearOfflineCaches() {
        Logger.getLogger("[JawaCore][PlayerManager][Cleanup Task]").log(Level.INFO, " A cache mismatch was found. All offline player caches have been cleared.");
        offlineCacheAccessTime.clear();
        offlineNickNameMap.clear();
        offlinePlayerCache.clear();
    }

    public static void putPlayerCode(String code, UUID uuid) {
        if (playerDiscordCodes.containsValue(uuid)) {
            String cCode = playerDiscordCodesReverse.get(uuid);
            playerDiscordCodes.remove(cCode);
            playerDiscordCodesReverse.remove(uuid);
        }

        playerDiscordCodes.put(code, uuid);
        playerDiscordCodesReverse.put(uuid, code);

        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (playerDiscordCodes.containsKey(code)) {
                playerDiscordCodes.remove(code);
            }
            if (playerDiscordCodesReverse.containsKey(uuid) && playerDiscordCodesReverse.get(uuid).equals(code)) {
                playerDiscordCodesReverse.remove(uuid);
                if (playerDataObjects.containsKey(uuid)) {
                    playerDataObjects.get(uuid).sendMessage(ChatColor.RED + "> Your discord code has expired.");
                }
            }
        }, 6000);
    }

    public static void removePlayerCode(String code, UUID uuid) {
        playerDiscordCodes.remove(code);
        playerDiscordCodesReverse.remove(uuid);
    }
    
    public static boolean codeExists(String linkCode){
        return playerDiscordCodes.containsKey(linkCode);
    }
    
    public static UUID getPlayerCodedUUID(String linkCode){
        return playerDiscordCodes.get(linkCode);
    }

}
