/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package net.jawasystems.jawacore.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jawasystems.jawacore.JawaCore;
import net.jawasystems.jawacore.PlayerManager;
import net.jawasystems.jawacore.dataobjects.PlayerDataObject;
import net.jawasystems.jawacore.events.PlayerDataIDReturn;
import net.jawasystems.jawacore.utils.ESRequestBuilder;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
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
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indexlifecycle.GetLifecyclePolicyRequest;
import org.elasticsearch.client.indexlifecycle.GetLifecyclePolicyResponse;
import org.elasticsearch.client.indexlifecycle.PutLifecyclePolicyRequest;
import org.elasticsearch.client.indices.CreateDataStreamRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetComponentTemplatesRequest;
import org.elasticsearch.client.indices.GetComponentTemplatesResponse;
import org.elasticsearch.client.indices.GetComposableIndexTemplateRequest;
import org.elasticsearch.client.indices.GetComposableIndexTemplatesResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutComponentTemplateRequest;
import org.elasticsearch.client.indices.PutComposableIndexTemplateRequest;
import org.elasticsearch.cluster.metadata.ComponentTemplate;
import org.elasticsearch.cluster.metadata.ComposableIndexTemplate;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
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

    private static CredentialsProvider credentialsProvider;
    
    private final static String handlerSlug = "[ESHandler] ";
    private static boolean esDebug;
    private final static String REDMESSAGEPLUG = ChatColor.RED + "> ";
    private final static String GREENMESSAGEPLUG = ChatColor.GREEN + "> ";
    
    private final static HashMap<String, String> indices = new HashMap();
    private final static HashMap<String, String> indicesRegistration = new HashMap();

    /** Use the actionable name of an index (ie players, homes, bans) to locate the 
     * literal name of the index (ie player-main, home-fbmp, etc).
     * @param index index actionable name (must be registered on startup)
     * @return index literal name
     */
    public static String getIndexByAction(String index){
        return indices.get(index);
    }
    
    /** Register an actionable index name with a literal index name
     * @param indexActionableName
     * @param indexLiteralName
     * @param pluginName
     * @return
     */
    public static boolean registerIndexLiteral(String indexActionableName, String indexLiteralName, String pluginName){
        if (!indices.containsKey(indexActionableName)){
            indices.put(indexActionableName, indexLiteralName);
            indicesRegistration.put(indexActionableName, pluginName);
            return true;
        } else {
            return false;
        }
    }
    
    /** Initialize the RestHighLevelClient and validate that it can communicate with
     * the ElasticSearch Database.Returns this state and sets the debug value
     * for ESDB operations.
     * @param host
     * @param port
     * @param user
     * @param password
     * @param debug
     * @return 
     */
    public static boolean startESHandler(String host, int port, String user, String password, boolean debug) {
        eshost = host;
        esport = port;
        
        LOGGER.log(Level.INFO, "Starting the ElasticSearch Database rest client on {0}:{1}.", new Object[]{eshost,esport});
        /*
        When configuring elasticsearch for credentials:
        In /etc/elasticsearch/elasticsearch.yml set:    
            xpack.security.enabled: true
        */
        
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        try {
            if (!JawaCore.plugin.getConfig().getBoolean("es-x-security", false)) {
                restClient = new RestHighLevelClient(RestClient.builder(new HttpHost(eshost, esport, "http"))
                        .setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder)
                                -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000)));
            } else {
                restClient = new RestHighLevelClient(RestClient.builder(new HttpHost(eshost, esport, "http"))
                        .setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder)
                                -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000)).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        httpClientBuilder.disableAuthCaching();
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                }));
            }
        } catch (ElasticsearchException e) {
            //LOGGER.log(Level.SEVERE, "Unable to connect to database");
            return false;
        }
                        
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
    
    
    /** Returns true if an index exists and false if it does not. If there is an
     * error then it return null;
     * @param index
     * @return 
     */
    public static boolean indexExists(String index) throws IOException{
        GetIndexRequest request = new GetIndexRequest(indices.get(index));
            return restClient.indices().exists(request, RequestOptions.DEFAULT);

    }
    
    /** Get a list of the indicies in the cluster.
     * @return 
     */
    public static Set<String> getAllIndices(){
        ClusterHealthRequest request = new ClusterHealthRequest();
        try {
            ClusterHealthResponse response = restClient.cluster().health(request, RequestOptions.DEFAULT);
            Set<String> indicesList = response.getIndices().keySet();
            return indicesList;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return new HashSet();
    }
    
    /** Create the specified index without any defined mappings. This creates
     * an ElasticSearch index with only defaults.
     * @param index The name of the index to create
     * @return 
     */
    public static boolean createIndex(String index){
        return createLiteralIndex(index, null, null);
    }
    
    /** Create the specified index with the defined mappings. This assumes all 
     * defaults other wise. 
     * @param index The name of the index to be created
     * @param mappings The mappings for the index
     * @return True if shard's were acknowledge start during index creation. False if not or on error.
     */
    public static boolean createIndex(String index, Map<String,Object> mappings){
        return createLiteralIndex(index, mappings, null);
    }
    
    /** Create the specified index with the defined mappings. This assumes all 
     * defaults other wise. 
     * @param index The name of the index to be created
     * @param indexSettings JSONObject with the settings (THIS DOESN'T WORK YET)
     * @return True if shard's were acknowledge start during index creation. False if not or on error.
     */
    public static boolean createIndex(String index, JSONObject indexSettings){
        return createLiteralIndex(index, null, indexSettings);
    }
    
    /** Create the specified index with the defined mappings and settings 
     * @param index The name of the index to be created
     * @param mappings The mappings for the index
     * @param indexSettings JSONObject with the settings (THIS DOESN'T WORK YET)
     * @return True if shard's were acknowledge start during index creation. False if not or on error.
     */
    public static boolean createIndex(String index, Map<String,Object> mappings, JSONObject indexSettings){
        return createLiteralIndex(index, mappings, indexSettings);
    }
    
    /** Create the specified index with the defined mappings and settings 
     * @param index The actionable name of the index to be created
     * @param mappings The mappings for the index
     * @param indexSettings JSONObject with the settings (THIS DOESN'T WORK YET)
     * @return True if shard's were acknowledge start during index creation. False if not or on error.
     */
    private static boolean createLiteralIndex(String index, Map<String,Object> mappings, JSONObject indexSettings){
        CreateIndexRequest indexRequest = new CreateIndexRequest(indices.get(index));
        if (mappings != null) indexRequest.mapping(mappings);
        if (indexSettings != null && !indexSettings.isEmpty()) {
            //do stuff
        }
        try {
            CreateIndexResponse response = restClient.indices().create(indexRequest, RequestOptions.DEFAULT);
            return response.isShardsAcknowledged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "An error occured attempting to create the index {0}. Stack trace follows:", index);
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
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
        GetRequest getRequest = new GetRequest(indices.get("players"), target.toString());
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");

        try {
            return restClient.exists(getRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            Logger.getLogger(ESHandler.class.getName()).log(Level.SEVERE, null, ex);
//            System.out.println("CHECK ELASTIC SEARCH!!");
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
        IndexRequest indexRequest = new IndexRequest(indices.get("players"))
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
                arg0.printStackTrace();
                //System.out.println(JawaCore.pluginSlug + targetUUID + "'s information update failed.");
            }
        });
    }

    /** This will run and Async bulk request and print generic indexing success
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

    /** This will return a PlayerDataObject containing all user data. If the player 
     * is not found this will return null. 
     * @param playerName A string of the player UUID
     * @return
     */
    public static PlayerDataObject findOfflinePlayer(String playerName) { //TODO clean up all fingOfflinePlayer methods
        
        try {
            SearchResponse sResponse = restClient.search(ESRequestBuilder.buildSearchRequest("players", "name", playerName), RequestOptions.DEFAULT);
            SearchHit[] hits = sResponse.getHits().getHits();
            if (hits.length != 1) {
                return null; //If 1 entry is not found then return null.
            }
            return new PlayerDataObject(UUID.fromString(hits[0].getId()),hits[0].getSourceAsMap(),null,null);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }
 
    /** Request a player data object be created from the database.
     * This will return the PlayerDataObject to the PlayerManager and set the request state.
     * If the player is not found the request state from the player manager will be set to false.
     * If the player is found, home and ban data will be retrieved, the PDO sent to the PlayerManager,
     * then the status of the request registered.
     * @param playerName the minecraft name that is being searched for
     * @param requestID the request ID generated by the requesting task
     */
    public static void findOfflinePlayerAsync(String playerName, int requestID) {
//        LOGGER.log(Level.INFO, "findOfflinePlayerAsync playername:" + playerName + " requestID:" + requestID);
        SearchRequest playerRequest = new SearchRequest(indices.get("players"));
        playerRequest.source(new SearchSourceBuilder().query(QueryBuilders.matchQuery("name", playerName)));
        restClient.searchAsync(playerRequest, RequestOptions.DEFAULT, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse response) {
//                LOGGER.log(Level.INFO, "findOfflinePlayerAsync success playername:" + playerName + " requestID:" + requestID);
                if (response.getHits().getHits().length == 1) {
                    findOfflinePlayerDataAsync(response.getHits().getHits()[0].getId(), requestID);
                } else {
                    synchronized (this) {
                        PlayerManager.setRequestStatus(requestID, false);
                    }
                }
            }

                @Override
                public void onFailure (Exception e) {
                    synchronized (this) {
                        LOGGER.log(Level.SEVERE, null, e);
                        PlayerManager.setRequestStatus(requestID, false);
                    }
                }
            }
        );
        
    }
    
    /** Async search for the player data, home data, and ban data after the player's
     * UUID has been retrieved
     * @param targetUUID the target UUID
     * @param requestID the Request id to send the data back to
     */
    private static void findOfflinePlayerDataAsync(String targetUUID, int requestID){
//           LOGGER.log(Level.INFO, "findOfflinePlayerDataAsync playerUUID:" + targetUUID + " requestID:" + requestID);

        MultiSearchRequest mrequest = new MultiSearchRequest();

        SearchRequest playerRequest = new SearchRequest(indices.get("players"));
        playerRequest.source(new SearchSourceBuilder().query(QueryBuilders.matchQuery("_id", targetUUID)));
        mrequest.add(playerRequest);

//        SearchRequest banRequest = new SearchRequest(indices.get("bans"));
//        banRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("uuid", targetUUID))
//                .filter(QueryBuilders.matchQuery("uuid", targetUUID))).size(100));
//        mrequest.add(banRequest);
//
//        //Search for player's homes
//        SearchRequest homeRequest = new SearchRequest(indices.get("homes"));
//        homeRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("owner", targetUUID)).mustNot(QueryBuilders.matchQuery("deleted", true))
//                .filter(QueryBuilders.matchQuery("server", JawaCore.getServerName()))).size(100));
//        
        //Search for player's bans
        SearchRequest banRequest = new SearchRequest(indices.get("bans"));
        banRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("uuid.keyword", targetUUID))).size(100));
