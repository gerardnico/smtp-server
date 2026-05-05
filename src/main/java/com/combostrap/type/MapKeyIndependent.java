package com.combostrap.type;



import com.combostrap.exception.InternalException;

import java.util.*;

/**
 * A map that normalize the key when looking up
 * with the {@link KeyNormalizer}
 * <p>
 * ie:
 * - case independent
 * - Space and separator independent
 *
 * @param <V>
 */
public class MapKeyIndependent<V> extends AbstractMap<String, V> implements Map<String, V> {

  /**
   * A map of normalized key to natural key
   */
  Map<KeyNormalizer, String> normalizedToNormalKeyMap = new HashMap<>();

  /**
   * The original map
   */
  Map<String, V> map = new HashMap<>();

  public static <V> MapKeyIndependent<V> createFrom(Map<?, ?> map, Class<V> classValue) {
    MapKeyIndependent<V> mapKey = new MapKeyIndependent<>();
    try {
      mapKey.putAll(Casts.castToSameMap(map, String.class, classValue));
    } catch (CastException e) {
      throw new InternalException("Should not throw as every object have a string method");
    }
    return mapKey;
  }


  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return containsKey(KeyNormalizer.createSafe(key.toString()));
  }

  public boolean containsKey(KeyNormalizer key) {
    return normalizedToNormalKeyMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  public V get(KeyNormalizer key) {
    String naturalKey = normalizedToNormalKeyMap.get(key);
    if (naturalKey == null) {
      return null;
    }
    return map.get(naturalKey);
  }


  /**
   * @param key the key whose associated value is to be returned
   * @throws IllegalArgumentException if the key does not have any letter or digit
   */
  @Override
  public V get(Object key) {
    KeyNormalizer key1 = KeyNormalizer.createSafe(key);
    return get(key1);
  }

  /**
   * @param key   key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @throws IllegalArgumentException if the key does not have any letter or digit
   */
  @Override
  public V put(String key, V value) {

    KeyNormalizer normalizedKey = KeyNormalizer.createSafe(key);
    String naturalKey = normalizedToNormalKeyMap.get(normalizedKey);
    V oldValue = null;
    if (naturalKey != null) {
      oldValue = remove(key);
    }
    normalizedToNormalKeyMap.put(normalizedKey, key);
    map.put(key, value);
    return oldValue;


  }

  /**
   * @param key key whose mapping is to be removed from the map
   * @throws IllegalArgumentException if the key does not have any letter or digit
   */
  @Override
  public V remove(Object key) {

    KeyNormalizer normalizedKey = KeyNormalizer.createSafe(key);
    String naturalKey = normalizedToNormalKeyMap.remove(normalizedKey);
    return map.remove(naturalKey);

  }


  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    for (Entry<? extends String, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    map.clear();
    normalizedToNormalKeyMap.clear();
  }


  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Set<Entry<String, V>> entrySet() {
    return map.entrySet();
  }

  /**
   * @throws CastException            - if the retrieved value could be casted
   * @throws IllegalArgumentException if the key does not have any letter or digit
   */
  public <T> T get(Object key, Class<T> clazz) throws CastException {
    V v = get(key);
    if (v == null) {
      return null;
    }
    return Casts.cast(v, clazz);
  }
}
