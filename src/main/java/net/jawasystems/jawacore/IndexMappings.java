/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class IndexMappings {
    
    public static Map<String, Object> getPlayerIndexMappings(){
        Map<String, Object> playerIndexMap = new HashMap();
        
        Map<String, Object> topProperties = new HashMap();

        Map<String, Object> typeDate = new HashMap();
            typeDate.put("type", "date");

        Map<String, Object> typeText = new HashMap();
            typeText.put("type", "text");

        Map<String, Object> typeKeyword = new HashMap();
            typeKeyword.put("type", "keyword");
            
        Map<String, Object> typeBoolean = new HashMap();
            typeBoolean.put("type", "boolean");
            
        Map<String, Object> ignoreAbove = new HashMap();
            ignoreAbove.put("ignore_above", 256);
            
        Map<String, Object> dynamic = new HashMap();
            dynamic.put("dynamic", false);
            
        Map<String, Object> typeObject = new HashMap();
            typeObject.put("type", "object");
            
            Map<String, Object> homeData = new HashMap();
                Map<String, Object> homeProperties = new HashMap();
//                    Map<String, Object> mainObj = new HashMap();
//                        Map<String, Object> mainProp = new HashMap();
//                            mainProp.put("homeName", typeObject);
//                            mainProp.put("dynamic", false);
//                    mainObj.put("properties", mainProp);
//                homeProperties.put("main", mainObj);
                    Map<String, Object> homeFields = new HashMap();
                        homeFields.put("type", "object");
                    homeProperties.put("fields",homeFields);
                    //homeProperties.put("dynamic", "false");
            homeData.put("properties", homeProperties);
            homeData.put("dynamic", "false");
        
        topProperties.put("home-data", homeData);
            
        topProperties.put("last-login", typeDate);
        topProperties.put("last-logout", typeDate);
        
            Map<String, Object> star = new HashMap();
                star.put("type", "text");
                Map<String, Object> starFields = new HashMap();
                    Map<String, Object> starKeyword = new HashMap();
                        starKeyword.put("ignore_above", 256);
                        starKeyword.put("type","keyword");
                starFields.put("keyword", starKeyword);
                star.put("fields", starFields);
        
        topProperties.put("star", star);
        
            Map<String, Object> banData = new HashMap();
                Map<String, Object> banProperties = new HashMap();
                    banProperties.put("date", typeDate);
                    banProperties.put("unbanned-via-console", typeBoolean);
                    banProperties.put("ban-lock", typeBoolean);
                    banProperties.put("active", typeBoolean);
                    banProperties.put("via-console", typeBoolean);
                    banProperties.put("banned-until", typeDate);
                    banProperties.put("unbanned-on", typeDate);
                    
                        Map<String, Object> reason = new HashMap();
                            reason.put("type", "text");
                                Map<String, Object> reasonFields = new HashMap();
                                    Map<String, Object> reasonKeyword = new HashMap();
                                        reasonKeyword.put("ignore_above", 256);
                                        reasonKeyword.put("type", "keyword");
                                reasonFields.put("keyword", reasonKeyword);
                            reason.put("fields", reasonFields);
                    banProperties.put("reason", reason);
                    banProperties.put("banned-by", reason);
                    banProperties.put("unbanned-by", reason);
            banData.put("properties", banProperties);
        topProperties.put("ban-data", banData);
        
            Map<String, Object> adminComments = new HashMap();
                Map<String, Object> adminCommentsProperties = new HashMap();
                    banProperties.put("DATE", typeDate);
                    banProperties.put("COMMENT", reason);
                    banProperties.put("ADMIN", reason);
            adminComments.put("properties", adminCommentsProperties);
        topProperties.put("admin-comments", reason);
        
        topProperties.put("ips", reason);
        topProperties.put("first-login", typeDate);
        topProperties.put("name-data", reason);
        topProperties.put("rank-data", reason);
        topProperties.put("nick", reason);
            
            Map<String, Object> rankData = new HashMap();
                Map<String, Object> rankProperties = new HashMap();
                    rankProperties.put("date", typeDate);
                    rankProperties.put("from-rank", reason);
                    rankProperties.put("to-rank", reason);
                    rankProperties.put("changed-by", reason);
                rankData.put("properties", rankProperties);
        topProperties.put("rank-data", rankData);
        topProperties.put("name", reason);
        topProperties.put("rank", reason);
        topProperties.put("banned", typeBoolean);
        topProperties.put("tag", reason);
        topProperties.put("nick-data", reason);
        
        playerIndexMap.put("properties", topProperties);
        return playerIndexMap;
    }
}
