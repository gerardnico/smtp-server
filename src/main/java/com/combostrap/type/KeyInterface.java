package com.combostrap.type;

public interface KeyInterface {

  /**
   * @return the name
   * Equivalent to the  {@link Enum#name()}
   */
  String name();

  /**
   * @return the name normalized
   */
  default KeyNormalizer toKeyNormalizer() {
    return KeyNormalizer.createSafe(this.name().toLowerCase());
  }

}
