package com.combostrap.smtp.exceptions;

/**
 * Throws when a conf is mandatory and was not found
 */
public class NoConfException extends RuntimeException {

    public NoConfException(String s) {
        super(s);
    }

}
