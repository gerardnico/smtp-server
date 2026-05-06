package com.combostrap.smtp;

import com.combostrap.common.JavaEnvs;
import com.combostrap.dns.DnsClient;
import com.combostrap.dns.DnsIllegalArgumentException;
import com.combostrap.dns.XBillDnsClient;
import com.combostrap.email.BMailInternetAddress;
import com.combostrap.smtp.exceptions.ConfigIllegalException;
import com.combostrap.smtp.mailbox.SmtpMailbox;
import com.combostrap.smtp.mailbox.SmtpMailboxForward;
import com.combostrap.smtp.mailbox.SmtpMailboxS3;
import com.combostrap.smtp.mailbox.SmtpMailboxStdout;
import com.combostrap.smtp.milter.DmarcMilter;
import com.combostrap.smtp.milter.SmtpMilter;
import com.combostrap.type.DnsName;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import jakarta.mail.internet.AddressException;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static com.combostrap.smtp.SmtpSyntax.LOG_TAB;

public class SmtpServer {

    private static final Logger LOGGER = Logger.getLogger(SmtpServer.class.getName());


    private final Map<SmtpSession, SmtpSession> activeSessions = new HashMap<>();


    /**
     * {@link SocketAddress} has a hash code making it unique
     * The {@link SmtpSocket#getRemoteAddress()}
     */
    private final ConcurrentMap<SocketAddress, Integer> totalConnectionsByIp = new ConcurrentHashMap<>();

    private final Map<String, SmtpHost> hostedDomains = new HashMap<>();
    private final SmtpHost defaultHostedHost;


    private final Map<String, SmtpDomain> smtpDomains = new HashMap<>();
    private final SmtpReception smtpReception;
    private final SmtpDelivery smtpDelivery;

    private final boolean enableDnsBlockList;
    private final DnsClient dnsClient;
    private final SmtpConfigBean config;

    public List<SmtpService> getSmtpServices() {
        return services;
    }

    private final List<SmtpService> services = new ArrayList<>();


