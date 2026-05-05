package com.combostrap.smtp.sasl;

import com.combostrap.smtp.SmtpDomain;
import com.combostrap.smtp.SmtpUser;

/**
 * <a href="https://www.rfc-editor.org/rfc/rfc4616.html#section-4">...</a>
 * The input is: `<NUL>tim<NUL>tanstaaftanstaaf`
 */
public class SimpleAuthPlain extends SimpleAuth {

    private static final Character NUL_CHARACTER = '\u0000';

    public SimpleAuthPlain(SimpleAuthMechanism simpleAuthMechanism) {
        super(simpleAuthMechanism);
    }

    /**
     * The client presents:
     * * the authorization identity (identity to act as),
     * * followed by a NUL (U+0000) character,
     * * followed by the authentication identity (identity whose password will be used),
     * * followed by a NUL (U+0000) character,
     * * followed by the clear-text password.
     */
    @Override
    public SmtpUser authenticate(SmtpDomain smtpDomain, String credential) throws SimpleAuthException {

        String[] credentials = credential.split(NUL_CHARACTER.toString());
        if (credentials.length != 3) {
            throw SimpleAuthException.create("Plain auth should have 3 tokens separated by the NUL character");
        }
        // By default, the authorizationIdentity is equals to the authentication identity
        // String authorizationIdentity = credentials[0];
        String authenticationIdentity = credentials[1];
        String passwordIdentity = credentials[2];
        SmtpUser smtpUser = smtpDomain.getUser(authenticationIdentity).orElse(null);
        if (smtpUser == null) {
            throw SimpleAuthException.create("Unknown authorization identity");
        }

        String password = smtpUser.getPassword();
        if (password == null) {
            throw SimpleAuthException.create("User not allowed to login");
        }
        if (!password.equals(passwordIdentity)) {
            throw SimpleAuthException.create("Bad credentials");
        }

        return smtpUser;


    }

    @Override
    public boolean isImplemented() {
        return true;
    }

}
