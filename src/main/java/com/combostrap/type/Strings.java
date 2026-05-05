package com.combostrap.type;



import com.combostrap.exception.InternalException;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link StringBuilder} wrapper with extra's
 * * NULL safe
 */
public class Strings {


  public static final String EOL = System.lineSeparator();
  public static final String EOL_WINDOWS = "\r\n";
  public static final String EOL_LINUX = "\n";


  private final StringBuilder stringBuilderField;


  public Strings(String string) {
    this.stringBuilderField = new StringBuilder();
    this.stringBuilderField.append(string);
  }

  public Strings(StringBuilder stringBuilder) {
    this.stringBuilderField = stringBuilder;

  }

  @SuppressWarnings("unused")
  public static char getRandomLetter() {
    Random r = new Random();
    return (char) (r.nextInt(26) + 'a');
  }

  /**
   * @param object the object
   * @return If the object is null, the string will be "null"
   */
  public static Strings createFromObjectNullSafe(Object object) {
    if (object == null) {
      return new Strings("null");
    } else {
      return new Strings(object.toString());
    }
  }

  public static <T> Strings createFromStrings(String separator, Collection<T> strings) {
    return new Strings(
      strings.stream()
        .map(Object::toString)
        .collect(Collectors.joining(separator))
    );
  }

  /**
   * @param aClass - the reference class (generally the calling class)
   * @param path   - the absolute path to the resource
   */
  public static Strings createFromResource(Class<?> aClass, String path) {
    /**
     * {@link Class#getResourceAsStream(String)} instantiate the zip file
     * system.
     */
    InputStream resource = aClass.getResourceAsStream(path);
    if (resource == null) {
      /**
       * We don't throw an exception to manage because
       * the path is normally given in a code as a literal
       */
      throw new InternalException("The resource path (" + path + ") was not found");
    }
    String s = new BufferedReader(new InputStreamReader(resource))
      .lines()
      .collect(Collectors.joining(EOL));
    return new Strings(s);
  }

  public static Strings createFromInputStream(InputStream is) {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    return new Strings(s.hasNext() ? s.next() : "");
  }

  public static boolean isUpperCase(String string) {
    return string.toUpperCase().equals(string);
  }

  public static Strings createFromByte(byte[] bytes, Charset charset) {
    String string = new String(bytes, charset);
    return new Strings(string);
  }

  public List<String> splitWithoutRemovingTheSplitCharacter(String patter) {
    // zero-width positive lookahead
    return Arrays.asList(
      this.stringBuilderField.toString().split("(?=" + patter + ")")
    );
  }

  public Integer numberOfOccurrences(String regexp) {

    Pattern pattern = Pattern.compile(regexp, Pattern.DOTALL);
    Matcher matcher = pattern.matcher(stringBuilderField.toString());
    Integer counter = 0;
    while (matcher.find()) {
      counter++;
    }
    return counter;

  }

  /**
   * Return a string from character list
   */
  public static Strings createFromCharacters(List<Character> list) {
    {
      StringBuilder builder = new StringBuilder(list.size());
      for (Character ch : list) {
        builder.append(ch);
      }
      return new Strings(builder.toString());
    }
  }

  /**
   * Function used before a text comparison to normalize the text
   *
   * @return a compact string that is written on one line, has no double space and is trimmed
   */
  public Strings normalize() {
    return new Strings(this.stringBuilderField.toString().replaceAll("\r\n|\n", " ") // No new line
      .replaceAll("[ ]{2,10}", " ")
      .trim()
    ); // No double space;
  }

  public static Strings createFromPath(Path path) {
    return createFromPath(path, Charset.defaultCharset());
  }

