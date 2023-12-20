/*
 * Copyright (C) 2021 Jawamaster (Arthur Bulin)
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

import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class MaterialHandler {
    
    /** Attempt a search for a modern material. If none is found attempt a search
     * by legacy name. If no material is found, return null, else return the Material.
     * @param name
     * @return 
     */
    public static Material getMaterial(String name){

        Material search = Material.matchMaterial(name);
        if (search == null){
            search = Material.matchMaterial(name, true);
        }
        return search;
    }
    
    /** This will pack an inventory into a JSONObject and return it.
     * @param inventory
     * @return 
     */
    public static JSONArray packInventory(List<ItemStack> inventory){
        JSONArray packedInventory = new JSONArray();
        for (ItemStack stack : inventory){
            packedInventory.put(packItemStack(stack));
        }
        return packedInventory;
    }

    /** This will pack a single ItemStack and return a JSONObject representation.
     * @param stack
     * @return 
     */
    public static JSONObject packItemStack(ItemStack stack) {
        JSONObject itemStack = new JSONObject();

        itemStack.put("material", stack.getType().toString());
        itemStack.put("quantity", 1);

        if (!stack.getEnchantments().isEmpty()) {

            JSONArray enchants = new JSONArray();

            for (Enchantment enchantment : stack.getEnchantments().keySet()) {
                JSONObject enchant = new JSONObject();
                enchant.put("enchant", enchantment.getKey().getKey());
                enchant.put("level", stack.getEnchantmentLevel(enchantment));
                enchants.put(enchant);
            }

            itemStack.put("enchantments", enchants);
        }
        return itemStack;
    }
    
//    public static ItemStack calculateCostOfRepair(ItemStack item){
//        
//    }

}
