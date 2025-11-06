#!/usr/bin/env bash
set -x

BUFF_REPO=${BUFF_REPO:-"."}

PROTOS=""
for lib in "${BUFF_REPO}/meshtastic"/*.proto
do
  PROTOS="${PROTOS} ${lib}"
done

protoc --include_imports --descriptor_set_out=mesh.desc --java_out ./src/manin/java "${PROTOS}"
