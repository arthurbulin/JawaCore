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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

/**
 *
 * @author alexander
 */
public class JSONHandler {
    
    public static JSONObject LoadJSONConfig(JavaPlugin plugin, String file){
        
        File JSONFile = new File(plugin.getDataFolder()+file);
                
        try {
            String source = new String(Files.readAllBytes(Paths.get(JSONFile.toURI())));
            return new JSONObject(source);
        }catch (NoSuchFileException ex){
            //Logger.getLogger(JSONHandler.class.getName()).log(Level.SEVERE, null, ex);
            return new JSONObject();
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(JSONHandler.class.getName()).log(Level.SEVERE, null, ex);
            return new JSONObject();
        } catch (IOException ex) {
            //Logger.getLogger(JSONHandler.class.getName()).log(Level.SEVERE, null, ex);
            return new JSONObject();
        } 
    }
    
    public static void WriteJSONToFile(JavaPlugin plugin, String file, JSONObject obj){
        File JSONFile = new File(plugin.getDataFolder() + file);

        try ( //open our writer and write the player file
            PrintWriter writer = new PrintWriter(JSONFile)) {
            //System.out.println(obj);
            writer.print(obj.toString(4));
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JSONHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    
}
