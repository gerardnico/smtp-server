package com.combostrap.type;


import com.combostrap.exception.InternalException;

import java.util.regex.Pattern;

/**
 * A class that encapsulate a color string
 */
public class Color {


  static Pattern pattern;

  static {
    // Regular expression to match common color formats
    String colorRegex = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^((rgb|hsl)a?\\(((\\s*\\d+%?\\s*,){2}\\s*\\d+%?\\s*,?\\s*\\d*(\\.\\d+)?\\s*\\)))$|^\\b(black|silver|gray|white|maroon|red|purple|fuchsia|green|lime|olive|yellow|navy|blue|teal|aqua)\\b$";
    pattern = Pattern.compile(colorRegex);
  }

  private final String color;

  private Color(String color) throws ColorCastException {

    // Match the colorString against the pattern
    if (!pattern.matcher(color).matches()) {
      throw new ColorCastException("The color value (" + color + ") is not a valid color");
    }

    this.color = color;
  }

  public static Color of(String primaryColor) throws ColorCastException {
    return new Color(primaryColor);
  }

  /**
   * @param primaryColor - a known good primary color value (ie a literal or a value from a database)
   * @return a color and fail if the value is not good
   */
  public static Color ofFailSafe(String primaryColor) {
      try {
          return new Color(primaryColor);
      } catch (ColorCastException e) {
          throw new InternalException(e);
      }
  }

  public String getValue() {
    return color;
  }

  @Override
  public String toString() {
    return color;
  }

}
