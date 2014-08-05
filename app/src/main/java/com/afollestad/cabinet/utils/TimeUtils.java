package com.afollestad.cabinet.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Utilities for getting human readable time strings.
 *
 * @author Aidan Follestad (afollestad)
 */
public class TimeUtils {

    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int HOURS_IN_DAY = 24;
    private static final int DAYS_IN_YEAR = 365;
    private static final int MILLIS_IN_SECOND = 1000;
    private static final long MILLISECONDS_IN_MINUTE = (long) MILLIS_IN_SECOND * SECONDS_IN_MINUTE;
    private static final long MILLISECONDS_IN_HOUR = (long) MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR;
    private static final long MILLISECONDS_IN_DAY = (long) MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY;
    private static final long MILLISECONDS_IN_YEAR = (long) MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY * DAYS_IN_YEAR;

    public static String toStringLong(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return toStringLong(cal);
    }

    /**
     * Gets a human-readable long time string (includes both the time and date, always).
     */
    public static String toStringLong(Calendar date) {
        Calendar now = Calendar.getInstance();
        int hourInt = date.get(Calendar.HOUR);
        int minuteInt = date.get(Calendar.MINUTE);
        String dayStr = getNumberWithSuffix(date.get(Calendar.DAY_OF_MONTH));

        String timeStr = "";
        if (hourInt == 0) timeStr += "12";
        else timeStr += "" + hourInt;
        if (minuteInt < 10) timeStr += ":0" + minuteInt;
        else timeStr += ":" + minuteInt;
        if (date.get(Calendar.AM_PM) == Calendar.AM) timeStr += "AM";
        else timeStr += "PM";

        if (now.get(Calendar.YEAR) == date.get(Calendar.YEAR)) {
            // Same year
            return timeStr + " " + convertMonth(date.get(Calendar.MONTH), false) + " " + dayStr;
        } else {
            // Different year
            return timeStr + " " + convertMonth(date.get(Calendar.MONTH), false) + " " + dayStr + ", " + date.get(Calendar.YEAR);
        }
    }

