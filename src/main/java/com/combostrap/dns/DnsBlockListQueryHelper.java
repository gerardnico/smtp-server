package com.combostrap.dns;

import com.combostrap.exception.InternalException;

import com.combostrap.type.CastException;
import com.combostrap.type.DnsName;


import java.util.*;
import java.util.logging.Logger;

/**
 * Create a Host that should be queried
 */
public class DnsBlockListQueryHelper {

    private static final Logger LOGGER = Logger.getLogger(DnsBlockListQueryHelper.class.getName());
    private final DnsBlockList dnsBlockList;
    private final String queryTerm;

    /**
     * @param dnsBlockList - the block list
     * @param queryTerm    - the domain or the ip
     */
    public DnsBlockListQueryHelper(DnsBlockList dnsBlockList, String queryTerm) {
        this.dnsBlockList = dnsBlockList;
        this.queryTerm = queryTerm;
    }

    public static BuilderConf forDomain(DnsName domain) {
        return new BuilderConf(DnsBlockListType.DOMAIN, domain.toStringWithoutRoot());
    }

    /**
     * @param dnsIp - the IP response to check (note that by default, no response means not on the list)
     * @return the response code
     */
    public DnsBlockListResponseCode createResponseCode(DnsIp dnsIp) {

        Set<DnsBlockListResponseCode> blockingKnownResponses = dnsBlockList.getBlockingKnownResponses();
        if (blockingKnownResponses.isEmpty()) {
            /**
             * Response may be error response, we don't know, normally
             * you are blocked if we get a response
             */
            LOGGER.info("No known responses for the blocking list. We block because we got the DNS response (" + dnsIp + ") for the host (" + this.getDnsNameToQuery() + ")");
            return DnsBlockListResponseCode.R_INTERNAL_HOST_BLOCKED;
        }
        /**
         * Not all responses are blocking response
         * If we know the blocking responses, we check
         */
        String responseCode = dnsIp.getInetAddress().getHostAddress().replace("." + dnsBlockList.getZone(), "");
        DnsBlockListResponseCode response = blockingKnownResponses.stream()
                .filter(r -> r.getCode().equals(responseCode))
                .findFirst()
                .orElse(null);
        if (response == null) {
            // Error on our end
            LOGGER.warning("Internal Error: No response code for the response (" + responseCode + ")");
            return DnsBlockListResponseCode.R_INTERNAL_RESPONSE_NOT_IN_LIST;
        }
        if (!response.getBlocked()) {
            LOGGER.warning("Dns Blocking Query Error: Response code: " + response.getCode() + ", description: " + response.getDescription());
        }
        return response;

    }

    /**
     * ***************************************************************************
     * IP Query:
     * as explained here:
     * <a href="https://en.wikipedia.org/wiki/Domain_Name_System_blocklist#DNSBL_queries">...</a>
     * and here:
     * <a href="https://www.spamhaus.org/faq/section/DNSBL%20Usage#366">How do I check my DNS server results?</a>
     * For the reason:
     * You can look up the name as a text record ("TXT" record).
     * Most DNSBLs publish information about why a client is listed as TXT records.
     * Doc: https://www.spamhaus.org/faq/section/DNSBL%20Usage#200
     * ***************************************************************************
     * Domain Query:
     * As explained here:
     * <a href="https://en.wikipedia.org/wiki/Domain_Name_System_blocklist#URI_DNSBL">...</a>
     * <p>
     * Can also be used for web app:
     * <a href="https://www.spamhaus.org/faq/section/DNSBL%20Usage#252">Can a Spamhaus DNSBL
     * be used on a web server or other applications?</a>
     * <p>
     * We don't perform the query with InetAddress.getxxx
     * because
     * * it uses the local DNS of the machine. Unfortunately, it does not work for public DNS (ie VPS in the cloud). A resolver (ie DNS server) should be defined.
     * * we may use an async or sync dns client
     */
    public DnsName getDnsNameToQuery() {
        try {
            return DnsName.create(this.queryTerm + "." + dnsBlockList.getZone());
        } catch (CastException e) {
            throw new RuntimeException("We create the name, it should be good", e);
        }
    }

    public static BuilderConf forIp(String ipv4Address) throws IllegalStructure {
        if (ipv4Address == null) {
            throw new IllegalStructure("Ip Remote Address should not be null");
        }
        List<String> ipOctets = Arrays.asList(ipv4Address.split("\\."));
        if (ipOctets.size() != 4) {
            throw new IllegalStructure("This is not an IPv4 address");
        }
        Collections.reverse(ipOctets);
        String ipToCheckReversed = String.join(".", ipOctets);
        return new BuilderConf(DnsBlockListType.IP, ipToCheckReversed);
    }

    public DnsBlockList getBlockList() {
        return this.dnsBlockList;
    }


    public static class BuilderConf {
        private final DnsBlockListType dnsBlockListType;
        private final String queryTerm;
        private final List<DnsBlockList> blockLists = new ArrayList<>();

        /**
         * @param dnsBlockListType - the type (domain or ip)
         * @param queryTerm        - the term (the domain or the ip)
         */
        public BuilderConf(DnsBlockListType dnsBlockListType, String queryTerm) {
            this.dnsBlockListType = dnsBlockListType;
            this.queryTerm = queryTerm;
        }

        @SuppressWarnings("unused")
        public BuilderConf addAllBlockLists(List<DnsBlockList> blockLists) {
            for (DnsBlockList blockList : blockLists) {
                this.blockLists.addAll(blockLists);
            }
            return this;
        }

        @SuppressWarnings("unused")
        public BuilderConf addBlockList(DnsBlockList blockList) {
            addAllBlockLists(Collections.singletonList(blockList));
            return this;
        }

        public List<DnsBlockListQueryHelper> build() {
            if (this.blockLists.isEmpty()) {
                switch (this.dnsBlockListType) {
                    case IP:
                        this.addBlockList(DnsBlockList.ZEN_SPAMHAUS_ORG);
                        break;
                    case DOMAIN:
                        this.addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG);
                        break;
                    default:
                        throw new InternalException("The query type is unknown");
                }
            }
            List<DnsBlockListQueryHelper> dnsBlockListQueries = new ArrayList<>();
            for (DnsBlockList dnsBlockList : this.blockLists) {
                if (dnsBlockList.getType() != this.dnsBlockListType) {
                    throw new RuntimeException("The block list is not from the good type");
                }
                dnsBlockListQueries.add(new DnsBlockListQueryHelper(dnsBlockList, queryTerm));
            }
            return dnsBlockListQueries;
        }


    }
}
