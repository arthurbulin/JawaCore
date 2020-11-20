/*
 * Copyright (C) 2020 Jawamaster (Arthur Bulin)
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
package net.jawasystems.jawacore.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class TargetSelectorProcessor {
    
    //Execute @p
    public static Player getAtP(String atP, CommandSender sender) {
        //@[p-P]\[[x-X]=-?[0-9]+,[y-Y]=-?[0-9]+,[z-Z]=-?[0-9]+,d[x-X]=-?[0-9]+,d[y-Y]=-?[0-9]+,d[z-Z]=-?[0-9]+\]
        //@[p-P]\[[x-X]=-?[0-9]+,[y-Y]=-?[0-9]+,[z-Z]=-?[0-9]+,d=[0-9]+\]
        //@[p-P]\[[x-X]=-?[0-9]+,[y-Y]=-?[0-9]+,[z-Z]=-?[0-9]+\]
        //@[p-P]
        Location senderLoc;
        if (sender instanceof BlockCommandSender) senderLoc = ((BlockCommandSender) sender).getBlock().getLocation();
        else if (sender instanceof Player) senderLoc = ((Player) sender).getLocation();
        else return null;
        
        if (atP.matches("@[p|P]")) { //Closest to command sender
            return closestToLocation(senderLoc, senderLoc.getWorld().getPlayers());
        } 
        //Closest to this location
        else if (atP.matches("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+\\]")) { 
            int x = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=", "").replaceAll(",[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+\\]", ""));
            int y = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=", "").replaceAll(",[z|Z]=-?[0-9]+\\]", ""));
            int z = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=", "").replaceAll("\\]", ""));
            senderLoc = new Location(senderLoc.getWorld(), x, y, z);
            return closestToLocation(senderLoc, senderLoc.getWorld().getPlayers());
        } 
        //Within d of this location
        else if (atP.matches("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,[d|D]=[0-9]+\\]")){
            System.out.println("d regex");
            int x = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=", "").replaceAll(",[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,[d|D]=[0-9]+\\]", ""));
            int y = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=", "").replaceAll(",[z|Z]=-?[0-9]+,[d|D]=[0-9]+\\]", ""));
            int z = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=", "").replaceAll(",[d|D]=[0-9]+\\]", ""));
            int d = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,[d|D]=", "").replaceAll("\\]", ""));
            senderLoc = new Location(senderLoc.getWorld(), x, y, z);
            return withinUniformLocation(senderLoc, d);
            //return closestToLocation(senderLoc, d);
        } 
        
        //Within cuboid location
        else if (atP.matches("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]")) {
            int x = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=", "").replaceAll(",[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int y = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=", "").replaceAll(",[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int z = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=", "").replaceAll(",d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int dx = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=", "").replaceAll(",d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int dy = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=", "").replaceAll(",d[z|Z]=-?[0-9]+\\]", ""));
            int dz = Integer.valueOf(atP.replaceAll("@[p|P]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=", "").replaceAll("\\]", ""));
            senderLoc = new Location(senderLoc.getWorld(), x, y, z);
            return withinLocation(senderLoc, dx, dy, dz);
        }
        //Uninterpretable
        else {
            return null;
        }
    }
    
    /** Get all player within the defined @a area. This sill return a List<Player>,
     * null if bad syntax or no online players are in that world, or an empty list.
     * @param atA
     * @param sender
     * @return 
     */
    public static List<Player> getAtA(String atA, CommandSender sender) {
        //@a - all online players
        //@a[world=<world>] - all players in a specific world
        //@a[x=##,y=##,z=##,d=##] - all players within a cuboid of radius d/2 centered around x,y,z in the world of the command sender
        //@a[x=##,y=##,z=##,dx=##,dy=##,dz=##] - all players within a cuboid between these two areas
        //@a[gamemode=<GAMEMODE>] - all players with a specific game mode
        //@a[gamedata=<JSONString>] - access custom game data
        Location senderLoc;
        if (sender instanceof BlockCommandSender) senderLoc = ((BlockCommandSender) sender).getBlock().getLocation();
        else if (sender instanceof Player) senderLoc = ((Player) sender).getLocation();
        else return null;
        
        if (atA.matches("@[a|A]")) { //Closest to command sender
            return new ArrayList(Bukkit.getServer().getOnlinePlayers());
        } 
        else if (atA.matches("@[a|A]\\[world\\]")) {
            return senderLoc.getWorld().getPlayers();
        }
        
        else if (atA.matches("@[a|A]\\[world=.+\\]")){
            String worldName = atA.replaceAll("@[a|A]\\[world=", "").replaceAll("\\]", "");
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            else return world.getPlayers();
        }
        
        //Within d of this location
        else if (atA.matches("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,[d|D]=[0-9]+\\]")){
            int x = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=", "").replaceAll(",[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,[d|D]=[0-9]+\\]", ""));
            int y = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=", "").replaceAll(",[z|Z]=-?[0-9]+,[d|D]=[0-9]+\\]", ""));
            int z = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=", "").replaceAll(",[d|D]=[0-9]+\\]", ""));
            int d = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,[d|D]=", "").replaceAll("\\]", ""));
            senderLoc = new Location(senderLoc.getWorld(), x, y, z);
            return allWithinUniformLocation(senderLoc, d);
            //return closestToLocation(senderLoc, d);
        } 
        
        //Within cuboid location
        else if (atA.matches("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]")) {
            int x = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=", "").replaceAll(",[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int y = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=", "").replaceAll(",[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int z = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=", "").replaceAll(",d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int dx = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=", "").replaceAll(",d[y|Y]=-?[0-9]+,d[z|Z]=-?[0-9]+\\]", ""));
            int dy = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=", "").replaceAll(",d[z|Z]=-?[0-9]+\\]", ""));
            int dz = Integer.valueOf(atA.replaceAll("@[a|A]\\[[x|X]=-?[0-9]+,[y|Y]=-?[0-9]+,[z|Z]=-?[0-9]+,d[x|X]=-?[0-9]+,d[y|Y]=-?[0-9]+,d[z|Z]=", "").replaceAll("\\]", ""));
            senderLoc = new Location(senderLoc.getWorld(), x, y, z);
            return allWithinLocation(senderLoc, dx, dy, dz);
        }
        
        //Uninterpretable
        else {
            return null;
        }

    }
    
    private void strip(String atA){
        String modified = atA.replaceFirst("@[a|A]\\[", "").replaceAll("\\]", "");
    }
    
    /** Will return a player within the uniform cuboid area centered on the location
     * and offset in each direction by d/2.
     * @param location The center point of the cuboid
     * @param d The diameter of the cuboid in each direction
     * @return The player within this area, if there are more than one, this returns
     * the closest, if there are none this returns null
     */
    private static Player withinUniformLocation(Location location, int d) {
        List<Player> targetList = allWithinUniformLocation(location, d);
        if (1 == targetList.size()) {
            return targetList.get(0);
        } else if (targetList.size() >= 2) { //More than one player in the zone, find the closest
            return closestToLocation(location, targetList);
        } else {
            return null;
        }
    }
    
    /** Will return all players within the uniform cuboid area centered on the location
     * and offset in each direction by d/2.
     * @param location The center point of the cuboid
     * @param d The diameter of the cuboid in each direction
     * @return The players within this area, if there are none this returns an empty list
     */
    private static List<Player> allWithinUniformLocation(Location location, int d){
        int x1 = location.getBlockX() + (d / 2);
        int x2 = location.getBlockX() - (d / 2);
        int y1 = location.getBlockY() + (d / 2);
        int y2 = location.getBlockY() - (d / 2);
        int z1 = location.getBlockZ() + (d / 2);
        int z2 = location.getBlockZ() - (d / 2);

        List<Player> targetList = new ArrayList();

        for (Player ply : location.getWorld().getPlayers()) {
            Location tmp = ply.getLocation();
            if (between(tmp.getBlockX(), x1, x2) && between(tmp.getBlockY(), y1, y2) && between(tmp.getBlockZ(), z1, z2)) {
                targetList.add(ply);
            }
        }
        return targetList;
    }
    
    /** Will return the player within a cuboid location. The location defines the starting point
     * and each delta represents the offset in that coordinate direction. 
     * @param location Begining point locations
     * @param deltaX X offset
     * @param deltaY Y offset
     * @param deltaZ Z offset
     * @return A single player within the area. If no player meets this criteria this is null
     */
    private static Player withinLocation(Location location, int deltaX, int deltaY, int deltaZ){
        List<Player> targetList = allWithinLocation(location, deltaX, deltaY, deltaZ);
        if (1 == targetList.size()) return targetList.get(0);
        else if (targetList.size() >= 2) { //More than one player in the zone, find the closest
            return closestToLocation(new Location(location.getWorld(), (location.getBlockX() + (deltaX / 2)), (location.getBlockY() + (deltaY / 2)), (location.getBlockZ() + (deltaZ / 2))), targetList); 
        } else {
            return null;
        }
    }
    
     /** Will return all the players within a cuboid location. The location defines the starting point
     * and each delta represents the offset in that coordinate direction. 
     * @param location Begining point locations
     * @param deltaX X offset
     * @param deltaY Y offset
     * @param deltaZ Z offset
     * @return All players within the area. If no player meets this criteria the returned list is empty
     */
    private static List<Player> allWithinLocation(Location location, int deltaX, int deltaY, int deltaZ){
        int x1 = location.getBlockX();
        int x2 = x1 + deltaX;
        int y1 = location.getBlockY();
        int y2 = y1 + deltaY;
        int z1 = location.getBlockZ();
        int z2 = z1 + deltaZ;
        //System.out.println("Searching between: (" + x1 +","+y1+","+z1+") and ("+ x2 +","+y2+","+z2+")");
        List<Player> targetList = new ArrayList();

        for (Player ply : location.getWorld().getPlayers()) {
            Location tmp = ply.getLocation();
            if (between(tmp.getBlockX(), x1, x2) && between(tmp.getBlockY(), y1, y2) && between(tmp.getBlockZ(), z1, z2)) targetList.add(ply);
        }
        
        return targetList;
    }
    
    /** Returns the player closest to the specified location. The return will be
     * null only if targets is empty or players are in another dimension.
     * @param location The location with which to check
     * @param targets The list of targets to check, if all online players, then will
     * get any player closest.
     * @return A single Player that represents the player closes to the location
     * this will be null only if targets is empty or players are in another dimension.
     */
    private static Player closestToLocation(Location location, List<Player> targets){
        double intDistance = Double.MAX_VALUE;
        Player target = null;

        for (Player ply : targets) {
            double plyDist = ply.getLocation().distanceSquared(location);
            if (plyDist < intDistance) {
                target = ply;
                intDistance = plyDist;
            }
        }
        return target;
    }
    
    /** Given a value to check this will return true if between value1 and value2
     * value1 and value2 do not have to be in order as this method will detect which
     * is the upper and lower value.
     * @param valueToCheck The value to be evaluated
     * @param value1 One value of the bound, which does not matter
     * @param value2 The other value
     * @return True if valueToCheck falls between value1 and value2
     */
    private static boolean between(int valueToCheck, int value1, int value2){
        if (value1 > value2){
            return (valueToCheck < value1 ) && (valueToCheck > value2);
        } else {
            return (valueToCheck > value1 ) && (valueToCheck < value2);
        }
        
    }
    
    
    
    /** Checks if players in list have the specified GameMode, if they do they are returned
     * inside the list. If no players are found an empty list is returned, the same
     * if a gamemode cannot be matched.
     * @param gameMode
     * @param players
     * @return 
     */
    private static List<Player> getAllWithGamemode(String gameMode, List<Player> players){
        GameMode gm = null;
        List<Player> playersWithGM = new ArrayList();
        for (GameMode mode : GameMode.values()){
            if (gameMode.equalsIgnoreCase(mode.toString())) gm = mode; 
            else return null;
        }

        for (Player ply : players) {
            if (ply.getGameMode().equals(gm)) playersWithGM.add(ply);
        };
        return playersWithGM;
        
    }

}
