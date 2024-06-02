package sh.dominick.commissions.pixelmonrankings.data.facade;

import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.util.TimeUtil;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
    public CompletableFuture<Void> recordChange(Key key, double value) {
        Double valueAllTime = aggregateCache.get(AggregateParams.key(key, null, null));
        if (valueAllTime != null)
            aggregateCache.put(AggregateParams.key(key, null, null), valueAllTime + value);

        Double valueThisMonth = aggregateCache.get(AggregateParams.key(key, TimeUtil.getStartOfMonth(), TimeUtil.getEndOfMonth()));
        if (valueThisMonth != null)
            aggregateCache.put(AggregateParams.key(key, TimeUtil.getStartOfMonth(), TimeUtil.getEndOfMonth()), valueThisMonth + value);

        return reference.recordChange(key, value);
    }

    private <K, V> CompletableFuture<V> cacheOrComplete(Map<K, V> cache, K cacheKey, Supplier<CompletableFuture<V>> supplier) {
        V cacheValue = cache.get(cacheKey);
        if (cacheValue != null)
            return CompletableFuture.completedFuture(cacheValue);

        return supplier.get().thenApply((v) -> {
            cache.put(cacheKey, v);
            return v;
        });
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
    public CompletableFuture<Double> aggregate(Key key, @Nullable Instant from, @Nullable Instant to) {
        clearAllIfRefreshed();
        return cacheOrComplete(aggregateCache, AggregateParams.key(key, from, to), () -> reference.aggregate(key, from, to));
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
    public CompletableFuture<Entry[]> sort(Statistic statistic, @Nullable Instant from, @Nullable Instant to, int limit) {
        clearAllIfRefreshed();
        return cacheOrComplete(sortCache, SortParams.key(statistic, from, to, limit), () -> reference.sort(statistic, from, to, limit));
    }

    protected static class FindPositionSortedParams {
        private static String key(Key key, @Nullable Instant from, @Nullable Instant to) {
            return AggregateParams.key(key, from, to);
        }
    }

    protected Map<String, Long> findPositionSortedCache = new HashMap<>();

    @Override
    public CompletableFuture<Long> findPositionSorted(Key key, @Nullable Instant from, @Nullable Instant to) {
        clearAllIfRefreshed();
        return cacheOrComplete(findPositionSortedCache, FindPositionSortedParams.key(key, from, to), () -> reference.findPositionSorted(key, from, to));
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
    public CompletableFuture<Long> count(Statistic statistic, @Nullable Instant from, @Nullable Instant to) {
        clearAllIfRefreshed();
        return cacheOrComplete(countCache, CountParams.key(statistic, from, to), () -> reference.count(statistic, from, to));
    }

    @Override
    public CompletableFuture<Void> recordGameProfile(UUID player, String playerName, String texture) {
        reference.recordGameProfile(player, playerName, texture);
        return null;
    }

    protected final Map<UUID, CachedGameProfile> gameProfileCache = new HashMap<>();

    @Override
    public CompletableFuture<CachedGameProfile> getGameProfile(UUID player) {
        clearAllIfRefreshed();
        return cacheOrComplete(gameProfileCache, player, () -> reference.getGameProfile(player));
    }

    @Override
    public CompletableFuture<Void> compact(@Nullable Instant from, @Nullable Instant to) {
        return reference.compact(from, to);
    }
}
