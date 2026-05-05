package com.combostrap.type;


public class BooleanCastException extends CastException {
    public BooleanCastException(String message) {
        super(message);
    }

    public BooleanCastException(String message, Exception e) {
        super(message, e);
    }
}
