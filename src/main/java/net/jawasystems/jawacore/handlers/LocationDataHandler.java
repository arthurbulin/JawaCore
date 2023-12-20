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
package net.jawasystems.jawacore.handlers;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.json.JSONObject;

/**
 *
 * @author alexander
 */
public class LocationDataHandler {
    
    /** Will return a Bukkit Location Object from the JSONData Object.
     * @param locationData
     * @return 
     */
    public static Location unpackLocation(JSONObject locationData){
        
        World world = Bukkit.getServer().getWorld(locationData.getString("world"));
        
        double X = locationData.getDouble("X");
        double Y = locationData.getDouble("Y");
        double Z = locationData.getDouble("Z");
        
        float yaw = locationData.getFloat("yaw");
        float pitch = locationData.getFloat("pitch");
        
        return new Location(world, X, Y, Z, yaw, pitch);
    }
    
    /** Will return a JSONObject containing all the information within the Bukkit Location
     * Object in readable JSON form.
     * @param location
     * @return 
     */
    public static JSONObject packLocation(Location location){
        JSONObject packedLocation = new JSONObject();
        packedLocation.put("world", location.getWorld().getName());
        packedLocation.put("X", location.getX());
        packedLocation.put("Y", location.getY());
        packedLocation.put("Z", location.getZ());
        
        packedLocation.put("pitch", location.getPitch());
        packedLocation.put("yaw", location.getYaw());
        
        return packedLocation;
    }
    
    /** Will return a top level JSONObject for home data. This means it will 
     * return a nested JSONObject with the uppermost key being the homename and 
     * the locationdata being in the nested JSONObject.
     * @param location
     * @param homeName
     * @return 
     */
    public static JSONObject createTopLevelHomeObject(Location location, String homeName){
        JSONObject topLevel = new JSONObject();
        topLevel.put(homeName, packLocation(location));
        return topLevel;
    }
    
    /** This will teleport an online player to their bed location if they have 
     * one set and return true. If not it will return false. The player's online
     * status should be checked prior to calling this method as it has no null
     * checking built in.
     * @param player
     * @return 
     */
    public static boolean sendToBed(Player player){
        Location bed = player.getBedSpawnLocation();
        if (bed == null) return false;
        else { 
            player.teleport(bed);
            return true;
        }
        
    }
    
    /** This will attempt to unpack a rawLocation of the format x,y,z,yaw,pitch into a valid location.
     * @param rawLocation the x,y,z,yaw,pitch
     * @param world
     * @return 
     */
    public static Location getLocation(String[] rawLocation, World world){
        try {
            Location newLoc = new Location(world, Double.valueOf(rawLocation[0]), Double.valueOf(rawLocation[1]), Double.valueOf(rawLocation[2]), Float.valueOf(rawLocation[3]), Float.valueOf(rawLocation[4]));
            return newLoc;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e){
            return null;
        }
    }
//    
//    public static void addToBackStack(Player player, Location location){
//        JSONObject playerBackData = JawaCommands.getBackStack(player.getUniqueId());
//        
//        if (playerBackData.isEmpty()){
//            playerBackData = generateEmptyStack();
//        }
//        
//        JSONArray worldStack = playerBackData.getJSONArray(location.getWorld().getName());
//        
//        if ( worldStack.length() < WorldHandler.getConfigNumber(location.getWorld(), "back count")){ //if less than the limit
//            worldStack.put(location);
//        } else { // if greater than the limit
//            JSONArray newWorldStack = new JSONArray(); //
//            
//            worldStack.remove(worldStack.length()-1);
//            newWorldStack.put(location);
//            
//            worldStack.forEach((loc) -> {
//                newWorldStack.put(loc);
//            });
//        }
//    }
//    
//    public static JSONObject generateEmptyStack(){
//        JSONObject obj = new JSONObject();
//        
//        JawaCommands.getPlugin().getServer().getWorlds().forEach((world) -> {
//            obj.put(world.getName(), new JSONArray());
//        });
//        
//        return obj;
//    }
//    
//    public static void removeFromStack(Player player){
//        JSONObject playerBackData = JawaCommands.getBackStack(player.getUniqueId());
//        if (!playerBackData.isEmpty()){
//            JSONArray worldStack = playerBackData.getJSONArray(player.getWorld().getName());
//        } else {
//            player.sendMessage(ChatColor.RED + " > You do not have any valid back locations!");
//        }
//    }
//    
//    public static void sendPlayerBack(Player player){
//        JSONObject playerBackData = JawaCommands.getBackStack(player.getUniqueId());
//        if (!playerBackData.isEmpty()){
//            JSONArray worldStack = playerBackData.getJSONArray(player.getWorld().getName());
//        } else {
//            player.sendMessage(ChatColor.RED + " > You do not have any valid back locations!");
//        }
//    }

    public static Location randomLocation(Player player, int worldborder) {
        Random r = new Random();
        int x = r.nextInt(worldborder + worldborder) - worldborder;
        int z = r.nextInt(worldborder + worldborder) - worldborder;
        int y = player.getWorld().getHighestBlockYAt(x, z);
        Location loc = new Location(player.getWorld(), x, y, z);
        return loc;
    }

    public static boolean blockCheck(Location loc) {
        Block block = loc.getWorld().getHighestBlockAt(loc);
        Material type = block.getType();
        if ((type == Material.LAVA) || (type == Material.WATER) || (type == Material.AIR) || (type == Material.WATER) || (type == Material.LAVA) || (type == Material.CACTUS) || (type == Material.MAGMA_BLOCK)) {
            return false;
        } else {
            return true;
        }
    }
    
}
