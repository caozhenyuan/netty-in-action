package io.netty.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.client.codec.OperationToRequestMessageEncoder;
import io.netty.example.client.codec.OrderFrameDecoder;
import io.netty.example.client.codec.OrderFrameEncoder;
import io.netty.example.client.codec.OrderProtocolDecoder;
import io.netty.example.client.codec.OrderProtocolEncoder;
import io.netty.example.client.dispatcher.OperationResultFuture;
import io.netty.example.client.dispatcher.RequestPendingCenter;
import io.netty.example.client.dispatcher.ResponseDispatcherHandler;
import io.netty.example.common.OperationResult;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.order.OrderOperation;
import io.netty.example.util.IdUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.ExecutionException;

/**
 * @author: czy
 * @date: 2025/5/14 13:49
 * @Deprecated 响应式写法
 */
public class ClientV2 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class);

        bootstrap.group(new NioEventLoopGroup());

        RequestPendingCenter requestPendingCenter = new RequestPendingCenter();

        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new OrderFrameDecoder());
                pipeline.addLast(new OrderFrameEncoder());
                pipeline.addLast(new OrderProtocolEncoder());
                pipeline.addLast(new OrderProtocolDecoder());

                pipeline.addLast(new ResponseDispatcherHandler(requestPendingCenter));

                pipeline.addLast(new OperationToRequestMessageEncoder());
                pipeline.addLast(new LoggingHandler(LogLevel.INFO));
            }
        });
        //这里是异步的
        ChannelFuture channelFuture= bootstrap.connect("127.0.0.1",8090);
        channelFuture.sync();

        long streamId = IdUtil.nextId();
        RequestMessage requestMessage = new RequestMessage(streamId, new OrderOperation(1001, "tudou"));
        OperationResultFuture future = new OperationResultFuture();
        requestPendingCenter.add(streamId,future);
        channelFuture.channel().writeAndFlush(requestMessage);
        OperationResult result = future.get();
        System.out.println(result);
        channelFuture.channel().closeFuture().get();
    }
}
