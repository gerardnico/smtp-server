package com.combostrap.smtp;

import io.netty.handler.ssl.*;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.spi.tls.DefaultSslContextFactory;
import io.vertx.core.spi.tls.SslContextFactory;

import javax.net.ssl.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A modified SSLEngineOptions
 * that adds STARTTLS capability
 */
public class SmtpSslOptionsWithStartTLS extends SSLEngineOptions {

  private static final List<String> DEFAULT_JDK_CIPHER_SUITE;

  /**
   * Comes from DefaultJDKCipherSuite that is private
   */
  static {
    ArrayList<String> suite = new ArrayList<>();
    try {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
      SSLEngine engine = context.createSSLEngine();
      Collections.addAll(suite, engine.getEnabledCipherSuites());
    } catch (Throwable e) {
      suite = null;
    }
    DEFAULT_JDK_CIPHER_SUITE = suite != null ? Collections.unmodifiableList(suite) : null;
  }

  public SmtpSslOptionsWithStartTLS() {
  }

  public SmtpSslOptionsWithStartTLS(SmtpSslOptionsWithStartTLS that) {
      super(that);
  }


  public static SmtpSslOptionsWithStartTLS create() {
    return new SmtpSslOptionsWithStartTLS();
  }

  /**
   * The instantiation uses it to copy it
   * {@link io.vertx.core.net.TCPSSLOptions} ligne 156
   */
  @Override
  public SmtpSslOptionsWithStartTLS copy() {
    return new SmtpSslOptionsWithStartTLS(this);
  }

  @Override
  public SslContextFactory sslContextFactory() {
    return new DefaultSslContextFactoryWithStartTLS(SslProvider.JDK, false);
  }

  private static class DefaultSslContextFactoryWithStartTLS extends DefaultSslContextFactory {
    private boolean forClient;
    private KeyManagerFactory kmf;
    private TrustManagerFactory tmf;
    private ClientAuth clientAuth;
    private boolean useAlpn;
    private List<String> applicationProtocols;

    public DefaultSslContextFactoryWithStartTLS(SslProvider sslProvider, boolean sslSessionCacheEnabled) {
      super(sslProvider, sslSessionCacheEnabled);
    }

    @Override
    public SslContextFactory forClient(boolean forClient) {
      this.forClient = forClient;
      return super.forClient(forClient);
    }

    @Override
    public SslContextFactory keyMananagerFactory(KeyManagerFactory kmf) {
      this.kmf = kmf;
      return super.keyMananagerFactory(kmf);
    }

    @Override
    public SslContextFactory trustManagerFactory(TrustManagerFactory tmf) {
      this.tmf = tmf;
      return super.trustManagerFactory(tmf);
    }

    @Override
    public SslContextFactory clientAuth(ClientAuth clientAuth) {
      this.clientAuth = clientAuth;
      return super.clientAuth(clientAuth);
    }

    @Override
    public SslContextFactory useAlpn(boolean useAlpn) {
      this.useAlpn = useAlpn;
      return super.useAlpn(useAlpn);
    }

    @Override
    public SslContextFactory applicationProtocols(List<String> applicationProtocols) {
      this.applicationProtocols = applicationProtocols;
      return super.applicationProtocols(applicationProtocols);
    }

    @Override
    public SslContext create() throws SSLException {


      if (this.forClient) {
        return super.create();
      }

      /**
       * We build the SSL context with start TLS
       * Copy of the parent create method
       */
      SslContextBuilder builder = SslContextBuilder.forServer(kmf)
        .startTls(true) // that's the StartTLS modification
        .sslProvider(SslProvider.JDK);
      if (tmf != null) {
        builder.trustManager(tmf);
      }
      Collection<String> cipherSuites = DEFAULT_JDK_CIPHER_SUITE;
      if (cipherSuites != null && cipherSuites.size() > 0) {
        builder.ciphers(cipherSuites);
      }
      if (useAlpn && applicationProtocols != null && applicationProtocols.size() > 0) {
        builder.applicationProtocolConfig(new ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          applicationProtocols
        ));
      }
      if (clientAuth != null) {
        builder.clientAuth(clientAuth);
      }

      return builder.build();

    }


  }

}
