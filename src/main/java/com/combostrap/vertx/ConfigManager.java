package com.combostrap.vertx;

import com.combostrap.common.Fs;
import com.combostrap.common.JavaEnvs;
import com.combostrap.exception.InternalException;
import com.combostrap.smtp.exceptions.ConfigIllegalException;
import com.combostrap.type.KeyNormalizer;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A wrapper around a {@link ConfigRetriever}
 * to manage the configuration
 */
public class ConfigManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());

    private static ConfigRetriever configRetriever;
    public final Vertx vertx;
    private final Path secretFilePath;
    private final JsonObject actualConfig;

    /**
     * This file is never deleted in contrario to the secret file.
     * <p>
     * In dev, this file may contain also basic secrets such as admin/admin
     * because the secret file is deleted
     * after the web server starts.
     */
    private final Path configFile;

    private final String configName;


    public ConfigManager(ConfigManagerConfig configManagerConfig) {

        this.configName = configManagerConfig.name;
        this.vertx = configManagerConfig.vertx;

        Path currentPath = Paths.get(".");

        if (configManagerConfig.confSecretFilePath == null) {
            Path secretFilePath = currentPath.resolve("." + this.configName + ".secret.yml").normalize();
            if (!Files.exists(secretFilePath)) {
                Path envSecretFilePath = currentPath.resolve("." + this.configName + ".secret.env").normalize();
                if (Files.exists(envSecretFilePath)) {
                    secretFilePath = envSecretFilePath;
                }
            }
            this.secretFilePath = secretFilePath;
        } else {
            this.secretFilePath = configManagerConfig.confSecretFilePath;
        }
        //noinspection ReplaceNullCheck
        if (configManagerConfig.confFilePath == null) {
            this.configFile = currentPath.resolve("." + this.configName + ".yml").normalize();
        } else {
            this.configFile = configManagerConfig.confFilePath;
        }

        this.actualConfig = configManagerConfig.jsonConfig;


        /**
         * Vertex Config
         * The ConfigRetriever provide a way to access the stream of configuration. It’s a ReadStream of JsonObject.
         * By registering the right set of handlers you are notified:
         *   * when a new configuration is retrieved
         *   * when an error occur while retrieving a configuration
         *   * when the configuration retriever is closed (the endHandler is called).
         */
        ConfigRetrieverOptions configRetrieverOptions = buildConfigStoresAndOptions();
        configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

    }

    /**
     * Use {@link #getConfigAccessor()} instead
     */
    private Future<JsonObject> getConfig() {
        return configRetriever.getConfig()
                .compose(json -> {

                    if (Files.exists(this.secretFilePath)) {
                        if (!JavaEnvs.isDev()) {
                            try {
                                Fs.write(this.secretFilePath, "");
                                // we don't delete because the retriever will try to load it
                                LOGGER.info("The secret configuration file content was deleted.");
                            } catch (RuntimeException e) {
                                String errorMessage = "The secret configuration file content could not be deleted";
                                InternalException internal = new InternalException(errorMessage, e);
                                return Future.failedFuture(internal);
                            }
                        }
                    }

                    /**
                     * In test, the verticle configurations take over
                     * (for instance to set the port)
                     */
                    json.mergeIn(actualConfig);

                    return Future.succeededFuture(json);

                });
    }

    public Future<ConfigAccessor> getConfigAccessor() {
        return getConfig()
                .compose(jsonObject -> {
                    ConfigAccessor result;
                    try {
                        result = ConfigAccessor.createManually(this.configName, jsonObject)
                                .setConfigManager(this);
                    } catch (ConfigIllegalException e) {
                        return Future.failedFuture(e);
                    }
                    return Future.succeededFuture(result);
                });
    }

    public String getName() {
        return this.configName;
    }


    /**
     * @param name   - the name of the configuration used to create the conf file name and env
     * @param vertx  - vertx (needed by vertx ConfigRetriever)
     *               We don't pass the {@link io.vertx.core.Verticle} to be able
     *               to test it without starting a verticle
     * @param config - the extra configuration (We make it mandatory so that the verticle config is always given)
     */
    public static ConfigManagerConfig config(String name, Vertx vertx, JsonObject config) {

        return new ConfigManagerConfig(name, vertx, config);

    }

    /**
     * <a href="https://vertx.io/docs/vertx-config/java/">Doc</a>
     *
     * @return a configuration object that defines a list of store where to find the configuration
     */
    private ConfigRetrieverOptions buildConfigStoresAndOptions() {

        ConfigRetrieverOptions configRetriever = new ConfigRetrieverOptions();

        /**
         * Retrieve the config data
         * File in the resources directory
         * <a href="https://vertx.io/docs/vertx-config/java/#_yaml_configuration_format">Doc</a>
         * We use Yaml and not properties because it can handle multiple line values.
         * It's handy for cryptographic key
         */
        if (Files.exists(this.configFile)) {
            LOGGER.info("Configuration: " + configName + " configuration file found (" + this.configFile.toAbsolutePath() + ")");
        } else {
            LOGGER.warning("Configuration: " + configName + " configuration file not found (" + this.configFile.toAbsolutePath() + ")");
        }

        ConfigStoreOptions yamlFile = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setOptional(true)
                .setConfig(new JsonObject()
                        .put("path", this.configFile.toAbsolutePath().toString())
                        .put("raw-data", true)
                );

        /**
         * Secret
         */
        ConfigStoreOptions secretFile = new ConfigStoreOptions()
                .setType("file")
                .setOptional(true)
                .setConfig(new JsonObject()
                        .put("path", this.secretFilePath.toAbsolutePath().toString())
                        .put("raw-data", true)
                );
        String secretFileExtension = Fs.getExtension(secretFilePath);
        switch (secretFileExtension) {
            case "env":
            case "properties":
                // env file (ie property) are used by third party to configure secret (fly.io, ...)
                // https://vertx.io/docs/vertx-config/java/#_file
                secretFile.setFormat("properties");
                break;
            default:
            case "yml":
            case "yaml":
                secretFile.setFormat("yaml");
                break;
        }
        if (Files.exists(secretFilePath)) {
            LOGGER.info("Configuration: " + configName + " secret configuration file found (" + secretFilePath.toAbsolutePath() + ")");
        } else {
            LOGGER.warning("Configuration: " + configName + " secret configuration file not found (" + secretFilePath.toAbsolutePath() + ")");
        }


        /**
         * Env store
         * <a href="https://vertx.io/docs/vertx-config/java/#_environment_variables">Doc</a>
         */
        LOGGER.info("Configuration: Loading environment");
        JsonArray envVarKeys = new JsonArray();
        String envConfigName = KeyNormalizer.createSafe(this.configName).toEnvName();
        Map<String, String> systemEnvs = System.getenv();
        systemEnvs.keySet()
                .forEach(s -> {
                    if (s.startsWith(envConfigName)) {
                        LOGGER.info("Configuration: environment variable (" + s + ") found and monitored");
                        envVarKeys.add(s);
                    }
                });
        ConfigStoreOptions environment = new ConfigStoreOptions()
                .setType("env")
                .setOptional(true)
                .setConfig(
                        new JsonObject()
                                .put("keys", envVarKeys)
                );

        /**
         * Sys store
         */
        ConfigStoreOptions sysPropsStore = new ConfigStoreOptions()
                .setType("sys")
                .setConfig(
                        new JsonObject()
                                .put("cache", true)
                                .put("raw-data", false)
                );

        return configRetriever
                .addStore(environment)
                .addStore(secretFile)
                .addStore(yamlFile)
                .addStore(sysPropsStore)
                .setScanPeriod(10000);


    }

    /**
     * @return the path of the expected config file
     */
    public Path getConfigPath() {
        return this.configFile;
    }


    public static class ConfigManagerConfig {

        private final String name;
        private final Vertx vertx;
        private final JsonObject jsonConfig;
        private Path confSecretFilePath;
        private Path confFilePath;

        public ConfigManagerConfig(String name, Vertx vertx, JsonObject config) {
            this.name = name;
            this.vertx = vertx;
            this.jsonConfig = config;
        }

        public ConfigManagerConfig setSecretFilePath(Path secretFilePath) {
            this.confSecretFilePath = secretFilePath;
            return this;
        }


        public ConfigManager build() {

            return new ConfigManager(this);

        }

        public ConfigManagerConfig setFilePath(Path path) {
            this.confFilePath = path;
            return this;
        }
    }
}
