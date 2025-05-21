package io.netty.example.server.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author: czy
 * @date: 2025/4/24 16:35
 */
public class OrderFrameDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 重写解决粘包半包问题
     */
    public OrderFrameDecoder() {
        super(Integer.MAX_VALUE, 0, 2, 0, 2);
    }
}