//        banRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("uuid", targetUUID))
//                .filter(QueryBuilders.matchQuery("uuid", targetUUID))).size(100));
        mrequest.add(banRequest);

        //Search for player's homes
        SearchRequest homeRequest = new SearchRequest(indices.get("homes"));
        homeRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("owner.keyword", targetUUID)).mustNot(QueryBuilders.matchQuery("deleted", true))
                .filter(QueryBuilders.termQuery("server.keyword", JawaCore.getServerName()))).size(100));
//        homeRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("owner", targetUUID)).mustNot(QueryBuilders.matchQuery("deleted", true))
//                .filter(QueryBuilders.matchQuery("server", JawaCore.getServerName()))).size(100));
        
        mrequest.add(homeRequest);

        restClient.msearchAsync(mrequest, RequestOptions.DEFAULT, new ActionListener<MultiSearchResponse>() {
            @Override
            public void onResponse(MultiSearchResponse mresponse) {
                //SearchResponse response = Main.restClient.search(request, RequestOptions.DEFAULT);
                if (JawaCore.debug) LOGGER.log(Level.INFO, "findOfflinePlayerDataAsync success playername:{0} requestID:{1}", new Object[]{targetUUID, requestID});
                Map<String, Object> playerData = new HashMap();
                Map<String, JSONObject> homeData = new HashMap();
                Map<String, JSONObject> banData = new HashMap();
                for (MultiSearchResponse.Item hit : mresponse.getResponses()) {
                    if (hit.getResponse().getHits().getHits().length > 0) {
                        for (SearchHit searchHit : hit.getResponse().getHits().getHits()) {
                            if (indices.get("players").equalsIgnoreCase(searchHit.getIndex())) {
                                playerData = searchHit.getSourceAsMap();
                            } else if (indices.get("bans").equalsIgnoreCase(searchHit.getIndex())) {
                                banData.put(searchHit.getId(), new JSONObject(searchHit.getSourceAsMap()));
                            } else if (indices.get("homes").equalsIgnoreCase(searchHit.getIndex())) {
                                homeData.put(searchHit.getId(), new JSONObject(searchHit.getSourceAsMap()));
                            }
                        }
                    }
                }

                //return new PlayerDataObject(UUID.fromString(targetUUID), playerData, bans, homeData);
                PlayerDataObject pdo = new PlayerDataObject(UUID.fromString(targetUUID), playerData, banData, homeData);
                if (JawaCore.debug) LOGGER.log(Level.INFO, "findOfflinePlayerDataAsync created pdo playername:{0} requestID:{1} playerdata:{2} homedata:{3} banData:{4}", new Object[]{targetUUID, requestID, !playerData.isEmpty(), !homeData.isEmpty(), !banData.isEmpty()});
                synchronized (this) {
                    PlayerManager.cacheOfflinePlayer(pdo);
                    PlayerManager.setRequestStatus(requestID, true);
//                    LOGGER.log(Level.INFO, "findOfflinePlayerDataAsync caching and setting status playername:" + targetUUID + " requestID:" + requestID);
                }
            }

            @Override
            public void onFailure(Exception e) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        });


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
    
    /** Issues a single document update async
     * @param request The update request to run async
     */
    public static void singleAsyncUpdateRequest(UpdateRequest request) {
            restClient.updateAsync(request, RequestOptions.DEFAULT, new ActionListener<UpdateResponse>() {
                @Override
                public void onResponse(UpdateResponse response) {
                    if (esDebug) LOGGER.log(Level.INFO, "singleAsyncUpdateRequest successfull for ID: {0} in index: {1}", new Object[]{response.getId(), response.getIndex()});
                    //System.out.println("singleAsyncUpdateRequest completed");
                }

                @Override
                public void onFailure(Exception e) {
                    if (e.getMessage().contains("type=version_conflict_engine_exception")) {
                        LOGGER.log(Level.SEVERE, "singleAsyncUpdateRequest failed with a version conflict. Retrying...");
                        retryAsyncUpdateRequest(request, 0);
                    } else {
                        LOGGER.log(Level.SEVERE, "singleAsyncUpdateRequest failed");
                    }
                }
            });
    }
    
    private static void retryAsyncUpdateRequest(UpdateRequest request, int retryAttempt){
        restClient.updateAsync(request, RequestOptions.DEFAULT, new ActionListener<UpdateResponse>() {
                @Override
                public void onResponse(UpdateResponse response) {
                    if (esDebug) LOGGER.log(Level.INFO, "retryAsyncUpdateRequest successfull for ID: {0} in index: {1}", new Object[]{response.getId(), response.getIndex()});
                    //System.out.println("singleAsyncUpdateRequest completed");
                }

                @Override
                public void onFailure(Exception e) {
                    boolean versionEx = e.getMessage().contains("type=version_conflict_engine_exception");
                    LOGGER.log(Level.SEVERE, "retryAsyncUpdateRequest failed for version conflict: " + versionEx + " retry count: " + retryAttempt);
                    //LOGGER.log(Level.INFO, "Type is exception:" + e.getMessage().contains("type=version_conflict_engine_exception"));
                    if (versionEx && retryAttempt < 3) {
                        retryAsyncUpdateRequest(request, retryAttempt + 1);
                    } else {
                        LOGGER.log(Level.SEVERE, "singleAsyncUpdateRequest failed and can no longer be retried");
                    }
                    
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
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Perform a single Asynchronous request.
     * @param indexRequest 
     */
    public static void runAsyncSingleIndexRequest(IndexRequest indexRequest) {

        restClient.indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse response) {
                if (JawaCore.debug) LOGGER.log(Level.INFO, "Single Async Index Request completed successfully");
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.log(Level.SEVERE, "Single Async Index Request failed. Stack trace follows:");
                LOGGER.log(Level.SEVERE, null, e);
            }
        });
        

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

    /** Return a PlayerDataObject with located player data. If data is not located
     * null is returned.
     * @param target Player object.
     * @return PlayerDataObject. If player data is not found null.
     */
    public static PlayerDataObject getPlayerData(Player target) {
        return getPlayerData(target.getUniqueId().toString());
    }

    /** Return a PlayerDataObject with located player data. If data is not located
     * null is returned.
     * @param targetUUID Player uuid in string form.
     * @return PlayerDataObject. If player data is not found null.
     */
    public static PlayerDataObject getPlayerData(String targetUUID) {
        
        MultiSearchRequest mrequest = new MultiSearchRequest();

        //Search for player
        SearchRequest playerRequest = new SearchRequest(indices.get("players"));
        playerRequest.source(new SearchSourceBuilder().query(QueryBuilders.termQuery("_id", targetUUID)));
        mrequest.add(playerRequest);

        //Search for player's bans
        SearchRequest banRequest = new SearchRequest(indices.get("bans"));
        banRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("uuid.keyword", targetUUID))).size(100));
