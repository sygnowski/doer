#!/bin/bash

#set -x

export ZOO_LOG4J_PROP="ERROR,ROLLINGFILE"
ZOO_CMD=/opt/zookeeper/apache-zookeeper-3.7.1-bin/bin/zkCli.sh

CLUSTER="doer-cluster-test"


zoo_ls () {
    ${ZOO_CMD} ls $1 2> /dev/null | grep -oP '(?<=\[).+?(?=\])' | tr "," " "
}

zoo_del() {
    ${ZOO_CMD} delete $1 > /dev/null
}


get_msg_list() {
    zoo_ls ${MSG_PATH}
}

for_each_delete() {
    local args=("$@")
    local INST=${args[0]}
    local TO_DELETE=${args[@]:1}

    if [[ -n "${TO_DELETE}" ]]; then
        for m in ${TO_DELETE}
        do
            echo "delete message: $m"
            zoo_del /${CLUSTER}/INSTANCES/${INST}/MESSAGES/$m
        done
    else
        echo "Instance $INST has no remaining messages."
    fi
}

main() {
    for inst in $( zoo_ls /${CLUSTER}/INSTANCES )
    do
        for_each_delete $inst $( zoo_ls /${CLUSTER}/INSTANCES/${inst}/MESSAGES )
    done
}

main $@
