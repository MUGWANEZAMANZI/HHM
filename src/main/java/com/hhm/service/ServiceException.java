package com.hhm.service;

public class ServiceException extends RuntimeException {

    public enum Type { CONFLICT, NOT_FOUND, VALIDATION }

    private final Type type;

    public ServiceException(String message) {
        super(message);
        this.type = Type.VALIDATION;
    }

    public ServiceException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.type = Type.VALIDATION;
    }

    public Type getType() { return type; }
}
