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


FROM node:12.22-bullseye-slim
RUN apt-get update && \
    apt-get install -y make gcc g++ python libx11-dev libxkbfile-dev libsecret-1-0 wget openjdk-11-jre git mc
WORKDIR /opt/theia
RUN wget -nv https://raw.githubusercontent.com/theia-ide/theia-apps/master/theia-docker/latest.package.json -O ./package.json
RUN yarn --pure-lockfile && \
    NODE_OPTIONS="--max_old_space_size=4096" yarn theia build && \
    yarn theia download:plugins && \
    yarn --production && \
    yarn autoclean --init && \
    echo *.ts >> .yarnclean && \
    echo *.ts.map >> .yarnclean && \
    echo *.spec.* >> .yarnclean && \
    yarn autoclean --force && \
    yarn cache clean

ARG VERSION
ARG VCS_REF

ENV DOER_VERSION $VERSION
ENV DOER_VCS_REF $VCS_REF

ENV THEIA_DEFAULT_PLUGINS=local-dir:/opt/theia/plugins

ENV DOER_HOME /opt/doer
RUN mkdir $DOER_HOME

WORKDIR $DOER_HOME
COPY --from=builder /opt/doer/build/libs/doer-all.jar ./doer.jar
COPY doer.sh doer.sh

RUN chmod +x ./doer.sh \
  && ln -s /opt/doer/doer.sh /usr/bin/doer

WORKDIR /opt/theia
CMD ["node", "/opt/theia/src-gen/backend/main.js", "/opt/doer", "--hostname=0.0.0.0"]

EXPOSE 8080
EXPOSE 3000