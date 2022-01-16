package io.el.example.channel;

import io.el.connection.Channel;
import io.el.connection.ChannelInitializer;
import io.el.connection.ChannelEventLoopGroup;
import io.el.bootstrap.ServerBootstrap;
import io.el.connection.nio.NioChannelEventLoopGroup;

import io.el.connection.socket.nio.NioServerSocketChannel;
import java.util.concurrent.TimeUnit;

public class Example {
    private int port;

    public Example(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException {
        ChannelEventLoopGroup parent = new NioChannelEventLoopGroup();
        ChannelEventLoopGroup child = new NioChannelEventLoopGroup();
        try {
            ServerBootstrap<Channel> b = new ServerBootstrap<>();
            b
                    .group(parent, child)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {

                      @Override
                      protected void initChannel(Channel ch) throws Exception {

                      }
                    });

            b.bind(port).await();
        } finally {
            parent.shutdownGracefully(1, TimeUnit.SECONDS);
            child.shutdownGracefully(1, TimeUnit.SECONDS);
        }
    }
}
