# Docker build stage: copy builder output and configure entry point
FROM adoptopenjdk:11-jre-hotspot
ENV APP_DIR /application
ENV APP_NAME smtp-server
# uber/fat jar
ENV APP_FILE  bytle-${APP_NAME}-all.jar


RUN echo "==> Update"  && \
    apt-get update && \
    echo "==> Install Dig"  && \
    # added because it's used in private network dns query
    apt-get install -y bind9-dnsutils && \
    apt-get install -y telnet

COPY ./build/libs/$APP_FILE $APP_DIR/$APP_FILE
COPY ./deploy/inbox.eraldy.com/.smtp-server-inbox.yml $APP_DIR/.$APP_NAME.yml

WORKDIR $APP_DIR
EXPOSE 25
ENTRYPOINT ["sh", "-c"]
# 192 = 256/4*3
CMD ["exec java -Xmx192m -jar $APP_FILE"]
