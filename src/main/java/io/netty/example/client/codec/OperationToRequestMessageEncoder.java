package io.netty.example.client.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.example.common.Operation;
import io.netty.example.common.RequestMessage;
import io.netty.example.util.IdUtil;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * @author: czy
 * @date: 2025/5/14 13:51
 */
public class OperationToRequestMessageEncoder extends MessageToMessageEncoder<Operation> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Operation operation, List<Object> out) throws Exception {
        RequestMessage requestMessage = new RequestMessage(IdUtil.nextId(), operation);
        out.add(requestMessage);
    }
}
