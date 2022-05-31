package io.el.channel;

import io.el.internal.ObjectUtil;
import java.lang.reflect.Constructor;

/**
 * A {@link ChannelFactory} that instantiates a new {@link Channel} by invoking its default constructor reflectively.
 * */
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {

  private final Constructor<? extends T> constructor;

  public ReflectiveChannelFactory(Class<? extends T> clazz) {
    ObjectUtil.checkNotNull(clazz, "clazz");
    try {
      this.constructor = clazz.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Class " + clazz.getSimpleName() +
          " does not have a public non-arg constructor", e);
    }
  }

  @Override
  public T newChannel() {
    try {
      return constructor.newInstance();
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
    }
  }
}
