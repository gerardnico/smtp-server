package com.combostrap.type;

import java.util.*;

public class Maps {

  /**
   * Search the map for a key where the case does not matter
   * @return an object or null if not found
   */
  public static Object getPropertyCaseIndependent(Map<String, Object> properties, String key) {
    KeyNormalizer keyNormalizer = KeyNormalizer.createSafe(key);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      if (keyNormalizer.equals(KeyNormalizer.createSafe(entry.getKey()))) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * @return a list of the entry sorted in natural order
   */
  public static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> getMapAsListEntrySortedByValue(Map<K, V> map) {

    // Making a list of entry
    List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
    // Sorting
    list.sort(Map.Entry.comparingByValue());

    return list;
  }

  public static <K, V extends Comparable<? super V>> Map<K, V> getMapSortByValue(Map<K, V> map) {

    Map<K, V> result = new LinkedHashMap<>();
    for (Map.Entry<K, V> entry : getMapAsListEntrySortedByValue(map)) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;

  }

  public static <K, V extends Comparable<? super V>> Map<K, V> getMapSortByKey(Map<K, V> map) {

    /**
     * A tree map sort the map naturally by key
     */
    if (map instanceof TreeMap) {
      return map;
    } else {
      return new TreeMap<>(map);
    }

  }

  /**
   * A build such as in Java 9 to build a map
   *
   * @param elements - an even number of elements
   * @param <T>
   * @return a {@link HashMap} with all the elements given
   */
  public static <T> Map<T, T> of(T... elements) {
    Map<T, T> map = new HashMap<>();
    if (elements.length % 2 != 0) {
      throw new RuntimeException("The number of elements must be an even number. The number of elements given (" + elements.length + ") is uneven.");
    }
    for (int i = 0; i < elements.length; i = i + 2) {
      map.put(elements[i], elements[i + 1]);
    }
    return map;
  }

  public static <K, V> Properties toProperties(Map<K, V> mapProperties) {
    Properties properties = new Properties();
    properties.putAll(mapProperties);
    return properties;
  }
}
