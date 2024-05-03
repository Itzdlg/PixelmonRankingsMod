package sh.dominick.commissions.pixelmonrankings.data;

import com.mojang.authlib.GameProfile;
import sh.dominick.commissions.pixelmonrankings.Statistic;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public interface IDataManager {
    /**
     * Represents a player and statistic pair.
     */
    class Key {
        private final UUID player;
        private final Statistic statistic;
        public Key(UUID player, Statistic statistic) {
            this.statistic = statistic;
            this.player = player;
        }

        public Statistic statistic() {
            return statistic;
        }

        public UUID player() {
            return player;
        }
    }

    /**
     * Represents a key-value pair.
     */
    class Entry {
        private final Key key;
        private final double value;
        public Entry(Key key, double value) {
            this.key = key;
            this.value = value;
        }

        public Key key() {
            return key;
        }

        public double value() {
            return value;
        }
    }

    /**
     * Records a +/- change for the provided player
     * and statistic.
     * <br />
     * For example, if a player has played an extra
     * 2 minutes, and the statistic has key playtime
     * stored as the number of seconds played, the call
     * <code>recordChange(Key(player, "playtime"), 120)</code>
     * will result in the following entry:
     * player, "playtime", 120, (unix milliseconds).
     *
     * @param key The player and statistic
     * @param value The (+/-) change
     */
    void recordChange(Key key, double value);

    /**
     * Calculate the total change over the period
     * provided.
     * <br />
     * For example, if a player has the following
     * entries over the period: 20, -5, 1, 1, 2,
     * then this method will return 19.
     * <br />
     *
     * @param key The player and statistic
     * @param from the starting period. null for beginning of records
     * @param to the ending period. null for end of records
     *
     * @return the aggregate value over the period for the provided
     * player and statistic
     */
    double aggregate(Key key, @Nullable Instant from, @Nullable Instant to);

    /**
     * Sorts players, by aggregate change, for the provided
     * statistic, from the provided period, in descending
     * order, with at most `num` results.
     *
     * @see #aggregate(Key, Instant, Instant)
     *
     * @param statistic the statistic to sort
     * @param from the starting period. null for the beginning of records
     * @param to the ending period. null for the end of records
     * @param limit the limit of results, or -1 for no limit
     *
     * @return an array of Entry with num elements
     */
    Entry[] sort(Statistic statistic, @Nullable Instant from, @Nullable Instant to, int limit);

    /**
     * Find the position a player would stand in a
     * sorted ranking.
     *
     * @param key the player and statistic pair
     * @param from the starting period. null for the beginning of records
     * @param to the ending period. null for the end of records
     * @return the position after sorting, 1-indexed. -1 if not found
     */
    long findPositionSorted(Key key, @Nullable Instant from, @Nullable Instant to);

    /**
     * @param statistic the statistic
     * @param from the starting period. null for the beginning of records
     * @param to the ending period. null for the end of records
     * @return the count of unique players for the provided statistic
     */
    long count(Statistic statistic, @Nullable Instant from, @Nullable Instant to);

    class CachedGameProfile {
        private final UUID player;
        private final String playerName;
        private final String texture;

        public CachedGameProfile(UUID player, String playerName, String texture) {
            this.player = player;
            this.playerName = playerName;
            this.texture = texture;
        }

        public UUID player() {
            return player;
        }

        public String playerName() {
            return playerName;
        }

        public String texture() {
            return texture;
        }
    }

    /**
     * Saves the necessary information of a player to
     * the database as a cache for when they are offline.
     *
     * @param player the player's uuid
     * @param playerName the player's username
     * @param texture the player's profile texture
     */
    void recordGameProfile(UUID player, String playerName, String texture);

    /**
     * @return the player's cached GameProfile. null if not found
     */
    CachedGameProfile getGameProfile(UUID player);

    /**
     * Takes all records between the timeframe and replaces
     * it with a single change record.
     *
     * @param from the starting period. null for the beginning of records
     * @param to the ending period. null for the end of records
     */
    void compact(@Nullable Instant from, @Nullable Instant to);
}
