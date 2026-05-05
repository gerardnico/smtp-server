package com.combostrap.smtp;

import com.combostrap.smtp.command.SmtpBdatCommandHandler;
import com.combostrap.smtp.command.SmtpMailCommandHandler;
import com.combostrap.smtp.exceptions.Discouraged;


/**
 * The type of data for the {@link SmtpInput}
 * <a href="https://datatracker.ietf.org/doc/html/rfc1830#section-4">...</a>
 */
public enum SmtpInputType {


    /**
     * Binary MIME message format.
     * This is format that is given to the data after a {@link SmtpBdatCommandHandler}
     * when the {@link SmtpExtensionParameter#BODY} get this value
     * <p>
     * When found in body parameter of the {@link SmtpMailCommandHandler mail command},
     * it indicates that the MIME message is in Binary format.
     * <p>
     * The BINARYMIME service extension can only be used with
     * the {@link SmtpExtensionParameter#CHUNKING } service extension.
     */
    BINARYMIME("BINARYMIME", true),

    /**
     * Text format on 7bit
     */
    @Discouraged(
            reason = "Not supported, because our parser supports only 8bit = 1 char"
    )
    TEXT_BIT7("7BIT", false),

    /**
     * Text format on 8bit, 8bit = 1 char
     * <a href="https://www.rfc-editor.org/info/rfc6152">...</a>
     * As a body parameter, it indicates that the MIME message is in text
     */
    TEXT_BIT8("8BITMIME", false);

    private final String smtpName;
    private final boolean isBinary;

    SmtpInputType(String smtpName, boolean isBinary) {

        this.smtpName = smtpName;
        this.isBinary = isBinary;

    }

    @Override
    public String toString() {
        return smtpName;
    }

    public boolean getIsBinary() {
        return isBinary;
    }

}
