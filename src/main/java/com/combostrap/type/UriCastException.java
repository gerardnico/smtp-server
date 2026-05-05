package com.combostrap.type;


public class UriCastException extends CastException {
    public UriCastException(String message) {
        super(message);
    }

    public UriCastException(String message, Exception e) {
        super(message, e);
    }
}
