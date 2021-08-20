/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import net.jawasystems.jawacore.handlers.ESHandler;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import net.jawasystems.jawacore.utils.TimeParser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.elasticsearch.action.search.MultiSearchRequest;

/**
 *
 * @author alexander
 */
public class PlayerPreJoin implements Listener {
    private static final Logger LOGGER = Logger.getLogger("[JawaCore][PlayerPreJoin] ");

    @EventHandler
    public static void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        //Create a new blank PlayerDataObject
        //Populate it with the player's index information
        //pdObject.updateDataContents();
//        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
//        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("players", "_id", event.getUniqueId().toString()));
//        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("bans", "_id", event.getUniqueId().toString()));
//        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("homes", "_id", event.getUniqueId().toString()));
        //PlayerDataObject pdObject = ESHandler.runMultiIndexSearch(multiSearchRequest, new PlayerDataObject(event.getUniqueId()));
        PlayerDataObject pdObject = ESHandler.getPlayerData(event.getUniqueId().toString());
        
        //pdObject.spillData();
        if (pdObject != null) { //If data isnt null

            try {
                //If a user is banned
                //Below has some stupid converting bullshit because ElasticSearch returns Objects instead of their string type
                if (pdObject.isBanned()) { //If user is banned disallow joining and end the event

                    String bannedUntil = pdObject.isBannedUntil();
                    boolean stillBanned = LocalDateTime.parse(bannedUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME).isAfter(LocalDateTime.now());
//                    boolean stillBanned = !TimeParser.inPast(bannedUntil);

                    if (stillBanned || pdObject.isBanLocked()) {
                        String latestBanDate = pdObject.getLatestBanDate();
                        String message = "You have been banned for: "
                            + pdObject.getBanReason(latestBanDate).concat(".");
                        
                        //Verify if the user ban is longer than 10 years, if so then don't tell them when it expires
                        if (LocalDateTime.parse(bannedUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME).isBefore(LocalDateTime.now().plusYears(10L))) {
                            message += ". This ban will end on: " + TimeParser.getHumanReadableDateTime(bannedUntil) + ".";
                        }
                        //Verify that the user isn't ban locked. If they aren't then give them this message about appeals.
                        if (!pdObject.isBanLocked() && JawaCore.getPluginConfiguration("jawamaster.jawapermissions.JawaPermissions").contains("ban-appeal-suffix")) {
                            message += " " + JawaCore.getPluginConfiguration("jawamaster.jawapermissions.JawaPermissions").getString("ban-appeal-suffix", "");
                        }
                        //They are banned so no matter what disallow their fully joining.
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
                    } else {
                        //Player join will unban
                        pdObject.banExpired();
                        pdObject.registerPlayer();
                        pdObject.onJoinUpdate(event.getName(), event.getAddress().toString());
                    }
                    
                } else {
                    //register the player
                    pdObject.registerPlayer();
                    pdObject.onJoinUpdate(event.getName(), event.getAddress().toString());
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Malformed player data was detected attempting to fix.");
                if (JawaCore.debug) {
                    LOGGER.log(Level.INFO, "THIS IS NOT AN ERROR. THIS IS DEBUG FOR PLAYER DATA VALIDATION");
                    e.printStackTrace();
                }
                pdObject.validateData(event.getName(), event.getAddress().toString());
            }

        } else {
            pdObject = new PlayerDataObject(event.getUniqueId());
            pdObject.installPlayer(event.getName(), event.getAddress().toString());
        }
    }
}


