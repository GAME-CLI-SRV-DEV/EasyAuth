package xyz.nikitacartes.easyauth;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nikitacartes.easyauth.config.*;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.storage.PlayerCacheV0;
import xyz.nikitacartes.easyauth.storage.database.*;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class EasyAuth extends JavaPlugin {
    public static DbApi DB = null;
    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();
    public static final HashMap<String, PlayerCacheV0> playerCacheMap = new HashMap<>();
    public static final HashSet<String> mojangAccountNamesCache = new HashSet<>();
    public static Path gameDirectory;
    public static final Properties serverProp = new Properties();
    public static MainConfigV1 config;
    public static ExtendedConfigV1 extendedConfig;
    public static LangConfigV1 langConfig;
    public static TechnicalConfigV1 technicalConfig;
    public static StorageConfigV1 storageConfig;
    private static final Logger LOGGER = Bukkit.getLogger();

    @Override
    public void onEnable() {
        gameDirectory = getDataFolder().toPath();
        LOGGER.info("EasyAuth plugin by NikitaCartes");

        try {
            serverProp.load(new FileReader(new File(gameDirectory.toFile(), "server.properties")));
            if (Boolean.parseBoolean(serverProp.getProperty("enforce-secure-profile"))) {
                LOGGER.warning("Disable enforce-secure-profile to allow offline players to join the server");
                LOGGER.warning("For more info, see https://github.com/NikitaCartes/EasyAuth/issues/68");
            }
        } catch (IOException e) {
            LOGGER.severe("Error while reading server properties: " + e.getMessage());
        }

        File configDir = new File(gameDirectory.toFile(), "config/EasyAuth");
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw new RuntimeException("[EasyAuth] Error creating directory for configs");
        }

        loadConfigs();

        if (DB != null && !DB.isClosed()) {
            DB.close();
        }

        switch (EasyAuth.storageConfig.databaseType.toLowerCase()) {
            case "mysql":
                DB = new MySQL(EasyAuth.storageConfig);
                break;
            case "mongodb":
                DB = new MongoDB(EasyAuth.storageConfig);
                break;
            default:
                DB = new LevelDB(EasyAuth.storageConfig);
                break;
        }

        try {
            DB.connect();
        } catch (DBApiException e) {
            LOGGER.severe("onEnable error: " + e.getMessage());
        }

        // Registering events and commands
        getServer().getPluginManager().registerEvents(new AuthEventHandler(), this);
        getCommand("easyauth").setExecutor(new EasyAuthCommand());
    }

    @Override
    public void onDisable() {
        if (DB != null && !DB.isClosed()) {
            DB.close();
        }
        THREADPOOL.shutdown();
        try {
            if (!THREADPOOL.awaitTermination(60, TimeUnit.SECONDS)) {
                THREADPOOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.severe("Error on stop: " + e.getMessage());
            THREADPOOL.shutdownNow();
        }
    }

    private void onStartServer() {
        if (DB.isClosed()) {
            LOGGER.severe("Couldn't connect to database. Stopping server");
            Bukkit.shutdown();
        }
    }

    private void onStopServer() {
        LOGGER.info("Shutting down EasyAuth.");
        DB.saveAll(playerCacheMap);

        // Closing threads
        try {
            THREADPOOL.shutdownNow();
            if (!THREADPOOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                Thread.currentThread().interrupt();
            }
        } catch (InterruptedException e) {
            LOGGER.severe("Error on stop: " + e.getMessage());
            THREADPOOL.shutdownNow();
        }

        // Closing DbApi connection
        DB.close();
    }

    public static void loadConfigs() {
        VersionConfig version = VersionConfig.load();

        switch (version.configVersion) {
            case -1:
                EasyAuth.config = MainConfigV1.load();
                EasyAuth.config.save();

                EasyAuth.technicalConfig = TechnicalConfigV1.load();
                EasyAuth.technicalConfig.save();

                EasyAuth.langConfig = LangConfigV1.load();
                EasyAuth.langConfig.save();

                EasyAuth.extendedConfig = ExtendedConfigV1.load();
                EasyAuth.extendedConfig.save();

                EasyAuth.storageConfig = StorageConfigV1.load();
                EasyAuth.storageConfig.save();
                break;
            case 1:
                EasyAuth.config = MainConfigV1.load();
                EasyAuth.technicalConfig = TechnicalConfigV1.load();
                EasyAuth.langConfig = LangConfigV1.load();
                EasyAuth.extendedConfig = ExtendedConfigV1.load();
                EasyAuth.storageConfig = StorageConfigV1.load();
                break;
            default:
                LOGGER.severe("Unknown config version: " + version.configVersion + "\n Using last known version");
                EasyAuth.config = MainConfigV1.load();
                EasyAuth.technicalConfig = TechnicalConfigV1.load();
                EasyAuth.langConfig = LangConfigV1.load();
                EasyAuth.extendedConfig = ExtendedConfigV1.load();
                EasyAuth.storageConfig = StorageConfigV1.load();
                break;
        }
        AuthEventHandler.usernamePattern = Pattern.compile(EasyAuth.extendedConfig.usernameRegexp);
    }

    public static void saveConfigs() {
        EasyAuth.config.save();
        EasyAuth.technicalConfig.save();
        EasyAuth.langConfig.save();
        EasyAuth.extendedConfig.save();
        EasyAuth.storageConfig.save();
    }
}
