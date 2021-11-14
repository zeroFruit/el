package io.el.connection;

import io.el.connection.util.IntSupplier;

public interface SelectStrategy {

  /**
   * Indicates a blocking select should follow.
   */
  int SELECT = -1;
  /**
   * Indicates the IO loop should be retried, no blocking select to follow directly.
   */
  int CONTINUE = -2;
  /**
   * Indicates the IO loop to poll for new events without blocking.
   */
  int BUSY_WAIT = -3;

  /**
   * The {@link SelectStrategy} can be used to steer the outcome of a potential select call.
   *
   * @param selectSupplier The supplier with the result of a select result.
   * @param hasTasks       true if tasks are waiting to be processed.
   * @return {@link #SELECT} if the next step should be blocking select {@link #CONTINUE} if the
   * next step should be to not select but rather jump back to the IO loop and try again. Any value
   * >= 0 is treated as an indicator that work needs to be done.
   */
  int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;
}
