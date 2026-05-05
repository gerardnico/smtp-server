package com.combostrap.type;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@SuppressWarnings("unused")
public class Casts {

    /**
     * The strings that are transformed to the NULL value when cast
     */
    protected static Set<String> nullableStrings = new HashSet<>(Arrays.asList("", "null", "na"));
    /**
     * The map that contains the widening map.
     * All first element can be cast to the second element without losing any information.
     */
    static final Map<Class<?>, Set<Class<?>>> wideningMap = new HashMap<>();

    static {

        wideningMap.put(byte.class, Set.of(short.class, int.class, long.class, float.class, double.class));
        wideningMap.put(short.class, Set.of(int.class, long.class, float.class, double.class));
        wideningMap.put(int.class, Set.of(long.class, float.class, double.class));
        wideningMap.put(long.class, Set.of(float.class, double.class));
        wideningMap.put(float.class, Set.of(double.class));
        // Include wrapper classes
        wideningMap.put(Byte.class, Set.of(Short.class, Integer.class, Long.class, Float.class, Double.class));
        wideningMap.put(Short.class, Set.of(Integer.class, Long.class, Float.class, Double.class));
        wideningMap.put(Integer.class, Set.of(Long.class, Float.class, Double.class));
        wideningMap.put(Long.class, Set.of(Float.class, Double.class));
        wideningMap.put(Float.class, Set.of(Double.class));
        // time
        wideningMap.put(java.sql.Date.class, Set.of(java.sql.Timestamp.class));
        wideningMap.put(java.sql.Time.class, Set.of(java.sql.Timestamp.class));
        // character
        wideningMap.put(char.class, Set.of(String.class));

    }

    /**
     * Cast to a collection
     *
     * @param sourceObject - the source
     * @param typeClazz    - the collection type
     * @param elementClass - the element type
     * @param <T>          - the collection type
     * @param <E>          - the element type
     * @return the collection typed with these elements
     * @throws CastException if any errors
     */
    public static <T, E> T cast(Object sourceObject, Class<T> typeClazz, Class<E> elementClass) throws CastException {

        if (sourceObject == null) {
            return null;
        }

        if (elementClass == null) {
            return cast(sourceObject, typeClazz);
        }

        boolean isCollection = Collection.class.isAssignableFrom(typeClazz);
        if (!isCollection) {
            throw new CastException("The class " + typeClazz + " is not a collection");
        }

        if (sourceObject.getClass().equals(typeClazz)) {
            if (sourceObject.getClass().getComponentType() == elementClass) {
                return typeClazz.cast(sourceObject);
            }
        }

        // Collection Target Type or Element Type is different
        // We need to create a new one and to add the element
        Collection<E> target;
        try {
            //noinspection unchecked
            target = (Collection<E>) typeClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new CastException(e);
        } catch (NoSuchMethodException e) {
            throw new CastException(typeClazz + " is a interface, not a collection type", e);
        }
        for (Object value : (Collection<?>) sourceObject) {
            target.add(Casts.cast(value, elementClass));
        }
        return typeClazz.cast(target);


    }


