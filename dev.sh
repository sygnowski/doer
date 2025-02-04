#!/usr/bin/env bash

function doer() {
  if [[ "x${DEBUG}" == "x1" ]]; then
    set -x
    local LOCAL_DOER_JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
  fi

  java ${LOCAL_DOER_JVM_OPTS} -jar "./build/libs/doer-$(cat ./version)-all.jar" "$@"
}
