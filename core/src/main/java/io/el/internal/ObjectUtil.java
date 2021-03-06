package io.el.internal;

public final class ObjectUtil {

  private ObjectUtil() {}

  public static <T> T checkNotNull(T arg, String text) {
    if (arg == null) {
      throw new NullPointerException(text);
    }
    return arg;
  }

  public static long checkPositiveOrZero(long l, String name) {
    if (l < 0) {
      throw new IllegalArgumentException(name + ": " + l + " (expected: >= 0)");
    }
    return l;
  }

  public static long checkPositive(long l, String name) {
    if (l <= 0) {
      throw new IllegalArgumentException(name + ": " + l + " (expected: > 0)");
    }
    return l;
  }

  public static String checkNonEmpty(final String value, final String name) {
    if (checkNotNull(value, name).isEmpty()) {
      throw new IllegalArgumentException("Param '" + name + "' must not be empty");
    }
    return value;
  }

  public static String checkNonEmptyAfterTrim(final String value, final String name) {
    String trimmed = checkNotNull(value, name).trim();
    return checkNonEmpty(trimmed, name);
  }
}
