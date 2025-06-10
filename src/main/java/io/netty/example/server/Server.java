package io.netty.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.server.codec.OrderFrameDecoder;
import io.netty.example.server.codec.OrderFrameEncoder;
import io.netty.example.server.codec.OrderProtocolDecoder;
import io.netty.example.server.codec.OrderProtocolEncoder;
import io.netty.example.server.codec.handler.MetricHandler;
import io.netty.example.server.codec.handler.OrderServerProcessHandler;
import io.netty.example.server.codec.handler.ServerIdleCheckHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

import java.util.concurrent.ExecutionException;

/**
 * @author: czy
 * @date: 2025/5/12 11:10
 */
public class Server {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.channel(NioServerSocketChannel.class);

        serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));

        NioEventLoopGroup boss = new NioEventLoopGroup(0, new DefaultThreadFactory("boss"));
        NioEventLoopGroup work = new NioEventLoopGroup(0, new DefaultThreadFactory("work"));
        serverBootstrap.group(boss, work);

        //调整System参数
        serverBootstrap.childOption(NioChannelOption.TCP_NODELAY, true);
        serverBootstrap.option(NioChannelOption.SO_BACKLOG, 1024);

        MetricHandler metricHandler = new MetricHandler();
        UnorderedThreadPoolEventExecutor business = new UnorderedThreadPoolEventExecutor(10, new DefaultThreadFactory("business"));
        GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(
                new NioEventLoopGroup(), 100 * 1024 * 1024, 100 * 1024 * 1024
        );

        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                //流量整形
                pipeline.addLast("TShaping", globalTrafficShapingHandler);
                //服务器加上 read idle check-服务器10s 接受不到 channel的请求就断掉连接
                pipeline.addLast("IdleCheck",new ServerIdleCheckHandler());

                pipeline.addLast("frameDecoder", new OrderFrameDecoder());
                pipeline.addLast(new OrderFrameEncoder());
                pipeline.addLast(new OrderProtocolEncoder());
                pipeline.addLast(new OrderProtocolDecoder());

                pipeline.addLast("metricHandler", metricHandler);
                pipeline.addLast(new LoggingHandler(LogLevel.INFO));

                //配置flush增强
                pipeline.addLast("flushEnhance", new FlushConsolidationHandler(5, true));


                pipeline.addLast(business, new OrderServerProcessHandler());
                //修改日志级别
//                pipeline.addLast(new LoggingHandler(LogLevel.ERROR));
            }
        });
        ChannelFuture channelFuture = serverBootstrap.bind(8090).sync();
        channelFuture.channel().closeFuture().get();
    }
}
