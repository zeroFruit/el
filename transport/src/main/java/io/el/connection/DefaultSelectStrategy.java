package io.el.connection;

import io.el.connection.util.IntSupplier;

public class DefaultSelectStrategy implements SelectStrategy {

  static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

  private DefaultSelectStrategy() {
  }

  @Override
  public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
    return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT;
  }
}
