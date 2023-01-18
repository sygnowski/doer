ARG VERSION=0.1.1
ARG VCS_REF=unknown
FROM s71/ubuntu-dev-jdk11 as builder

ARG VERSION
ARG VCS_REF

RUN mkdir /opt/doer
WORKDIR /opt/doer

ADD . /opt/doer
RUN set -ex; \
    chmod +x ./gradlew; \
    ls -1 .; \
    ./gradlew test distTar -x generateGitProperties;

FROM s71/ubuntu-dev-jdk11

ARG VERSION
ARG VCS_REF
ARG BUILD_DATE

ENV DOER_VERSION $VERSION
ENV DOER_VCS_REF $VCS_REF

ENV DOER_HOME=/opt/doer \
    DOER_CONTAINER=1

LABEL build.git.ref=${VCS_REF} build.date=${BUILD_DATE} \
  org.opencontainers.image.source="https://github.com/sygnowski/doer"


WORKDIR $DOER_HOME
COPY --from=builder /opt/doer/build/distributions/doer.tar /opt

RUN set -ex; \
    tar -xf /opt/doer.tar -C /opt; \
    rm /opt/doer.tar; \
    ls -la /opt; \
    ln -s /opt/doer/bin/doer /usr/bin/doer;

EXPOSE 8080
