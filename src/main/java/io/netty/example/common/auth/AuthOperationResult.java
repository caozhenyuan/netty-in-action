package io.netty.example.common.auth;

import io.netty.example.common.OperationResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AuthOperationResult extends OperationResult {

    private final boolean passAuth;

}