    /**
     * @param sourceObject - the object to cast
     * @param targetClass  - the class to cast
     * @param <T>          - the receiver class
     * @return null if the object is null, throw an exception if the class is not the expected one
     * the object to the asked clazz
     * @throws CastException when the cast does not work
     *                       If the class is an interface, we just check if it's an instance of and fail otherwise
     *                       Example: Number can be an integer, a float, a double, ...
     */
    public static <T> T cast(Object sourceObject, Class<T> targetClass) throws CastException {

        /*
         * Null
         */
        if (sourceObject == null) {
            return null;
        }


        if (targetClass == Number.class) {
            if (sourceObject instanceof Number) {
                /*
                 * Number is an interface and
                 * can't be instantiated
                 * We just return the object
                 */
                return targetClass.cast(sourceObject);
            }
            throw new CastException("The source object is not a number. Value: " + sourceObject);
        }

        try {

            Class<?> sourceObjectClass = sourceObject.getClass();

            /*
             * Same class
             */
            if (sourceObjectClass.equals(targetClass)) {
                return targetClass.cast(sourceObject);
            }

            /*
             * Array
             */
            if (sourceObjectClass.isArray()) {
                if (!targetClass.isArray()) {
                    if (targetClass.equals(String.class)) {
                        String[] values = castToArray(sourceObject, String.class);
                        return targetClass.cast(String.join(", ", values));
                    }
                    throw new CastException("The source object is an array and the target class is not");
                }
                return targetClass.cast(castToArray(sourceObject, targetClass.getComponentType()));
            }

            /*
             * Nullable string
             */
            if (targetClass != String.class && sourceObjectClass == String.class) {
                if (nullableStrings.contains(sourceObject)) {
                    return null;
                }
            }

            /*
             * Long
             */
            if (targetClass == Long.class) {
                return targetClass.cast(Longs.createFromObject(sourceObject).toLong());
            }


            /*
             * Key Normalizer
             */
            if (targetClass == KeyNormalizer.class) {
                if (sourceObject.getClass() != String.class) {
                    throw new CastException("A string source object is mandatory to cast to KeyNormalizer. The source object is not a string but a " + sourceObject.getClass().getSimpleName());
                }
                return targetClass.cast(KeyNormalizer.create(sourceObject));
            }

            /*
             * Duration
             */
            if (targetClass == DurationShort.class) {
                if (sourceObject instanceof String) {
                    return targetClass.cast(DurationShort.create(sourceObject.toString()));
                }
                throw new CastException("A duration short cannot be created because the value is not a string but a " + sourceObject.getClass().getSimpleName());
            }
            if (targetClass == Duration.class) {
                if (sourceObject instanceof String) {
                    Duration parse;
                    String durationString = sourceObject.toString();
                    try {
                        parse = Duration.parse(durationString);
                    } catch (Exception e) {
                        throw new CastException("The iso duration string (" + durationString + ") is not valid. Error: " + e.getMessage(), e);
                    }
                    return targetClass.cast(parse);
                }
                throw new CastException("A duration cannot be created because the value is not a string but a " + sourceObject.getClass().getSimpleName());
            }

            /*
             * Uri Enhanced
             */
            if (targetClass == UriEnhanced.class) {
                String uri = sourceObject.toString();
                try {

                    return targetClass.cast(UriEnhanced.createFromString(uri));

                } catch (Exception e) {
                    String message = "The string `" + uri + "` is not a valid uri.";
                    if (uri.startsWith("\"") || uri.startsWith("'")) {
                        message += " You should delete the character quote.";
                    }
                    message += " Error: " + e.getMessage();
                    throw new CastException(message, e);
                }

            }

            if (targetClass == DnsName.class) {

                String dnsNameAsString = sourceObject.toString();
                return targetClass.cast(DnsName.create(dnsNameAsString));

            }

            /*
             * Boolean
             */
            if (targetClass == Boolean.class) {
                return targetClass.cast(Booleans.createFromObject(sourceObject).toBoolean());
            }
            /*
             * Integer and Smallint
             */
            if (targetClass == Integer.class) {
                return targetClass.cast(Integers.createFromObject(sourceObject).toInteger());
            }

            /*
             * Big integer
             */
            if (targetClass == BigInteger.class) {
                return targetClass.cast(BigIntegers.createFromObject(sourceObject).toBigInteger());
            }

            /*
             * Big Decimal (exact number), Numeric, decimal
             */
            if (targetClass == BigDecimal.class) {
                return targetClass.cast(BigDecimals.createFromObject(sourceObject).toBigDecimal());
            }

            /*
             * Float Double precision
             */
            if (targetClass == Double.class) {
                return targetClass.cast(Doubles.createFromObject(sourceObject).toDouble());
            }

            /*
             * Float Single precision
             */
            if (targetClass == Float.class) {
                /*
                 * Not really error proof against precision error but yeah
                 * Float is no more used
                 */
                return targetClass.cast(Doubles.createFromObject(sourceObject).toFloat());
            }

            /*
             * Date
             */
            if (targetClass == java.sql.Date.class) {
                return targetClass.cast(Date.createFromObject(sourceObject).toSqlDate());
            }
            if (targetClass == LocalDate.class) {
                return targetClass.cast(Date.createFromObject(sourceObject).toLocalDate());
            }

            /*
             * Timestamp
             */
            if (targetClass == java.sql.Timestamp.class) {
                return targetClass.cast(Timestamp.createFromObject(sourceObject).toSqlTimestamp());
            }
            if (targetClass == LocalDateTime.class) {
                return targetClass.cast(Timestamp.createFromObject(sourceObject).toLocalDateTime());
            }
            if (targetClass == java.util.Date.class) {
                return targetClass.cast(Date.createFromObject(sourceObject).toDate());
            }


            /*
             * String
             */
            if (targetClass == String.class) {
                /*
                 * Input Stream
                 * Not sure where the character set fit here
                 * https://stackoverflow.com/questions/309424/how-to-read-convert-an-inputstream-into-a-string-in-java
                 */
                if (sourceObject instanceof InputStream) {
                    InputStream is = (InputStream) sourceObject;
                    try (Scanner s = new Scanner(is)) {
                        String value = s.useDelimiter("\\A").hasNext() ? s.next() : "";
                        return targetClass.cast(value);
                    }
                }
                return targetClass.cast(sourceObject.toString());
            }

            /*
             * Character
             */
            if (targetClass == Character.class) {
                if (sourceObject.toString().length() != 1) {
                    throw new CastException("The source object is not a string of length 1 (" + sourceObject + ")");
                }
                return targetClass.cast(sourceObject.toString().charAt(0));
            }

            /*
             * Time
             */
            if (targetClass == java.sql.Time.class) {
                return targetClass.cast(Time.createFromObject(sourceObject).toSqlTime());
            }



            /*
             * Path from string
             */
            if (targetClass == java.nio.file.Path.class) {
                return targetClass.cast(Paths.get(sourceObject.toString()));
            }

            /*
              Enum
             */
            if (targetClass.isEnum()) {
                /*
                  {@link Enums#valueOf(Class, String)} is not used
                  because it needs exact match
                 */
                KeyNormalizer normalizedLookupKey;
                if (sourceObject instanceof KeyNormalizer) {
                    normalizedLookupKey = (KeyNormalizer) sourceObject;
                } else {
                    normalizedLookupKey = KeyNormalizer.create(sourceObject.toString());
                }
                for (T constant : targetClass.getEnumConstants()) {
                    if (constant == null) {
                        throw new InternalError("The enum class (" + targetClass + ") does not have any constants");
                    }
                    Enum<?> constantAsEnum = (Enum<?>) constant;
                    if (KeyNormalizer.create(constantAsEnum.name()).equals(normalizedLookupKey)) {
                        return constant;
                    }
                }
                // Not uppercase
                String enumsPossibleValues = Enums.toConstantAsStringOfUriAttributeCommaSeparated(targetClass);
                throw new CastException("We couldn't cast the value (" + sourceObject + ") with the class (" + sourceObjectClass.getSimpleName() + ") to the enum class (" + targetClass.getSimpleName() + "). Possible values: " + enumsPossibleValues);
            }

            /*
              Charset
             */
            if (targetClass == Charset.class) {
                String charsetValue = sourceObject.toString();
                if (!Charset.isSupported(charsetValue)) {
                    throw new IllegalCharsetNameException("The character set value (" + charsetValue + ") is not supported. You may set the character set to one of this values: " + String.join(", ", Charset.availableCharsets().keySet()));
                }
                return targetClass.cast(Charset.forName(charsetValue));

            }

            /*
              Media Type
             */
            if (targetClass == MediaType.class) {
                return targetClass.cast(MediaType.parse(sourceObject.toString()));
            }


            /*
              If we are here, we have not yet a
              transformation,
              we try to cast it directly
             */
            try {
                return targetClass.cast(sourceObject);
            } catch (ClassCastException e) {
                throw new CastException("We couldn't cast the value (" + sourceObject + ") with the class (" + sourceObjectClass.getSimpleName() + ") to the class (" + targetClass.getSimpleName() + ")");
            }

        } catch (IllegalCharsetNameException | ClassCastException e) {
            throw new CastException(e.getMessage(), e);
        }

    }

