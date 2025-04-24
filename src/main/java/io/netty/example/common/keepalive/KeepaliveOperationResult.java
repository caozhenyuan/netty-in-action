package io.netty.example.common.keepalive;

import io.netty.example.common.OperationResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KeepaliveOperationResult extends OperationResult {

    private final long time;

}