  /**
   * @return the string preserving the end of line
   */
  public static Strings createFromPath(Path path, Charset charset) {

    try {
      return new Strings(Files.readString(path, charset));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Unable to find the file (" + path.toAbsolutePath().normalize() + ")", e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


  }

  @SuppressWarnings("unused")
  private static String getEndOfLine(Path path) {
    try (FileInputStream fis = new FileInputStream(path.toFile())) {
      String eol = parseEndOfLine(fis, String.class);
      if (eol == null) {
        return System.lineSeparator();
      } else {
        return eol;
      }
    } catch (
      IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * @param clazz - String.class, you get the eol, Integer.class, you get the count
   */
  private static <T> T parseEndOfLine(InputStream inputStream, Class<T> clazz) throws IOException {
    char current;

    int eolCount = 0;
    StringBuilder localLineSeparator = new StringBuilder();
    while (inputStream.available() > 0) {
      current = (char) inputStream.read();
      if ((current == '\n') || (current == '\r')) {
        localLineSeparator.append(current);
        if (inputStream.available() > 0) {
          char next = (char) inputStream.read();
          if (next == current) {
            eolCount++;
            localLineSeparator = new StringBuilder(String.valueOf(next));
          } else if (((next == '\r') || (next == '\n'))) {
            localLineSeparator.append(next);
            return clazz.cast(localLineSeparator.toString());
          }
        }
        if (clazz.equals(String.class)) {
          return clazz.cast(localLineSeparator.toString());
        } else {
          eolCount++;
          localLineSeparator = new StringBuilder();
        }
      }
    }
    if (clazz == String.class) {
      return clazz.cast(Strings.EOL);
    } else {
      return clazz.cast(eolCount);
    }
  }


  /**
   * Left Trim
   * Credits: <a href="https://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java">...</a>
   */
  public Strings ltrim() {
    int i = 0;
    while (i < this.stringBuilderField.length() && Character.isWhitespace(this.stringBuilderField.charAt(i))) {
      i++;
    }
    this.stringBuilderField.delete(0, i);
    return this;
  }


  /**
   * Right Trim
   * Credits: <a href="https://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java">...</a>
   */
  public Strings rtrim() {
    int i = stringBuilderField.length() - 1;
    while (i >= 0 && Character.isWhitespace(stringBuilderField.charAt(i))) {
      i--;
    }
    stringBuilderField.delete(i + 1, stringBuilderField.length());
    return this;
  }

  /**
   * Right Trim only end of line
   * Credits: <a href="https://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java">...</a>
   * Note: {@link #rtrim()} by default does it also but on all whitespace
   */
  public Strings rtrimEol() {
    char[] eofChar = {'\n', '\r'};

    int i = stringBuilderField.length() - 1;
    while (i >= 0 && Arrayss.in(eofChar, stringBuilderField.charAt(i))) {
      i--;
    }
    stringBuilderField.delete(i + 1, stringBuilderField.length());
    return this;
  }

  /**
   * @return a camel cased string
   */
  public Strings toFirstLetterCapitalCase() {
    stringBuilderField.setCharAt(0, Character.toUpperCase(stringBuilderField.charAt(0)));
    stringBuilderField.replace(1, stringBuilderField.length(), stringBuilderField.substring(1, stringBuilderField.length()).toLowerCase());
    return this;
  }

  /**
   * Takes all first letter of each word and put them in uppercase
   */
  public Strings toCapitalize() {
    if (stringBuilderField == null) {
      return this;
    }

    boolean newWord = true; // Indicates the start of a new word
    for (int i = 0; i < stringBuilderField.length(); i++) {
      char c = stringBuilderField.charAt(i);

      /**
       * White space handling
       */
      if (Character.isWhitespace(c)) {
        newWord = true;
        continue;
      }

      char newChar;
      if (newWord) {
        // Capitalize the first letter of the new word
        newChar = Character.toUpperCase(c);
        newWord = false;
      } else {
        // Convert the rest of the word to lowercase
        newChar = Character.toLowerCase(c);
      }
      stringBuilderField.setCharAt(i, newChar);

    }

    return this;
  }

  /**
   * @return the number of digit in the string
   */
  public Integer getDigitCount() {

    return Math.toIntExact(stringBuilderField.chars()
      .filter(i -> Character.isDigit((char) i))
      .count());

  }


  public String toString() {

    return stringBuilderField.toString();

  }

  /**
   * If the suffix is found, it will be deleted
   */
  public Strings rtrim(String suffix) {

    int i = stringBuilderField.lastIndexOf(suffix);
    if (i != -1 && i == stringBuilderField.length() - suffix.length()) {
      stringBuilderField.delete(i, stringBuilderField.length());
    }
    return this;

  }

  /**
   * A utility function that splits the string
   * with a separator:
   * * that will be escaped if this is a regexp character
   * * and does not remove the trailing empty string
   *
   * @param separator a separator
   */
  public List<String> split(String separator) {
    String regexpSeparator;
    // the split operator is a regexp, we need then to add a \ to escape it
    // for the split operations
    switch (separator) {
      case "\\":
        // if windows
        regexpSeparator = "\\\\";
        break;
      case ".":
        // if sql
        regexpSeparator = "\\.";
        break;
      default:
        regexpSeparator = separator;
    }

    /**
     * By default, the value of 0 will delete the empty string
     * between separator, this value avoids that
     */
    int doesNotRemoveEmptyStringBetweenSeparator = -1;

    return Arrays.asList(stringBuilderField.toString().split(regexpSeparator, doesNotRemoveEmptyStringBetweenSeparator));
  }


  public static Strings createFromString(String string) {
    return new Strings(string);
  }

  public static Strings createFromStrings(String separator, String... strings) {
    return new Strings(Arrays.stream(strings)
      .map(s -> s == null ? "null" : s)
      .collect(Collectors.joining(separator)));

  }

  /**
   * Create a multiline string
   * from strings
   */
  public static Strings createMultiLineFromStrings(String... strings) {

    // Multiline, we delete the trailing spaces
    return new Strings(Arrays.stream(strings)
      .map(s -> s == null ? "null" : s.stripTrailing())
      .collect(Collectors.joining(EOL)));

  }

  @SuppressWarnings("unused")
  public BigInteger toBigInteger() {
    return new BigInteger(stringBuilderField.toString());
  }

  @SuppressWarnings("unused")
  public Long toLong() {
    return Long.valueOf(stringBuilderField.toString());
  }


  @SuppressWarnings("unused")
  public Integer toInteger() throws CastException {
    /**
     * A format may used with {@link Integers#createFromString(String, NumberFormat)}
     * but for now, KISS
     */
    return Integers.createFromString(stringBuilderField.toString()).toInteger();

  }

  @SuppressWarnings("unused")
  public Double toDouble() {
    return Double.parseDouble(stringBuilderField.toString());
  }

  @SuppressWarnings("unused")
  public Float toFloat() {
    return Float.valueOf(stringBuilderField.toString());
  }

  public BigDecimal toBigDecimal() {
    return BigDecimal.valueOf(Double.parseDouble(stringBuilderField.toString()));
  }

  public Strings add(String s) {
    this.stringBuilderField.append(s);
    return this;
  }

  @SuppressWarnings("unused")
  public Boolean toBoolean() {
    return Booleans.createFromString(this.stringBuilderField.toString()).toBoolean();
  }

  @SuppressWarnings("unused")
  public String toBase64() {
    return Base64Utility.stringToBase64UrlString(this.stringBuilderField.toString());
  }

  @SuppressWarnings("unused")
  public Reader toReader() {
    return new StringReader(this.stringBuilderField.toString());
  }

  public Strings multiply(int length) {
    String string = this.stringBuilderField.toString();
    this.stringBuilderField.append(string.repeat(Math.max(0, length - 1)));
    return this;
  }

  public Strings onOneLine() {

    return Strings.createFromString(this.stringBuilderField.toString().replace(EOL_WINDOWS, " ").replace(EOL_LINUX, " "));

  }

  public Strings max(int max) {
    if (stringBuilderField.length() > max) {
      stringBuilderField.delete(max, stringBuilderField.length());
    }
    return this;
  }


  public Strings trim(String s) {
    rtrim(s);
    ltrim(s);
    return this;
  }

  private Strings ltrim(String s) {
    int i = stringBuilderField.indexOf(s, 0);
    if (i == 0) {
      stringBuilderField.delete(0, s.length());
    }
    return this;
  }

  /**
   * Add the string in a new line
   */
  @SuppressWarnings("unused")
  public Strings addLine(String s) {
    stringBuilderField.append(EOL)
      .append(s);
    return this;
  }

  public int getLineCount() {
    String s = toString();
    try (InputStream fis = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))) {
      // +1 because if it's a line without end of line, you get still one line
      return parseEndOfLine(fis, Integer.class) + 1;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getEol() {
    String s = toString();
    try (InputStream fis = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))) {
      return parseEndOfLine(fis, String.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String toPrintableCharacter() {
    return toString()
      .replaceAll("\n", "\\\\n")
      .replaceAll("\r", "\\\\r");
  }

  public String toMaxLength(int length, String suffix) {

    if (this.stringBuilderField.length() < length) {
      return this.stringBuilderField.toString();
    }
    return this.stringBuilderField.substring(0, length) + suffix;

  }


  /**
   * Replace double/triple/N whitespaces to one whitespace
   */
  public Strings normalizeWhiteSpaceToOnlyOneConsecutive() {

    List<Integer> whiteSpaceToDelete = new ArrayList<>();
    boolean whiteSpaceonPreviousCharacter = false;
    for (int i = 0; i < stringBuilderField.length(); i++) {

      char c = stringBuilderField.charAt(i);

      /**
       * White space handling
       */
      if (Character.isWhitespace(c)) {
        if (whiteSpaceonPreviousCharacter) {
          // double whitespace
          whiteSpaceToDelete.add(i);
        }
        whiteSpaceonPreviousCharacter = true;
      } else {
        whiteSpaceonPreviousCharacter = false;
      }

    }
    for (int i : whiteSpaceToDelete) {
      stringBuilderField.delete(i, i);
    }
    return this;

  }

    /**
     * @param string the content that should be on one line
     * @return a compact string that is written on one line and has no double space
     */
    static public String onOneLine(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll(" ", " "); // No double space;
    }

}