    public static <T> T[] castToArray(List<T> list) {

        //noinspection unchecked
        return (T[]) list.toArray();

    }

    /**
     * @param object      - a sequence of single values
     * @param targetClazz - the class to cast
     * @param <T>         - the type of class to return
     * @return A mix of Source: Effective Java; Item 26
     * @throws CastException - if any errors
     */
    public static <T> T[] castToArray(Object object, Class<T> targetClazz) throws CastException {

        Class<?> valueObjectClass = object.getClass();

        if (targetClazz.isArray()) {
            throw new CastException("The target clazz should not be an array");
        }

        if (valueObjectClass.isArray()) {
            if (valueObjectClass.getComponentType() == targetClazz) {
                //noinspection unchecked
                return (T[]) object;
            }
            List<T> target = new ArrayList<>();
            for (Object value : (Object[]) object) {
                target.add(Casts.cast(value, targetClazz));
            }
            //noinspection unchecked
            return target.toArray((T[]) Array.newInstance(targetClazz, target.size()));
        }

        if (object instanceof Collection) {
            /*
              The cast of the generic throw an unchecked
              warning that is not true
             */
            List<T> target = new ArrayList<>();
            for (Object value : (Collection<?>) object) {
                target.add(Casts.cast(value, targetClazz));
            }
            //noinspection unchecked
            return target.toArray((T[]) Array.newInstance(targetClazz, target.size()));
        }

        throw new CastException("We could cast the value to an array of (" + targetClazz + ")");
    }


