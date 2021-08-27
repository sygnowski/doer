ARG VERSION=0.1.1
ARG VCS_REF=unknown
FROM openjdk:11 as builder

ARG VERSION
ARG VCS_REF

RUN mkdir /opt/doer
WORKDIR /opt/doer

COPY src/ src/
COPY gradle/ gradle/
COPY gradlew gradlew
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle

RUN ./gradlew build --console=plain

FROM openjdk:11-jre

ARG VERSION
ARG VCS_REF

ENV DOER_VERSION $VERSION
ENV DOER_VCS_REF $VCS_REF

ENV DOER_HOME /opt/doer
RUN mkdir $DOER_HOME

WORKDIR $DOER_HOME
COPY --from=builder /opt/doer/build/libs/doer-all.jar ./doer.jar
COPY doer.sh doer.sh

RUN chmod +x ./doer.sh \
  && ln -s /opt/doer/doer.sh /usr/bin/doer

EXPOSE 8080