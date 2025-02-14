#!/bin/bash

# Script for in-container use.

set -e

if [[ "x${DEBUG}" == "xtrue" ]]; then
  set -x
  export DOER_JVM_OPTS="${DOER_JVM_OPTS} "
fi

args=("$@")


export DOER_HOME=${DOER_HOME:-"/opt/doer"}

DOER_LIBS=${DOER_HOME}/lib

DOER_CP=""

for lib in "${DOER_LIBS}"/*.jar
do
  if [[ -z "${DOER_CP}" ]]; then
    DOER_CP=$lib
  else
    DOER_CP=${DOER_CP}:$lib
  fi
done

if [[ -n "${DOER_CP_ADDON}" ]]; then
  DOER_CP=${DOER_CP}:${DOER_CP_ADDON}
fi

DOER_LOG_CFG_FILE="-Dlogback.configurationFile=${LOGBACK_CONFIG_FILE_LOCATION}"
DOER_JVM_OPTS="${DOER_LOG_CFG_FILE} ${DOER_JVM_OPTS:-"-Xmx100M"}"

java ${DOER_JVM_OPTS} -cp "${DOER_CP}" io.github.s7i.doer.Doer "${args[@]}"
