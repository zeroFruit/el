package io.el.connection.util;

/**
 * Represents a supplier of {@code int}-valued results.
 */
public interface IntSupplier {

  int get() throws Exception;
}