    /**
     * @param object    - the object to cast
     * @param clazzK    - the key class
     * @param clazzV    - the value class
     * @param <K>       - the type of key
     * @param <V>       - the type of value
     * @param strictKey - When casting to a new map, you may want a strict key and a loose value
     * @return the map
     * @throws CastException if any errors
     */
    public static <K, V> Map<K, V> castToNewMap(Object object, Class<K> clazzK, Class<V> clazzV, Boolean strictKey) throws CastException {
        Map<?, ?> map;
        if (!(object instanceof Map)) {
            throw new CastException("The object is not a map but a " + object.getClass().getSimpleName() + " and can't be then casted");
        } else {
            map = (Map<?, ?>) object;
        }

        Map<K, V> result = new HashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            K key;
            if (!strictKey) {
                key = Casts.cast(e.getKey(), clazzK);
            } else {
                Object elementKey = e.getKey();
                if (!clazzK.equals(elementKey.getClass())) {
                    throw new CastException("The key (" + elementKey + ") is not a " + clazzK.getSimpleName() + ".");
                }
                //noinspection unchecked
                key = (K) elementKey;
            }
            result.put(
                    key,
                    Casts.cast(e.getValue(), clazzV)
            );
        }

        return result;

    }

    /**
     * Cast a map to another one b y creating a new map
     *
     * @param object - the object to cast
     * @param clazzK - the key class
     * @param clazzV - the value class
     * @param <K>    - the type of key
     * @param <V>    - the type of value
     * @return the map
     * @throws CastException - if any error
     */
    public static <K, V> Map<K, V> castToNewMap(Object object, Class<K> clazzK, Class<V> clazzV) throws CastException {

        return castToNewMap(object, clazzK, clazzV, false);

    }

    /**
     * Same function as {@link #castToSameMap(Object, Class, Class)}
     * but without exception. To use when you know the data in advance.
     *
     * @param object - the raw map input object
     * @param clazzK - the key class
     * @param clazzV - the value class
     * @param <K>    - the key type
     * @param <V>    - the value type
     * @return the map cast
     */
    public static <K, V> Map<K, V> castToSameMapSafe(Object object, Class<K> clazzK, Class<V> clazzV) {
        try {
            return castToSameMap(object, clazzK, clazzV);
        } catch (CastException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * This function cast the object to a map and don't create new one (meaning that the object
     * are not cast)
     *
     * @param object - an object
     * @param clazzK - the key class
     * @param clazzV - the value class
     * @param <K>    the key type
     * @param <V>    the value type
     * @return the same object but cast
     * @throws CastException if there is a problem
     */
    public static <K, V> Map<K, V> castToSameMap(Object object, Class<K> clazzK, Class<V> clazzV) throws CastException {

        if (!(object instanceof Map)) {
            throw new CastException("The object (value: " + object + ") is not a map but a " + object.getClass().getSimpleName() + " and can't be then casted");
        }

        Map.Entry<?, ?> firstElement = ((Map<?, ?>) object).entrySet().iterator().next();
        if (firstElement == null) {
            //noinspection unchecked
            return (Map<K, V>) object;
        }
        if (firstElement.getKey() != null && !clazzK.equals(Object.class)) {
            if (!firstElement.getKey().getClass().equals(clazzK)) {
                throw new CastException("The key (" + firstElement.getKey() + ") is not a " + clazzK.getSimpleName() + " but a " + firstElement.getKey().getClass().getSimpleName());
            }
        }
        if (firstElement.getValue() != null && !clazzV.equals(Object.class)) {
            if (!firstElement.getValue().getClass().equals(clazzV)) {
                throw new CastException("The key (" + firstElement.getValue() + ") is not a " + clazzV.getSimpleName() + ".");
            }
        }

        //noinspection unchecked
        return (Map<K, V>) object;

    }

    /**
     * @param o     - the object to cast
     * @param clazz - the class target
     * @param <T>   - the type target class
     * @return the object cast
     * @throws CastException - if any cast error
     */
    public static <T> List<T> castToListScalar(Object o, Class<T> clazz) throws CastException {
        List<T> result = castToNewList(o, clazz);
        for (Object element : result) {
            if (isNotCollection(element)) {
                continue;
            }
            throw new CastException("The value " + element + " is not a scalar but a collection of type " + element.getClass().getSimpleName());
        }
        return result;
    }

    /**
     * @param obj - the object
     * @return true if this is not a collection
     */
    public static boolean isNotCollection(Object obj) {
        return !(obj instanceof Collection) &&
                !(obj instanceof Map) &&
                !(obj.getClass().isArray());
    }

    /**
     * Cast a list of unknown class to a list of clazz.
     *
     * @param clazz - the target class to cast
     * @param o     - a {@link Collection collection (list,array)} or an array
     * @param <T>   the return type
     * @return the list
     * @throws CastException - if any errors
     */
    public static <T> List<T> castToNewList(Object o, Class<T> clazz) throws CastException {

        if (o == null) {
            return null;
        }


        if (o instanceof List) {
            List<?> list = (List<?>) o;
            List<T> returnList = new ArrayList<>();
            for (Object object : list) {
                if (object == null) {
                    returnList.add(null);
                    continue;
                }
                returnList.add(cast(object, clazz));
            }
            return returnList;
        }

        if (o instanceof Collection) {
            Collection<?> array = ((Collection<?>) o);
            List<T> returnList = new ArrayList<>();
            for (Object object : array) {
                returnList.add(cast(object, clazz));
            }
            return returnList;
        }

        if (o.getClass().isArray()) {
            Object[] array = (Object[]) o;
            List<T> returnList = new ArrayList<>();
            for (Object object : array) {
                returnList.add(cast(object, clazz));
            }
            return returnList;
        }

        throw new CastException("The object is not a collection (list, set) nor an array but a " + o.getClass().getSimpleName() + " and can't therefore be cast to a list");

    }

    /**
     * Casts to a set with the same target type
     *
     * @param object - the object to cast to a set
     * @param clazzV - the type of element
     * @param <T>    - the type of element
     * @return - the input cast
     * @throws CastException if any error occurs
     */
    @SuppressWarnings("unused")
    public static <T> Set<T> toSameSet(Object object, Class<T> clazzV) throws CastException {

        Set<?> set;
        if (!(object instanceof Set)) {
            throw new CastException("The object (value: " + object + ") is not a set but a " + object.getClass().getSimpleName() + " and can't be then casted");
        } else {
            set = (Set<?>) object;
        }

        for (Object value : set) {
            if (!clazzV.equals(Object.class)) {
                if (!clazzV.isAssignableFrom(value.getClass())) {
                    throw new CastException("The value (" + value + ") is not a " + clazzV.getSimpleName() + ".");
                }
            }
        }

        //noinspection unchecked
        return (Set<T>) object;

    }

    /**
     * A cast that throws errors only at runtime
     *
     * @param value  the value
     * @param aClass the class
     * @param <T>    the type
     * @return the cast object
     */
    public static <T> T castSafe(Object value, Class<T> aClass) {
        try {
            return cast(value, aClass);
        } catch (CastException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param o     the object to cast
     * @param aClass - the target class
     * @param <T>    the target class parameter
     * @return - return the object cast
     */
    public static <T> List<T> castToNewListSafe(Object o, Class<T> aClass) {
        try {
            return castToNewList(o, aClass);
        } catch (CastException e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V> Map<K, V> castToNewMapSafe(Object o, Class<K> kClass, Class<V> vClass) {
        try {
            return castToNewMap(o, kClass, vClass);
        } catch (CastException e) {
            throw new ClassCastException(e.getMessage());
        }
    }


    /**
     * Same as {@link #castToCollection} but when you have checked before that the object is a collection
     * Example:
     * <pre>{@code
     * boolean isCollection = Collection.class.isAssignableFrom(originalValue.getClass());
     * }</pre>
     *
     * @param o      - the input object to cast
     * @param clazzV the element clazz
     * @param <T>    the element type clazz
     * @return the collection cast
     */
    public static <T> Collection<T> castToCollectionSafe(Object o, Class<T> clazzV) {
        try {
            return castToCollection(o, clazzV);
        } catch (CastException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    /**
     * @param o      the input object
     * @param clazzV the type of element
     * @param <T>    - the type of input
     * @return the input object cast
     * @throws CastException if the object is not a collection or an element is not from the same class
     */
    public static <T> Collection<T> castToCollection(Object o, Class<T> clazzV) throws CastException {
        if (o == null) {
            return null;
        }
        if (o instanceof Collection) {
            Collection<?> array = ((Collection<?>) o);
            for (Object object : array) {
                if (!clazzV.equals(Object.class)) {
                    if (!clazzV.isAssignableFrom(object.getClass())) {
                        throw new CastException("The element (" + object + ") of the collection is not a " + clazzV.getSimpleName() + " but a " + object.getClass().getSimpleName());
                    }
                }
            }
            //noinspection unchecked
            return (Collection<T>) array;
        }
        throw new CastException("The object (" + o + ") is not a collection but a " + o.getClass().getSimpleName());

    }


    /**
     * Converts a Reader (character stream) to a String.
     *
     * @param reader The character stream to read from
     * @return The string content from the reader
     * @throws CastException If an I/O error occurs during reading
     */
    @SuppressWarnings("unused")
    public static String castReaderToString(Reader reader) throws CastException {

        if (reader == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new CastException(e);
        }

        return stringBuilder.toString();

    }


    /**
     * A narrowing conversion changes a value to a data type that might not be able to hold some of the possible values. For example, a fractional value is rounded when it is converted to an integral type, and a numeric type being converted to Boolean is reduced to either True or False.
     * A widening conversion changes a value to a data type that can allow for any possible value of the original data.
     * Widening conversions preserve the source value but can change its representation.
     * This occurs if you convert from an integral type to Decimal, or from Char to String.
     * <a href="https://learn.microsoft.com/en-us/dotnet/visual-basic/programming-guide/language-features/data-types/widening-and-narrowing-conversions">...</a>
     *
     * @param from - the `from` type
     * @param to   - the `to` type
     * @return true if this is a narrowing convertion
     */
    public static boolean isNarrowingConversion(Class<?> from, Class<?> to) {

        if (from.equals(to)) {
            return false;
        }
        return !wideningMap.getOrDefault(from, Collections.emptySet()).contains(to);

    }


}
