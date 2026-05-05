package com.combostrap.vertx;

import com.combostrap.exception.InternalException;
import com.combostrap.smtp.exceptions.ConfigIllegalException;
import com.combostrap.smtp.exceptions.NoConfException;
import com.combostrap.type.KeyNormalizer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A config accessor is a proxy through config stores
 * <p>
 * it helps retrieve configuration
 * from:
 * * the conf file (yaml) that does not have any prefix
 * * the environment that does have a prefix and underscore separator
 */
public class ConfigAccessor {

    private final JsonObject jsonObject;
    private final List<String> keys;
    private final ConfigAccessor parentConfigAccessor;
    private ConfigManager configManager;

    /**
     * @param keys                 - the key or the prefix for the root config accessor
     * @param jsonObject           - the data for the root or null
     * @param parentConfigAccessor - the parent or null
     */
    private ConfigAccessor(List<String> keys, JsonObject jsonObject, ConfigAccessor parentConfigAccessor) throws ConfigIllegalException {
        this.parentConfigAccessor = parentConfigAccessor;
        if (keys.isEmpty()) {
            throw new InternalException("Internal Error: We should have at least one key");
        }
        if (this.parentConfigAccessor != null) {
            JsonObject actualData = parentConfigAccessor.getJsonObject(keys.get(0));
            for (int i = 1; i < keys.size(); i++) {
                String key = keys.get(i);
                actualData = actualData.getJsonObject(key);
            }
            if (actualData == null) {
                throw new ConfigIllegalException("The sub keys (" + String.join(",", keys) + ") has no data");
            }
            this.jsonObject = actualData;
        } else {
            this.jsonObject = jsonObject;
            if (this.jsonObject == null) {
                throw new InternalException("Internal Error: The json object should not be null");
            }
        }
        this.keys = keys;
    }

    public static ConfigAccessor createManually(String key, JsonObject jsonObject) throws ConfigIllegalException {
        return new ConfigAccessor(Collections.singletonList(key), jsonObject, null);
    }

    public static ConfigAccessor empty() {
        try {
            return new ConfigAccessor(Collections.singletonList("empty"), new JsonObject(), null);
        } catch (ConfigIllegalException e) {
            throw new InternalException(e);
        }
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String def) {
        String value = this.getEnvJson().getString(getEnvName(key));
        if (value != null) {
            return value;
        }
        return this.jsonObject.getString(key, def);
    }

    /**
     * @return the root json that contains the environment variable
     */
    private JsonObject getEnvJson() {
        ConfigAccessor parent = this;
        while (parent.parentConfigAccessor != null) {
            parent = parent.parentConfigAccessor;
        }
        // the root
        return parent.jsonObject;
    }

    /**
     * @param key - the searched key
     * @return - the env name (in generally used in the env)
     */
    private String getEnvName(String key) {

        ConfigAccessor parent = this;
        List<String> keyParts = new ArrayList<>();
        while (parent != null) {
            ArrayList<String> parentKeys = new ArrayList<>(parent.keys);
            Collections.reverse(parentKeys);
            keyParts.addAll(parentKeys);
            parent = parent.parentConfigAccessor;
        }
        Collections.reverse(keyParts);
        keyParts.add(key);
        String envName = String.join(KeyNormalizer.UNDERSCORE, keyParts).toUpperCase();
        return KeyNormalizer.createSafe(envName).toEnvName();

    }

    public Integer getInteger(String key) {
        return getInteger(key, null);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        Integer value = this.getEnvJson().getInteger(getEnvName(key));
        if (value != null) {
            return value;
        }
        return this.jsonObject.getInteger(key, defaultValue);
    }

    public JsonObject getConfig() {
        return this.jsonObject;
    }

    public Object getValue(String key) {
        return getValue(key, null);
    }

    public Object getValue(String key, Object defaultValue) {
        Object value = this.getEnvJson().getValue(getEnvName(key));
        if (value != null) {
            return value;
        }
        return this.jsonObject.getValue(key, defaultValue);
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean b) {
        Boolean value = this.getEnvJson().getBoolean(getEnvName(key));
        if (value != null) {
            return value;
        }
        return this.jsonObject.getBoolean(key, b);
    }

    public JsonObject getJsonObject(String key) {
        return this.jsonObject.getJsonObject(key);
    }

    public ConfigAccessor getSubConfigAccessor(String key, String... keys) throws ConfigIllegalException {
        List<String> finalKeys = new ArrayList<>();
        finalKeys.add(key);
        if (keys != null) {
            finalKeys.addAll(Arrays.asList(keys));
        }
        return new ConfigAccessor(finalKeys, null, this);
    }

    public long getLong(String key, long defaultValue) {
        Long value = this.getEnvJson().getLong(getEnvName(key));
        if (value != null) {
            return value;
        }
        return this.jsonObject.getLong(key, defaultValue);
    }


    public List<String> getList(String key) {
        JsonArray jsonArray = this.jsonObject.getJsonArray(key);
        if (jsonArray == null) {
            return new ArrayList<>();
        }
        return jsonArray.stream().map(Object::toString).collect(Collectors.toList());
    }


    public Path getPath(String key, Path defaultValue) {
        String path = getString(key);
        if (path == null) {
            return defaultValue;
        }
        return Paths.get(path);
    }

    /**
     * @throws NoConfException if the conf is not found
     */
    public Object getMandatoryValue(String key) throws NoConfException {
        Object value = getValue(key);
        if (value == null) {
            String s = "The conf key " + key + " was not found and is mandatory.\nYou can add it\n  * as env with the name " + getEnvName(key);
            if (this.configManager != null) {
                Path configPath = this.configManager.getConfigPath();
                s += "\n  * in the config file " + configPath.toAbsolutePath();
                if (!Files.exists(configPath)) {
                    s += " (The config file was not found, Working directory: " + Paths.get(".").toAbsolutePath() + ")";
                }
            }
            throw new NoConfException(s);
        }
        return value;
    }

    public ConfigAccessor setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
        return this;
    }
}
