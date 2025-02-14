#!/usr/bin/env bash

# Local development script
# Usage: $ . ./dev.sh && doer misc info

function doer() {
  local LOCAL_DOER_JVM_OPTS="-Dlogback.configurationFile=./src/main/resources/container-logback.xml"
  if [[ "x${DEBUG}" == "x1" ]]; then
    LOCAL_DOER_JVM_OPTS="${LOCAL_DOER_JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
  fi

  java ${LOCAL_DOER_JVM_OPTS} -jar "./build/libs/doer-$(cat ./version)-all.jar" "$@"
}
