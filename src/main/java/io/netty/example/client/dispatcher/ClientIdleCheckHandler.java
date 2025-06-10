package io.netty.example.client.dispatcher;

import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author: czy
 * @date: 2025/6/6 15:50
 */
public class ClientIdleCheckHandler extends IdleStateHandler {
    public ClientIdleCheckHandler() {
        super(0, 5, 0);
    }
}
