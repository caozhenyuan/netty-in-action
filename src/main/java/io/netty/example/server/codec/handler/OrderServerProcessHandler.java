package io.netty.example.server.codec.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.common.Operation;
import io.netty.example.common.OperationResult;
import io.netty.example.common.RequestMessage;
import io.netty.example.common.ResponseMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: czy
 * @date: 2025/5/9 15:41
 */
@Slf4j
public class OrderServerProcessHandler extends SimpleChannelInboundHandler<RequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RequestMessage requestMessage) throws Exception {
        //演示内存泄漏
//        ByteBuf buffer = channelHandlerContext.alloc().buffer();

        Operation operation = requestMessage.getMessageBody();
        OperationResult operationResult = operation.execute();

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setMessageHeader(requestMessage.getMessageHeader());
        responseMessage.setMessageBody(operationResult);

        if (channelHandlerContext.channel().isActive() && channelHandlerContext.channel().isWritable()) {
            channelHandlerContext.writeAndFlush(responseMessage);
        }else{
            log.error("message dropped");
        }
    }
}
