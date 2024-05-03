package sh.dominick.commissions.pixelmonrankings;

import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

public class PixelmonRankingsConfig {
    public static class StatisticConfig {
        private final ForgeConfigSpec.ConfigValue<Integer> itemPosition;
        private final ForgeConfigSpec.ConfigValue<String> itemMaterial;
        private final ForgeConfigSpec.ConfigValue<Integer> itemAmount;

        private final ForgeConfigSpec.ConfigValue<String> itemDisplayName;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> itemLore;

        public StatisticConfig(ForgeConfigSpec.ConfigValue<Integer> itemPosition, ForgeConfigSpec.ConfigValue<String> itemMaterial, ForgeConfigSpec.ConfigValue<Integer> itemAmount, ForgeConfigSpec.ConfigValue<String> itemDisplayName, ForgeConfigSpec.ConfigValue<List<? extends String>> itemLore) {
            this.itemPosition = itemPosition;
            this.itemMaterial = itemMaterial;
            this.itemAmount = itemAmount;
            this.itemDisplayName = itemDisplayName;
            this.itemLore = itemLore;
        }

        public ForgeConfigSpec.ConfigValue<Integer> itemPosition() {
            return itemPosition;
        }

        public ForgeConfigSpec.ConfigValue<String> itemMaterial() {
            return itemMaterial;
        }

        public ForgeConfigSpec.ConfigValue<Integer> itemAmount() {
            return itemAmount;
        }

        public ForgeConfigSpec.ConfigValue<String> itemDisplayName() {
            return itemDisplayName;
        }

        public ForgeConfigSpec.ConfigValue<List<? extends String>> itemLore() {
            return itemLore;
        }
    }

    protected final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    private final ForgeConfigSpec.ConfigValue<Boolean> devMode;

    private final ForgeConfigSpec.ConfigValue<String> databaseType;
    private final ForgeConfigSpec.ConfigValue<String> databaseUrl;
    private final ForgeConfigSpec.ConfigValue<Boolean> runStatisticsCompaction;

    private final ForgeConfigSpec.ConfigValue<Boolean> cacheDatabase;
    private final ForgeConfigSpec.ConfigValue<Long> cacheLifetime;

    private final Map<Statistic, StatisticConfig> statisticConfigMap = new HashMap<>();

    public PixelmonRankingsConfig() {
        devMode = builder.define("devmode", false);

        databaseType = builder.defineInList("database.type", "sqlite", Collections.singletonList("sqlite"));
        databaseUrl = builder.define("database.url", "jdbc:sqlite:pixelmonrankings_statistics.db");
        runStatisticsCompaction = builder.comment("Compact records older than 60 days?").define("database.compact", true);

        cacheDatabase = builder.define("cache.enabled", true);
        cacheLifetime = builder.define("cache.lifetime", 5 * 60 * 1000L);

        Collection<String> materials = ForgeRegistries.ITEMS.getKeys().stream().map(ResourceLocation::getPath).collect(Collectors.toList());
        for (Statistic statistic : Statistic.values()) {
            String configKey = "statistics." + statistic.name() + ".item.";

            statisticConfigMap.put(statistic, new StatisticConfig(
                    builder.define(configKey + "position", statistic.ordinal()),
                    builder.defineInList(configKey + "material", "minecraft:clay", materials),
                    builder.define(configKey + "amount", 1),
                    builder.define(configKey + "display_name", statistic.displayName()),
                    builder.defineList(configKey + "lore", Arrays.asList("..."), (it) -> true)
            ));
        }
    }

    public void registerToForge(String modId) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, builder.build(), modId + ".toml");
    }

    public ForgeConfigSpec.ConfigValue<Boolean> devMode() {
        return devMode;
    }

    public ForgeConfigSpec.ConfigValue<String> databaseType() {
        return databaseType;
    }

    public ForgeConfigSpec.ConfigValue<String> databaseUrl() {
        return databaseUrl;
    }

    public ForgeConfigSpec.ConfigValue<Boolean> runStatisticsCompaction() {
        return runStatisticsCompaction;
    }

    public ForgeConfigSpec.ConfigValue<Boolean> cacheDatabase() {
        return cacheDatabase;
    }

    public ForgeConfigSpec.ConfigValue<Long> cacheLifetime() {
        return cacheLifetime;
    }

    public StatisticConfig statistic(Statistic statistic) {
        if (!statisticConfigMap.containsKey(statistic))
            throw new IllegalArgumentException();

        return statisticConfigMap.get(statistic);
    }
}
