/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.listeners;

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

    @EventHandler
    public static void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        //Create a new blank PlayerDataObject
        //Populate it with the player's index information
        //pdObject.updateDataContents();
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("players", "_id", event.getUniqueId().toString()));
        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("bans", "_id", event.getUniqueId().toString()));
        multiSearchRequest.add(ESRequestBuilder.buildSearchRequest("homes", "_id", event.getUniqueId().toString()));
        PlayerDataObject pdObject = ESHandler.runMultiIndexSearch(multiSearchRequest, new PlayerDataObject(event.getUniqueId()));

        //pdObject.spillData();
        if (pdObject.containsPlayerData()) { //If data isnt null

            try {
                //If a user is banned
                //Below has some stupid converting bullshit because ElasticSearch returns Objects instead of their string type
                if (pdObject.isBanned()) { //If user is banned disallow joining and end the event

                    String bannedUntil = pdObject.isBannedUntil();
                    boolean stillBanned = !TimeParser.inPast(bannedUntil);

                    String message = "You have been banned for: "
                            + pdObject.getBanReason(pdObject.getLatestBanDate());

                    if (stillBanned) {
                        if (!"forever".equals(bannedUntil)) {
                            message += ". This ban will end on: " + TimeParser.getHumanReadableDateTime(bannedUntil);
                        }
                        if (!pdObject.isBanLocked() && JawaCore.getPluginConfiguration("jawamaster.jawapermissions.JawaPermissions").contains("ban-appeal-suffix")) {
                            message += " " + JawaCore.getPluginConfiguration("jawamaster.jawapermissions.JawaPermissions").getString("ban-appeal-suffix", "");
                        }
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
                    } else {
                        pdObject.banExpired();
                        pdObject.registerPlayer();
                    }

                    //Player join will unban
                } else {
                    //register the player
                    pdObject.registerPlayer();
                    pdObject.onJoinUpdate(event.getName(), event.getAddress().toString());
                }
            } catch (Exception e) {
                Logger.getLogger("PlayerPreJoin").warning("Malformed player data was detected attempting to fix.");
                pdObject.validateData(event.getName(), event.getAddress().toString());
            }

        } else {
            pdObject.installPlayer(event.getName(), event.getAddress().toString());
        }
    }
}


