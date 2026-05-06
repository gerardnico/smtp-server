package com.combostrap.vertx;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonConverter {

    /**
     * {@link JsonObject#getMap()} transform only the first level
     *
     * @param jsonObject - a json object
     * @return a map
     */
    public static Map<String, Object> toDeepMap(JsonObject jsonObject) {
        return jsonObject.getMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> convertValue(e.getValue())
                ));
    }

    private static Object convertValue(Object value) {
        if (value instanceof JsonObject) {
            return toDeepMap((JsonObject) value);
        } else if (value instanceof JsonArray) {
            return toDeepList((JsonArray) value);
        }
        return value;
    }

    private static List<Object> toDeepList(JsonArray jsonArray) {
        return IntStream.range(0, jsonArray.size())
                .mapToObj(jsonArray::getValue)
                .map(JsonConverter::convertValue)
                .collect(Collectors.toList());
    }

}
