package com.nursing.home.dto;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
