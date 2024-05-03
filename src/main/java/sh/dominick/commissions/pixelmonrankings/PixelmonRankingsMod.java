package sh.dominick.commissions.pixelmonrankings;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.dominick.commissions.pixelmonrankings.commands.RankingsCommand;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.data.SQLiteDataManager;
import sh.dominick.commissions.pixelmonrankings.data.facade.CachedDataManager;
import sh.dominick.commissions.pixelmonrankings.listeners.CacheListener;
import sh.dominick.commissions.pixelmonrankings.listeners.PixelmonListener;
import sh.dominick.commissions.pixelmonrankings.listeners.PlaytimeListener;

import java.time.Instant;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PixelmonRankingsMod.MOD_ID)
public class PixelmonRankingsMod {
    public static final String MOD_ID = "pixelmonrankings";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    private final PixelmonRankingsConfig config = new PixelmonRankingsConfig();
    private IDataManager dataManager;

    public PixelmonRankingsMod() {
        // Register the config
        config.registerToForge(MOD_ID);

        if (!config.databaseType().get().equalsIgnoreCase("sqlite"))
            throw new IllegalStateException("Only `sqlite` databases are supported.");

        dataManager = new SQLiteDataManager(config.databaseUrl().get(), "changes", "profiles");

        if (config.cacheDatabase().get())
            dataManager = new CachedDataManager(dataManager,
                    config.cacheLifetime().get());

        // Register necessary event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CacheListener(dataManager));

        // Register listeners for each statistic
        MinecraftForge.EVENT_BUS.register(new PlaytimeListener(dataManager));
        MinecraftForge.EVENT_BUS.register(new PixelmonListener(dataManager));

        if (config.runStatisticsCompaction().get()) {
            long timeAgo = 60L * 1000 * 60 * 60 * 24; // 60 days
            dataManager.compact(null, Instant.ofEpochMilli(System.currentTimeMillis() - timeAgo));
        }
    }
    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent event) {
        RankingsCommand.register(this, event.getDispatcher());
    }

    public PixelmonRankingsConfig config() {
        return config;
    }

    public IDataManager dataManager() {
        return dataManager;
    }
}
