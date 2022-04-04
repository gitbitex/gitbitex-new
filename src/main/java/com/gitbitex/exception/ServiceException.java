package com.gitbitex.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final ErrorCode code;

    public ServiceException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ServiceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
