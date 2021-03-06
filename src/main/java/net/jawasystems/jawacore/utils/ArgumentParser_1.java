/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

/** Evaluates a String[] for '-' arguments and their values.
 *
 * @author alexander
 */
public class ArgumentParser_1 {

    /** Will return a HashMap with keys composed of containing '-' arguments and
     * their values. Will return null if the String[] does not contain arguments
     * starting with '-'.
     * @param args
     * @return 
     */
    public static HashMap getArgumentValues(String[] args) {
        //Define holding variables
        Object[] indexes = getIndexes(args);

        //Get first wave of parsing
        Object[] diffs = getDiffs((Object[]) indexes[1], args.length);

        //Complete and return parsed arguments object
        return parse((Object[]) indexes[0], (Object[]) indexes[1], diffs, args);

    }

    private static Object[] getIndexes(String[] args) {
        LinkedHashMap<String, Integer> indexes = new LinkedHashMap();
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                indexes.put(args[i], i);
            }
        }
        Object[] values = indexes.keySet().toArray();
        Object[] inds = indexes.values().toArray();
        Object[] returnValues = {values, inds};
        return returnValues;
    }

    private static Object[] getDiffs(Object[] rawIndexes, int argsL) {
        Object[] diffs = new Object[rawIndexes.length];
        Object[] indecies = Arrays.copyOf(rawIndexes, rawIndexes.length + 1);

        indecies[rawIndexes.length] = argsL;

        for (int i = 0; i < rawIndexes.length; i++) {

            diffs[i] = (int) indecies[i + 1] - (int) indecies[i];
        };

        return diffs;
    }

    private static HashMap parse(Object[] values, Object[] inds, Object[] diffs, String[] args) {

        HashMap<String,String> returnvalues = new HashMap();

        for (int i = 0; i < values.length; i++) {
            if (((int) inds[i] + 1) > ((int) inds[i] + (int) diffs[i] -1)) {
                //System.out.println(((int) inds[i] - 1));
                returnvalues.put("flags", ((String) values[i]).replace("-", ""));
            }else {
                //System.out.println(((int) inds[i] - 1) + " : " + String.join(" ", Arrays.copyOfRange(args, (int) inds[i] + 1, (int) inds[i] + (int) diffs[i])));
                returnvalues.put(((String) values[i]).replace("-", ""), String.join(" ", Arrays.copyOfRange(args, (int) inds[i] + 1, (int) inds[i] + (int) diffs[i])));
            }
        }

        return returnvalues;

    }

}
