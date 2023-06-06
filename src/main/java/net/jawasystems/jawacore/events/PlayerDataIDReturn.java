/*
 * The MIT License
 *
 * Copyright 2021 Arthur Bulin.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.jawasystems.jawacore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.json.JSONObject;

/**
 *
 * @author Arthur Bulin
 */
public class PlayerDataIDReturn extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String INDEX;
    private final String ID;
    private final JSONObject DATA;
    
    /** Call an even to take this data syncronous
     * @param index the index the data was indexed to
     * @param id the id of the data after indexing
     * @param data the data
     */
    public PlayerDataIDReturn(String index, String id, JSONObject data){
        //Make the event async
        super(true);
        this.INDEX = index;
        this.ID = id;
        this.DATA = data;
    }
    
    /** Get the index
     * @return the index
     */
    public String getIndex(){
        return this.INDEX;
    }
    
    /** Get the indexed ID
     * @return The ID
     */
    public String getID(){
        return this.ID;
    }
    
    /** Get the data
     * @return The data
     */
    public JSONObject getData(){
        return this.DATA;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }   
    
}
