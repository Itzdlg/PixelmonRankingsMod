package sh.dominick.commissions.pixelmonrankings.util;

public class TimeUtil {
    private TimeUtil() {}

    public static String formatSeconds(long seconds) {
        if (seconds < 0)
            throw new IllegalArgumentException("Seconds must be non-negative.");

        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;

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
}
