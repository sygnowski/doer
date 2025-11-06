#!/usr/bin/env bash

args=("$@")

export DOER_REPO=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Local development script
# Usage: $ . ./dev.sh && doer misc info

function doer() {
  local OPTS="-Xms128M -Xmx512M -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
  local LOGBACK="container-logback.xml"
  if [[ "x${PLAIN}" == "x1" ]]; then
    local LOGBACK="console.xml"
  fi
  if [[ "x${NOLOG}" == "x1" ]]; then
    local LOGBACK="off-logback.xml"
  fi
  local OPTS="-Dlogback.configurationFile=${DOER_REPO}/src/main/resources/${LOGBACK}"
  if [[ "x${DEBUG}" == "x1" ]]; then
    local OPTS="${OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
  fi
  if [[ "x${DEBUG}" == "x2" ]]; then
    local OPTS="${OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
  fi

  "${DOER_REPO}/gradlew" shadowJar --console=plain

  java ${OPTS} -jar "${DOER_REPO}/build/libs/doer-$(cat ${DOER_REPO}//version)-all.jar" "$@"
}

case $1 in
    play)
      PLAIN=1
      doer -v
      doer misc info
      doer misc chk
      ;;
esac
