package sh.dominick.commissions.pixelmonrankings;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
import sh.dominick.commissions.pixelmonrankings.data.SQLiteDataManager;
import sh.dominick.commissions.pixelmonrankings.data.facade.CachedDataManager;
import sh.dominick.commissions.pixelmonrankings.listeners.CacheListener;
import sh.dominick.commissions.pixelmonrankings.listeners.PixelmonListener;
import sh.dominick.commissions.pixelmonrankings.listeners.PlaytimeListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PixelmonRankingsMod.MOD_ID)
public class PixelmonRankingsMod {
    public static final String MOD_ID = "pixelmonrankings";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    private Path configPath, langPath;
    private ConfigurationLoader<?> configLoader, langLoader;
    private ConfigurationNode configNode, langNode;

    private PixelmonRankingsConfig config = new PixelmonRankingsConfig();
    private PixelmonRankingsLang langDefinition = new PixelmonRankingsLang();
    private IDataManager dataManager;

    public PixelmonRankingsMod() {
        // Configurations
        try {
            configPath = new File("./mods/PixelmonRankings/config.yml").toPath();
            configLoader = createLoader(configPath);
            configNode = configLoader.load();
            config = configNode.get(PixelmonRankingsConfig.class);

            configNode.set(PixelmonRankingsConfig.class, config);
            configLoader.save(configNode);

            langPath = new File("./mods/PixelmonRankings/lang.yml").toPath();
            langLoader = createLoader(langPath);
            langNode = langLoader.load();
            langDefinition = langNode.get(PixelmonRankingsLang.class);

            langNode.set(PixelmonRankingsLang.class, langDefinition);
            langLoader.save(langNode);
        } catch (ConfigurateException ex) {
            throw new RuntimeException(ex);
        }

        if (!config.database.type.equalsIgnoreCase("sqlite"))
            throw new IllegalStateException("Only `sqlite` databases are supported.");

        dataManager = new SQLiteDataManager(config.database.url, "changes", "profiles");

        if (config.cache.enabled)
            dataManager = new CachedDataManager(dataManager,
                    config.cache.lifetime);

        // Register necessary event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CacheListener(dataManager));

        // Register listeners for each statistic
        MinecraftForge.EVENT_BUS.register(new PlaytimeListener(dataManager));
        MinecraftForge.EVENT_BUS.register(new PixelmonListener(dataManager));

        if (config.database.compact) {
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
