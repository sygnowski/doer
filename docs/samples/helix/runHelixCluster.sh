#!/bin/bash

#set -e

args=("$@")

CLUSTER_NAME="doer-cluster-test"
RESOURCE="doer"


SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

export DOER_HOME=${SCRIPT_DIR}

if [[ "${ESOUT}" == "1" ]]; then
  echo "Using elasticsearch output."
  export DOER_OUTPUT="http://localhost:9200/helix/_doc"
fi

helix () {
  export DOER_CTX_NAME=d$!
  doer helix "$@"
}


run_main() {

  run_zk_if_needed
  rm -rf ${SCRIPT_DIR}/logs

  echo "Doer : Helix testing..."

  echo "env: $( env | grep -e DOER )"

  helix -t create -c ${CLUSTER_NAME} -r ${RESOURCE} --replicas 3

  helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctl&
  run_helix_spectator_if_needed

  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-1&
  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-2&
  helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-participant-3&

#  PRTC=(participant-{1..5})
#
#
#  for prtcName in ${PRTC[@]}; do
#    helix -t participant -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-$prtcName&
#  done

  TOKILL=$(pgrep -P $$ | tr "\n" " ")

  #trap 'kill $TOKILL' SIGINT SIGTERM EXIT

  echo "subprocs:  $TOKILL"

  read

  kill ${TOKILL}



  sleep 1

  helix -t delete -c ${CLUSTER_NAME}

}

run_zk_if_needed() {
  if [[ "${ZKON}" == "1" ]]; then
    echo "Running zookeeper server..."
    doer zoosrv --work-dir ./helix-zoo-data &

    sleep 1
  fi
}

run_helix_spectator_if_needed() {
  if [[ "${SPECT}" == "1" ]]; then
    helix -t spectator -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-spectator&
  fi
}

run_grade_model_cluster() {
  run_zk_if_needed

  CLUSTER_NAME="grade-model-demo"
  NODES=('A' 'B' 'C' 'D' 'E')

  echo "Num of nodes: ${#NODES[@]}"

  echo "Making cluster..."
  helix -c ${CLUSTER_NAME} --model ./grade-cluster.yaml

  sleep 1
  run_helix_spectator_if_needed

  helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctlA&
  helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctlB&

  nn=${#NODES[@]}
  nodeFct=$((10 * nn))
  idx=0

  for index in ${!NODES[@]}; do
    local partName=part-${NODES[$index]}
    gold=$((nodeFct - idx * 10))

    
    echo "Running $part with gold.start=$gold"
    helix -t participant -c ${CLUSTER_NAME} -n $partName -f gold.start=$gold &
    
    
    ((idx++))
    #echo "idx: $idx" 
    sleep 0.5
  done

  echo "nodes working...."

  # Cleanup....

  TOKILL=$(pgrep -P $$ | tr "\n" " ")

  #trap 'kill $TOKILL' SIGINT SIGTERM EXIT

  echo "subprocs:  $TOKILL"

  read

  helix -t delete -c ${CLUSTER_NAME}

  kill ${TOKILL}

  rm -rf ./helix-zoo-data/ ./logs/ 2> /dev/null
}

usage() {
    (lolcat || cat) << EOF 2> /dev/null
Usage:
main     -  Master Slave Model.
grade    -  Grade State Model.

misc:
ZKON=1   - local zookeeper
ESOUT=1  - log events into elasticsearch (DOER_OUTPUT="http://localhost:9200/helix/_doc")
SPECT=1  - run spectator with with 'all' listeners on

sample:
ZKON=1 ESOUT=1 SPECT=1 ./runHelixCluster.sh grade
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
