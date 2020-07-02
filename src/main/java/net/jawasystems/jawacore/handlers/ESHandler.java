/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package net.jawasystems.jawacore.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.json.JSONObject;

/**
 *
 * @author Arthur Bulin
 */
public class ESHandler {
    private static final Logger LOGGER = Logger.getLogger("ESHandler");

    private static RestHighLevelClient restClient;
    private static String eshost;
    private static int esport;

    private final static String handlerSlug = "[ESHandler] ";
    private static boolean esDebug;
    private final static String REDMESSAGEPLUG = ChatColor.RED + "> ";
    private final static String GREENMESSAGEPLUG = ChatColor.GREEN + "> ";

    /** Initialize the RestHighLevelClient and validate that it can communicate with
     * the ElasticSearch Database. Returns this state and sets the debug value
     * for ESDB operations.
     * @param host
     * @param port
     * @param debug
     * @return 
     */
    public static boolean startESHandler(String host, int port, boolean debug) {
        eshost = host;
        esport = port;
        
        LOGGER.log(Level.INFO, "Starting the ElasticSearch Database rest client on {0}:{1}.", new Object[]{eshost,esport});
        
        //TODO add credentials handling
        restClient = new RestHighLevelClient(RestClient.builder(new HttpHost(eshost, esport, "http"))
                .setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder)
                        -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000)));
        boolean restPing = false;
        try {
            restPing = restClient.ping(RequestOptions.DEFAULT);
            //LOGGER.log(Level.INFO, "debug: {0}, eshost: {1}, esport: {2}", new Object[]{debug, eshost, esport});
            LOGGER.log(Level.INFO, "ElasticSearch database pings: {0}", restPing);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        //Logger.getLogger(JawaCore.class.getName()).log(Level.INFO, "Constructing the ElasticSearch Database communications handler (ESHandler)");
        esDebug = debug;
        
        return restPing;
    }
    
    public static void shutdown(){
        try {
            restClient.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Will search the Elasticsearch database syncrounously to find if a user
     * has already been indexed. If their UUID exists in the index it will
     * return true.
     *
     * @param target
     * @return
     */
    public static boolean alreadyIndexed(UUID target) {
        GetRequest getRequest = new GetRequest("players", target.toString());
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");

        try {
            return restClient.exists(getRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("CHECK ELASTIC SEARCH!!");
            return false;
        }
    }

    /**
     * Indexes the player's data to the ElasticSearch database. This should only
     * be used to add a new player to the database. This method constructs the
     * request and passes it to generalIndex(indexRequest) to process the actual
     * indexing.
     *
     * @param target
     * @param playerData
     * @return
     */
    public static boolean indexPlayerData(UUID target, JSONObject playerData) { //TODO evaluate if this can be replaced with the asyncUpdateData or a more generic deleteRequest
        //indexRequest = new IndexRequest("mc", "players", target.toString()).source(playerData);
        IndexRequest indexRequest = new IndexRequest("players")
                .id(target.toString())
                .source(playerData.toMap());
        restClient.indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                LOGGER.log(Level.INFO,"{0}:{1} has been indexed successfully.", new Object[]{playerData.getString("name"),target.toString()});
                //System.out.println(JawaCore.pluginSlug + handlerSlug + playerData.get("name") + " has been indexed successfully.");
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });

        return true;
    }

    public static void asyncUpdateData(UUID targetUUID, JSONObject updateData, String index) {
        UpdateRequest updateRequest = ESRequestBuilder.updateRequestBuilder(updateData, index, targetUUID.toString(), true);

        restClient.updateAsync(updateRequest, RequestOptions.DEFAULT, new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
                LOGGER.log(Level.INFO, "Details have been updated for {0}.", targetUUID.toString());
                //System.out.println(JawaCore.pluginSlug + " " + targetUUID + "'s details have been updated.");
            }

            @Override
            public void onFailure(Exception arg0) {
                LOGGER.log(Level.SEVERE, "Detail updated has FAILED for {0}.", targetUUID.toString());
                //System.out.println(JawaCore.pluginSlug + targetUUID + "'s information update failed.");
            }
        });
    }

    /**
     * This will run and Async bulk request and print generic indexing success
     * or failure messages to the commandSender. TODO later this will accept a
     * message object so that good response can be sent to the players and
     * server without having to overload the hell out of the call.
     *
     * @param request
     * @param messageToo
     */
    public static void runAsyncBulkRequest(BulkRequest request, CommandSender messageToo) {
        restClient.bulkAsync(request, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse arg0) {
                for (BulkItemResponse resp : arg0.getItems()) {
                    if (!resp.isFailed()) {
                        messageToo.sendMessage(GREENMESSAGEPLUG + "Request to index: " + resp.getIndex() + " for _id: " + resp.getId() + " has been successful!");
                    } else {
                        messageToo.sendMessage(REDMESSAGEPLUG + "Request to index: " + resp.getIndex() + " for _id: " + resp.getId() + " has failed!!!");
                        messageToo.sendMessage(REDMESSAGEPLUG + "Failure Message: " + resp.getFailureMessage());
                        messageToo.sendMessage(REDMESSAGEPLUG + "Give your technical administrator this id to trace the error in the log: " + resp.toString());
                        LOGGER.log(Level.SEVERE, "runAsyncBulkRequest failure!! \nResponse: {0} \nFailure Message: {1}", new Object[]{resp.toString(),resp.getFailureMessage()});
                        //System.out.println(resp.toString() + ": runAsyncBulkRequest failure!! FailureMessage: " + resp.getFailureMessage());
                    }
                }
            }

            @Override
            public void onFailure(Exception arg0) {
                LOGGER.log(Level.SEVERE, "runArynxBulkRequest has encountered a SEVERE error, this is likely a database issue.");
//                System.out.println(JawaCore.pluginSlug + handlerSlug + "Something severe happend in runAsyncBulkRequest!!");
//                System.out.println(JawaCore.pluginSlug + handlerSlug + "Exception:");
                LOGGER.log(Level.SEVERE, null, arg0);
            }
        });
    }

    /**
     * Executes a multi index search and returns the information in a
     * PlayerDataObject.
     *
     * @param multiSearchRequest
     * @param pdObject
     * @return
     */
    public static PlayerDataObject runMultiIndexSearch(MultiSearchRequest multiSearchRequest, PlayerDataObject pdObject) {

        try {
            MultiSearchResponse mSResponse = restClient.msearch(multiSearchRequest, RequestOptions.DEFAULT);
            for (MultiSearchResponse.Item resp : mSResponse.getResponses()) {
                //if it isnt a failure add it
                if (!resp.isFailure() && (resp.getResponse().getHits().getHits().length == 1)) {
                    pdObject.addSearchData(resp.getResponse().getHits().getHits()[0].getIndex(), resp.getResponse().getHits().getHits()[0].getSourceAsMap());
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "runMultiIndexSearch has encountred a SEVERE exception. This is likely a database issue.");
            //System.out.println(JawaCore.pluginSlug + handlerSlug + "Something severe happend in runMultiIndexSearch!!");
            //System.out.println(JawaCore.pluginSlug + handlerSlug + "Exception:");
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return pdObject;
    }

    /**
     * This allows a findOfflinePlayer call that moves player existence checking
     * to this method. This method is backed by findOfflinePlayer(String
     * target).
     *
     * @param target
     * @param commandSender
     * @return
     */
    public static SearchHit findOfflinePlayer(String target, CommandSender commandSender) {
        SearchHit hits = findOfflinePlayer(target);
        if (hits == null) {
            commandSender.sendMessage("That target player was not found the Elastic Database. Please be sure to use the exact minecraft name and not their nickname!");
            return null;
        } else {
            return hits;
        }
    }

    /**
     * Will return the UUID of an offline player. If the information is not
     * found this will return null.
     *
     * @param target
     * @return
     */
    public static SearchHit findOfflinePlayer(String target) {
        try {
            SearchRequest playerSearchRequest = new SearchRequest("players");
            SearchSourceBuilder playerSearchSourceBuilder = new SearchSourceBuilder();

            playerSearchSourceBuilder.query(QueryBuilders.matchQuery("name", target));
            playerSearchRequest.source(playerSearchSourceBuilder);

            SearchResponse playerSearchResponse = restClient.search(playerSearchRequest, RequestOptions.DEFAULT);

            if (JawaCore.debug) {
                LOGGER.log(Level.INFO, "Debug Search Response: {0}", playerSearchResponse);
                //System.out.print(JawaCore.pluginSlug + handlerSlug + " Search Response: " + playerSearchResponse);
            }
            SearchHit[] hits = playerSearchResponse.getHits().getHits();
            if (hits.length != 1) {
                return null;
            }

            return hits[0];
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

    /**
     * This will return a PlayerDataObject containing all user data. If the
     * player is not found this will return null
     *
     * @param ident
     * @param getData
     * @return
     */
    public static PlayerDataObject findOfflinePlayer(String ident, boolean getData) { //TODO clean up all fingOfflinePlayer methods
        PlayerDataObject pdObject;
        if (getData) {
            SearchResponse sResponse;
            try {
                sResponse = restClient.search(ESRequestBuilder.buildSearchRequest("players", "name", ident), RequestOptions.DEFAULT);
                SearchHit[] hits = sResponse.getHits().getHits();
                if (hits.length != 1) {
                    return null; //If 1 entry is not found then return null.
                }
                pdObject = new PlayerDataObject(UUID.fromString(hits[0].getId()));
                pdObject.addPlayerData(hits[0].getSourceAsMap());
            } catch (IOException ex) {
                Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }

        } else {
            SearchHit hit = findOfflinePlayer(ident);
            pdObject = new PlayerDataObject(UUID.fromString(hit.getId()));
            pdObject.addPlayerData(findOfflinePlayer(ident).getSourceAsMap());
        }
        return pdObject;

    }

    public static boolean singleUpdateRequest(UpdateRequest request) {
        try {
            UpdateResponse response = restClient.update(request, RequestOptions.DEFAULT);
            //System.out.println(response);
            if ((response.getResult() == Result.CREATED) || (response.getResult() == Result.UPDATED)) {
                return true;
            } else {
                return false;
            }

        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public static void singleAsyncUpdateRequest(UpdateRequest request) {
            restClient.updateAsync(request, RequestOptions.DEFAULT, new ActionListener<UpdateResponse>() {
                @Override
                public void onResponse(UpdateResponse response) {
                    //System.out.println("singleAsyncUpdateRequest completed");
                }

                @Override
                public void onFailure(Exception e) {
                    System.out.println("singleAsyncUpdateRequest failed");
                }
            });

    }

    public static SearchHit[] runSearchRequest(SearchRequest request) {
        try {
            SearchResponse response = restClient.search(request, RequestOptions.DEFAULT);
            return response.getHits().getHits();
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static boolean runSingleIndexRequest(IndexRequest indexRequest) {
        try {
            IndexResponse response = restClient.index(indexRequest, RequestOptions.DEFAULT);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static boolean runSingleDocumentDeleteRequest(DeleteRequest deleteRequest) {
        try {
            DeleteResponse response = restClient.delete(deleteRequest, RequestOptions.DEFAULT);
            return response.getResult().equals(Result.DELETED);
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static MultiSearchResponse runMultiSearchRequest(MultiSearchRequest request) {
        try {
            return restClient.msearch(request, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static PlayerDataObject getPlayerData(Player target) throws IOException {
        return getPlayerData(target.getUniqueId().toString());
    }

    public static PlayerDataObject getPlayerData(String target) throws IOException {
        PlayerDataObject pdObject = new PlayerDataObject(UUID.fromString(target));
        SearchResponse sResponse = restClient.search(ESRequestBuilder.buildSearchRequest("players", "_id", target), RequestOptions.DEFAULT);
        SearchHit[] hits = sResponse.getHits().getHits();
        pdObject.addPlayerData(hits[0].getSourceAsMap());
        return pdObject;
    }
    
    /** Searches for a value within an index. This should only be used when the queryValue
     * is unique to a document. (i.e. UUID, DiscordLink code, etc) If this value is not
     * unique this will return only the zeroth result within the hits. If no document is
     * found this will return null.
     * @param index
     * @param docValue
     * @param queryValue
     * @return
     * @throws IOException 
     */
    public static PlayerDataObject getPlayerData(String index, String docValue, String queryValue) {
        MultiSearchResponse response;
        try {
            response = restClient.msearch(ESRequestBuilder.buildSingleMultiSearchRequest(index, docValue, queryValue), RequestOptions.DEFAULT);
            System.out.println(response.getResponses()[0].getResponse().getHits().getHits().length);
            if (response.getResponses().length != 0) {
                //System.out.println(response.getResponses());
                Map<String, Object> data = response.getResponses()[0].getResponse().getHits().getHits()[0].getSourceAsMap();
                UUID uuid = UUID.fromString(response.getResponses()[0].getResponse().getHits().getHits()[0].getId());
                PlayerDataObject pdObject = new PlayerDataObject(uuid);
                pdObject.addPlayerData(data);
                return pdObject;
            } else {
                Logger.getLogger(handlerSlug).log(Level.INFO, "No ElasticSearch entry was found for{0} with a query value of {1} in the {2} index", new Object[]{docValue, queryValue, index});
                return null;
            }
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

//    public static SearchHit[] runAltSearch(CommandSender commandSender, String player){
//        Player target = JawaCore.plugin.getServer().getPlayer(player);
//        if (target == null){
//            PlayerDataObject pdObject = findOfflinePlayer(player, true);
//            if (pdObject == null ) return null; //This means failure to find player
//            return runAltSearch(commandSender, pdObject.getPlayerUUID());
//        } else {
//            return runAltSearch(commandSender, target.getUniqueId());
//        }
//        
//    }
    
//    public static SearchHit[] runAltSearch(CommandSender sender, UUID uuid) {
//        try {
//            SearchResponse response = restClient.search(deleteRequest, RequestOptions.DEFAULT);
//            return response.getHits().getHits();
//        } catch (IOException ex) {
//            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
//    }

    public static SearchHit[] runAltSearch(String ip){
        
        try {
            SearchResponse response = restClient.search(ESRequestBuilder.altSearchRequest(ip), RequestOptions.DEFAULT);
            return response.getHits().getHits();
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static void correctMalformedField(String field, String index, UUID uuid) {
        UpdateRequest deleteRequest = new UpdateRequest();
        Script scr = new Script("ctx._source.remove('" + field + "')");
        deleteRequest.index(index).id(uuid.toString()).script(scr);

        // UpdateRequest updateRequest = ESRequestBuilder.updateRequestBuilder(updateData, index, uuid.toString(), true);
        try {
            System.out.println("[ESHandler] Attempting to correct malformed data for " + uuid.toString());
            UpdateResponse response = restClient.update(deleteRequest, RequestOptions.DEFAULT); //Delete the field
            System.out.println("[ESHandler] Field delete result: " + response.getResult());

//                    response = restClient.update(updateRequest, RequestOptions.DEFAULT);
//                    System.out.println("[ESHandler] Field update result: " + response.getResult());
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
//        Bukkit.getServer().getScheduler().runTaskAsynchronously(JawaCore.plugin, new Consumer<BukkitTask>() {
//            @Override
//            public void accept(BukkitTask t) {
//
//            }
//
//        });
    }

}
