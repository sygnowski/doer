#!/bin/bash

PROTO_SET=../../proto-doer/build/descriptors/main.desc
set -e

doer misc pipeline record --help

print_color() {
    local TEXT=$1
    local CLR_NAME=$2
    local RED="\e[31m"
    local GREEN="\e[32m"
    local ENDCOLOR="\e[0m"
    local COLOR=${!CLR_NAME}
    echo -e "${COLOR}${TEXT}${ENDCOLOR}"
}

green() {
    print_color $1 "GREEN"
}

make_record() {
    doer misc pipeline record \
      --meta test.data=true \
      --key my-key \
      --data "some test data"
}

make_record_pl() {
    doer misc pipeline record \
      --meta test.data=true \
      --key my-key \
      --data "some test data" \
      --pipeline-load
}

green "[HEX:record]"
make_record | xxd

# https://github.com/protocolbuffers/protoscope
green "[protoscope]"
make_record | protoscope

green "[protoscope:print-field-names]"

make_record | protoscope \
   -descriptor-set ${PROTO_SET} \
   -message-type io.github.s7i.doer.proto.Record \
   -print-field-names

green "[HEX:pipeline-load]"
make_record_pl | xxd

green "[protoscope:pipeline-load]"
make_record_pl | protoscope

green "[protoscope:pipeline-load:print-field-names]"
make_record_pl | protoscope \
   -descriptor-set ${PROTO_SET} \
   -message-type io.github.s7i.doer.pipeline.proto.PipelineLoad \
   -print-field-names