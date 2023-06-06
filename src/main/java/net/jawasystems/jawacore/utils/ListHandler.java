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
package net.jawasystems.jawacore.utils;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Arthur Bulin
 */
public class ListHandler {
    
//    private static final HashMap<UUID, List<List>> LISTCACHE = new HashMap();
//    private static final HashMap<LocalDateTime, 
    
    
    public static JSONArray buildPages(JSONArray list, int pageSize){
//        List<List<JSONObject>> assembledPages = new ArrayList();
        JSONArray tmpList = new JSONArray();
        int lastEntry = list.length()-1;
        int numberOfPages = (list.length() % pageSize == 0) ? list.length() / pageSize : (list.length() / pageSize) + 1;
        JSONArray assembledPages = new JSONArray();
        int mod = list.length() % pageSize;
        
        int iteration = 0;
        for (Object obj : list){
            if (iteration < pageSize){
                tmpList.put(obj);
                iteration++;
            } else if (iteration == pageSize){
                assembledPages.put(tmpList);
                tmpList = new JSONArray();
                tmpList.put(obj);
                iteration = 1;
            }              
            
            if (assembledPages.length() == numberOfPages-1 && iteration == mod){
                assembledPages.put(tmpList);
            }
            System.out.println(tmpList);
            
        }
        System.out.println(assembledPages);
        return assembledPages;
        
    }
}
