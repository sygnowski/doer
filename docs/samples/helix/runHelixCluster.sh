#!/bin/bash

set -xe

args=("$@")

CLUSTER_NAME="doer-cluster-test"
RESOURCE="doer"


SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

export DOER_HOME=${SCRIPT_DIR}


helix () {
  export DOER_CTX_NAME=d$!
  doer helix "$@"
}


main() {

  rm -rf ${SCRIPT_DIR}/logs

  echo "Doer : Helix testing..."

  echo "env: $( env | grep -e DOER )"

  helix -t create -c ${CLUSTER_NAME} -r ${RESOURCE}

  #helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctl&

  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-1&
  #helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-2&
  #helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-3&

  TOKILL=$(pgrep -P $$ | tr "\n" " ")

  #trap 'kill $TOKILL' SIGINT SIGTERM EXIT

  echo "subprocs:  $TOKILL"

  read

  kill ${TOKILL}



  sleep 1

  helix -t delete -c ${CLUSTER_NAME}

}


main "${args[@]}"