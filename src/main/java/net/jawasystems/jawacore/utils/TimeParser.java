/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jawasystems.jawacore.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** This class deals with time and date parsing.
 *
 * @author alexander
 */
public class TimeParser {

    /** Takes a LocalDateTime converted to a string and outputs a string in the 
     * requested by the integer listed.
     * default - Month DD, YYYY at HH:MM ZZZZZZZZZZZZ...
     * 1 - MMM DD, YYYY at HH:MM ZZZ
     * @param time
     * @param format
     * @return 
     */
    public static String getHumanReadableDateTime(String time, int format) {
        LocalDateTime parsedTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String hRTime;
        switch (format) {
            case 1: //MMM DD, YYYY at HH:MM ZZZ
                hRTime = parsedTime.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " "
                    + parsedTime.getDayOfMonth() + ", "
                    + parsedTime.getYear() + " at "
                    + parsedTime.getHour() + ":"
                    + parsedTime.getMinute() + " "
                    + java.util.TimeZone.getDefault().getDisplayName(false, 0);
                break;
            default: //Month DD, YYYY at HH:MM ZZZZZZZZZZZZ...
                hRTime = parsedTime.getMonth().toString() + " "
                    + String.valueOf(parsedTime.getDayOfMonth()) + ", "
                    + parsedTime.getYear() + " at "
                    + parsedTime.getHour() + ":"
                    + parsedTime.getMinute() + " "
                    + java.util.TimeZone.getDefault().getDisplayName();
                break;

        }
        return hRTime;

    }
    
    /** Takes a LocalDateTime converted to a string and outputs a string in the form
     * of Month Day, Year at H:M <long time zone>
     * @param time
     * @return 
     */
    public static String getHumanReadableDateTime(String time) {
        return getHumanReadableDateTime(time, 0);
    }

    /** *  Takes a LocalDateTime converted to a string and outputs false if that LocalDateTime is 
     * after the current time.I.E.false if in the past. true
     * @param time
     * @return 
     */
    public static boolean inPast(String time) {
        if ("forever".equals(time.toLowerCase())) return false;
        LocalDateTime toTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return toTime.isBefore(LocalDateTime.now());

    }
    
    public static int getSeconds(String value){
        if (value.toLowerCase().contains("s")){
            return Integer.valueOf(value.toLowerCase().replace('s', ' '));
        }else if (value.toLowerCase().contains("m")){
            return Integer.valueOf(value.toLowerCase().replace('m', ' '));
        } else {
            return 0;
        }
    }

}
