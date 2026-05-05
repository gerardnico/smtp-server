package com.combostrap.smtp;

import io.vertx.core.Handler;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * When an exception is thrown and not
 * captured such NPE (null pointer exception)
 */
public class SmtpExceptionHandler implements Handler<Throwable> {


    private static final Logger LOGGER = Logger.getLogger(SmtpExceptionHandler.class.getName());

    public static Handler<Throwable> create() {
        return new SmtpExceptionHandler();
    }

    public static void logTheException(Throwable e) {

        if (e instanceof SmtpException) {
            LOGGER.log(Level.SEVERE, "A SMTP exception has occurred.", e);
            return;
        }
        LOGGER.log(Level.SEVERE, "A unforeseen exception has occurred.", e);
    }


    @Override
    public void handle(Throwable event) {
        logTheException(event);
    }

}
