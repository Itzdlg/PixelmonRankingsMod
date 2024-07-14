package sh.dominick.commissions.pixelmonrankings.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class TimeUtil {
    private TimeUtil() {}

    public static String formatSeconds(long seconds) {
        if (seconds < 0)
            throw new IllegalArgumentException("Seconds must be non-negative.");

        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;

        if (days == 0 && hours == 0 && minutes == 0)
            return seconds + "s";

        StringBuilder formattedTime = new StringBuilder();

        if (days > 0) {
            formattedTime.append(days).append("d");
        }

        if (hours > 0 || days != 0) {
            formattedTime.append(hours).append("h");
        }

        if (minutes > 0 || days != 0 || hours != 0) {
            formattedTime.append(minutes).append("m");
        }

        return formattedTime.toString();
    }

    public static Instant getStartOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 0);
        return c.toInstant();
    }

    public static Instant getEndOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return c.toInstant();
    }

    public static String getISODate() {
        return getISODate(LocalDate.now());
    }

    public static String getISODate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
        return date.format(formatter);
    }
}