    public SmtpServer(AbstractVerticle smtpVerticle, SmtpConfigBean config) throws ConfigIllegalException {


        this.config = config;

        /**
         * General conf
         */
        LOGGER.info(SmtpSyntax.LOG_TAB + "Software Name set to " + config.softwareName);

        /**
         * Session Conf
         */
        LOGGER.info(LOG_TAB + "Session idle timeout set to " + config.limits.idleTimeoutSecond);
        smtpVerticle.getVertx().setPeriodic(config.limits.idleTimeoutSecond + 20, this::removeIdleSessions);
        LOGGER.info(LOG_TAB + "Session replay set to " + config.sessionReplayEnabled);


        /**
         * Max, Limit Settings, Quotas
         */
        LOGGER.info(LOG_TAB + "Smtp Max, Limit Settings:");
        LOGGER.info(LOG_TAB + "Max total connections set to " + config.limits.maxTotalConnections);

        LOGGER.info(LOG_TAB + "Max connection count by IP set to " + config.limits.maxConnectionByIp);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Max message size in bytes set to " + config.limits.maxMessageSizeInBytes);

        LOGGER.info(SmtpSyntax.LOG_TAB + "Max exceptions by session set to " + config.limits.maximumExceptionCountBySession);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Max recipients by email set to " + config.limits.maxRecipientsByEmail);

        /**
         * Host(s) settings
         */
        LOGGER.info("Host(s) Settings:");

        String host = config.listeningHost;


        for (Map.Entry<String, SmtpHostConfig> virtualHostnameString : config.hosts.entrySet()) {

            String domainName = virtualHostnameString.getKey();
            if (domainName == null) {
                throw new ConfigIllegalException("The domain for the hostname (" + virtualHostnameString + ") is mandatory");
            }
            SmtpDomain smtpDomain;
            try {
                smtpDomain = this.getOrCreateDomainByName(domainName);
            } catch (DnsIllegalArgumentException e) {
                throw new ConfigIllegalException("The domain name (" + domainName + ") for the hostname (" + virtualHostnameString + ") is not valid");
            }

            SmtpHost smtpHost = new SmtpHost(domainName, smtpDomain, virtualHostnameString.getValue());
            this.hostedDomains.put(domainName, smtpHost);
            LOGGER.info(LOG_TAB + "Virtual Host added: " + smtpHost.getHostedHostname() + " (Domain: " + smtpHost.getDomain() + ", Postmaster: " + smtpHost.getPostmaster().getPostmasterAddressInConfiguration() + ")");
        }
        this.defaultHostedHost = this.hostedDomains.get(host);
        if (this.defaultHostedHost == null) {
            throw new ConfigIllegalException("The main host (" + host + ") was not found in the hosts. It is mandatory");
        }

        /**
         * Smtp Services
         */


        for (Map.Entry<Integer, SmtpServiceConfig> serviceKey : config.services.entrySet()) {
            Integer servicePort = serviceKey.getKey();

            SmtpService smtpService = new SmtpService(this, servicePort, serviceKey.getValue());
            this.services.add(smtpService);
            LOGGER.info(LOG_TAB + "Service added: " + smtpService);
        }

        /**
         * Dns Client for block list
         * (Should become {@link com.combostrap.vertx.TowerDnsClient})
         */
        this.dnsClient = XBillDnsClient.builder().build();

        /**
         * Create the mailboxes
         */
        HashMap<String, Class<? extends SmtpMailbox>> mailboxClasses = new HashMap<>();
        mailboxClasses.put("stdout", SmtpMailboxStdout.class);
        mailboxClasses.put("forward", SmtpMailboxForward.class);
        mailboxClasses.put("s3", SmtpMailboxS3.class);
        mailboxClasses.put("memory", SmtpMailboxMemory.class);

        // milter
        HashMap<String, SmtpMilter> milters = new HashMap<>();
        milters.put("dmarc", new DmarcMilter());

        /**
         * Define the users
         * Smtp Users
         */
        for (Map.Entry<String, Map<String, SmtpUserConf>> userEntry : config.users.entrySet()) {
            String domain = userEntry.getKey();
            SmtpDomain smtpDomain = this.smtpDomains.get(domain.toLowerCase());
            if (smtpDomain == null) {
                throw new ConfigIllegalException("The users domain (" + domain + ") was not found in the hosts");
            }
            for (Map.Entry<String, SmtpUserConf> user : userEntry.getValue().entrySet()) {

                String username = user.getKey();
                SmtpUserConf userConf = user.getValue();
                SmtpMailBoxConf mailBox = userConf.mailbox;
                Class<? extends SmtpMailbox> smtpMailboxClass;
                Map<String,Object> mailBoxProps = new HashMap<>();
                if (mailBox == null) {
                    smtpMailboxClass = SmtpMailboxStdout.class;
                } else {
                    String type = mailBox.type;
                    if (type == null) {
                        throw new ConfigIllegalException("The type of mailbox of the user (" + username + ") is not set");
                    }
                    smtpMailboxClass = mailboxClasses.get(type);
                    if (smtpMailboxClass == null) {
                        throw new ConfigIllegalException("The type (" + type + ") of mailbox of the user (" + username + ") is unknown");
                    }
                    mailBoxProps = mailBox.props;
                }

                List<SmtpMilter> mailBoxMiltersObject = new ArrayList<>();
                if (mailBox != null) {
                    List<String> mailBoxMiltersConf = mailBox.milters;
                    for (String mailboxMilterConfKey : mailBoxMiltersConf) {
                        SmtpMilter mailBoxMilterObject = milters.get(mailboxMilterConfKey);
                        if (mailBoxMilterObject == null) {
                            throw new ConfigIllegalException("The milter (" + mailboxMilterConfKey + ") of the mailbox of the user (" + username + ") is unknown");
                        }
                        mailBoxMiltersObject.add(mailBoxMilterObject);
                    }
                }

                String password = userConf.password;
                SmtpUser smtpUser = SmtpUser.createFrom(smtpDomain, username, password);
                smtpDomain.addUser(smtpUser);

                SmtpMailbox smtpMailbox;
                try {
                    smtpMailbox = smtpMailboxClass.getDeclaredConstructor(SmtpUser.class, Vertx.class, List.class, Map.class).newInstance(smtpUser, smtpVerticle.getVertx(), mailBoxMiltersObject, mailBoxProps);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                         InvocationTargetException e) {
                    throw new ConfigIllegalException("Error while creating the mailbox (" + smtpMailboxClass.getName() + ") of the user (" + user + ")", e);
                }
                smtpUser.setMailBox(smtpMailbox);

                LOGGER.info(LOG_TAB + "User added: " + smtpUser);
            }

        }

        /**
         * Reception/Delivery
         */
        this.enableDnsBlockList = config.reception.enableDnsBlockList;
        LOGGER.info(LOG_TAB + "Dns Block List check set to " + this.enableDnsBlockList);

        this.smtpDelivery = new SmtpDelivery(smtpVerticle.getVertx(), config.delivery);
        this.smtpReception = new SmtpReception(smtpDelivery);


    }


    private SmtpDomain getOrCreateDomainByName(String domainName) throws DnsIllegalArgumentException {
        String domainNameNormalization = domainName.toLowerCase();
        SmtpDomain smtpDomain = this.smtpDomains.get(domainNameNormalization);
        if (smtpDomain == null) {
            smtpDomain = SmtpDomain.createFromName(domainName);
            this.smtpDomains.put(domainNameNormalization, smtpDomain);
        }
        return smtpDomain;
    }

    public static SmtpServer create(AbstractVerticle smtpVerticle, SmtpConfigBean configAccessor) throws ConfigIllegalException {
        return new SmtpServer(smtpVerticle, configAccessor);
    }

