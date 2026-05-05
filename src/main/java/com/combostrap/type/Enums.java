package com.combostrap.type;


import com.combostrap.exception.InternalException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Enums {
    /**
     * Utility function to create a string of all enum constant of an enum class
     * This is used generally in a log to give feedback to the user over
     * which enum, he/she may enter
     *
     * @param anEnum - an enum class
     * @return a string with all enum constants as uri argument separated by a comma
     * @throws InternalException it the class is not an enum class
     */
    public static String toConstantAsStringOfUriAttributeCommaSeparated(Class<?> anEnum) {
        if (!anEnum.isEnum()) {
            throw new InternalException("An enum constant should be passed. " + anEnum.getSimpleName() + " is not an enum");
        }
        Object[] constants = anEnum.getEnumConstants();
        if (constants == null) {
            return "";
        }
        return Arrays.stream(constants)
                .map(enumValue -> KeyNormalizer.createSafe(enumValue.toString()).toCliLongOptionName())
                .collect(Collectors.joining(", "));

    }

    public static String toConstantAsStringCommaSeparated(Class<?> anEnum) {
        if (!anEnum.isEnum()) {
            throw new InternalException("An enum constant should be passed. " + anEnum.getSimpleName() + " is not an enum");
        }
        Object[] constants = anEnum.getEnumConstants();
        if (constants == null) {
            return "";
        }
        return Arrays.stream(constants)
                .map(Object::toString)
                .collect(Collectors.joining(", "));

    }
}
