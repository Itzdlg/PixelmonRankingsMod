package sh.dominick.commissions.pixelmonrankings.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class PixelmonRankingsConfig {
    @Setting("dev-mode") public boolean devMode = false;

    @Setting public DatabaseConfig database = new DatabaseConfig();
    @ConfigSerializable
    public static class DatabaseConfig {
        @Setting public String type = "sqlite";
        @Setting public String url = "jdbc:sqlite:config/PixelmonRankings/database.db";

        @Setting public CompactionConfig compact = new CompactionConfig();
        @ConfigSerializable
        public static class CompactionConfig {
            @Setting public boolean enabled = true;
            @Setting public long daysUnprocessed = 3;
        }
    }

    @Setting public CacheConfig cache = new CacheConfig();
    @ConfigSerializable
    public static class CacheConfig {
        @Setting public boolean enabled = true;
        @Setting public long lifetime = 5 * 60 * 1000L;
    }

    @Setting("max-ranking")
    public int maxRanking = 100;
}