//        banRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("uuid", targetUUID))
//                .filter(QueryBuilders.matchQuery("uuid", targetUUID))).size(100));
        mrequest.add(banRequest);

        //Search for player's homes
        SearchRequest homeRequest = new SearchRequest(indices.get("homes"));
        homeRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("owner.keyword", targetUUID)).mustNot(QueryBuilders.matchQuery("deleted", true))
                .filter(QueryBuilders.termQuery("server.keyword", JawaCore.getServerName()))).size(100));
//        homeRequest.source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("owner", targetUUID)).mustNot(QueryBuilders.matchQuery("deleted", true))
//                .filter(QueryBuilders.matchQuery("server", JawaCore.getServerName()))).size(100));
        mrequest.add(homeRequest);
        
        try {
            MultiSearchResponse response = restClient.msearch(mrequest, RequestOptions.DEFAULT);
            //SearchResponse response = Main.restClient.search(request, RequestOptions.DEFAULT);
            Map<String, Object> player = new HashMap();
            Map<String, JSONObject> homes = new HashMap();
            Map<String, JSONObject> bans = new HashMap();
            for (MultiSearchResponse.Item hit : response.getResponses()) {
                if (hit.getResponse().getHits().getHits().length > 0) {
                    for (SearchHit searchHit : hit.getResponse().getHits().getHits()) {
                        if (indices.get("players").equalsIgnoreCase(searchHit.getIndex())) {
                            player = searchHit.getSourceAsMap();
                        } else if (indices.get("bans").equalsIgnoreCase(searchHit.getIndex())) {
                            bans.put(searchHit.getId(), new JSONObject(searchHit.getSourceAsMap()));
                        } else if (indices.get("homes").equalsIgnoreCase(searchHit.getIndex())) {
                            homes.put(searchHit.getId(), new JSONObject(searchHit.getSourceAsMap()));
                        }
                    }
                }
            }
            
            if ( player.isEmpty() ){
                return null;
            }
            
            return new PlayerDataObject(UUID.fromString(targetUUID), player, bans, homes);

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
        
    }
    
    /** Searches for a value within an index. This should only be used when the queryValue
     * is unique to a document. (i.e. UUID, DiscordLink code, etc) If this value is not
     * unique this will return only the zeroth result within the hits. If no document is
     * found this will return null.
     * @param index
     * @param docValue
     * @param queryValue
     * @return
     */
    public static PlayerDataObject searchPlayerData(String index, String docValue, String queryValue) {
        
        try {
            MultiSearchResponse response = restClient.msearch(ESRequestBuilder.buildSingleMultiSearchRequest(index, docValue, queryValue), RequestOptions.DEFAULT);
//            System.out.println(response.getResponses()[0].getResponse().getHits().getHits().length);
            if (response.getResponses().length != 0) {
                return new PlayerDataObject(UUID.fromString(response.getResponses()[0].getResponse().getHits().getHits()[0].getId()),
                        response.getResponses()[0].getResponse().getHits().getHits()[0].getSourceAsMap(),
                        null,
                        null);
            } else {
                LOGGER.log(Level.INFO, "No ElasticSearch entry was found for{0} with a query value of {1} in the {2} index", new Object[]{docValue, queryValue, index});
                return null;
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }

    }
    
    /** Async data index and return an ID async
     * @param index the index's action name
     * @param source The JSONObject source to be indexed
     */
    public static void asyncDataIndexWithIDReturn(String index, JSONObject source){
        IndexRequest request = new IndexRequest(indices.get(index)).source(source.toMap());
        restClient.indexAsync(request, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse response) {
                if (esDebug) LOGGER.log(Level.INFO, "Data update with ID successfully indexed. Calling PlayerDataIDReturn event with index: {0} response ID: {1}", new Object[]{indices.get(index), response.getId()});
                Bukkit.getServer().getPluginManager().callEvent(new PlayerDataIDReturn(index, response.getId(), source));
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.log(Level.SEVERE,"asyncDataUpdateWithID failed");
            }
        });
        
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
    
