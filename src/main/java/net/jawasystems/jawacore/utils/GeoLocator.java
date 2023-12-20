/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class GeoLocator {
    private static final Logger LOGGER = Logger.getLogger("GeoLocator");
    private static DatabaseReader dbReader;
    
    /** Load the MaxMind DataBase from location.
     * @param location Location of the MaxMind GeoIP database
     * @return true if the database was successfully loaded into a database reader, else false
     */
    public boolean loadDataBase(String location){
        File database = new File(location);
        try {
            dbReader = new DatabaseReader.Builder(database).build();
            return true;
        } catch (IOException ex) {
            //Logger.getLogger(GeoLocator.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /** Returns a JSONObject containing the Country, City, Postal code, State, Long, and Lat
     * @param inetAddress
     * @return 
     */
    public JSONObject getLocation(String inetAddress) {
        JSONObject location = new JSONObject();
        try {
            CityResponse response = dbReader.city(InetAddress.getByName(inetAddress));
            
            location.put("COUNTRY", response.getCountry().getName());
            location.put("CITY", response.getCity().getName());
            location.put("POSTAL", response.getPostal().getCode());
            location.put("STATE", response.getLeastSpecificSubdivision().getName());
            location.put("LONGITUDE", response.getLocation().getLongitude());
            location.put("LATITUDE", response.getLocation().getLatitude());
            
            return location;
        } catch (IOException | GeoIp2Exception ex) {
            return location;
            //Logger.getLogger(GeoLocator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
