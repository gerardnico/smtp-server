package com.combostrap.type;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A key name normalizer that will see as equals different name with different casing
 * If the key is built from a collection of string, the final name separator
 * used to construct the key name is by default the underscore, but you can set it yourself {@link KeyNormalizerBuilder#setSeparator(String)}
 */
@SuppressWarnings("unused")
public class KeyNormalizer implements Comparable<KeyNormalizer>, KeyInterface {


  public static final String UNDERSCORE = "_";
  public static final String HYPHEN = "-";

  public final String stringOrigin;

  /**
   * A map of the string parsed and the parts in a list
   * String is lowercase
   */
  private final List<String> normalizedParts = new ArrayList<>();
  private final List<String> originalParts = new ArrayList<>();


  /**
   * @param builder - the string to normalize
   *                Note that
   *                * environment variable may start with _
   *                * cli option and flag may start with -
   *                therefore they will be
   * @throws CastException if the name does have any letter or digit
   */
  KeyNormalizer(KeyNormalizerBuilder builder) throws CastException {

    if (builder.stringOrigin != null) {
      stringOrigin = builder.stringOrigin;
      buildFromString(stringOrigin);
    } else if (builder.originalParts != null) {
      Collection<String> originalParts = builder.originalParts;
      this.originalParts.addAll(originalParts);
      this.normalizedParts.addAll(originalParts.stream().map(String::toLowerCase).collect(Collectors.toList()));
      this.stringOrigin = String.join(builder.separator, originalParts);
    } else {
      throw new CastException("The original parts are empty. A string or a collection should be provided.");
    }


  }

  private void buildFromString(String stringOrigin) throws CastException {

    StringBuilder currentWord = new StringBuilder();
    /*
     * To handle UPPER SNAKE CASE
     * such as UPPER_SNAKE_CASE
     * We split on a UPPER case character only if the previous character is not
     */
    boolean previousCharacterIsNotUpperCase = false;

    for (char c : stringOrigin.toCharArray()) {

      // Separator (ie whitespace, comma, dollar, underscore, ...)
      boolean isCharacterSeparator = Character.isWhitespace(c) || !Character.isLetterOrDigit(c);
      boolean currentCharacterIsUpperCase = Character.isUpperCase(c);
      /*
       * Separate on Uppercase if the previous character is not UPPER Case
       * For example: to separate UPPER_CASE key in 2 words UPPER and CASE
       */
      boolean separateOnCase = currentCharacterIsUpperCase && previousCharacterIsNotUpperCase;
      if (isCharacterSeparator || separateOnCase) {
        if (currentWord.length() > 0) {
          String string = currentWord.toString();
          normalizedParts.add(string.toLowerCase());
          originalParts.add(string);
          currentWord.setLength(0);
        }
      }
      /*
       * End
       */
      previousCharacterIsNotUpperCase = !currentCharacterIsUpperCase;
      if (isCharacterSeparator) {
        // we don't collect character separator
        continue;
      }
      currentWord.append(c);
    }

    if (currentWord.length() > 0) {
      String string = currentWord.toString();
      normalizedParts.add(string.toLowerCase());
      originalParts.add(currentWord.toString());
    }
    if (normalizedParts.isEmpty()) {
      throw new CastException("The key value (" + this.stringOrigin + ") after normalization is empty, It does not have any letter or digits.");
    }
  }


  /**
   * @param key - the name key to normalize
   *            This normalizer accepts all cases.
   *            It will split the key in words that are separated
   *            by separators characters (not letter or digit)
   *            by uppercase letter (if not preceded by another uppercase character to handle UPPER_SNAKE_CASE)
   *            The words can then be printed/normalized into a {@link KeyCase}
   * @throws CastException when the key is null, does not have any letter or digit
   */
  public static KeyNormalizer create(Object key) throws CastException {
    if (key == null) {
      throw new CastException("The key should not be null");
    }
    if (key instanceof KeyNormalizer) {
      return (KeyNormalizer) key;
    }
    KeyNormalizerBuilder keyBuilder = KeyNormalizer.builder();
    if (key instanceof String) {
      return keyBuilder.setString((String) key).build();
    }
    if (key instanceof StringBuilder) {
      return keyBuilder.setString(((StringBuilder) key).toString()).build();
    }
    if (key instanceof Enum) {
      return keyBuilder.setEnum((Enum<?>) key).build();
    }
    if (key instanceof Number) {
      return keyBuilder.setString(key.toString()).build();
    }
    if (key instanceof KeyInterface) {
      return ((KeyInterface) key).toKeyNormalizer();
    }
    /**
     * Passing a collection of sub-parts
     */
    if (key instanceof Collection) {
      Collection<String> castedKey = Casts.castToCollection(key, String.class);
      return keyBuilder.setOriginalParts(castedKey).build();
    }
    /**
     * Note: simpleName does work with inner class
     */
    String name = key.getClass().getName();
    throw new CastException("The object is not a string, an enum, a keyNormalizer, a string collection or a keyInterface but a " + name);


  }

  public List<String> getOriginalParts() {
    return originalParts;
  }

  public static KeyNormalizerBuilder builder() {
    return new KeyNormalizerBuilder();
  }

  /**
   * Same as {@link #create(Object)} but with a runtime exception
   * To use when the key is known in advance to have letters and digits
   */
  public static KeyNormalizer createSafe(Object key) {
    try {
      return create(key);
    } catch (CastException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * @return the words in camelCase
   * For JSON keys, the most widely adopted convention is camelCase.
   */
  public String toJsonCase() {
    return this.toCamelCase();
  }

  /**
   * @return the words in a camel case (ie UserCount)
   */
  public String toCamelCase() {
    return this
      .normalizedParts
      .stream()
      .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
      .collect(Collectors.joining());
  }


  /**
   * @return a name in event case. ie camel case with a space between words
   */
  public String toHandleCase() {
    return this
      .normalizedParts
      .stream()
      .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
      .collect(Collectors.joining(" "));
  }

  /**
   * @return the words in a Snake Case (ie user_count)
   */
  public String toSnakeCase() {
    return normalizedParts
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("_"));
  }

  /**
   * @return the words in an Upper Snake Case (ie USER_COUNT)
   */
  public String toUpperSnakeCase() {
    return normalizedParts
      .stream()
      .map(String::toUpperCase)
      .collect(Collectors.joining("_"));
  }


  /**
   * @return the words in a Snake Case (ie user_count)
   */
  public String toSqlCase() {

    return toSnakeCase();

  }

  /**
   * @return a case specific for sql type (ie words separated by a space). It's an alias for {@link #toSpaceCase()})
   */
  public String toSqlTypeCase() {

    return toSpaceCase();

  }

  /**
   * @return normalized words separated by a space
   */
  public String toSpaceCase() {
    return normalizedParts
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining(" "));
  }

  /**
   * @return to HTTP header
   * * Title-Case: Each word is capitalized (first letter uppercase, rest lowercase)
   * * Hyphen-separated: Words are separated by hyphens
   */
  public String toHttpHeaderCase() {
    return normalizedParts
      .stream()
      .map(part->{
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(part.charAt(0)));
        if (part.length() > 1) {
          result.append(part.substring(1));
        }
        return result.toString();
      })
      .collect(Collectors.joining("-"));
  }


  /**
   * @return the words in an Upper Snake Case (ie USER_COUNT)
   * Old case that conflicts with shouting.
   */
  @SuppressWarnings("unused")
  public String toUpperSqlCase() {
    return this.toSqlCase().toUpperCase();
  }


  public String toCase(KeyCase keyCase) {
    switch (keyCase) {
      case HANDLE:
        return toHandleCase();
      case CAMEL:
        return toCamelCase();
      case HYPHEN:
      case KEBAB:
        return toHyphenCase();
      case FILE:
        return toFileCase();
      case SNAKE:
        return toSnakeCase();
      case SNAKE_UPPER:
        return toUpperSnakeCase();
      case SQL:
        return toSqlCase();
      default:
        throw new IllegalArgumentException("The word-case (" + keyCase + ") is unknown");
    }
  }


  /**
   * @return the words in a Hyphen Case (ie user-count)
   * Aeries of lowercase name separated by a minus (used by the command line and in HTML template variable)
   */
  public String toHyphenCase() {
    return this
      .normalizedParts
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("-"));
  }

  /**
   * @return the long option name used in cli (ie {@link #toHyphenCase()}
   */
  public String toCliLongOptionName() {
    return toHyphenCase();
  }

  /**
   * @return a name that can be used as file name in the file system (ie the {@link #toHyphenCase()}
   */
  public String toFileCase() {
    /**
     * Underscore advantages:
     * <p>
     * Works consistently across all operating systems (Windows, macOS, Linux)
     * No issues with command-line tools or scripts
     * Widely accepted convention in programming and system administration
     * Never conflicts with command-line argument syntax
     */
    return this
      .normalizedParts
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining(UNDERSCORE));
  }

  /**
   * The hyphen (-) is the convention for Docker container names.
   * Docker container names have specific requirements:
   * * Must contain only lowercase letters, digits, underscores, periods, and hyphens
   * * Cannot start with a period or hyphen
   * * Must be between 1 and 63 characters
   */
  public String toDockerCase() {
    return this
      .normalizedParts
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining(HYPHEN));
  }

  /**
   * @return the string origin
   */
  @Override
  public String toString() {
    return this.stringOrigin;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    KeyNormalizer that = (KeyNormalizer) o;
    return Objects.equals(this.normalizedParts, that.normalizedParts);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.normalizedParts);
  }

  /**
   * @return only the first letter of each word concatenated
   * This is not posix compliant
   */
  public String toCliShortOptionName() {
    return this
      .normalizedParts
      .stream()
      .map(s -> String.valueOf(s.charAt(0)).toLowerCase())
      .collect(Collectors.joining());
  }

  /**
   * @return a css compliant name in {@link #toHyphenCase()}
   */
  public String toCssPropertyName() {
    return toHyphenCase();
  }

  /**
   * @return a standard case for java system property ie `user.count`
   * There is no default but this is the most used
   * Example: "web.environment", "vertx.web.environment"
   */
  public String toJavaSystemPropertyName() {
    return this
      .normalizedParts
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("."));
  }

  /**
   * @return a html attribute name compliant case in {@link #toHyphenCase()}
   */
  public String toHtmlAttributeName() {
    return toHyphenCase();
  }

  /**
   * @return kebab case (ie user-count). Alias for {@link #toHyphenCase()}
   * The name comes from the similarity of the words to meat on a kebab skewer.
   * <a href="https://developer.mozilla.org/en-US/docs/Glossary/Kebab_case">...</a>
   */
  public String toKebabCase() {
    return toHyphenCase();
  }

  /**
   * @return env name (ie USER_NAME)
   */
  @SuppressWarnings("unused")
  public String toEnvName() {
    return this
      .normalizedParts
      .stream()
      .map(String::toUpperCase)
      .collect(Collectors.joining(UNDERSCORE));
  }

  /**
   * @return the words, parts of the name in lowercase to implement your own case
   */
  public List<String> getNormalizedParts() {
    return normalizedParts;
  }

  @Override
  public int compareTo(KeyNormalizer o) {
    /**
     * Comparison is
     * * case-insensitive
     * * separator insensitive (space/underscore)
     */
    return this.toFileCase().compareTo(o.toFileCase());
  }

  @Override
  public String name() {
    return toString();
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return this;
  }

  public static class KeyNormalizerBuilder {

    public String stringOrigin;
    private Collection<String> originalParts = new ArrayList<>();
    private String separator = UNDERSCORE;


    /**
     * @param s - normally the {@link #stringOrigin} but it may be first process to
     *          normalize the string to a valid name
     */
    private KeyNormalizerBuilder setString(String s) throws CastException {

      this.stringOrigin = s;
      return this;
    }

    public KeyNormalizer build() throws CastException {

      return new KeyNormalizer(this);
    }

    public KeyNormalizerBuilder setSeparator(String separator) throws CastException {
      this.separator = separator;
      return this;
    }

    public KeyNormalizerBuilder setEnum(Enum<?> key) throws CastException {
      return setString(key.name());
    }

    public KeyNormalizerBuilder setOriginalParts(Collection<String> originalParts) {
      this.originalParts = originalParts;
      return this;
    }
  }
}
