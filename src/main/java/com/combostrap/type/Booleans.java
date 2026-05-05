package com.combostrap.type;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Boolean wrapper
 * <p>
 * Leading or trailing whitespace is ignored, and case does not matter
 * The key words TRUE and FALSE are the preferred (SQL-compliant) usage.
 */
public class Booleans {

    private final Boolean internBoolean;

    /**
     * List comes from
     * https://www.postgresql.org/docs/9.1/datatype-boolean.html
     */
    protected static Set<String> trueString = new HashSet<>(Arrays.asList("t", "true", "y", "yes", "on", "1"));
    protected static Set<String> falseString = new HashSet<>(Arrays.asList("f", "false", "n", "no", "off", "0"));
    protected static Set<Integer> trueIntegers = new HashSet<>(Collections.singletonList(1));
    protected static Set<Integer> falseInteger = new HashSet<>(Collections.singletonList(0));

    /**
     * The boolean type can have several states: "true", "false", and a third state,
     * "unknown", which is represented by the SQL null value.
     */
    public Booleans(Boolean b) {
        this.internBoolean = b;
    }

    /**
     * A wrapper around {@link Boolean#parseBoolean(String)}
     */
    public static Booleans createFromString(String s) {

        if (s == null) {
            return createFromBoolean(null);
        }
        String string = s.toLowerCase().trim();
        if (trueString.contains(string)) {
            return createFromBoolean(true);
        } else if (falseString.contains(string)) {
            return createFromBoolean(false);
        } else if (Casts.nullableStrings.contains(string)) {
            return createFromBoolean(null);
        } else {
            throw new RuntimeException(
                    String.join(System.lineSeparator(), "The string (" + s + ") is not recognized as ",
                            "- a true value (" + String.join(",", trueString) + ")",
                            "- a false value (" + String.join(",", falseString) + ")",
                            "- a null value (" + String.join(",", Casts.nullableStrings) + ")")
            );

        }

    }

    /**
     *
     */
    public static Booleans createFromObject(Object o) throws BooleanCastException {

        if (o == null) {
            return createFromBoolean(null);
        } else if (o instanceof Boolean) {
            return createFromBoolean((Boolean) o);
        } else if (o instanceof String) {
            return createFromString((String) o);
        } else if (o instanceof Integer) {
            return createFromInteger((Integer) o);
        } else if (o instanceof Short) {
            return createFromShort((Short) o);
        } else if (o instanceof Long) {
            return createFromLong((Long) o);
        } else if (o instanceof BigInteger) {
            return createFromBigInteger((BigInteger) o);
        } else if (o instanceof BigDecimal) {
            return createFromBigDecimal((BigDecimal) o);
        } else if (o instanceof Double) {
            return createFromDouble((Double) o);
        } else if (o instanceof Float) {
            return createFromFloat((Float) o);
        } else {
            return new Booleans(null);
        }

    }

    private static Booleans createFromFloat(Float o) throws BooleanCastException {
        if (o == null) {
            return createFromBoolean(null);
        } else {
            return createFromInteger(o.intValue());
        }
    }

    private static Booleans createFromDouble(Double o) throws BooleanCastException {
        if (o == null) {
            return createFromBoolean(null);
        } else {
            return createFromInteger(o.intValue());
        }
    }

    private static Booleans createFromBigDecimal(BigDecimal o) throws BooleanCastException {
        if (o == null) {
            return createFromBoolean(null);
        } else {
            return createFromInteger(o.intValue());
        }
    }

    private static Booleans createFromBigInteger(BigInteger o) throws BooleanCastException {
        if (o == null) {
            return createFromBoolean(null);
        }
        return createFromInteger(o.intValue());
    }

    private static Booleans createFromShort(Short o) throws BooleanCastException {
        if (o == null) {
            return createFromBoolean(null);
        }
        return createFromInteger(o.intValue());

    }

    private static Booleans createFromLong(Long o) throws BooleanCastException {
        if (o == null) {
            return createFromBoolean(null);
        }
        return createFromInteger(o.intValue());

    }

    private static Booleans createFromInteger(Integer integer) throws BooleanCastException {
        if (integer == null) {
            return createFromBoolean(null);
        } else if (trueIntegers.contains(integer)) {
            return new Booleans(true);
        } else if (falseInteger.contains(integer)) {
            return new Booleans(false);
        } else {
            throw new BooleanCastException(String.join(System.lineSeparator(), "The number value (" + integer + ") is not recognized as ",
                    "- a true value (" + trueIntegers.stream().map(Object::toString).collect(Collectors.joining(",")) + ")",
                    "- a false value (" + falseInteger.stream().map(Object::toString).collect(Collectors.joining(",")) + ")")
            );
        }
    }

    private static Booleans createFromBoolean(Boolean o) {
        return new Booleans(o);
    }

    public static Set<Integer> getDefaultTrueIntegers() {
        return trueIntegers;

    }

    public static Set<Integer> getDefaultFalseIntegers() {
        return falseInteger;
    }


    public Boolean toBoolean() {
        return internBoolean;
    }

    /**
     * @param nullWord - the string returned when the boolean is null (ie unknown)
     */
    public String toString(String nullWord) {
        if (internBoolean == null) {
            return nullWord;
        } else {
            return internBoolean.toString();
        }
    }

    /**
     * A sql null value allows null
     */
    public String toSqlString() {
        if (internBoolean == null) {
            return null;
        } else {
            return internBoolean.toString();
        }
    }

    public String toString() {
        return toString("null");
    }

    public static Set<String> getDefaultTrueStrings() {
        return trueString;
    }

    public static Set<String> getDefaultFalseStrings() {
        return falseString;
    }

    public static Set<String> getDefaultNullStrings() {
        return Casts.nullableStrings;
    }


    /**
     * @return 1 for true, 0 for false
     */
    public Optional<Integer> toInteger() {
        if (this.internBoolean != null) {
            return Optional.of(this.internBoolean.equals(true) ? 1 : 0);
        }
        return Optional.empty();
    }


}
