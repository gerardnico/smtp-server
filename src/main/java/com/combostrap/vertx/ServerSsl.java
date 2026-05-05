package com.combostrap.vertx;

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

    /**
     * The default path for the key
     */
    public static final String DEV_KEY_PEM = "../cert/key.pem";
    public static final String DEV_CERT_PEM = "../cert/cert.pem";

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

    public static ServerSsl create(ConfigAccessor configAccessor)  {

        Path sslKeyPath;
        String sslKeyKey = "ssl.key";
        try {
            sslKeyPath = Paths.get((String) configAccessor.getMandatoryValue(sslKeyKey));
        } catch (NoConfException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if (!Files.exists(sslKeyPath)) {
            throw new IllegalStateException("The ssl key file (" + sslKeyPath.toAbsolutePath() + ") does not exists");
        }

        Path sslCertPath;
        String sslKeyCert = "ssl.cert";
        try {
            sslCertPath = Paths.get((String) configAccessor.getMandatoryValue(sslKeyCert));
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
