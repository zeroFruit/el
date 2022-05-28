package io.el.channel.local;

import static io.el.internal.ObjectUtil.checkNonEmptyAfterTrim;

import io.el.channel.Channel;
import java.net.SocketAddress;

public final class LocalAddress extends SocketAddress {
  private final String id;
  private final String strVal;

  LocalAddress(Channel channel) {
    StringBuilder buf = new StringBuilder(16);
    buf.append("local:E");
    buf.append(channel.id());
    buf.setCharAt(7, ':');
    id = buf.substring(6);
    strVal = buf.toString();
  }

  public LocalAddress(String id) {
    this.id = checkNonEmptyAfterTrim(id, "id").toLowerCase();
    this.strVal = "local:" + this.id;
  }

  public String id() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LocalAddress)) {
      return false;
    }

    return id.equals(((LocalAddress) o).id);
  }

  @Override
  public String toString() {
    return strVal;
  }
}
