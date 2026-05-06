package com.combostrap.vertx;

import com.combostrap.smtp.SmtpConfigBean;
import com.combostrap.smtp.exceptions.NoConfException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ssl meta info
 * Note that Chrome does not allow to set a third-party cookie (ie same site: None)
 * if the connection is not secure.
 * It must be true then everywhere.
 * For non-app, https comes from the proxy.
 * See the https.md documentation for more info.
 */
public class ServerSsl {


    public Path getKey() {
        return key;
    }


    public Path getCert() {
        return cert;
    }

    public ServerSsl(Path key, Path cert) {
        this.key = key;
        this.cert = cert;
    }

    Path key;
    Path cert;

    public static ServerSsl create(SmtpConfigBean configAccessor)  {

        Path sslKeyPath;
        try {
            sslKeyPath = Paths.get(configAccessor.sslKey);
        } catch (NoConfException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if (!Files.exists(sslKeyPath)) {
            throw new IllegalStateException("The ssl key file (" + sslKeyPath.toAbsolutePath() + ") does not exists");
        }

        Path sslCertPath;
        try {
            sslCertPath = Paths.get(configAccessor.sslCert);
        } catch (NoConfException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if (!Files.exists(sslCertPath)) {
            throw new IllegalStateException("The ssl cert file (" + sslCertPath.toAbsolutePath() + ") does not exists");
        }

        return new ServerSsl(sslKeyPath, sslCertPath);


    }

    public boolean isOn() {
        return this.key != null;
    }

}