//    public static SearchHit[] runAltSearch(MultiSearchRequest msRequest) {
//        MultiSearchResponse response = restClient.ms
//        
//    }
    
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
    
    /** Create a ComponentTemplate. Return true if the response is acknowledged but the user should still
     * test for successful creation. This will return false if there is an error or if the server does not
     * acknowledge the request. CAUTION: This runs within the main thread
     * @param request
     * @return True if acknowledged. False if not acknowledged or error.
     */
    public static boolean createComponentTemplate(PutComponentTemplateRequest request){
        try {
            AcknowledgedResponse response = restClient.cluster().putComponentTemplate(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Validate the existence and version of a component template. CAUTION: This runs within the main thread.
     * This is backed by checkComponentTemplate(GetComponentTemplatesRequest request, String componentTemplateName, Long version)
     * @param request GetComponentTemplatesRequest containing componentTemplateName and options
     * @param componentTemplateName Exact string name of the component template being checked
     * @return True if the template exists. False in all other cases including errors.
     */
    public static boolean checkComponentTemplate(GetComponentTemplatesRequest request, String componentTemplateName){
        return checkComponentTemplate(request, componentTemplateName, null);
    }
    
    /** Validate the existence and version of a component template. CAUTION: This runs within the main thread.
     * @param request GetComponentTemplatesRequest containing componentTemplateName and options
     * @param componentTemplateName Exact string name of the component template being checked
     * @param version Long version identifier of the component template. can be null and only existence will be checked
     * @return True if the template exists or the optional version matches. False in all other cases including errors.
     */
    public static boolean checkComponentTemplate(GetComponentTemplatesRequest request, String componentTemplateName, Long version){
        try {
            ComponentTemplate template;
            try {
                GetComponentTemplatesResponse response = restClient.cluster().getComponentTemplate(request, RequestOptions.DEFAULT);
                template = response.getComponentTemplates().get(componentTemplateName);
            } catch (ElasticsearchStatusException ex){
                template = null;
            }
            if (template != null ){
                if (version == null) {
                    //Validate existance only
                    return true;
                } else {
                    //Validate version
                    return version.equals(template.version());
                }
            } else {
                //Template does not exist
                return false;
            }
            
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Create an Index Template. Return true if the response is acknowledged but the user should still
     * test for successful creation. This will return false if there is an error or if the server does not
     * acknowledge the request. CAUTION: This runs within the main thread
     * @param request
     * @return True if acknowledged. False if not acknowledged or error.
     */
    static boolean createIndexTemplate(PutComposableIndexTemplateRequest request) {
        try {
            AcknowledgedResponse response = restClient.indices().putIndexTemplate(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Validate the existence and version of an index template. CAUTION: This runs within the main thread.
     * This is backed by checkIndexTemplate(GetComposableIndexTemplateRequest request, String indexTemplateName, Long version)
     * @param request GetComposableIndexTemplateRequest containing index template name and options
     * @param indexTemplateName Exact string name of the index template being checked
     * @return True if the template exists or the optional version matches. False in all other cases including errors.
     */
    public static boolean checkIndexTemplate(GetComposableIndexTemplateRequest request, String indexTemplateName){
        return checkIndexTemplate(request, indexTemplateName, null);
    }
    
    /** Validate the existence and version of an index template. CAUTION: This runs within the main thread.
     * @param request GetComposableIndexTemplateRequest containing index template name and options
     * @param indexTemplateName Exact string name of the index template being checked
     * @param version Long version identifier of the index template. can be null and only existence will be checked
     * @return True if the template exists or the optional version matches. False in all other cases including errors.
     */
    public static boolean checkIndexTemplate(GetComposableIndexTemplateRequest request, String indexTemplateName, Long version){
        try {
            ComposableIndexTemplate template;
            try {
                GetComposableIndexTemplatesResponse response = restClient.indices().getIndexTemplate(request, RequestOptions.DEFAULT);
                template = response.getIndexTemplates().get(indexTemplateName);
            } catch (ElasticsearchStatusException ex){
                template = null;
            }
            if (template != null ){
                if (version == null) {
                    //Validate existance only
                    return true;
                } else {
                    //Validate version
                    return version.equals(template.version());
                }
            } else {
                //Template does not exist
                return false;
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Create a Life cycle management policy. Return true if the response is acknowledged but the user should still
     * test for successful creation. This will return false if there is an error or if the server does not
     * acknowledge the request. CAUTION: This runs within the main thread
     * @param request
     * @return True if acknowledged. False if not acknowledged or error.
     */
    static boolean createLifecyclePolicy(PutLifecyclePolicyRequest request) {
        try {
            org.elasticsearch.client.core.AcknowledgedResponse response = restClient.indexLifecycle().putLifecyclePolicy(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Validate the existence and version of a life cycle policy. CAUTION: This runs within the main thread.
     * This is backed by checkIndexLifeCyclePolicy(GetLifecyclePolicyRequest request, String lifecyclePolicyName, Long version)
     * @param request GetLifecyclePolicyRequest containing policy name and options
     * @param lifecyclePolicyName Exact string name of the life cycle policy being checked
     * @return True if the life cycle policy exists or the optional version matches. False in all other cases including errors.
     */
    public static boolean checkIndexLifeCyclePolicy(GetLifecyclePolicyRequest request, String lifecyclePolicyName){
        return checkIndexLifeCyclePolicy(request, lifecyclePolicyName, null);
    }
    
    /** Validate the existence and version of a life cycle policy. CAUTION: This runs within the main thread.
     * @param request GetLifecyclePolicyRequest containing policy name and options
     * @param lifecyclePolicyName Exact string name of the life cycle policy being checked
     * @param version Long version identifier of the life cycle policy. Can be null and only existence will be checked
     * @return True if the life cycle policy exists or the optional version matches. False in all other cases including errors.
     */
    public static boolean checkIndexLifeCyclePolicy(GetLifecyclePolicyRequest request, String lifecyclePolicyName, Long version){
        try {
//            LifecyclePolicyMetadata policy;
            GetLifecyclePolicyRequest testRequest = new GetLifecyclePolicyRequest("RollingChatLogPolicy");
            GetLifecyclePolicyResponse response;
            response = restClient.indexLifecycle().getLifecyclePolicy(testRequest, RequestOptions.DEFAULT);
            
            if (!response.getPolicies().isEmpty() && response.getPolicies().containsKey(lifecyclePolicyName)){
                if (version == null) {
                    //Validate existance only
                    return true;
                } else {
                    //Validate version
                    return version.equals(response.getPolicies().get(lifecyclePolicyName).getVersion());
                }
            } else {
                //Template does not exist
                return false;
            }
        } catch (ElasticsearchStatusException | IOException ex) {
//            System.out.println(response);
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
        
    }
    
    /** Create a data stream. Return true if the response is acknowledged but the user should still
     * test for successful creation. This will return false if there is an error or if the server does not
     * acknowledge the request. CAUTION: This runs within the main thread
     * @param request CreateDataStreamRequest containing name and all needed settings
     * @return True if acknowledged. False if not acknowledged or error.
     */
    public static boolean createDataStream(CreateDataStreamRequest request){
        try {
            AcknowledgedResponse response = restClient.indices().createDataStream(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    } 

}
