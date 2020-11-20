/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class FileHandler {
    
    private static final Logger LOGGER = Logger.getLogger("FileHandler");
    
    public static FileConfiguration getYMLFile(String filePath, InputStream defaultFile) throws FileNotFoundException, IOException, InvalidConfigurationException{
        File file = new File(filePath);
        InputStream io;
        FileConfiguration ymlFile = new YamlConfiguration();
        
        if (file.exists()){
            io = new FileInputStream(file);
        } else if (defaultFile != null) {
            LOGGER.log(Level.INFO, "{0} was not found and is being loaded from the default.", filePath);
            io = defaultFile;
        } else {
            LOGGER.log(Level.INFO, "{0} was not found and a default was not provided.", filePath);
            return null;
        }
        
        ymlFile.load(new InputStreamReader(io));
        return ymlFile;
    }
    
    public static void writeYMLFile(String filePath, InputStream content) throws FileNotFoundException, IOException{
        File file = new File(filePath);
        byte[] buffer = new byte[content.available()];
        OutputStream io = new FileOutputStream(file);
        io.write(buffer);
    }
}
