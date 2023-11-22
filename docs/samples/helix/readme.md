# Apache Helix Toolkit

Read more about Apache Helix [here](https://github.com/apache/helix).

Command Overview:
```
Usage: doer helix [--help] [-c=<clusterName>] [--model=<helixModel>]
                  [-n=<instanceName>] [--num-partition=<numPartitions>]
                  [-r=<resource>] [--rebalanceMode=<rebalanceMode>]
                  [--replicas=<replicas>] [-s=<server>]
                  [--stateModel=<stateModel>] [-t=<type>]
                  [-f=<String=String>]... [-sf=<String=String>]...
Helix Toolkit.
  -c=<clusterName>
  -f=<String=String>           Default: {}
      --help
      --model=<helixModel>   Helix State Model YAML definition file.
  -n=<instanceName>            Default: doer
      --num-partition=<numPartitions>
                               Default: 1
  -r=<resource>
      --rebalanceMode=<rebalanceMode>
                               Default: FULL_AUTO
      --replicas=<replicas>    Default: 1
  -s=<server>                  Default: localhost:2181
      -sf=<String=String>
      --stateModel=<stateModel>
                               Default: MasterSlave
  -t=<type>                    Default: spectator

```

## Commands:

 - Create Cluster
 ```bash
 doer helix -t create -c ${CLUSTER_NAME} -r ${RESOURCE} --replicas 3
 ```
 - Create Cluster from yaml model definition, [see](grade-cluster.yaml)
 ```bash
  doer helix -c ${CLUSTER_NAME} --model cluster-model.yaml
 ```
 - Connect the Controller instance
 ```bash
 doer helix -t controller -c ${CLUSTER_NAME} -n ${CLUSTER_NAME}-ctl
 ```

More examples [here](runHelixCluster.sh).
Usage:
```
main     -  Master Slave Model.
grade    -  Grade State Model.
msgs     -  Helix Messaging test.

misc:
ZKON=1   - local zookeeper
ESOUT=1  - log events into elasticsearch (DOER_OUTPUT="http://localhost:9200/helix/_doc")
SPECT=1  - run spectator with with 'all' listeners on

sample:
ZKON=1 ESOUT=1 SPECT=1 ./runHelixCluster.sh grade
```


### Notes

Source https://helix.apache.org/1.0.2-docs/tutorial_state.html


Dynamic State Constraints
We also support two dynamic upper bounds for the number of replicas in each state:
 - N: The number of replicas in the state is at most the number of live participants in the cluster
 - R: The number of replicas in the state is at most the specified replica count for the partition


```bash
doer helix -c doer-cluster-test -t rebalance -r doer --replicas 3

```