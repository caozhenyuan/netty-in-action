package io.netty.example.server.codec.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.common.Operation;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.auth.AuthOperation;
import io.netty.example.common.auth.AuthOperationResult;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: czy
 * @date: 2025/6/10 10:07
 */
@Slf4j
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<RequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestMessage msg) throws Exception {
        try {
            Operation operation = msg.getMessageBody();
            if (operation instanceof AuthOperation) {
                AuthOperation authOperation = AuthOperation.class.cast(operation);
                AuthOperationResult authOperationResult = authOperation.execute();
                if (authOperationResult.isPassAuth()) {
                    log.info("pass auth");
                } else {
                    log.error("fail to auth");
                    ctx.close();
                }

            } else {
                log.error("expect first msg is auth");
                ctx.close();
            }
        } catch (Exception e) {
            log.error("exception happer");
            ctx.close();
        }finally {
            //如果授权已经成功的话，没必要重复授权
            ctx.pipeline().remove(this);
        }
    }
}
