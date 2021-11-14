package io.el.connection;

public class DefaultSelectStrategyFactory implements SelectStrategyFactory {

  public static final DefaultSelectStrategyFactory INSTANCE = new DefaultSelectStrategyFactory();

  private DefaultSelectStrategyFactory() {
  }


  @Override
  public SelectStrategy newSelectStrategy() {
    return DefaultSelectStrategy.INSTANCE;
  }
}
