# Smtp Server (MDA, Mx, MTA)


## About

An SMTP server that can receive email message for local and remote delivery. ie that can be used as:
  * MDA (MX)
  * MTA

There are 2 Transaction Type (ie delivery):
* remote delivery (MTA):
  * transmitting the message to a remote mailbox
* local delivery (MDA):
  * storing the email in a local mailbox (disk, http endpoint)
  * forwarding - (alias/SRS)


## TODO, Not implemented

The following is not implemented:
  * Reception/Acceptance: We don't check SPF/Dkim
  * Delivery:
    * Remote delivery: Not done (no Dkim)
    * Local delivery: We have only one mailbox that sends to stdout
    * Forwarding: Not implemented at all (SRS, alias)
  * Server certificate: Not generated/get (only one location)

## Status

### Service

The SMTP server is capable to start a Net Server on multiple port with different configurations.

The standard configuration in [.smtp.yml](.smtp-server.yml) is:
* a PLAIN port with STARTTLS (port 25 or 2525)
* a TLS port (port 465)

### Hosting

The server can host SMTP server (virtual SMTP host)

In case of PLAIN connection, we can get the requested host via SNI, we
show then not the domain but the hostname.

See the [configuration file](.smtp-server.yml) for an example of hosts definition.

### Extensions

The following extensions are implemented:
  * CHUNKING (to receive data in chunk of byes with the BDAT command)
  * PIPELINING (to receive command in batch)
  * STARTTLS (to upgrade a PLAIN connection to TLS)
  * AUTH PLAIN (to authenticate with the PLAIN mechanism)

The following extensions are not implemented:
  * ENHANCED_STATUS (the 3 digits class enhanced status code)
  * SMTP_UTF8 (UTF8 everywhere. It may work, not tested)

### Commands

The following commands are implemented:
  * EHLO and HELO (to get the SMTP extensions)
  * STARTTLS (to upgrade the connection to TLS)
  * MAIL FROM (to define the admin sender)
  * RCP TO (to define the asked recipients)
  * DATA (to send data in text format)
  * BDAT (to send data in bytes for chunking)
  * RSET (to reset)
  * NOOP (to test the connection)

### Reception

Transactional Reception:
  * State Machine:
    * A state machine is implemented
  * Filtering:
    * Third party:
      * We filter early for each SMTP command
      * We filter on IP and Domain block list
      * Spf, Dkim email authentication is not finished
    * First party:
      * The sender should be authenticated and on an SSL connection
      * Localhost is authenticated by default to allow sending via command line tool
  * History:
    * A transactional history exists
  * Streaming:
    * Not implemented: the whole message is in memory
  * Quota:
    * See the [configuration file](.smtp-server.yml)
  * Tracing:
    * We add a `Received` headers (not yet `Return Path`)
  * Postmaster:
    * Postmaster reception is enabled

### Processing

The SMTP parser:
  * parses line by line in line mode
  * fetch a fixed size in fixed mode
  * stops if the data received is bigger than the allowed max size for a message.
  * returns multiple command line to allow pipelining

### Delivery

The delivery into `mailbox` is done in the [SmtpDelivery class](./src/main/java/net/bytle/smtp/SmtpDelivery.java)


### Configuration

There is only one configuration file, the [.smtp.yml](.smtp-server.yml) file.

## Dev note

### Third SMTP Java Server Inspiration

* com.github.kirviq:dumbster
* subetha
* James


### Vertx NetServer

This SMTP server is based on the NetServer of Vertx that is based on Netty.

* [NetDoc](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/asciidoc/net.adoc)
* [NetExample](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/java/examples/NetExamples.java)

To not use a lot of memory:
* [Streams](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/asciidoc/streams.adoc)
* [StreamsExample](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/java/examples/StreamsExamples.java)
