package io.netty.example.server.codec;

import io.netty.handler.codec.LengthFieldPrepender;

/**
 * @author: czy
 * @date: 2025/5/12 11:01
 */
public class OrderFrameEncoder extends LengthFieldPrepender {
    public OrderFrameEncoder() {
        super(2);
    }
}
