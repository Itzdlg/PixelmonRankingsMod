package sh.dominick.commissions.pixelmonrankings.data.facade;

import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CachedDataManager implements IDataManager {
    protected final IDataManager reference;
    protected final long refreshAfter;

    protected long refreshedAt = System.currentTimeMillis();

    public CachedDataManager(IDataManager reference, long refreshAfter) {
        this.reference = reference;
        this.refreshAfter = refreshAfter;
    }

    public void clearAll() {
        this.aggregateCache.clear();
        this.sortCache.clear();
        this.gameProfileCache.clear();
        this.findPositionSortedCache.clear();
        this.countCache.clear();
    }

    private void clearAllIfRefreshed() {
        long toRefresh = refreshedAt + refreshAfter;
        if (toRefresh > System.currentTimeMillis())
            return;

        clearAll();
    }

    public IDataManager reference() {
        return reference;
    }

    @Override
    public void recordChange(Key key, double value) {
        reference.recordChange(key, value);
    }

    protected static class AggregateParams {
        private static String key(Key key, @Nullable Instant from, @Nullable Instant to) {
            if (from != null && to != null)
                return "{" + key.player() + "," + key.statistic().ordinal() + "}{" + from.toEpochMilli() + "," + to.toEpochMilli() + "}";

            if (from == null && to != null)
                return "{" + key.player() + "," + key.statistic().ordinal() + "}{null," + to.toEpochMilli() + "}";

            if (from != null && to == null)
                return "{" + key.player() + "," + key.statistic().ordinal() + "}{" + from.toEpochMilli() + ",null}";

            return "{" + key.player() + "," + key.statistic().ordinal() + "}{null,null}";
        }
    }

    protected Map<String, Double> aggregateCache = new HashMap<>();

    @Override
    public double aggregate(Key key, @Nullable Instant from, @Nullable Instant to) {
        clearAllIfRefreshed();

        return aggregateCache.computeIfAbsent(
                AggregateParams.key(key, from, to),
                k -> reference.aggregate(key, from, to)
        );
    }

    protected static class SortParams {
        private static String key(Statistic statistic, @Nullable Instant from, @Nullable Instant to, int limit) {
            if (from != null && to != null)
                return "{" + statistic.ordinal() + "}{" + from.toEpochMilli() + "," + to.toEpochMilli() + "}{" + limit + "}";

            if (from == null && to != null)
                return "{" + statistic.ordinal() + "}{null," + to.toEpochMilli() + "}{" + limit + "}";

            if (from != null && to == null)
                return "{" + statistic.ordinal() + "}{" + from.toEpochMilli() + ",null}{" + limit + "}";

            return "{" + statistic.ordinal() + "}{null,null}{" + limit + "}";
        }
    }

    protected Map<String, Entry[]> sortCache = new HashMap<>();

    @Override
    public Entry[] sort(Statistic statistic, @Nullable Instant from, @Nullable Instant to, int limit) {
        clearAllIfRefreshed();
        return sortCache.computeIfAbsent(
                SortParams.key(statistic, from, to, limit),
                k -> reference.sort(statistic, from, to, limit)
        );
    }

    protected static class FindPositionSortedParams {
        private static String key(Key key, @Nullable Instant from, @Nullable Instant to) {
            return AggregateParams.key(key, from, to);
        }
    }

    protected Map<String, Long> findPositionSortedCache = new HashMap<>();

    @Override
    public long findPositionSorted(Key key, @Nullable Instant from, @Nullable Instant to) {
        clearAllIfRefreshed();

        return findPositionSortedCache.computeIfAbsent(
                FindPositionSortedParams.key(key, from, to),
                k -> reference.findPositionSorted(key, from, to)
        );
    }

    protected static class CountParams {
        private static String key(Statistic statistic, @Nullable Instant from, @Nullable Instant to) {
            if (from != null && to != null)
                return "{" + statistic.ordinal() + "}{" + from.toEpochMilli() + "," + to.toEpochMilli() + "}";

            if (from == null && to != null)
                return "{" + statistic.ordinal() + "}{null," + to.toEpochMilli() + "}";

            if (from != null && to == null)
                return "{" + statistic.ordinal() + "}{" + from.toEpochMilli() + ",null}";

            return "{" + statistic.ordinal() + "}{null,null}";
        }
    }

    protected Map<String, Long> countCache = new HashMap<>();

    @Override
    public long count(Statistic statistic, @Nullable Instant from, @Nullable Instant to) {
        clearAllIfRefreshed();

        return countCache.computeIfAbsent(
                CountParams.key(statistic, from, to),
                k -> reference.count(statistic, from, to)
        );
    }

    @Override
    public void recordGameProfile(UUID player, String playerName, String texture) {
        reference.recordGameProfile(player, playerName, texture);
    }

    protected final Map<UUID, CachedGameProfile> gameProfileCache = new HashMap<>();

    @Override
    public CachedGameProfile getGameProfile(UUID player) {
        clearAllIfRefreshed();

        return gameProfileCache.computeIfAbsent(player, k -> reference.getGameProfile(player));
    }

    @Override
    public void compact(@Nullable Instant from, @Nullable Instant to) {
        reference.compact(from, to);
    }
}
