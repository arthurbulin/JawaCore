/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.debug;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class JawaPluginsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        String usage = ChatColor.GREEN + "> /jawaplugins <info|changelog> <plugin>";
        String action = "";
        
        if (args == null || args.length == 0) {
            commandSender.sendMessage(usage);
        } else if (args.length >= 2 && args.length <= 3) {
            switch (args[0].toUpperCase()){
                case "INFO":
                    action = "INFO";
                    break;
                case "CHANGELOG":
                    action = "CHANGELOG";
                    break;
            }
            
        }
        return true;
    }

}
