package com.combostrap.type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MediaType {

    private final String type;
    private final String subtype;
    private final Map<String, String> parameters;

    private MediaType(String type, String subtype, Map<String, String> parameters) {
        this.type = type.toLowerCase();
        this.subtype = subtype.toLowerCase();
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public static MediaType of(String type, String subtype) {
        return new MediaType(type, subtype, Map.of());
    }

    public static MediaType parse(String value) {
        Objects.requireNonNull(value, "Media type value must not be null");

        String[] parts = value.split(";");
        String[] typeParts = parts[0].trim().split("/");

        if (typeParts.length != 2) {
            throw new IllegalArgumentException("Invalid media type: " + value);
        }

        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].trim().split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].toLowerCase(), kv[1]);
            }
        }

        return new MediaType(typeParts[0], typeParts[1], params);
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subtype;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameter(String name) {
        return parameters.get(name.toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type).append('/').append(subtype);
        parameters.forEach((k, v) -> sb.append("; ").append(k).append('=').append(v));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaType)) return false;
        MediaType that = (MediaType) o;
        return type.equals(that.type)
                && subtype.equals(that.subtype)
                && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subtype, parameters);
    }

    // Common constants (add as needed)
    public static final MediaType TEXT_PLAIN = MediaType.of("text", "plain");
    public static final MediaType TEXT_HTML = MediaType.of("text", "html");
    public static final MediaType TEXT_JSON = MediaType.of("text", "json");
    public static final MediaType DIR = MediaType.of("inode", "directory");
    public static final MediaType BINARY = MediaType.of("application", "octet-stream");

}

