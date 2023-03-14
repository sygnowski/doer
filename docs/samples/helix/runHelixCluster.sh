#!/bin/bash

set -e

args=("$@")

CLUSTER_NAME="doer-cluster-test"
RESOURCE="doer"


SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

export DOER_HOME=${SCRIPT_DIR}


helix () {
  export DOER_CTX_NAME=d$!
  doer helix "$@"
}


run_main() {

  rm -rf ${SCRIPT_DIR}/logs

  echo "Doer : Helix testing..."

  echo "env: $( env | grep -e DOER )"

  helix -t create -c ${CLUSTER_NAME} -r ${RESOURCE} --replicas 3

  helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctl&

  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-1&
  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-2&
  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-3&

  TOKILL=$(pgrep -P $$ | tr "\n" " ")

  #trap 'kill $TOKILL' SIGINT SIGTERM EXIT

  echo "subprocs:  $TOKILL"

  read

  kill ${TOKILL}



  sleep 1

  helix -t delete -c ${CLUSTER_NAME}

}


run_grade_model_cluster() {

  CLUSTER_NAME="grade-model-demo"
  NODES=(participant-{1..5})

  echo "Making cluster..."
  helix --model ./grade-cluster.yaml

  helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctl&

  nodeFct=50
  idx=0

  for prt in "${NODES[@]}"; do
    gold=((nodeFct - idx * 10))
    echo "Running $part with gold.start=$gold"
    helix -t participant -c ${CLUSTER_NAME} -n prt -f gold.start=$gold &
    ((idx++))
  done

}

usage() {
    (lolcat || cat) << EOF 2> /dev/null
Usage:
main     -  Master Slave Model.
grade    -  Grade State Model.
EOF
}


case $1 in
  main)
    run_main "${args[@]:1}"
    ;;
  grade)
    run_grade_model_cluster "${args[@]:1}"
    ;;
  *)
  usage
esac
