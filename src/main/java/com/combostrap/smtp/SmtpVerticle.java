package com.combostrap.smtp;

import com.combostrap.smtp.exceptions.ConfigIllegalException;
import com.combostrap.type.CastException;
import com.combostrap.type.Casts;
import com.combostrap.vertx.ConfigManager;
import com.combostrap.vertx.MainLauncher;
import com.combostrap.vertx.VertxHttpServer;
import com.combostrap.vertx.VertxNetServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SmtpVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(SmtpVerticle.class.getName());
    private static final String APPLICATION_NAME = "smtp-server";
    private SmtpServer smtpServer;


    public static void main(String[] args) {


        new MainLauncher().dispatch(new String[]{"run", SmtpVerticle.class.getName()});

    }

    @Override
    public void start(Promise<Void> verticlePromise) {


        LOGGER.info("Smtp Verticle Started");
        ConfigManager.config(APPLICATION_NAME, this.vertx, this.config())
                .build()
                .getConfigAccessor()
                .onFailure(e -> this.handleVerticleFailure(verticlePromise, e))
                .compose(configAccessor -> vertx.executeBlocking(() -> {

                    /**
                     * Smtp Server
                     */
                    this.smtpServer = SmtpServer.create(this, configAccessor);


                    /**
                     * Build a net server for each smtp service
                     */
                    List<Future<NetServer>> netServers = new ArrayList<>();
                    for (SmtpService smtpService : smtpServer.getSmtpServices()) {

                        /**
                         * Server Certificates
                         */
                        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
                        for (SmtpHost host : smtpServer.getHostedHosts().values()) {
                            String keyPath = host.getPrivateKeyPath();
                            if (keyPath == null) {
                                continue;
                            }
                            String certificatePath = host.getCertificatePath();
                            if (certificatePath == null) {
                                throw new ConfigIllegalException("The certificate of the host (" + host + ") is null but not its key");
                            }
                            pemKeyCertOptions
                                    .addKeyPath(keyPath)
                                    .addCertPath(certificatePath);
                        }
                        /**
                         * Note on protocol: The protocol is TLS, there is no old SSL allowed
                         * The default are = { "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3" }
                         * SSLv3 is NOT enabled due to POODLE vulnerability http:/en.wikipedia.org/wiki/POODLE
                         * "SSLv2Hello" is NOT enabled since it's disabled by default since JDK7
                         */
                        /**
                         * Note on StartTls: {@link SmtpSslOptionsWithStartTLS} was created to be able to use the STARTTLS option
                         * of Netty in order to upgrade the server connection
                         * and then send a reply (but unfortunately, it does not work
                         * because Vertx wraps it and the wrapper set it back to non-enabled (false)
                         * SmtpSslOptionsWithStartTLS sslEngineOptions = SmtpSslOptionsWithStartTLS.create();
                         * then
                         * NetServerOptions.setSslEngineOptions(sslEngineOptions)
                         */
                        NetServerOptions serverOption = new NetServerOptions()
                                .setPort(smtpService.getListeningPort())
                                .setKeyCertOptions(pemKeyCertOptions)
                                .setSsl(smtpService.getIsTlsEnabled())
                                // SNI returns the certificate for the indicated server name in an SSL connection
                                .setSni(smtpService.getIsSniEnabled())
                                .setIdleTimeout(smtpServer.getIdleTimeoutSecond())
                                .setSslHandshakeTimeout(smtpServer.getHandShakeTimeoutSecond());

                        Future<NetServer> futureNetServer = vertx.createNetServer(serverOption)
                                .exceptionHandler(SmtpExceptionHandler.create())
                                .connectHandler(smtpService::handle)
                                .listen();
                        netServers.add(futureNetServer);

                    }

                    /**
                     * Promise handling
                     */
                    return Future.join(netServers)
                            .onFailure(err -> {
                                LOGGER.log(Level.SEVERE,"Smtp server could not be started. Error: "+err.getMessage(), err);
                                this.handleVerticleFailure(verticlePromise, err);
                            })
                            .compose(result -> {

                                List<NetServer> netServersList;
                                try {
                                    netServersList = Casts.castToNewList(result.list(), NetServer.class);
                                } catch (CastException e) {
                                    return Future.failedFuture(new RuntimeException("Should not happen", e));
                                }
                                for (NetServer netServer : netServersList) {
                                    LOGGER.info("Smtp server is now listening on port " + netServer.actualPort());
                                }

                                /**
                                 * HTTP Server for API and other HTTP request
                                 */
                                String userAgentName = "smtp-server";
                                VertxNetServer server = VertxNetServer.create("smtp", "smtp", vertx, configAccessor)
                                        .setFromConfigAccessorWithPort(25026)
                                        .enableSmtpClient(userAgentName)
                                        .build();
                                /**
                                 * Create the HTTP server
                                 */
                                VertxHttpServer httpServer = VertxHttpServer.builderFromServer(server)
                                        .addBodyHandler()
                                        .addWebLog()
                                        .setBehindProxy()
                                        .addMetrics()
                                        .build();
                                return httpServer
                                        .mountListen("Smtp");
                            });
                }))
                .onFailure(e -> this.handleVerticleFailure(verticlePromise, e));


    }

    private void handleVerticleFailure(Promise<Void> verticlePromise, Throwable e) {
        verticlePromise.fail(e);
        this.vertx.close();
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }


    public SmtpServer getSmtpServer() {
        return this.smtpServer;
    }


}
