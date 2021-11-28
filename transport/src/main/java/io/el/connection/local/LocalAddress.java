package io.el.connection.local;

import io.el.connection.Channel;
import java.net.SocketAddress;

public class LocalAddress extends SocketAddress implements Comparable<LocalAddress> {

  public static final LocalAddress ANY = new LocalAddress("ANY");

  private final String id;
  private final String strVal;

  public LocalAddress(String id) {
    this.id = id.trim().toLowerCase();
    this.strVal = "local:" + this.id;
  }

  LocalAddress(Channel channel) {
    StringBuilder buf = new StringBuilder(16);
    buf.append("local:E");
    buf.append(Long.toHexString(channel.hashCode() & 0xFFFFFFFFL | 0x100000000L));
    buf.setCharAt(7, ':');
    id = buf.substring(6);
    strVal = buf.toString();
  }

  public String id() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }


  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LocalAddress)) {
      return false;
    }
    return id.equals(((LocalAddress) o).id());
  }


  @Override
  public int compareTo(LocalAddress o) {
    return id.compareTo(o.id());
  }

  @Override
  public String toString() {
    return strVal;
  }
}
