package io.el.connection;

import java.lang.reflect.Constructor;

public class ReflectiveChannelFactory<C extends Channel> implements ChannelFactory<C> {

  private final Constructor<? extends C> constructor;

  public ReflectiveChannelFactory(Class<? extends C> clazz) {
    try {
      this.constructor = clazz.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Class " + clazz.getSimpleName() +
          " does not have a public non-arg constructor", e);
    }
  }

  @Override
  public C newChannel() {
    try {
      return constructor.newInstance();
    } catch (Throwable t) {
      // TODO: error handling
      throw new IllegalStateException(
          "Unable to create Channel from class " + constructor.getDeclaringClass(), t);
    }
  }
}
