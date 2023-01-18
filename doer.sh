#!/bin/bash

java $DOER_JVM_ARGS -jar $DOER_HOME/${DOER_JAR_NAME:-doer.jar} "$@"
