package com.combostrap.type;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Arrays utilities
 * Two ss to not clash with java.utils.Arrays
 */
public class Arrayss {

  /**
   * Does the array contains a character
   *
   * @param haystack - the array to search
   * @param needle - the needle to search in the haystack
   * @return yes or no
   */
  public static boolean in(char[] haystack, char needle) {
    for (char c : haystack) {
      if (needle == c) {
        return true;
      }
    }
    return false;
  }


  /**
   * Concat bytes array
   *
   * @param first - the first array
   * @param rest - the second
   * @return a concatenated array
   */
  @SuppressWarnings("unused")
  public static byte[] concatAll(byte[] first, byte[]... rest) {
    int totalLength = first.length;
    for (byte[] array : rest) {
      totalLength += array.length;
    }
    byte[] result = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (byte[] array : rest) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  /**
   * StackOverflow
   * <a href="https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java">...</a>
   *
   * @param first - the first array
   * @param rest - the other arrays
   * @param <T> the object type
   * @return the concat array
   */
  public static <T> T[] concatAll(T[] first, T[]... rest) {
    int totalLength = first.length;
    for (T[] array : rest) {
      totalLength += array.length;
    }
    T[] result = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (T[] array : rest) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  /**
   *
   * @param first
   * @param length
   * @param <T>
   * @return the same array but with an new length
   */
  public static <T> T[] extend(T[] first, int length) {
    return Arrays.copyOf(first, length);
  }

  /**
   *
   * Add an element at the beginning of an array
   *
   * @param first
   * @param second
   * @param <T>
   * @return an array
   */
  public static <T> T[] concat(T first, T[] second) {
    /*
      Creating an array from the first elements
     */
    T[] firsts = Arrays.copyOf(second, 1);
    firsts[0]=first;

    return concat(firsts, second);
  }

  public static long[] concat(long first, long[] second) {
    /*
      Creating an array from the first elements
     */
    long[] firsts = Arrays.copyOf(second, 1);
    firsts[0]=first;

    /*
    The length of the final array
     */
    int totalLength = second.length + firsts.length;
    /*
    Create the target array from the firsts array
    with the needed length
     */
    long[] target = Arrays.copyOf(firsts, totalLength);
    /*
    Copy the element of second into target
    at position firsts.length
    from the element 0 until second.length of the array second
     */
    System.arraycopy(second, 0, target, firsts.length, second.length);
    return target;
  }

  /**
   * Add an array at the beginning of an array
   * @param firsts
   * @param second
   * @param <T>
   * @return
   */
  public static <T> T[] concat(T[] firsts, T[] second) {
    /*
    The length of the final array
     */
    int totalLength = second.length + firsts.length;
    /*
    Create the target array from the firsts array
    with the needed length
     */
    T[] target = Arrays.copyOf(firsts, totalLength);
    /*
    Copy the element of second into target
    at position firsts.length
    from the element 0 until second.length of the array second
     */
    System.arraycopy(second, 0, target, firsts.length, second.length);
    return target;
  }

  /**
   * Print an array of string
   *
   * @param strings
   */
  public static void print(String[] strings) {
    System.out.println(String.join(",", strings));
  }

  @SafeVarargs
  public static <T> List<T> asList(T... varargs) {
    return Arrays.stream(varargs)
      .collect(Collectors.toList());
  }

  /**
   * Utility function that create a comma separated list of values
   * from a collection
   *
   * @param varargs
   * @return
   */
  public static <T> String toJoinedStringWithComma(T... varargs) {
    return toJoinedString(", ", varargs);
  }

  /**
   * Utility function that create a separated list of values
   * from a varargs
   *
   * @param varargs - the varargs
   * @return
   */
  public static <T> String toJoinedString(String separator, T... varargs) {
    return Arrays.stream(varargs)
      .map(Object::toString)
      .collect(Collectors.joining(separator));
  }


}
