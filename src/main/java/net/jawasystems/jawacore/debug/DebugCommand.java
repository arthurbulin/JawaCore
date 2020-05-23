/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.debug;

import net.jawasystems.jawacore.JawaCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author alexander
 */
public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (JawaCore.debug) {
            commandSender.sendMessage(ChatColor.GREEN + " > Debug mode is not toggling to" + ChatColor.YELLOW + " FALSE");
            JawaCore.debug = false;
        } else {
            commandSender.sendMessage(ChatColor.GREEN + " > Debug mode is not toggling to" + ChatColor.BLUE + " TRUE");
            JawaCore.debug = true;
        }
        
        return true;
    }
    
}
