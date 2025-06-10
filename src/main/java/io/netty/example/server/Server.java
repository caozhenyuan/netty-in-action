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
import io.netty.example.server.codec.handler.AuthHandler;
import io.netty.example.server.codec.handler.MetricHandler;
import io.netty.example.server.codec.handler.OrderServerProcessHandler;
import io.netty.example.server.codec.handler.ServerIdleCheckHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;

/**
 * @author: czy
 * @date: 2025/5/12 11:10
 */
public class Server {

    public static void main(String[] args) throws InterruptedException, ExecutionException, CertificateException, SSLException {
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
        //配置黑名单规则
        IpSubnetFilterRule ipSubnetFilterRule = new IpSubnetFilterRule("127.1.0.1", 16, IpFilterRuleType.REJECT);
        RuleBasedIpFilter ruleBasedIpFilter = new RuleBasedIpFilter(ipSubnetFilterRule);

        AuthHandler authHandler = new AuthHandler();

        //SSL自签
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        System.out.println(selfSignedCertificate.certificate());
        System.out.println(selfSignedCertificate.privateKey());
        SslContext sslCtx = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build();

        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                //配置黑名单
                pipeline.addLast("ipFilter", ruleBasedIpFilter);
                //流量整形
                pipeline.addLast("TShaping", globalTrafficShapingHandler);
                //统计客户端连接数
                pipeline.addLast("metricHandler", metricHandler);
                //服务器加上 read idle check-服务器10s 接受不到 channel的请求就断掉连接
                pipeline.addLast("IdleCheck", new ServerIdleCheckHandler());

                //SSL
                SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
                pipeline.addLast("sslServer", sslHandler);

                //编解码
                pipeline.addLast("frameDecoder", new OrderFrameDecoder());
                pipeline.addLast(new OrderFrameEncoder());
                pipeline.addLast(new OrderProtocolEncoder());
                pipeline.addLast(new OrderProtocolDecoder());

                pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                //配置flush增强
                pipeline.addLast("flushEnhance", new FlushConsolidationHandler(5, true));

                //自定义授权
                pipeline.addLast(authHandler);

                pipeline.addLast(business, new OrderServerProcessHandler());
                //修改日志级别
//                pipeline.addLast(new LoggingHandler(LogLevel.ERROR));
            }
        });
        ChannelFuture channelFuture = serverBootstrap.bind(8090).sync();
        channelFuture.channel().closeFuture().get();
    }
}