    /**
     * See timeout by phase:
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.5.3.2">Timeout</a>
     */
    protected void removeIdleSessions(Long aLong) {

        LocalDateTime now = LocalDateTime.now();
        LOGGER.fine("There is " + activeSessions.size() + " session");
        if (!JavaEnvs.isIsIdeDebugging()) {
            for (SmtpSession smtpSession : activeSessions.values()) {
                LocalDateTime deadlineTime = smtpSession.getLastInteractiveTime().plusSeconds(this.config.limits.idleTimeoutSecond);
                if (deadlineTime.isBefore(now)) {
                    smtpSession.closeSessionWithReply(SmtpReply.create(SmtpReplyCode.SERVICE_NOT_AVAILABLE_421, "Connection was idle for more than " + this.config.limits.idleTimeoutSecond + " seconds. Bye."));
                }
            }
        }
    }

    void removeSession(SmtpSession smtpSession) {
        this.activeSessions.remove(smtpSession);
        SocketAddress source = smtpSession.getSmtpSocket().getRemoteAddress();
        Integer connectionNumber = this.totalConnectionsByIp.getOrDefault(source, 0);
        if (connectionNumber.equals(1)) {
            this.totalConnectionsByIp.remove(source);
        } else {
            this.totalConnectionsByIp.put(source, connectionNumber - 1);
        }
    }

    public boolean tooMuchConnection() {
        return activeSessions.size() > this.config.limits.maxTotalConnections;
    }

    /**
     * @param smtpSession - the connection to add
     * @throws SmtpException - an exception if there is too much connection
     */
    public void connectionRateLimiter(SmtpSession smtpSession) throws SmtpException {

        if (tooMuchConnection()) {
            throw SmtpException.create(SmtpReplyCode.SERVICE_NOT_AVAILABLE_421, "Too many connections on the server, try again later")
                    .setShouldQuit(true);
        }

        Integer maxBySource = this.totalConnectionsByIp.getOrDefault(smtpSession.getSmtpSocket().getRemoteAddress(), 0);
        if (maxBySource > this.config.limits.maxConnectionByIp) {
            throw SmtpException.create(SmtpReplyCode.SERVICE_NOT_AVAILABLE_421, "Too many connections for your ip, try again later")
                    .setShouldQuit(true);
        }
        this.totalConnectionsByIp.put(smtpSession.getSmtpSocket().getRemoteAddress(), maxBySource + 1);
        activeSessions.put(smtpSession, smtpSession);

    }

    public Map<String, SmtpHost> getHostedHosts() {
        return this.hostedDomains;
    }


    public SmtpHost getDefaultHostedHost() {
        return this.defaultHostedHost;
    }

    public int getIdleTimeoutSecond() {
        return this.config.limits.idleTimeoutSecond;
    }

    public String getSoftwareName() {
        return this.config.softwareName;
    }

    public int maximumExceptionBySession() {
        return this.config.limits.maximumExceptionCountBySession;
    }

    public int getMaxMessageSizeInBytes() {
        return this.config.limits.maxMessageSizeInBytes;
    }

    public int getMaxRecipientsByEmail() {
        return this.config.limits.maxRecipientsByEmail;
    }


    public boolean getLocalHostAuthenticationRequired() {
        return this.config.localhostAuthenticationRequired;
    }


    public SmtpReception getSmtpReception() {
        return this.smtpReception;
    }

    public SmtpDelivery getSmtpDeliveryQueue() {
        return this.smtpDelivery;
    }

    public List<SmtpMessage> pumpMessagesForUser(String email) throws SmtpException {
        BMailInternetAddress internetAddress;
        try {
            internetAddress = BMailInternetAddress.of(email);
        } catch (AddressException e) {
            throw SmtpException.createForInternalException("bad email address" + email, e);
        }
        DnsName userDomain = internetAddress.getEmailAddress().getDomainName();
        SmtpDomain domain = this.hostedDomains
                .values()
                .stream()
                .map(SmtpHost::getDomain)
                .filter(d -> d.getDnsDomain().equals(userDomain))
                .findFirst()
                .orElse(null);
        if (domain == null) {
            throw SmtpException.createForInternalException("The domain (" + userDomain + ") of the user (" + email + ") does not exist");
        }

        String localPart = internetAddress.getEmailAddress().getLocalPart();
        SmtpUser user = domain.getUser(localPart).orElse(null);
        if (user == null) {
            throw SmtpException.create(SmtpReply.create(SmtpReplyCode.NO_SUCH_USER_550, "User (" + email + ") not found"));
        }

        SmtpMailbox mailbox = user.getMailbox();
        if (!(mailbox instanceof SmtpMailboxMemory)) {
            throw SmtpException.createForInternalException("The user (" + email + ") has not a memory mailbox. The message cannot be retrieved");
        }
        SmtpMailboxMemory mailboxMemory = (SmtpMailboxMemory) mailbox;
        return mailboxMemory.pumpMessages();

    }

    public boolean isSessionReplayEnabled() {
        return this.config.sessionReplayEnabled;
    }


    public boolean isDnsBlockListDisabled() {
        return !this.enableDnsBlockList;
    }

    public DnsClient getDnsClient() {
        return this.dnsClient;
    }

    public long getHandShakeTimeoutSecond() {
        return this.config.limits.handShakeTimeoutSecond;
    }
}
