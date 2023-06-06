/*
 * The MIT License
 *
 * Copyright 2022 Arthur Bulin.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Arthur Bulin
 */
public class VectorParser {
    private static final Logger LOGGER = Logger.getLogger("VectorParser");
    /** Parse a string of vectors, distances, and timeouts. This will take a string[] of the form vector1,vector2,...
     * where vector# is of the format x,y,z,distance,timeout. The x,y,z values must be castable to float. The distance
     * and timeout values must be castable to integers. If any value fails and generates a NumberFormatException that
     * vector will be skipped. The returned ArrayList should be checked that its size matches the sent size.
     * @param rawVectors a String[] of the vector strings of the format x,y,z,distance,timeout
     * @return ArrayList of JSONObjects containing the bukkit vector, integer timeout, and integer distance
     */
    public static ArrayList<JSONObject> parseVectors(String[] rawVectors){
        ArrayList<JSONObject> returnVectors = new ArrayList();
        for (String rawVector : rawVectors){
            String[] vectorArray = rawVector.split(",");
            if (vectorArray.length == 5) {
                JSONObject tmp = new JSONObject();
                try {
                    Vector newVector = new Vector(Float.valueOf(vectorArray[0]),Float.valueOf(vectorArray[1]),Float.valueOf(vectorArray[2]));
                    int distance = Integer.valueOf(vectorArray[3]);
                    int timeout = Integer.valueOf(vectorArray[4]);
                    tmp.put("vector", newVector);
                    tmp.put("timeout", timeout);
                    tmp.put("distance", distance);
                    returnVectors.add(tmp);
                } catch (NumberFormatException e){
                    LOGGER.log(Level.WARNING, rawVector + " is not formatted correctly");
                }
            } 
        }
        return returnVectors;
    }
}
