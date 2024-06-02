package pk.gov.pbs.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DateTimeUtil {
    public static final String defaultDateTimeFormat = "dd/MM/yyyy hh:mm a";
    public static final String defaultDateOnlyFormat = "dd/MM/yyyy";
    public static final String defaultTimeOnlyFormat = "hh:mm:ss a"; //24 hour format
    public static final String defaultTimeOnlyFormat24Hours = "HH:mm:ss"; //24 hour format
    public static final String sqlTimestampFormat = "yyyy-MM-dd'T'HH:mm:ss";
    private static final Map<String, SimpleDateFormat> cache = new HashMap<>();

    public static Calendar getCalendar(){
        return Calendar.getInstance();
    }

    /**
     * get current time stamp in unix time
     * @return number of seconds since epoch time
     */
    public static long getCurrentDateTimeUnix(){
        return System.currentTimeMillis() / 1000;
    }

    public static Date getCurrentDateTime() {
        return getCalendar().getTime();
    }

    public static String getCurrentDateTimeString() {
        return getCurrentDateTime(defaultDateTimeFormat);
    }

    public static String getCurrentDateTime(String format){
        if (!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.getDefault()));

        return cache.get(format).format(getCurrentDateTime());
    }

    public static String formatDateTime(long unix){
        return formatDateTime(unix, defaultDateTimeFormat);
    }

    public static String formatDateTime(long unixTs, String toFormat){
        if(!cache.containsKey(toFormat))
            cache.put(toFormat, new SimpleDateFormat(toFormat, Locale.getDefault()));

        try {
            return cache.get(toFormat).format(getDateFrom(unixTs));
        } catch (NullPointerException e) {
            ExceptionReporter.handle(e);
        }
        return String.valueOf(unixTs);
    }

    /**
     * Format date (Date object) to defaultDateTimeFormat
     * @param subject Date object
     * @return formatted date string
     */
    public static String formatDateTime(Date subject){
        return formatDateTime(subject, defaultDateTimeFormat);
    }

    /**
     * Format datetime (Date object) to specified output format (toFormat)
     * @param subject Date object
     * @param toFormat output format
     * @return formatted date string
     */
    public static String formatDateTime(Date subject, String toFormat){
        if (subject == null)
            return null;

        if(!cache.containsKey(toFormat))
            cache.put(toFormat, new SimpleDateFormat(toFormat, Locale.getDefault()));

        try {
            return cache.get(toFormat).format(subject);
        } catch (NullPointerException e) {
            ExceptionReporter.handle(e);
        }

        return subject.toString();
    }

    /**
     * Format date (subject) in specified format (fromFormat) to specified output format (toFormat)
     * @param subject date string
     * @param fromFormat format of date string (subject)
     * @param toFormat output format
     * @return formatted date string
     */
    public static String formatDateTime(String subject, String fromFormat, String toFormat){
        if (subject == null)
            return null;

        if(!cache.containsKey(fromFormat))
            cache.put(fromFormat, new SimpleDateFormat(fromFormat, Locale.getDefault()));

        if(!cache.containsKey(toFormat))
            cache.put(toFormat, new SimpleDateFormat(toFormat, Locale.getDefault()));

        try {
            Date date = cache.get(fromFormat).parse(subject);
            return cache.get(toFormat).format(date);
        } catch (ParseException | NullPointerException e) {
            ExceptionReporter.handle(e);
            return subject;
        }
    }

    /**
     * Format date string to default format for specified dateFormat
     * @param subject date string
     * @param fromFormat format of date string (subject)
     * @return formatted date string to defaultDateTimeFormat
     */
    public static String formatDateTime(String subject, String fromFormat){
        return formatDateTime(subject, fromFormat, defaultDateTimeFormat);
    }

    //============================== Construct Date object from ===================================
    /**
     * Get Date object from string date and specified format
     * @param subject string date
     * @param format format of subject
     * @return Date object
     */
    public static Date getDateFrom(String subject, String format){
        if(!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.getDefault()));
        try {
            return cache.get(format).parse(subject);
        } catch (ParseException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    /**
     * Get Date object from unix timestamp
     * @param unixInSeconds unix timestamp
     * @return Date object
     */
    public static Date getDateFrom(long unixInSeconds){
        return new Date(unixInSeconds*1000L);
    }

    /**
     *  Get Date object from year, month, day
     * @param year 4 digit year
     * @param month 1-12
     * @param day 1-31
     * @return Date object
     */
    public static Date getDateFrom(int year, int month, int day){
        String format = "MM/dd/yyyy";
        if(!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.getDefault()));
        try {
            return cache.get(format).parse(month + "/" + day + "/" + year);
        } catch (ParseException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    //========================== Duration calculations between two dates ==========================
    public static long getDurationBetweenInMillis(Date fromDate, Date toDate) {
        long difference_In_Time
                = toDate.getTime() - fromDate.getTime();
        difference_In_Time -= (long) getLeapYearCount(fromDate, toDate) * 24 * 60 * 60 * 1000;
        return difference_In_Time;
    }

    public static long getDurationBetweenInSeconds(Date fromDate, Date toDate) {
        return TimeUnit.MILLISECONDS
                .toSeconds(getDurationBetweenInMillis(fromDate, toDate));
    }

    public static long getDurationBetweenInDays(Date fromDate, Date toDate) {
        return TimeUnit.MILLISECONDS.toDays(getDurationBetweenInMillis(fromDate, toDate));
    }

    public static long getDurationBetweenInYears(Date fromDate, Date toDate) {
        return TimeUnit
                .MILLISECONDS
                .toDays(getDurationBetweenInMillis(fromDate, toDate))
                / 365;
    }

    public static long getDurationBetweenIn(TimeUnit timeUnit, Date fromDate, Date toDate) {
        return timeUnit.convert(getDurationBetweenInMillis(fromDate, toDate), TimeUnit.MILLISECONDS);
    }

    public static int getLeapYearCount(Date fromDate, Date toDate) {
        Calendar calendar = getCalendar();
        calendar.setTime(fromDate);
        int fy = calendar.get(Calendar.YEAR);
        calendar.setTime(toDate);
        int ty = calendar.get(Calendar.YEAR);

        int lyc = 0; //leap year count
        for (; fy <= ty; fy++){
            if (fy % 4 == 0 && (fy % 100 != 0 || fy % 400 == 0))
                lyc++;
        }
        return lyc;
    }

    // Function to print difference in (for reference and testing)
    // time start_date and end_date
    private static void printDifference(String start_date, String end_date, String date_format) {
        SimpleDateFormat sdf = new SimpleDateFormat(date_format, Locale.getDefault());
        try {
            Date d1 = sdf.parse(start_date);
            Date d2 = sdf.parse(end_date);

            // Calucalte time difference
            long difference_In_Time
                    = getDurationBetweenInMillis(d1, d2);

            long difference_In_Seconds
                    = TimeUnit.MILLISECONDS
                    .toSeconds(difference_In_Time)
                    % 60;

            long difference_In_Minutes
                    = TimeUnit
                    .MILLISECONDS
                    .toMinutes(difference_In_Time)
                    % 60;

            long difference_In_Hours
                    = TimeUnit
                    .MILLISECONDS
                    .toHours(difference_In_Time)
                    % 24;

            long difference_In_Days
                    = TimeUnit
                    .MILLISECONDS
                    .toDays(difference_In_Time)
                    % 365;

            long difference_In_Years
                    = TimeUnit
                    .MILLISECONDS
                    .toDays(difference_In_Time)
                    / 365l;

            // Print the date difference in
            // years, in days, in hours, in
            // minutes, and in seconds
            System.out.print(
                    "Difference"
                            + " between two dates is: ");

            // Print result
            System.out.println(
                    difference_In_Years
                            + " years, "
                            + difference_In_Days
                            + " days, "
                            + difference_In_Hours
                            + " hours, "
                            + difference_In_Minutes
                            + " minutes, "
                            + difference_In_Seconds
                            + " seconds");

        } catch (ParseException e) {
            ExceptionReporter.handle(e);
        }
    }

}
