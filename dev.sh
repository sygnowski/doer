#!/usr/bin/env bash

# Local development script
# Usage: $ . ./dev.sh && doer misc info

function doer() {
  local OPTS="-Xms128M -Xmx512M -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
  local LOGBACK="container-logback.xml"
    if [[ "x${PLAIN}" == "x1" ]]; then
      local LOGBACK="console.xml"
    fi
  local OPTS="-Dlogback.configurationFile=./src/main/resources/${LOGBACK}"
  if [[ "x${DEBUG}" == "x1" ]]; then
    local OPTS="${OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
  fi

  java ${OPTS} -jar "./build/libs/doer-$(cat ./version)-all.jar" "$@"
}
