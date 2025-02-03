#!/usr/bin/env bash

function doer() {
  java -jar "./build/libs/doer-$(cat ./version)-all.jar" "$@"
}
