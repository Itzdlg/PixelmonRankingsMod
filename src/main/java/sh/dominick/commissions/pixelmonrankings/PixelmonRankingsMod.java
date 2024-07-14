package sh.dominick.commissions.pixelmonrankings;

import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.NodeResolver;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import sh.dominick.commissions.pixelmonrankings.commands.RankingsCommand;
import sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsConfig;
import sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.data.MySQLDataManager;
import sh.dominick.commissions.pixelmonrankings.data.SQLiteDataManager;
import sh.dominick.commissions.pixelmonrankings.data.facade.CachedDataManager;
import sh.dominick.commissions.pixelmonrankings.listeners.CacheListener;
import sh.dominick.commissions.pixelmonrankings.listeners.PixelmonListener;
import sh.dominick.commissions.pixelmonrankings.listeners.PlaytimeListener;
import sh.dominick.commissions.pixelmonrankings.util.GzipUtil;
import sh.dominick.commissions.pixelmonrankings.util.TimeUtil;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PixelmonRankingsMod.MOD_ID)
public class PixelmonRankingsMod {
    public static final Executor EXECUTOR = Executors.newCachedThreadPool();

    public static final String MOD_ID = "pixelmonrankings";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    private final PixelmonRankingsConfig config;
    private final PixelmonRankingsLang langDefinition;
    private IDataManager dataManager;

    public PixelmonRankingsMod() {
        // Configurations
        try {
            Path configPath = new File("./config/PixelmonRankings/config.yml").toPath();
            ConfigurationLoader<?> configLoader = createLoader(configPath);
            ConfigurationNode configNode = configLoader.load();
            config = configNode.get(PixelmonRankingsConfig.class);

            configNode.set(PixelmonRankingsConfig.class, config);
            configLoader.save(configNode);

            Path langPath = new File("./config/PixelmonRankings/lang.yml").toPath();
            ConfigurationLoader<?> langLoader = createLoader(langPath);
            ConfigurationNode langNode = langLoader.load();
            langDefinition = langNode.get(PixelmonRankingsLang.class);

            langNode.set(PixelmonRankingsLang.class, langDefinition);
            langLoader.save(langNode);
        } catch (ConfigurateException ex) {
            throw new RuntimeException(ex);
        }

        if (config.database.backup.enabled && config.database.type.equals("sqlite")) {
            String databaseUrl = config.database.url;
            String fileUrl = databaseUrl.substring(config.database.url.lastIndexOf(':') + 1);

            File currentDatabaseFile = new File(fileUrl);
            String backupFolder = fileUrl + ".backups";
            File backupFile = new File(backupFolder, TimeUtil.getISODate() + ".db.gz");
            if (!backupFile.getParentFile().exists())
                backupFile.getParentFile().mkdirs();

            if (!backupFile.exists()) // Only backup once per day
                GzipUtil.gzip(currentDatabaseFile, backupFile);

            if (config.database.backup.deleteOld) {
                // Delete old and then 2 more in case server was offline.

                new ArrayList<LocalDate>() {{
                    this.add(LocalDate.now().minusDays(config.database.backup.daysOld));
                    this.add(LocalDate.now().minusDays(config.database.backup.daysOld + 1));
                    this.add(LocalDate.now().minusDays(config.database.backup.daysOld + 2));
                }}.stream().map(it -> new File(backupFolder, TimeUtil.getISODate(it) + ".db.gz")).forEach(it -> {
                    if (it.exists()) it.delete();
                });
            }
        }

        if (config.database.type.equals("sqlite"))
            dataManager = new SQLiteDataManager(config.database.url, "changes", "profiles");
        else if (config.database.type.equals("mysql"))
            dataManager = new MySQLDataManager(config.database.url, "changes", "profiles");
        else throw new RuntimeException("Illegal option `" + config.database.type + "` for database.type");

        if (config.cache.enabled)
            dataManager = new CachedDataManager(dataManager, config.cache.lifetime);

        // Register necessary event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CacheListener(this, dataManager));

        // Register listeners for each statistic
        MinecraftForge.EVENT_BUS.register(new PlaytimeListener(dataManager));
        Pixelmon.EVENT_BUS.register(new PixelmonListener(dataManager));

        if (config.database.compact.enabled) {
            long timeAgo = (config.database.compact.daysUnprocessed) * 1000 * 60 * 60 * 24; // X days
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

    public PixelmonRankingsLang lang() {
        return langDefinition;
    }

    public IDataManager dataManager() {
        return dataManager;
    }

    private ConfigurationLoader<?> createLoader(final Path source) {
        final ObjectMapper.Factory customFactory = ObjectMapper.factoryBuilder()
                .addNodeResolver(NodeResolver.onlyWithSetting())
                .build();

        return YamlConfigurationLoader.builder()
                .path(source)
                .defaultOptions(opts -> opts.serializers(build -> build.registerAnnotatedObjects(customFactory)))
                .nodeStyle(NodeStyle.BLOCK)
                .build();
    }
}
