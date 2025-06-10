package io.netty.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.client.codec.OrderFrameDecoder;
import io.netty.example.client.codec.OrderFrameEncoder;
import io.netty.example.client.codec.OrderProtocolDecoder;
import io.netty.example.client.codec.OrderProtocolEncoder;
import io.netty.example.client.dispatcher.ClientIdleCheckHandler;
import io.netty.example.client.dispatcher.KeepaliveHandler;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.auth.AuthOperation;
import io.netty.example.common.order.OrderOperation;
import io.netty.example.util.IdUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLException;
import java.util.concurrent.ExecutionException;

/**
 * @author: czy
 * @date: 2025/5/12 11:10
 */
public class Client {

    public static void main(String[] args) throws InterruptedException, ExecutionException, SSLException {
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class);

        bootstrap.group(new NioEventLoopGroup());

        //设置客户端连接超时时间。
        bootstrap.option(NioChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000);

        KeepaliveHandler keepaliveHandler = new KeepaliveHandler();

        //客户端SSL(这里演示单向验证)
        SslContext sslCtx = SslContextBuilder.forClient().build();

        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                //客户端加上 write idle check+keepalive-客户端 5s 不发送数据就发一个 keepalive
                pipeline.addLast("idleCheck", new ClientIdleCheckHandler());

                //SSL
                SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
                pipeline.addLast("sslClient",sslHandler);

                pipeline.addLast(new OrderFrameDecoder());
                pipeline.addLast(new OrderFrameEncoder());
                pipeline.addLast(new OrderProtocolEncoder());
                pipeline.addLast(new OrderProtocolDecoder());

                //因为keepalive消息也是需要编解码的，所以放在后面
                pipeline.addLast("keepalive", keepaliveHandler);

                pipeline.addLast(new LoggingHandler(LogLevel.INFO));
            }
        });
        //这里是异步的
        ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8090);
        channelFuture.sync();
        RequestMessage requestMessage = new RequestMessage(IdUtil.nextId(), new OrderOperation(1001, "todou"));
        //第一次发送授权消息
        AuthOperation authOperation = new AuthOperation("admin2", "123456");
        channelFuture.channel().writeAndFlush(new RequestMessage(IdUtil.nextId(), authOperation));
        //模拟发送
        for (int i = 0; i < 20; i++) {
            channelFuture.channel().writeAndFlush(requestMessage);
        }

        channelFuture.channel().closeFuture().get();
    }
}
