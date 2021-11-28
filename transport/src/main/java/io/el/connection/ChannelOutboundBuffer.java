package io.el.connection;

public final class ChannelOutboundBuffer {

  private Entry flushedEntry;

  public Object current() {
    Entry entry = flushedEntry;
    if (entry == null) {
      return null;
    }
    return entry.msg;
  }

  static final class Entry {

    Object msg;

    private Entry(Object msg) {
      this.msg = msg;
    }

    static Entry newInstance(Object msg) {
      return new Entry(msg);
    }
  }
}
