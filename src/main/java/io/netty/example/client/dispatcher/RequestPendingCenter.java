package io.netty.example.client.dispatcher;

import io.netty.example.common.OperationResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: czy
 * @date: 2025/5/14 14:30
 */
public class RequestPendingCenter {

    private Map<Long, OperationResultFuture> map = new ConcurrentHashMap<>();

    public void add(Long streamId, OperationResultFuture future) {
        this.map.put(streamId, future);
    }

    public void set(Long streamId, OperationResult result) {
        OperationResultFuture future = map.get(streamId);
        if (null != future) {
            future.setSuccess(result);
            this.map.remove(streamId);
        }
    }
}
