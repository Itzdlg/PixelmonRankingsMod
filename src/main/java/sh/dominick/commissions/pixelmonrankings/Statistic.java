package sh.dominick.commissions.pixelmonrankings;

import sh.dominick.commissions.pixelmonrankings.util.TimeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum Statistic {
    PLAY_TIME((byte) 0,
            "seconds",
            "Playing Time",
            "days",
            (real) -> TimeUtil.formatSeconds(real.longValue()) + " of playtime"),

    WILD_POKEMON_DEFEATED((byte) 1, "Wild Pokemon Defeated", "pokemon"),
    NORMAL_POKEMON_CAPTURED((byte) 2, "Normal Pokemon Captured", "pokemon"),
    POKEMON_SHINY_CAPTURED((byte) 3, "Pokemon Shiny Captured", "pokemon"),
    LEGENDARY_POKEMON_CAPTURED((byte) 4, "Legendary Pokemon Captured", "pokemon"),
    ULTRA_BEAST_CAPTURED((byte) 5, "Pokemon Ultra Beast Captured", "pokemon"),

    MADE_EGGS((byte) 6, "Made Egg", "eggs"),
    HATCHED_EGGS((byte) 7, "Hatched Egg", "eggs"),

    PVP_BATTLES_WON((byte) 8, "PVP Battles Won", "battles");

    private final byte storageId;
    private final String storageUnits;

    private final String displayName;
    private final String displayUnits;

    private final Function<Double, String> displayValueFn;

    private static final Map<Byte, Statistic> STATISTIC_TO_STORAGE_ID = new HashMap<>();

    static {
        for (Statistic stat : Statistic.values()) {
            STATISTIC_TO_STORAGE_ID.put(stat.storageId, stat);
        }
    }

    Statistic(byte storageId, String storageUnits, String displayName, String displayUnits, Function<Double, String> displayValueFn) {
        this.storageId = storageId;
        this.storageUnits = storageUnits;
        this.displayName = displayName;
        this.displayUnits = displayUnits;
        this.displayValueFn = displayValueFn;
    }

    Statistic(byte storageId, String displayName, String displayUnits) {
        this(storageId, displayUnits, displayName, displayUnits, (real) -> real.intValue() + " " + displayUnits);
    }

    public byte storageId() {
        return storageId;
    }

    public String storageUnits() {
        return storageUnits;
    }

    public String displayName() {
        return displayName;
    }

    public String displayUnits() {
        return displayUnits;
    }

    public String value(double real) {
        return displayValueFn.apply(real);
    }

    public static Statistic ofStorageId(byte storageId) {
        if (!STATISTIC_TO_STORAGE_ID.containsKey(storageId))
            throw new IllegalArgumentException("The provided statistic does not exist.");

        return STATISTIC_TO_STORAGE_ID.get(storageId);
    }
}
