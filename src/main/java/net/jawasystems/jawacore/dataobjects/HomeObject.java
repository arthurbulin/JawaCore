/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.dataobjects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.JSONObject;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class HomeObject {
    
    private final String NAME;
    private final Location LOCATION;
    private final JSONObject JSONOBJECT;
    
    /** Construct the HomeObject from an already saved home in the form of a JSON object
     * @param homeData JSONobject with save home data
     */
    public HomeObject(JSONObject homeData){
        this.NAME = homeData.getString("name");
        
        this.LOCATION = new Location(
                Bukkit.getWorld(homeData.getString("world")), 
                homeData.getDouble("X"), 
                homeData.getDouble("Y"), 
                homeData.getDouble("Z"), 
                homeData.getFloat("YAW"), 
                homeData.getFloat("pitch"));
        
        this.JSONOBJECT = new JSONObject(homeData);
    }
    
    /** Construct the HomeObject from raw location pieces.
     * @param name String representing the name of the home
     * @param world bukkit world where the home exists
     * @param pitch float value for pitch
     * @param yaw float value for yaw
     * @param x double value for X-coordinate
     * @param y double value for Y-coordinate
     * @param z double valye for Z-coordinate
     */
    public HomeObject(String name, World world, float pitch, float yaw, double x, double y, double z){
        this.NAME = name;
        
        this.LOCATION = new Location(world, x, y, z, yaw, pitch);
        
        this.JSONOBJECT = new JSONObject();
        this.JSONOBJECT.put("name", this.NAME);
        this.JSONOBJECT.put("world", world.getName());
        
        this.JSONOBJECT.put("X", x);
        this.JSONOBJECT.put("Y", y);
        this.JSONOBJECT.put("Z", z);
        
        this.JSONOBJECT.put("PITCH", pitch);
        this.JSONOBJECT.put("YAW", yaw);
    }
    
    /** Create a new home from the player's location.
     * @param player Player creating the home
     * @param name Name for the home
     */
    public HomeObject(Player player, String name){
        this.NAME = name;
        
        this.LOCATION = player.getLocation();
        
        this.JSONOBJECT = new JSONObject();
        this.JSONOBJECT.put("name", name);
        this.JSONOBJECT.put("world", LOCATION.getWorld().getName());
        
        this.JSONOBJECT.put("X", LOCATION.getX());
        this.JSONOBJECT.put("Y", LOCATION.getY());
        this.JSONOBJECT.put("Z", LOCATION.getZ());
        
        this.JSONOBJECT.put("PITCH", LOCATION.getPitch());
        this.JSONOBJECT.put("YAW", LOCATION.getYaw());
    }
    
    /** Gets the location object for this home.
     * @return 
     */
    public Location getLocation(){
        return this.LOCATION;
    }
    
    /** Get the name for this home.
     * @return 
     */
    public String getName(){
        return this.NAME;
    }
    
    /** Return the JSON representation of the home.
     * @return 
     */
    public JSONObject getJSON(){
        return this.JSONOBJECT;
    }
    
    /** Get the X coordinate.
     * @return 
     */
    public double getX(){
        return LOCATION.getX();
    }
    
    /** Get the Y coordinate.
     * @return 
     */
    public double getY(){
        return LOCATION.getY();
    }
    
    /** Get the Z coordinate.
     * @return 
     */
    public double getZ(){
        return LOCATION.getZ();
    }
    
    /** Get the Pitch.
     * @return 
     */
    public float getPitch(){
        return LOCATION.getPitch();
    }
    
    /** Get the Yaw.
     * @return 
     */
    public double getFloat(){
        return LOCATION.getYaw();
    }
    
    /** Get the world.
     * @return 
     */
    public World getWorld(){
        return LOCATION.getWorld();
    }
    
    /** Get the world name.
     * @return 
     */
    public String getWorldName(){
        return LOCATION.getWorld().getName();
    }
    
}