    public static String toString(Date date, boolean includeTime, boolean shortMonth) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return toString(cal, includeTime, shortMonth);
    }

    /**
     * Gets a human-readable time string (includes both the time and date, excluded certain parts if possible).
     *
     * @param shortMonth Whether or display a long or short month string (e.g. 'January' or 'Jan').
     */
    public static String toString(Calendar date, boolean includeTime, boolean shortMonth) {
        Calendar now = Calendar.getInstance();
        int hourInt = date.get(Calendar.HOUR);
        int minuteInt = date.get(Calendar.MINUTE);
        String dayStr = getNumberWithSuffix(date.get(Calendar.DAY_OF_MONTH));

        String timeStr = "";
        if (hourInt == 0) timeStr += "12";
        else timeStr += "" + hourInt;
        if (minuteInt < 10) timeStr += ":0" + minuteInt;
        else timeStr += ":" + minuteInt;
        if (date.get(Calendar.AM_PM) == Calendar.AM) timeStr += "AM";
        else timeStr += "PM";

        if (now.get(Calendar.YEAR) == date.get(Calendar.YEAR)) {
            // Same year
            if (now.get(Calendar.MONTH) == date.get(Calendar.MONTH)) {
                // Same year, same month
                if (now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) {
                    // Same year, same month, same day
                    return timeStr;
                } else {
                    // Same year, same month, different day
                    String toReturn = "";
                    if (includeTime) toReturn = timeStr + " ";
                    toReturn += convertMonth(date.get(Calendar.MONTH), shortMonth) + " " + dayStr;
                    return toReturn;
                }
            } else {
                // Different month, same year
                String toReturn = "";
                if (includeTime) toReturn = timeStr + " ";
                toReturn += convertMonth(date.get(Calendar.MONTH), shortMonth) + " " + dayStr;
                return toReturn;
            }
        } else {
            // Different year
            String year = Integer.toString(date.get(Calendar.YEAR));
            String toReturn = "";
            if (includeTime) toReturn = timeStr + " ";
            toReturn += convertMonth(date.get(Calendar.MONTH), shortMonth) + " " + dayStr + ", " + year;
            return toReturn;
        }
    }

    public static String toStringDate(Date date, boolean shortMonth, boolean alwaysIncludeYear) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return toStringDate(cal, shortMonth, alwaysIncludeYear);
    }

    /**
     * Gets a human-readable date string (month, day, and year).
     *
     * @param shortMonth        Whether or display a long or short month string (e.g. 'January' or 'Jan').
     * @param alwaysIncludeYear Include the year even if it's the current year.
     */
    public static String toStringDate(Calendar time, boolean shortMonth, boolean alwaysIncludeYear) {
        Calendar now = Calendar.getInstance();
        String day = getNumberWithSuffix(time.get(Calendar.DAY_OF_MONTH));
        if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) && !alwaysIncludeYear) {
            // Same year
            if (now.get(Calendar.MONTH) == time.get(Calendar.MONTH)) {
                // Same year, same month
                return convertMonth(time.get(Calendar.MONTH), shortMonth) + " " + day;
            } else {
                // Different month, same year
                return convertMonth(time.get(Calendar.MONTH), shortMonth) + " " + day;
            }
        } else {
            // Different year
            String year = Integer.toString(time.get(Calendar.YEAR));
            return convertMonth(time.get(Calendar.MONTH), shortMonth) + " " + day + ", " + year;
        }
    }

    public static String toStringTime(Calendar time) {
        int hourInt = time.get(Calendar.HOUR);
        int minuteInt = time.get(Calendar.MINUTE);
        String timeStr = "";
        if (hourInt == 0) timeStr += "12";
        else timeStr += "" + hourInt;
        if (minuteInt < 10) timeStr += ":0" + minuteInt;
        else timeStr += ":" + minuteInt;
        if (time.get(Calendar.AM_PM) == Calendar.AM) timeStr += "AM";
        else timeStr += "PM";
        return timeStr;
    }

    public static String toStringTime(Date time) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(time);
        return toStringTime(cal);
    }

    public static String toStringShort(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return toStringShort(cal);
    }

    public static String toStringShort(Calendar time) {
        Calendar now = Calendar.getInstance();
        long diff = now.getTimeInMillis() - time.getTimeInMillis();
        long years = diff / MILLISECONDS_IN_YEAR;
        if (years == 0) {
            long days = diff / MILLISECONDS_IN_DAY;
            if (days == 0) {
                long hours = diff / MILLISECONDS_IN_HOUR;
                if (hours == 0) {
                    long minutes = diff / MILLISECONDS_IN_MINUTE;
                    if (minutes == 0) {
                        long seconds = diff / MILLIS_IN_SECOND;
                        return seconds + "s";
                    } else {
                        return minutes + "m";
                    }
                } else {
                    return hours + "h";
                }
            } else {
                if (days == 7) return "1w";
                else if (days > 7) {
                    long weeks = days / 7;
                    days = days % 7;
                    String str = weeks + "w";
                    if (days > 0) str += days + "d";
                    return str;
                } else return days + "d";
            }
        } else {
            return years + "y";
        }
    }

    private static String convertMonth(int month, boolean useShort) {
        String monthStr;
        switch (month) {
            default:
                monthStr = "January";
                break;
            case Calendar.FEBRUARY:
                monthStr = "February";
                break;
            case Calendar.MARCH:
                monthStr = "March";
                break;
            case Calendar.APRIL:
                monthStr = "April";
                break;
            case Calendar.MAY:
                monthStr = "May";
                break;
            case Calendar.JUNE:
                monthStr = "June";
                break;
            case Calendar.JULY:
                monthStr = "July";
                break;
            case Calendar.AUGUST:
                monthStr = "August";
                break;
            case Calendar.SEPTEMBER:
                monthStr = "September";
                break;
            case Calendar.OCTOBER:
                monthStr = "October";
                break;
            case Calendar.NOVEMBER:
                monthStr = "November";
                break;
            case Calendar.DECEMBER:
                monthStr = "December";
                break;
        }
        if (useShort) monthStr = monthStr.substring(0, 3);
        return monthStr;
    }

    private static String getNumberWithSuffix(int number) {
        int j = number % 10;
        if (j == 1 && number != 11) {
            return number + "st";
        }
        if (j == 2 && number != 12) {
            return number + "nd";
        }
        if (j == 3 && number != 13) {
            return number + "rd";
        }
        return number + "th";
    }
}